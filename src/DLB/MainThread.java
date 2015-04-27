package DLB;

import DLB.Utils.Job;
import DLB.Utils.Message;
import DLB.Utils.MessageType;
import org.hyperic.sigar.SigarException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by manshu on 4/16/15.
 */
public class MainThread {
    protected static TransferManagerThread transferManagerThread;
    protected static StateManagerThread stateManagerThread;
    protected static AdapterThread adapterThread;
    protected static HWMonitorThread hwMonitorThread;
    protected static CommunicationThread[] communicationThread;
    protected static DynamicBalancerUI dynamicBalancerUI;

    protected volatile static boolean STOP_SIGNAL;
    protected volatile static double GUARD;

    protected static int numJobs = 2048;
    protected static int numWorkerThreads = 1;

    protected static int utilizationFactor = 100;
    protected static int numElementsPrint = 10;
    protected static int collectionRate = 5; // in ms

    protected static int queueDifferenceThreshold = 10;
    protected static int cpuThresholdLimit = 10;

    protected static int numElements = 1024 * 1024 * 2;//1024 * 1024 * 32;
    protected static int elementsPerJob = (numElements / numJobs);

    protected static double initVal = 1.111111, addVal = 1.111111;
    protected static double[] vectorA;
    protected static double[] vectorB;

    protected static BlockingDeque<Job> jobQueue;
    protected static BlockingDeque<Job> resultantJobQueue;

    protected static ServerSocket serverSocket[];

    protected volatile static Socket mySocket[];

    protected volatile static int transferFlag;
    protected volatile static double timePerJob;
    protected static double compressed = 0;
    protected static String resourcePath = "";

    protected static int machineId = 0;

    // locks for 2 volatile variables.
    protected static volatile Object jobInQueueLock = new Object();
    protected static volatile Object jobInComingLock = new Object();


    protected static volatile boolean jobsInQueue = false;
    protected static volatile boolean jobsInComing = false;

    protected static int elementsDone;
    protected static int localJobsDone;
    protected static int remoteJobsDone;
    protected static boolean processingDone;
    protected static int resultTransferred;
    protected static int finalRemoteJobs;
    protected static int balanceTransferred;
    protected static boolean USE_UI = false;

    protected static String ip = "localhost";//"jalatif2.ddns.net"; //"localhost";
    protected static int[] port = {2211, 2212, 2213};

    protected static enum TRANSFER_TYPE {
        DATA,
        STATE,
        RESULT
    }
    protected static enum TRANSFER_MODEL {
        SENDER_INIT,
        RECEIVER_INIT,
        SYMMETRIC
    }

    protected static double throttlingValue = 0.01;
    protected static boolean isLocal = true;
    protected static int numJobIteration = 2000;
    protected static TRANSFER_MODEL tModel = TRANSFER_MODEL.SENDER_INIT;

    public MainThread(String rpath) throws NoSuchFieldException, IllegalAccessException {
        this.resourcePath = rpath;
        String path = resourcePath + "/" + "lib";
        System.out.println("Path = " + path);
        System.setProperty("java.library.path", path);
        Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
        fieldSysPath.setAccessible( true );
        fieldSysPath.set(null, null);
    }

    protected void init() throws IOException, SigarException, InterruptedException {
        elementsPerJob = (numElements / numJobs);
        transferManagerThread = new TransferManagerThread();
        stateManagerThread = new StateManagerThread();
        hwMonitorThread = new HWMonitorThread();
        adapterThread = new AdapterThread();
        communicationThread = new CommunicationThread[TRANSFER_TYPE.values().length];
        for (int i = 0; i < TRANSFER_TYPE.values().length; i++) {
            communicationThread[i] = new CommunicationThread(i);
        }
        if (isLocal)
            dynamicBalancerUI = new DynamicBalancerUI(USE_UI, resourcePath);
    }

    protected static synchronized void setLocalJobsDone(int jobs) {
        if (jobs <= localJobsDone) return;
        localJobsDone = jobs;
    }

    protected static synchronized void setRemoteJobsDone(int jobs) {
        remoteJobsDone = jobs;
    }

    public void start() throws InterruptedException {
        STOP_SIGNAL = false;
        processingDone = false;
        finalRemoteJobs = 0;
        balanceTransferred = 0;
        WorkerThread.numIterations = numJobIteration;
        elementsPerJob = (numElements / numJobs);

        if (!isLocal) machineId = 1;

        jobQueue = new LinkedBlockingDeque<Job>();
        if (!isLocal)
            resultantJobQueue = new LinkedBlockingDeque<Job>();

        if (isLocal) {
            vectorA = new double[numElements];
            Arrays.fill(vectorA, initVal);

            vectorB = new double[numElements];
            elementsDone = 0;
            remoteJobsDone = 0;
            localJobsDone = 0;
            resultTransferred = 0;
        }

        adapterThread.start();
        transferManagerThread.start();
        stateManagerThread.start();
        if (isLocal)
            dynamicBalancerUI.start();
    }

