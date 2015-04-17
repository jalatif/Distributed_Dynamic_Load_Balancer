package TryThreading;

/**
 * Created by manshu on 4/15/15.
 */
public class MainThread {


    public static void main(String[] args) {

        WorkerThread workerThread = new WorkerThread();
        AdapterThread adapterObj = AdapterThread.getThreadObject(workerThread);
        adapterObj.setWorkerTime(7000, 3000);
        adapterObj.start();
//        Thread1 thread1 = new Thread1(adapterObj);
//        Thread2 thread2 = new Thread2(adapterObj);
//
//        thread1.start();
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        thread2.start();
    }
}
