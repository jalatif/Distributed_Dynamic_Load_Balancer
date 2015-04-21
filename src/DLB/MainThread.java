package DLB;

import DLB.Utils.Job;
import DLB.Utils.Message;
import DLB.Utils.MessageType;
import org.hyperic.sigar.SigarException;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by manshu on 4/16/15.
 */
public class MainThread {
    protected static TransferManagerThread transferManagerThread;
    protected static StateManagerThread stateManagerThread;
    protected static AdapterThread adapterThread;
    protected static HWMonitorThread hwMonitorThread;
    protected static CommunicationThread communicationThread;

    protected volatile static boolean STOP_SIGNAL;

    protected static int numJobs = 1024;
    protected static int numWorkerThreads = 1;

    protected static int utilizationFactor = 5000;
    protected static int numElementsPrint = 10;
    protected static int collectionRate = 500; // in ms

    protected static int queueDifferenceThreshold = 20;
    protected static int cpuThresholdLimit = 50;

    protected static int numElements = 1024 * 1024 * 32;//1024 * 1024 * 32;
    protected static double initVal = 1.111111, addVal = 1.111111;
    protected static double[] vectorA;
    protected static double[] vectorB;

    protected static BlockingDeque<Job> jobQueue;

    protected static ServerSocket serverSocket;
    protected static Socket otherSocket;
    protected volatile static Socket mySocket;


    protected static int machineId = 0;

    // locks for 2 volatile variables.
    protected static volatile Lock jobInQueueLock = new ReentrantLock(true);
    protected static volatile Lock jobInComingLock = new ReentrantLock(true);

    protected static volatile boolean jobsInQueue = false;
    protected static volatile boolean jobsInComing = false;

    private static int elementsDone;

    protected static double throttlingValue = 0.3;
    protected static boolean isLocal = !true;

    public MainThread() throws IOException, SigarException {
        transferManagerThread = new TransferManagerThread();
        stateManagerThread = new StateManagerThread();
        hwMonitorThread = new HWMonitorThread();
        adapterThread = new AdapterThread();
        communicationThread = new CommunicationThread();
    }

    public void start() throws InterruptedException {
        STOP_SIGNAL = false;

        if (!isLocal) machineId = 1;

        jobQueue = new LinkedBlockingDeque<Job>();
        if (isLocal) {
            vectorA = new double[numElements];
            Arrays.fill(vectorA, initVal);

            vectorB = new double[numElements];
            elementsDone = 0;
        }
        adapterThread.start();
        transferManagerThread.start();
        stateManagerThread.start();
    }

    protected static synchronized void addToResult(Job job) {
        Double[] data = job.getData();
        for (int i = job.getStartIndex(); i < job.getEndIndex(); i++) {
            vectorB[i] = data[i - job.getStartIndex()];
        }
        elementsDone += data.length;
        if (elementsDone == numElements) {
            System.out.println("Finished Computing everything");
            for (int i = 0; i < Math.min(numElements, numElementsPrint); i++)
                System.out.print(vectorB[i] + " ");
            System.out.println("......");
            Message msg = new Message(machineId, MessageType.FinishACK, 0);
            try {
                communicationThread.sendMessage(msg);
            } catch (IOException ie) {
                stop();
            }
        }
    }

    public static void stop() {
        STOP_SIGNAL = true;
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            mySocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /////////////////////Test Output//////////////////////////
        if (isLocal) {
            System.out.println("Wait testing the output");
            for (int i = 0; i < vectorB.length; i++) {
                if (vectorB[i] != (vectorA[i] + addVal)) {
                    System.out.println("Resultant Output incorrect at " + i + " index with value = " + vectorB[i]);
                    System.exit(1);
                }
            }
            System.out.println("Resultant Output is all correct");
        }
        ///////////////////////////////////////////////////////
        System.exit(0);
    }

    public void connect(String ip, int port) throws IOException {
        if (!isLocal) {
            mySocket = new Socket(ip, port);
        } else {

            InetAddress locIP = InetAddress.getByName(ip);

            System.out.println(Inet4Address.getLocalHost().getHostAddress());

            serverSocket = new ServerSocket(port, 0, locIP);

            System.out.println("Listening on " + serverSocket);

            mySocket = serverSocket.accept();

            System.out.println("Connection from " + mySocket);
        }

        communicationThread.setUpStreams();

        communicationThread.start();

    }

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException, SigarException {

        MainThread mainThread = new MainThread();
        String ip = "localhost"; int port = 1234;
        //ip = "jalatif2.ddns.net";
        mainThread.connect(ip, port);

        communicationThread.sendMessage("Got connection from " + port);

        mainThread.start();

    }
}