    protected static synchronized void addToResult(Job job) {
        if (job != null) {
            Double[] data = job.getData();
            for (int i = job.getStartIndex(); i < job.getEndIndex(); i++) {
                vectorB[i] = data[i - job.getStartIndex()];
            }
            if (processingDone) {
                resultTransferred += 1;
            } else {
                elementsDone += data.length;
                setLocalJobsDone(localJobsDone + 1);
            }
        }
        if (processingDone) {
            if (isLocal) {
                double progress = (resultTransferred) * 100.0;
                progress = progress / (1.0 * finalRemoteJobs);
                //dynamicBalancerUI.setProgress((int) progress);
                dynamicBalancerUI.addMessage(new Message(MainThread.machineId, MessageType.ResultProgress, progress));

                if (resultTransferred == finalRemoteJobs) {
                    MainThread.stop();
                }
            }
        } else {
            //int total_elements_done = elementsDone + (remoteJobsDone * elementsPerJob);
            int jobsDone = localJobsDone + remoteJobsDone;
            if (isLocal) {
//                double progress = (total_elements_done) * 10000.0;
//                progress = progress / (1.0 * numElements);
                double progress = (jobsDone * 100.0) / (1.0 * numJobs);
                //dynamicBalancerUI.setProgress((int) progress);
                dynamicBalancerUI.addMessage(new Message(MainThread.machineId, MessageType.Progress, progress));
            }

            if (jobsDone >= numJobs) {
                processingDone = true;
                finalRemoteJobs = remoteJobsDone;
                try {
                    dynamicBalancerUI.addMessage(new Message(MainThread.machineId, MessageType.SM,
                            hwMonitorThread.getCurrentState()));
                } catch (SigarException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("Finished Computing everything");
                for (int i = 0; i < Math.min(numElements, numElementsPrint); i++)
                    System.out.print(vectorB[i] + " ");
                System.out.println("......");
                Message msg = new Message(machineId, MessageType.FinishACK, 0);
                try {
                    communicationThread[0].sendMessage(msg);
                } catch (IOException ie) {
                    stop();
                }
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
            mySocket[0].close();
            mySocket[1].close();
            mySocket[2].close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /////////////////////Test Output//////////////////////////
        if (isLocal) {
            System.out.println("Wait testing the output");
            for (int i = 0; i < vectorB.length; i++) {
                if (vectorB[i] == vectorA[i] || vectorB[i] == 0.0) {
                    System.out.println("Resultant Output incorrect at " + i + " index with value = " + vectorB[i]);
                    System.exit(1);
                }
            }
            System.out.println("Resultant Output is all correct");
        }
        ///////////////////////////////////////////////////////
        System.exit(0);
    }

    public void connect(String ip) throws IOException {
        mySocket = new Socket[TRANSFER_TYPE.values().length];
        if (!isLocal) {
            for (int i = 0; i < TRANSFER_TYPE.values().length; i++) {
                mySocket[i] = new Socket(ip, port[i]);
            }
        } else {

            InetAddress locIP = InetAddress.getByName(ip);

            System.out.println(Inet4Address.getLocalHost().getHostAddress());
            serverSocket = new ServerSocket[TRANSFER_TYPE.values().length];
            for (int i = 0; i < TRANSFER_TYPE.values().length; i++) {
                serverSocket[i] = new ServerSocket(port[i], 0, locIP);
                System.out.println("Listening on " + serverSocket[i]);
            }

            for (int i = 0; i < TRANSFER_TYPE.values().length; i++) {
                mySocket[i] = serverSocket[i].accept();
                System.out.println("Connection from " + mySocket[i]);
            }
        }

        for (int i = 0; i < TRANSFER_TYPE.values().length; i++) {
            communicationThread[i].setUpStreams();
            communicationThread[i].start();
        }

    }

    public void timePerJobCalc() {
        int elementsPerJob = MainThread.numElements / MainThread.numJobs;
        long timeTotal= 0 ;
        for (int i = 0; i < 10; i++) { // calculating 10 times average
            long t1 = System.currentTimeMillis();
            double[] data = new double[elementsPerJob];
            Arrays.fill(data, MainThread.initVal);
            for (int j = 0; j < elementsPerJob; j++) { // a job unit
                for (int k = 0; k < WorkerThread.numIterations; k++) {
                    data[j] = data[j] + MainThread.addVal;
                }
            }
            long t2 = System.currentTimeMillis();
            timeTotal += (t2 - t1);
        }
        System.out.println("TOTAL TIME FOR 10 JOBS in ms : " + timeTotal);
        MainThread.timePerJob = ((double)(timeTotal)/10);
    }

    private void readConf(String confFile) throws IOException {
        BufferedReader  bufferedReader = new BufferedReader(new FileReader(confFile));
        String in_line;
        String[] temp ;
        while((in_line = bufferedReader.readLine()) != null){
            if (in_line.equals("")) continue;
            if (in_line.contains("#")) continue;
            String[] inputList = in_line.split(":");
            String key = inputList[0];
            String val = inputList[1];
            switch (key) {
                case "JOB_LOOP":
                    WorkerThread.numIterations = Integer.parseInt(val);
                    break;

                case "NUM_ELEMENTS":
                    MainThread.numElements = 1024 * 1024 * Integer.parseInt(val);
                    break;

                case "NUM_JOBS":
                    MainThread.numJobs = Integer.parseInt(val);
                    break;

                case "GUARD":
                    MainThread.GUARD = Double.parseDouble(val);
                    break;

                case "THROTTLING_VALUE":
                    throttlingValue = Double.parseDouble(val);
                    break;

                case "COLLECTION_RATE":
                    collectionRate = Integer.parseInt(val);
                    break;

                case "TRANSFER_FLAG":
                    MainThread.transferFlag = Integer.parseInt(val);
                    break;

                case "NUMWORKERS":
                    MainThread.numWorkerThreads = Integer.parseInt(val);
                    break;

                case "NODE_TYPE":
                    if ( val.equals("remote")) {
                        isLocal = false;
                    } else {
                        isLocal = true;
                    }
                    break;

                case "NODE_IP":
                    ip = val;
                    break;

                case "TRANSFER_MODEL":
                    switch (val.toUpperCase()) {
                        case "SE":
                            tModel = TRANSFER_MODEL.SENDER_INIT;
                            break;
                        case "RE":
                            tModel = TRANSFER_MODEL.RECEIVER_INIT;
                            break;
                        case "SY":
                            tModel = TRANSFER_MODEL.SYMMETRIC;
                            break;
                        default:
                            tModel = TRANSFER_MODEL.SENDER_INIT;
                            break;
                    }
                    break;

                case "UTILIZATION_FACTOR":
                    MainThread.utilizationFactor = Integer.parseInt(val);
                    break;

                case "QUEUE_DIFFERENCE":
                    MainThread.queueDifferenceThreshold = Integer.parseInt(val);
                    break;

                case "USE_UI":
                    MainThread.USE_UI = Boolean.parseBoolean(val);
                    break;

                case "COMPRESSED":
                    MainThread.compressed = Integer.parseInt(val);
                    break;

                default:
                    break;
            }
        }
    }


    private void printConf(){
        System.out.println("**** RUN CONFIGURATION LOADED **** ");
        System.out.println("JOB LOOP : "+ WorkerThread.numIterations);
        System.out.println("NUM_ELEMENTS : "+ MainThread.numElements);
        System.out.println("GUARD : "+ MainThread.GUARD);
        System.out.println("THROTTLING VALUE : "+ throttlingValue);
        System.out.println("COLLECTION_RATE : "+ collectionRate);
        System.out.println("TRANSFER_FLAG : "+ MainThread.transferFlag);


    }
    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException,
                                                    SigarException, NoSuchFieldException, IllegalAccessException {
//        MainThread.GUARD = 0.5;
//        MainThread.transferFlag = 0;// 0 default only queue length
//
//        if (args.length >= 1 && args[0].equals("remote"))
//            isLocal = false;
//        if (args.length >= 2)
//            throttlingValue = Double.parseDouble(args[1]);
//        if (args.length >= 3)
//            collectionRate = Integer.parseInt(args[2]);
//        if (args.length >= 4)
//            ip = args[3];
//        if (args.length >= 5)
//            MainThread.GUARD = Integer.parseInt(args[4]);
//        if (args.length >= 6)
//            MainThread.transferFlag = Integer.parseInt(args[5]);


        MainThread mainThread = new MainThread(args[0]);

        mainThread.readConf(args[1]);
        mainThread.printConf();

        mainThread.init();

        mainThread.timePerJobCalc();
        System.out.println("TIME PER JOB ON MACHINE ID #" + MainThread.machineId + " IS (in ms) :" + MainThread.timePerJob);
        
        mainThread.connect(ip);

        communicationThread[0].sendMessage("Got connection from " + port);

        mainThread.start();
    }
}
