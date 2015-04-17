package TryThreading;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by manshu on 4/15/15.
 */
public class AdapterThread extends Thread {
    private static AdapterThread adapterObj;
    private volatile int val;
    private volatile boolean stateChanged;
    private WorkerThread workerThread;
    private boolean workerRunning = false;
    private int workTime = 70, sleepTime = 30;
    private Timer workTimer, sleepTimer;

    private AdapterThread(WorkerThread workerThread) {
        System.out.println("Adapter Thread");
        val = 0;
        stateChanged = true;
        this.workerThread = workerThread;
        workerThread.start();
        workerRunning = true;
        workTimer = new Timer(true);
        sleepTimer = new Timer(true);
        setUpSleepTimer();
    }


    private void wakeUpWorker() {
        workerThread.resume();
        System.out.println("Thread waked up");
        setUpSleepTimer();
    }

    private void sleepWorker() {
        System.out.println("Worker 1 sleep");
        workerThread.suspend();
        setUpWorkTimer();
    }

    private void setUpWorkTimer() {
        sleepTimer.cancel();
        workTimer = new Timer();
        workTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                wakeUpWorker();
            }
        }, (long) (sleepTime));
    }

    private void setUpSleepTimer() {
        workTimer.cancel();
        sleepTimer = new Timer();
        sleepTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sleepWorker();
            }
        }, (long) (workTime));
    }

    public void setWorkerTime(int workTime, int sleepTime) {
        this.workTime = workTime;
        this.sleepTime = sleepTime;
        setUpSleepTimer();
    }

    public static AdapterThread getThreadObject(WorkerThread workerThread) {
        if (adapterObj == null) {
            adapterObj = new AdapterThread(workerThread);
        }
        return adapterObj;
    }

    public void setVal(int val) {
        this.val = val;
        stateChanged = true;
    }

    @Override
    public void run() {
        while (true) {
            if (stateChanged) {
                System.out.println("Adapter Working");
                System.out.println("Adapter val = " + val);
                stateChanged = false;
            }
            try {
                sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
