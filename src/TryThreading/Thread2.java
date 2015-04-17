package TryThreading;

/**
 * Created by manshu on 4/15/15.
 */
public class Thread2 extends Thread {
    private AdapterThread adapterThread;
    private int val;

    public Thread2(Thread obj) {
        System.out.println("Thread 2 ");
        adapterThread = (AdapterThread) obj;
        val = 2;
    }

    @Override
    public void run() {
        while (true) {
            System.out.println("Thread2 Working");
            adapterThread.setVal(val);
            val += 2;
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
