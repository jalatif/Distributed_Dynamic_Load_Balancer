package DLB;

import DLB.Utils.Message;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by manshu on 4/16/15.
 */
public class StateManagerThread extends Thread {

    private BlockingQueue<Message> messages;

    public StateManagerThread() {
        messages = new LinkedBlockingDeque<Message>();
    }

    protected void addMessage(Message msg) {
        messages.add(msg);
    }

    private void stateTransferWork() throws IOException, InterruptedException {
        Message incomingMsg = messages.take();
        switch (incomingMsg.getMsgType()) {
            default:
                MainThread.communicationThread.sendMessage(incomingMsg);
                break;
        }
    }

    @Override
    public void run() {
        while (!MainThread.STOP_SIGNAL) {
            try {
                stateTransferWork();
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
