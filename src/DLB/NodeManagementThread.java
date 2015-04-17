package DLB;

/**
 * Created by manshu on 4/17/15.
 */
public class NodeManagementThread extends Thread {

    public NodeManagementThread() {

    }

    private void handleConnections() {

    }

    @Override
    public void run() {
        while (!MainThread.STOP_SIGNAL) {
            handleConnections();
        }
    }
}
