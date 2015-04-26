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
        MainThread.communicationThread[MainThread.TRANSFER_TYPE.DATA.ordinal()].sendMessage(message);

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
        MainThread.communicationThread[MainThread.TRANSFER_TYPE.DATA.ordinal()].sendMessage(message);
        System.out.println("Message sent");
    }

    private void sendRequiredJob(Message incomingMsg) throws IOException {
        Integer queueToSend = (Integer) incomingMsg.getData();
        System.out.println("Sending " + queueToSend + " jobs to other node ");
        //StringBuilder stringBuilder = new StringBuilder();


        Message message = new Message(MainThread.machineId, MessageType.JOBHEADER, queueToSend);
        MainThread.communicationThread[MainThread.TRANSFER_TYPE.DATA.ordinal()].sendMessage(message);

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
            MainThread.communicationThread[MainThread.TRANSFER_TYPE.DATA.ordinal()].sendMessage(message);
        }
        message = new Message(MainThread.machineId, MessageType.JOBFOOTER, queueToSend);
        MainThread.communicationThread[MainThread.TRANSFER_TYPE.DATA.ordinal()].sendMessage(message);

        System.out.println("Message sent");
    }


    private void sendResult(Message incomingMsg) throws IOException {
        System.out.println("Sending resultant job back to the local Node");
        MainThread.communicationThread[MainThread.TRANSFER_TYPE.RESULT.ordinal()].sendMessage(incomingMsg);
        System.out.println("Message sent");
    }

    private void sendResults() throws IOException {
        int num_elements = MainThread.resultantJobQueue.size();
        System.out.println("Sending all results back to local node");
        for (int i = 0; i < num_elements; i++) {
            Job job = MainThread.resultantJobQueue.poll();
            if (job == null) {
                System.out.println("Not sufficient amount of result jobs to send back");
                break;
            }
            Message msg = new Message(MainThread.machineId, MessageType.JOBRESULT, job);
            addMessage(msg);
        }
        Message okMsg = new Message(MainThread.machineId, MessageType.OkACK, 0);
        addMessage(okMsg);
//        Message msg = new Message(MainThread.machineId, MessageType.FinishACK, 0);
//        addMessage(msg);
        System.out.println("OkAck message sent");
    }

    private void transferWork() throws IOException, InterruptedException {
        Message incomingMsg = messages.take();
        switch (incomingMsg.getMsgType()) {
            case BULKJOBTRANSFER:
                synchronized (MainThread.jobInQueueLock) {
                    MainThread.jobsInQueue = true;
                }
                if (MainThread.isLocal) {
                    MainThread.dynamicBalancerUI.changeTransferStatus(MainThread.machineId, true);
                }
                sendRequiredJobs(incomingMsg);
                break;
            case JOBTRANSFER:
                synchronized (MainThread.jobInQueueLock) {
                    MainThread.jobsInQueue = true;
                }
                if (MainThread.isLocal) {
                    MainThread.dynamicBalancerUI.changeTransferStatus(MainThread.machineId, true);
                }
                sendRequiredJob(incomingMsg);
                break;
            case JOBTRANSFERACK:
                MainThread.communicationThread[MainThread.TRANSFER_TYPE.DATA.ordinal()].sendMessage(incomingMsg);
                break;
            case JOBRESULT:
                sendResult(incomingMsg);
                break;
            case BULKJOBRESULT:
                sendResults();
                break;
            case TVALUE:
                MainThread.communicationThread[MainThread.TRANSFER_TYPE.STATE.ordinal()].sendMessage(incomingMsg);
                break;
            case UITVALUE:
                MainThread.communicationThread[MainThread.TRANSFER_TYPE.STATE.ordinal()].sendMessage(incomingMsg);
                break;
            case OkACK:
                MainThread.communicationThread[MainThread.TRANSFER_TYPE.DATA.ordinal()].sendMessage(incomingMsg);
                break;
            case REQUESTJOBS:
                MainThread.communicationThread[MainThread.TRANSFER_TYPE.RESULT.ordinal()].sendMessage(incomingMsg);
                break;
            default:
                MainThread.communicationThread[MainThread.TRANSFER_TYPE.DATA.ordinal()].sendMessage(incomingMsg);
                break;
        }
    }

    public static void addJobs(int machineId, List<Job> jobs) throws IOException {
        //System.out.println("There are some incoming Jobs to my node "); // ca
        //System.out.println("Current jobs are " + MainThread.jobQueue.size());
        for (Job job : jobs) {
            // add to job queue
            MainThread.jobQueue.addFirst(job);

            // print jobs info ================================================
            //System.out.println("Job Data ----->");
            //System.out.println(job.getStartIndex() + " " + job.getEndIndex());
//            Double[] data = job.getData();
//            for (Double element : data) {
//                System.out.print(element + " ");
//            }
//            System.out.println();
            //=================================================================
        }
        synchronized (MainThread.jobInComingLock) {
            MainThread.jobsInComing = false;
        }
        if (MainThread.isLocal) {
            MainThread.dynamicBalancerUI.changeTransferStatus(machineId, false);
        }
        System.out.println("Now jobs are " + MainThread.jobQueue.size());
        Message message = new Message(MainThread.machineId, MessageType.JOBTRANSFERACK, 0);
        MainThread.communicationThread[MainThread.TRANSFER_TYPE.DATA.ordinal()].sendMessage(message);

    }

    public static synchronized void addJob(int machineId, Job job) {
        MainThread.jobQueue.addFirst(job);
        //System.out.println("Job Data ----->");
        //System.out.println(job.getStartIndex() + " " + job.getEndIndex());
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
