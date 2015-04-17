package DLB;

import DLB.Utils.Job;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by manshu on 4/16/15.
 */
public class MainThread {
    protected static WorkerThread workerThread;
    protected static TransferManagerThread transferManagerThread;
    protected static StateManagerThread stateManagerThread;
    protected static AdapterThread adapterThread;
    protected static HWMonitorThread hwMonitorThread;
    protected static CommunicationThread communicationThread;

    protected volatile static boolean STOP_SIGNAL;

    protected static int numJobs = 512;
    protected static int numElements = 1024 * 1024 * 32;
    protected static double initVal = 1.111111, addVal = 1.111111;
    protected static double[] vectorA;

    protected static BlockingDeque<Job<Double>> jobQueue;

    protected static ServerSocket serverSocket;
    protected static Socket otherSocket;
    protected static Socket mySocket;

    protected static boolean isLocal;

    public MainThread() throws IOException {
        workerThread = new WorkerThread();
        transferManagerThread = new TransferManagerThread();
        stateManagerThread = new StateManagerThread();
        hwMonitorThread = new HWMonitorThread();
        adapterThread = new AdapterThread();
        communicationThread = new CommunicationThread();
    }

    public void start() throws InterruptedException {
        STOP_SIGNAL = false;
        jobQueue = new LinkedBlockingDeque<Job<Double>>();
        vectorA = new double[numElements];
        Arrays.fill(vectorA, initVal);
//        hwMonitorThread.start();
//        transferManagerThread.start();
//        stateManagerThread.start();
//        workerThread.start();
        adapterThread.start();
    }

    public void stop() {
        STOP_SIGNAL = true;
    }

    public void connect(String ip, int port, String ip2, int port2) throws IOException {
        if (!isLocal)
            mySocket = new Socket(ip2, port2);

        InetAddress locIP = InetAddress.getByName(ip);

        System.out.println(Inet4Address.getLocalHost().getHostAddress());

        serverSocket = new ServerSocket(port, 0, locIP);

        System.out.println("Listening on " + serverSocket);

        otherSocket = serverSocket.accept();

        System.out.println("Connection from " + otherSocket);

        if (isLocal)
            mySocket = new Socket(ip2, port2);

        communicationThread.setUpStreams();

        communicationThread.start();
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        isLocal = false;

        MainThread mainThread = new MainThread();
        String ip = "localhost"; int port = 1234;
        String ip2 = "localhost"; int port2 = 5678;

        if (!isLocal) {
            int temp = port;
            port = port2;
            port2 = temp;
        }

        mainThread.connect(ip, port, ip2, port2);

        communicationThread.sendMessage("I am at port " + port);
        System.out.println(communicationThread.receiveMessage());

        mainThread.start();

        try {
            Thread.sleep(50000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mainThread.stop();
    }
}
