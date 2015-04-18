package DLB;

import DLB.Utils.Job;
import DLB.Utils.Message;
import DLB.Utils.MessageType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by manshu on 4/16/15.
 */
public class TransferManagerThread extends Thread {

    private BlockingQueue<Message> messages;

    public TransferManagerThread() {
        messages = new LinkedBlockingDeque<Message>();
    }

    protected synchronized void addMessage(Message msg) {
        messages.add(msg);
    }

    private void sendRequiredJobs(Message incomingMsg) throws IOException {
        Integer queueToSend = (Integer) incomingMsg.getData();
        System.out.println("Sending " + queueToSend + " jobs to other node ");
        //StringBuilder stringBuilder = new StringBuilder();
        List<Job> jobsToSend = new LinkedList<Job>();
        for (int i = 0; i < queueToSend; i++) {
            //stringBuilder.append(MainThread.jobQueue.pollFirst()).append("|");
            jobsToSend.add(MainThread.jobQueue.pollFirst());
        }
        System.out.println("Sending message to other node");
        Message message = new Message(MessageType.JOBTRANSFER, jobsToSend);
        MainThread.communicationThread.sendMessage(message);
        System.out.println("Message sent");
    }

    private void sendResults(Message incomingMsg) throws IOException {
        System.out.println("Sending resultant job back to the local Node");
        MainThread.communicationThread.sendMessage(incomingMsg);
        System.out.println("Message sent");
    }

    private void transferWork() throws IOException, InterruptedException {
        Message incomingMsg = messages.take();
        switch (incomingMsg.getMsgType()) {
            case JOBTRANSFER:
                sendRequiredJobs(incomingMsg);
                break;
            case JOBRESULT:
                sendResults(incomingMsg);
                break;
            default:
                return;
        }
    }

    public static void addJobs(List<Job> jobs) {
        System.out.println("There are some incoming Jobs to my node "); // ca
        System.out.println("Current jobs are " + MainThread.jobQueue.size());
        for (Job job : jobs) {
            // add to job queue
            MainThread.jobQueue.addFirst(job);

            // print jobs info ================================================
            System.out.println("Job Data ----->");
            System.out.println(job.getStartIndex() + " " + job.getEndIndex());
            Double[] data = job.getData();
            for (Double element : data) {
                System.out.print(element + " ");
            }
            System.out.println();
            //=================================================================
        }
        System.out.println("Now jobs are " + MainThread.jobQueue.size());
    }

    @Override
    public void run() {
        while (!MainThread.STOP_SIGNAL) {
            try {
                transferWork();
            } catch (InterruptedException e) {
                e.printStackTrace();
                MainThread.stop();
            } catch (IOException e) {
                e.printStackTrace();
                MainThread.stop();
            }
        }
    }
}
