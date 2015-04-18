package DLB;

import DLB.Utils.Job;
import DLB.Utils.Message;
import DLB.Utils.MessageType;

import java.io.*;
import java.util.List;

/**
 * Created by manshu on 4/16/15.
 */
public class CommunicationThread extends Thread {
    private ObjectOutputStream dout;
    private ObjectInputStream din;


    public CommunicationThread() throws IOException {
        dout = null;
        din = null;
    }

    public void setUpStreams() throws IOException {
        dout = new ObjectOutputStream(MainThread.otherSocket.getOutputStream());
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
        System.out.println("Message = " + inString.substring(0, Math.min(inString.length(), 80)));
        System.out.println(incomingMsg.getClass());

        if (incomingMsg instanceof Message) {
            try {
                Message msg = (Message) incomingMsg;
                switch (msg.getMsgType()) {
                    case JOBTRANSFER:
                        List<Job> list = (List<Job>) msg.getData();
                        TransferManagerThread.addJobs(list);
                        break;
                    case JOBRESULT:
                        System.out.println("Got result from remote node");
                        Job resultJob = (Job) msg.getData();
                        MainThread.addToResult(resultJob);
                        break;
                    case FinishACK:
                        System.out.println("Got finished ack");
                        Message okMsg = new Message(MessageType.OkACK, 0);
                        sendMessage(okMsg);
                        System.out.println("OkAck message sent");
                        MainThread.stop();
                        break;
                    case OkACK:
                        System.out.println("Got Ok ack");
                        MainThread.stop();
                        break;
                    case HW:
                        System.out.println("Got HW State");
                        MainThread.adapterThread.addMessage(msg);
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
    }
}
