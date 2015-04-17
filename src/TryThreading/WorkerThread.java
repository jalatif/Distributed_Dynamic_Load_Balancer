package TryThreading;

/**
 * Created by manshu on 4/15/15.
 */
public class WorkerThread extends Thread {
    public WorkerThread() {
        System.out.println("Worker Thread made");
    }

    @Override
    public void run() {
        while (true) {
            System.out.println("Worker thread doing sth");
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
