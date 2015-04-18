package DLB;

import DLB.Utils.Job;
import DLB.Utils.Message;
import DLB.Utils.MessageType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by manshu on 4/16/15.
 */
public class AdapterThread extends Thread {
    private static volatile double throttlingValue = 0.5;

    private WorkerThread[] workerThreads;

    private BlockingQueue<Message> messages;

    public AdapterThread() {
        messages = new LinkedBlockingDeque<Message>();
    }

    public static void setThrottlingValue(double tValue) {
        throttlingValue = tValue;
    }

    private void adapterWork() throws InterruptedException {
        Message incomingMsg = messages.take();
        System.out.println("Adapter Thread Working with Message " + incomingMsg);
        // sleep switch worker
        // queue check - call function accordingly
    }

    protected synchronized void addMessage(Message msg) {
        messages.add(msg);
    }

    private void bootstrapJobs() {
        int jobItems = MainThread.numElements / MainThread.numJobs;
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < MainThread.numElements; i += jobItems) {
            //List<Double> list = new ArrayList<Double>(jobItems); SLOW
            Double[] arr = new Double[jobItems];
            for (int j = i; j < i + jobItems; j++) {
                //list.add(MainThread.vectorA[j]);
                arr[j - i] = MainThread.vectorA[j];
            }
            Job job = new Job(i, i + jobItems, arr);
            MainThread.jobQueue.add(job);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("First Job = " + MainThread.jobQueue.getFirst());
        System.out.println("Time taken = " + (t2 - t1));
        Message msg = new Message(MessageType.JOBTRANSFER, MainThread.numJobs / 2);
        MainThread.transferManagerThread.addMessage(msg);
    }

    private void startWorkers() {
        workerThreads = new WorkerThread[MainThread.numWorkerThreads];
        for (int i = 0; i < workerThreads.length; i++) {
            workerThreads[i] = new WorkerThread();
            workerThreads[i].start();
        }
    }

    @Override
    public void run() {
        if (MainThread.isLocal)
            bootstrapJobs();
        startWorkers();
        while (!MainThread.STOP_SIGNAL) {
            try {
                adapterWork();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                MainThread.stop();
            }
        }
    }
}
