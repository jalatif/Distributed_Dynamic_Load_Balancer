package DLB;

import DLB.Utils.Message;
import DLB.Utils.MessageType;

import java.io.IOException;
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

    private void transferWork() throws IOException, InterruptedException {
        Message incomingMsg = messages.take();
        if (incomingMsg.getMsgType() != MessageType.JOBTRANSFER) return;
        Integer queueToSend = (Integer) incomingMsg.getData();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < queueToSend; i++) {
            stringBuilder.append(MainThread.jobQueue.pollFirst()).append("|");
        }
        MainThread.communicationThread.sendMessage(stringBuilder.toString());
    }

    @Override
    public void run() {
        while (!MainThread.STOP_SIGNAL) {
            try {
                transferWork();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
