package DLB;

import DLB.Utils.Message;
import DLB.Utils.MessageType;
import DLB.Utils.StateInfo;

import java.io.IOException;

/**
 * Created by manshu on 4/16/15.
 */
public class HWMonitorThread extends Thread {

    public HWMonitorThread() {

    }

    private void doMonitoring() throws IOException {
        StateInfo state = new StateInfo(MainThread.jobQueue.size(), 0.5, 0.5);
        Message msg = new Message(MessageType.HW, state);
        MainThread.adapterThread.addMessage(msg);
        MainThread.communicationThread.sendMessage(msg);
    }

    @Override
    public void run() {
        while (!MainThread.STOP_SIGNAL) {
            try {
                doMonitoring();
                sleep(MainThread.collectionRate);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException ie) {
                ie.printStackTrace();
                System.out.println("Cannot continue w/o connection");
                MainThread.stop();
            }
        }
    }
}
