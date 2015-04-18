package DLB;

import DLB.Utils.Job;
import DLB.Utils.Message;

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

    public void sendMessage(Object message) throws IOException {
        if (dout == null) return;
        //dout.writeUTF(message);
        dout.writeObject(message);
    }

    public Object receiveMessage() throws IOException, ClassNotFoundException {
        if (din == null) return "";
        Object incomingMsg = din.readObject();

        System.out.println("Message = " + incomingMsg.toString());
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
            }
        }
    }
}
