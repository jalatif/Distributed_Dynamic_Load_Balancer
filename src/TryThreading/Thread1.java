package TryThreading;

/**
 * Created by manshu on 4/15/15.
 */
public class Thread1 extends Thread {
    private AdapterThread adapterThread;
    private int val;
    public Thread1(Thread obj) {
        System.out.println("Thread 1 ");
        adapterThread = (AdapterThread) obj;
        val = 1;
    }

    @Override
    public void run() {
        while (true) {
            System.out.println("Thread1 Working");
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
