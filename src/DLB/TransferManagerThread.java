package DLB;

import DLB.Utils.Job;
import DLB.Utils.Message;
import DLB.Utils.MessageType;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
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

    protected void addMessage(Message msg) {
        messages.add(msg);
    }

    private void sendRequiredJobs(Message incomingMsg) throws IOException {
        Integer queueToSend = (Integer) incomingMsg.getData();
        queueToSend = Math.min(MainThread.jobQueue.size(),
                queueToSend);

        System.out.println("Sending " + queueToSend + " jobs to other node ");

        Message message = new Message(MainThread.machineId, MessageType.JOBHEADER, queueToSend);
        MainThread.communicationThread.sendMessage(message);

        //StringBuilder stringBuilder = new StringBuilder();
        List<Job> jobsToSend = new LinkedList<Job>();
        for (int i = 0; i < queueToSend; i++) {
            //stringBuilder.append(MainThread.jobQueue.pollFirst()).append("|");
            Job job = MainThread.jobQueue.pollFirst();
            if (job == null) {
                System.out.println("Data insufficient to send. Sending only " + (i + 1) +
                        " jobs instead of " + queueToSend);
                if (i == 0) {
                    System.out.println("No data to send. Returning....");
                    return;
                }
                break;
            }
            jobsToSend.add(job);
        }

        System.out.println("Sending message to other node");
        message = new Message(MainThread.machineId, MessageType.BULKJOBTRANSFER, jobsToSend);
        MainThread.communicationThread.sendMessage(message);
        System.out.println("Message sent");
    }

    private void sendRequiredJob(Message incomingMsg) throws IOException {
        Integer queueToSend = (Integer) incomingMsg.getData();
        System.out.println("Sending " + queueToSend + " jobs to other node ");
        //StringBuilder stringBuilder = new StringBuilder();

        System.out.println("Sending message to other node");

        Message message = new Message(MainThread.machineId, MessageType.JOBHEADER, queueToSend);
        MainThread.communicationThread.sendMessage(message);

        for (int i = 0; i < queueToSend; i++) {
            //stringBuilder.append(MainThread.jobQueue.pollFirst()).append("|");
            Job job = MainThread.jobQueue.pollFirst();
            if (job == null) {
                System.out.println("Data insufficient to send. Sending only " + (i + 1) +
                        " jobs instead of " + queueToSend);
                if (i == 0) {
                    System.out.println("No data to send. Returning....");
                    return;
                }
                break;
            }
            message = new Message(MainThread.machineId, MessageType.JOBTRANSFER, job);
            MainThread.communicationThread.sendMessage(message);
        }
        message = new Message(MainThread.machineId, MessageType.JOBFOOTER, queueToSend);
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
            case BULKJOBTRANSFER:
                MainThread.jobsInQueue = true;
                sendRequiredJobs(incomingMsg);
                break;
            case JOBTRANSFER:
                MainThread.jobsInQueue = true;
                sendRequiredJob(incomingMsg);
                break;
            case JOBRESULT:
                sendResults(incomingMsg);
                break;
            default:
                MainThread.communicationThread.sendMessage(incomingMsg);
                break;
        }
    }

    public static void addJobs(List<Job> jobs) throws IOException {
        System.out.println("There are some incoming Jobs to my node "); // ca
        System.out.println("Current jobs are " + MainThread.jobQueue.size());
        for (Job job : jobs) {
            // add to job queue
            MainThread.jobQueue.addFirst(job);

            // print jobs info ================================================
            System.out.println("Job Data ----->");
            System.out.println(job.getStartIndex() + " " + job.getEndIndex());
//            Double[] data = job.getData();
//            for (Double element : data) {
//                System.out.print(element + " ");
//            }
//            System.out.println();
            //=================================================================
        }
        MainThread.jobsInComing = false;
        System.out.println("Now jobs are " + MainThread.jobQueue.size());
        Message message = new Message(MainThread.machineId, MessageType.JOBTRANSFERACK, 0);
        MainThread.communicationThread.sendMessage(message);

    }

    public static synchronized void addJob(Job job) {
        MainThread.jobQueue.addFirst(job);
        System.out.println("Job Data ----->");
        System.out.println(job.getStartIndex() + " " + job.getEndIndex());
    }

    @Override
    public void run() {
        while (!MainThread.STOP_SIGNAL) {
            try {
                transferWork();
            } catch (InterruptedException e) {
                //e.printStackTrace();
                System.out.println("Cannot continue w/o connection");
                MainThread.stop();
            } catch (IOException e) {
                //e.printStackTrace();
                System.out.println("Cannot continue w/o connection");
                MainThread.stop();
            }
        }
    }

}
