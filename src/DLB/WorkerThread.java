package DLB;

import DLB.Utils.Job;
import DLB.Utils.Message;
import DLB.Utils.MessageType;

/**
 * Created by manshu on 4/16/15.
 */
public class WorkerThread extends Thread {

    public WorkerThread() {

    }

    private Job getResult(Job job) {
        Double[] data = job.getData();
        for (int i = 0; i < data.length; i++) {
            data[i] = data[i] + MainThread.addVal;
        }
        return new Job(job.getStartIndex(), job.getEndIndex(), data);
    }

    private void doWork() throws InterruptedException {
        Job job = MainThread.jobQueue.take();
        Job resultJob = getResult(job);

        // if local call result function otherwise send result to the local node
        if (MainThread.isLocal) {
            System.out.println("Worker calculated the result on local");
            MainThread.addToResult(resultJob);
        } else {
            System.out.println("Worker sending message");
            Message msg = new Message(MessageType.JOBRESULT, resultJob);
            MainThread.transferManagerThread.addMessage(msg);
        }
    }

    @Override
    public void run() {
        while (!MainThread.STOP_SIGNAL) {
            try {
                doWork();
                sleep(500);
            } catch (InterruptedException e) {
            }
        }
    }
}
