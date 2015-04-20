package DLB;

import DLB.Utils.Job;
import DLB.Utils.Message;
import DLB.Utils.MessageType;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by manshu on 4/16/15.
 */
public class WorkerThread extends Thread {
    private int index;
    private double throttlingValue;
    private int workTime, sleepTime;
    private volatile boolean threadSuspended;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> workHandle, sleepHandle;
    private Job currentJob;

    public WorkerThread(int index, double tValue) {
        this.index = index;
        throttlingValue = tValue;
        threadSuspended = false;
        workTime = (int) (throttlingValue * MainThread.utilizationFactor);
        sleepTime = MainThread.utilizationFactor - workTime;
        scheduler = Executors.newScheduledThreadPool(1);
        workHandle = null;
        sleepHandle = null;
        setUpSleepTimer();
    }

    private Job getResult(Job job) {
        Double[] data = job.getData();
        for (int i = 0; i < data.length; i++) {
            data[i] = data[i] + MainThread.addVal;
        }
        return new Job(job.getStartIndex(), job.getEndIndex(), data);
    }

    protected void changeThrottleValue(double tValue) {
        throttlingValue = tValue;
        workTime = (int) throttlingValue * MainThread.utilizationFactor;
        sleepTime = MainThread.utilizationFactor - workTime;
        setUpSleepTimer();
    }

    private synchronized void wakeUpWorker() {
        threadSuspended = false;
        notify();

        //System.out.println("Worker thread " + index + " woke");
        setUpSleepTimer();
    }

    private synchronized void sleepWorker() {
        //System.out.println("Worker thread " + index + " sleep");
        threadSuspended = true;
        setUpWorkTimer();
    }

    private void setUpWorkTimer() {
        if (sleepHandle != null) {
            if (!sleepHandle.isDone() || !sleepHandle.isCancelled())
                sleepHandle.cancel(true);
        }
        if (workHandle != null) {
            if (!workHandle.isDone() || !workHandle.isCancelled())
                workHandle.cancel(true);
        }

        workHandle = scheduler.schedule(new Runnable() {
                                            @Override
                                            public void run() {
                                                wakeUpWorker();
                                            }
                                        }, sleepTime, TimeUnit.MILLISECONDS);
    }

    private void setUpSleepTimer() {
        if (workHandle != null) {
            if (!workHandle.isDone() || !workHandle.isCancelled())
                workHandle.cancel(true);
        }
        if (sleepHandle != null) {
            if (!sleepHandle.isDone() || !sleepHandle.isCancelled())
                sleepHandle.cancel(true);
        }

        sleepHandle = scheduler.schedule(new Runnable() {
                                             @Override
                                             public void run() {
                                                 sleepWorker();
                                             }
                                         }, workTime, TimeUnit.MILLISECONDS);
    }


    private void doWork() throws InterruptedException {
        Job job;
        if (currentJob != null)
            job = currentJob;
        else
            job = MainThread.jobQueue.take();//poll((long) 0.1 * MainThread.utilizationFactor, TimeUnit.MILLISECONDS);

        currentJob = job;
        //if (job == null) return;
        Job resultJob = getResult(job);

        // if local call result function otherwise send result to the local node
        if (MainThread.isLocal) {
            System.out.println("Worker thread " + index + " calculated the result on local");
            MainThread.addToResult(resultJob);
        } else {
            System.out.println("Worker thread " + index + " sending message");
            Message msg = new Message(MainThread.machineId, MessageType.JOBRESULT, resultJob);
            MainThread.transferManagerThread.addMessage(msg);
        }
        currentJob = null;
    }

//
//    public void stopWorker() {
//        Thread currentThread = waiter;
//        waiter = null;
//        moribund.interrupt();
//    }

    @Override
    public void run() {
        while (!MainThread.STOP_SIGNAL) {
            try {
                doWork();
                if (threadSuspended) {
                    synchronized (this) {
                        while (threadSuspended)
                            wait();
                    }
                }
            } catch (InterruptedException e) {}
        }
    }
}
