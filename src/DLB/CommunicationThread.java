package DLB;

import DLB.Utils.Job;
import DLB.Utils.Message;
import DLB.Utils.MessageType;
import DLB.Utils.StateInfo;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by manshu on 4/16/15.
 */
public class CommunicationThread extends Thread {
    private ObjectOutputStream dout;
    private ObjectInputStream din;
    private GZIPOutputStream gzout;
    private GZIPInputStream gzin;

    public CommunicationThread() throws IOException {
        dout = null;
        din = null;
        gzout = null;
        gzin = null;
    }

    public void setUpStreams() throws IOException {
//        gzout = new GZIPOutputStream(MainThread.mySocket.getOutputStream());
//        gzin = new GZIPInputStream(MainThread.mySocket.getInputStream());

        dout = new ObjectOutputStream(MainThread.mySocket.getOutputStream());
        din  = new ObjectInputStream(MainThread.mySocket.getInputStream());
    }

    public synchronized void sendMessage(Object message) throws IOException {
        if (dout == null) return;
        //dout.writeUTF(message);
        dout.reset();
        dout.writeObject(message);
        dout.flush();
    }

    public Object receiveMessage() throws IOException, ClassNotFoundException {
        if (din == null) return "";
        Object incomingMsg = null;
        try {
             incomingMsg = din.readObject();
        }catch (ArrayStoreException ase) {
            return null;
        }
        String inString = incomingMsg.toString();
        //System.out.println("Message = " + inString.substring(0, Math.min(inString.length(), 80)));
        //System.out.println(incomingMsg.getClass());

        if (incomingMsg instanceof Message) {
            try {
                Message msg = (Message) incomingMsg;
                if (msg.getMsgType() != MessageType.JOBTRANSFER)
                    System.out.println("Message = " + inString.substring(0, Math.min(inString.length(), 80)));
                switch (msg.getMsgType()) {
                    case BULKJOBTRANSFER:
                        List<Job> jobs = (List<Job>) msg.getData();
                        TransferManagerThread.addJobs(jobs);
                        break;

                    case JOBHEADER:
                        synchronized (MainThread.jobInComingLock) {
                            MainThread.jobsInComing = true;
                        }
                        System.out.println("Number of jobs that are coming starting are " + (int) msg.getData());
                        System.out.println("Current jobs are " + MainThread.jobQueue.size());
                        break;

                    case JOBTRANSFER:
                        Job job = (Job) msg.getData();
                        TransferManagerThread.addJob(job);
                        break;

                    case JOBFOOTER:
                        synchronized (MainThread.jobInComingLock) {
                            MainThread.jobsInComing = false;
                        }
                        System.out.println("Jobs have been completely transferred");
                        System.out.println("Current jobs are " + MainThread.jobQueue.size());
                        msg = new Message(MainThread.machineId, MessageType.JOBTRANSFERACK, msg.getData());
                        MainThread.transferManagerThread.addMessage(msg); //sendMessage(msg);
                        break;

                    case JOBTRANSFERACK:
                        synchronized (MainThread.jobInQueueLock) {
                            MainThread.jobsInQueue = false;
                        }
                        synchronized (MainThread.jobInComingLock) {
                            MainThread.jobsInComing = false;
                        }
                        System.out.println("Jobs successfully transferred to other node");
                        break;

                    case JOBRESULT:
                        System.out.println("Got result from remote node");
                        Job resultJob = (Job) msg.getData();
                        MainThread.addToResult(resultJob);
                        break;

                    case FinishACK:
                        System.out.println("Got finished ack");
                        Message okMsg = new Message(MainThread.machineId, MessageType.OkACK, 0);
                        MainThread.transferManagerThread.addMessage(msg); //sendMessage(okMsg);
                        System.out.println("OkAck message sent");
                        MainThread.stop();
                        break;

                    case OkACK:
                        System.out.println("Got Ok ack");
                        MainThread.stop();
                        break;
                    case HW:
                        System.out.println("Got HW State");
                        if (MainThread.isLocal) {
                            MainThread.dynamicBalancerUI.addMessage(new Message(msg.getMachineId(), MessageType.SM,
                                    msg.getData()));
                        }
                        MainThread.adapterThread.addMessage(msg);
                        break;

                    case TVALUE:
                        MainThread.adapterThread.setThrottlingValue((double) msg.getData());
                        break;

                    case UITVALUE:
                        MainThread.dynamicBalancerUI.addMessage(msg);
                        break;

                    default:
                        System.out.println("Unknown message");
                }
                //Class<?> theClass = Class.forName("DLB.Utils.Message");
                //(Message) theClass.cast(message);

            } catch (ClassCastException e) {
                e.printStackTrace();
                //System.out.println(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return incomingMsg;
    }


    @Override
    public void run() {
        while (!MainThread.STOP_SIGNAL) {
            try {
                receiveMessage();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Cannot continue w/o connection");
                MainThread.stop();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.out.println("Cannot continue w/o connection");
                MainThread.stop();
            }
        }
        try {
            dout.close();
            din.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
