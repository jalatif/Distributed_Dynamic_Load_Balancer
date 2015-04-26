package DLB;

import DLB.Utils.Job;
import DLB.Utils.Message;
import DLB.Utils.MessageType;
import DLB.Utils.SystemStat;

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
    protected static double timePerJob ;
    protected static int numIterations = 1000;

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
        //long t1 = System.currentTimeMillis();
        Double[] data = job.getData();
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < numIterations; j++) {
                data[i] = data[i] + MainThread.addVal;
            }
        }
//        long t2 = System.currentTimeMillis();
//        if ((long) timePerJob == 0){
//            WorkerThread.timePerJob = (double) (t2 - t1);
//        }
        return new Job(job.getStartIndex(), job.getEndIndex(), data);
    }

    protected void changeThrottleValue(double tValue) {
        throttlingValue = tValue;
        workTime = (int) throttlingValue * MainThread.utilizationFactor;
        sleepTime = MainThread.utilizationFactor - workTime;
        if (sleepHandle != null) {
            if (!sleepHandle.isDone() || !sleepHandle.isCancelled())
                sleepHandle.cancel(true);
        }
        if (workHandle != null) {
            if (!workHandle.isDone() || !workHandle.isCancelled())
                workHandle.cancel(true);
        }
        setUpSleepTimer();
    }

    private synchronized void wakeUpWorker() {
        threadSuspended = false;
        notify();

        System.out.println("Worker thread " + index + " woke");
        System.out.println("Var = " + MainThread.jobsInQueue + " " + MainThread.jobsInComing);
        setUpSleepTimer();
    }

    private synchronized void sleepWorker() {
        System.out.println("Worker thread " + index + " sleep");
        System.out.println("Var = " + MainThread.jobsInQueue + " " + MainThread.jobsInComing);
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
        synchronized (MainThread.jobInQueueLock) {
            if (MainThread.jobsInQueue) return;
        }
        synchronized (MainThread.jobInComingLock) {
            if (MainThread.jobsInComing) return;
        }

        Job job;
        if (currentJob != null)
            job = currentJob;
        else
            job = MainThread.jobQueue.poll((long) 0.01 * MainThread.utilizationFactor, TimeUnit.MILLISECONDS);
        if (job == null) return;

        currentJob = job;
        Job resultJob = getResult(job);

        // if local call result function otherwise send result to the local node
        if (MainThread.isLocal) {
            System.out.println("Worker thread " + index + " calculated the result on local");
            MainThread.addToResult(resultJob);
        } else {
            //System.out.println("Worker thread " + index + " sending message");
//            Message msg = new Message(MainThread.machineId, MessageType.JOBRESULT, resultJob);
//            MainThread.transferManagerThread.addMessage(msg);
            MainThread.resultantJobQueue.add(resultJob);
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
        while (!MainThread.STOP_SIGNAL && !MainThread.processingDone) {
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
