package DLB;

import DLB.Utils.Job;
import DLB.Utils.Message;
import DLB.Utils.MessageType;
import DLB.Utils.StateInfo;
import org.hyperic.sigar.SigarException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by manshu on 4/16/15.
 */
public class AdapterThread extends Thread {
    private static volatile double throttlingValue;

    private WorkerThread[] workerThreads;

    private BlockingQueue<Message> messages;
    private StateInfo prev_sLocal, prev_sRemote;
    private Date lastStateTime;

    public AdapterThread() {
        messages = new LinkedBlockingDeque<Message>();
        throttlingValue = MainThread.throttlingValue;
        prev_sLocal = null;
        prev_sRemote = null;
        workerThreads = null;
    }

    public void setThrottlingValue(double tValue) {
        System.out.println("Changing throttle value to " + tValue);
        throttlingValue = tValue;
        if (workerThreads == null || workerThreads.length == 0) return;
        for (int i = 0; i < workerThreads.length; i++) {
            workerThreads[i].changeThrottleValue(throttlingValue);
        }
        MainThread.throttlingValue = tValue;
    }

    private void workTransferCalc(StateInfo sRemote, StateInfo sLocal, int transferFlag) {
        System.out.println("TRASFER FLAG :" + transferFlag);
        System.out.println("SLOCAL IS :" + sLocal);
        System.out.println("SREMOTE IS :" + sRemote);
        transferFlag = 0;
        switch (transferFlag) {
            case 0: // case when only queue length is considered.
                if ((sLocal.getQueueLength() - sRemote.getQueueLength()) > MainThread.queueDifferenceThreshold) {
                    int jobsToSend = (sLocal.getQueueLength() - sRemote.getQueueLength()) / 2;
                    Message msg = new Message(MainThread.machineId, MessageType.JOBTRANSFER, jobsToSend);
                    System.out.println("Matching expected time to finish by sending " + jobsToSend + " number of jobs");
                    MainThread.transferManagerThread.addMessage(msg);
                    MainThread.balanceTransferred++;
                }
            break;

            case 1: // case when time to completion is considered.
                System.out.println("CASE CASE CASE CASE 1");
//                x * bytesperjob / bw = (jobqlength - x) * timetocomplteonejob * throttlefactor
//                lhs  =  rhs - gaurd
//                WorkerThread.timePerJob * MainThread.throttlingValue;
                double remoteMetrics = sRemote.getTimePerJob() * sRemote.getQueueLength() * sRemote.getThrottlingValue();
                double localMetrics = sLocal.getTimePerJob() * sLocal.getQueueLength() * sLocal.getThrottlingValue();
                int remoteQ = sRemote.getQueueLength();
                int localQ = sLocal.getQueueLength();
                int jobsToSend = 0 ;

                if (localMetrics > remoteMetrics + MainThread.GUARD) {
                    while ((localMetrics - remoteMetrics) >= MainThread.GUARD) {
                        localQ--;
                        localMetrics = sLocal.getTimePerJob() * localQ * sLocal.getThrottlingValue();
                        jobsToSend++;
                    }
                    Message msg = new Message(MainThread.machineId, MessageType.JOBTRANSFER, jobsToSend);
                    System.out.println("Matching expected time to finish by sending " + jobsToSend + " number of jobs");
                    MainThread.transferManagerThread.addMessage(msg);
                    MainThread.balanceTransferred++;
                }
                break;
        }

    }

    private void adapterWork() throws InterruptedException, SigarException, IOException {
        Message incomingMsg = messages.take();
        // sleep switch worker
        // queue check - call function accordingly

        if (incomingMsg.getMsgType() != MessageType.HW) return;


        StateInfo sRemote = (StateInfo) incomingMsg.getData();
        StateInfo sLocal = MainThread.hwMonitorThread.getCurrentState();

        if (MainThread.isLocal) {
            MainThread.setRemoteJobsDone(sRemote.getNumJobsDone());
            MainThread.dynamicBalancerUI.addMessage(new Message(incomingMsg.getMachineId(), MessageType.SM, sRemote));
            MainThread.dynamicBalancerUI.addMessage(new Message(MainThread.machineId, MessageType.SM, sLocal));
        }

        System.out.println("Adapter State Remote " + sRemote);
        System.out.println("Adapter State Local " + sLocal);

        synchronized (MainThread.jobInQueueLock) {
            if (MainThread.jobsInQueue) return;
        }

        synchronized (MainThread.jobInComingLock) {
            if (MainThread.jobsInComing) return;
        }

        System.out.println("TEST");
        workTransferCalc(sRemote, sLocal, MainThread.transferFlag);

//
//        if (lastStateTime != null && lastStateTime.compareTo(sLocal.getTimestamp()) < 1) return;
//        if (lastStateTime != null && lastStateTime.compareTo(sRemote.getTimestamp()) < 1) return;

//        if (prev_sRemote != null && prev_sLocal != null) {
//            int delta_queue1 = prev_sLocal.getQueueLength() - sLocal.getQueueLength();
//            int delta_queue2 = prev_sRemote.getQueueLength() - sRemote.getQueueLength();
//            int delta_time1 = (int) (sLocal.getTimestamp().getTime() - prev_sLocal.getTimestamp().getTime());
//            int delta_time2 = (int) (sRemote.getTimestamp().getTime() - prev_sRemote.getTimestamp().getTime());
//            int ql1 = sLocal.getQueueLength(), ql2 = sRemote.getQueueLength();
//
//            if (delta_queue1 <= 0) return;
//            if (delta_queue2 <= 0) return;
//
//            double ttf1 = (delta_time1 * ql1) / (1.0 * delta_queue1);
//            double ttf2 = (delta_time2 * ql2) / (1.0 * delta_queue2);
//
//            System.out.println("ttf1 = " + ttf1 + ", ttf2 = " + ttf2);
//            if (ttf1 > ttf2) {
//                int c1 = delta_time2 * delta_queue1;
//                int c2 = delta_time1 * delta_queue2;
//                int jobsToSend = (int) ((c2 * ql2 - c1 * ql1) / (1.0 * (c1 + c2)));
//                System.out.println("Matching expected time to finish by sending " + jobsToSend + " number of jobs");
//                Message msg = new Message(MainThread.machineId, MessageType.BULKJOBTRANSFER, jobsToSend);
//                MainThread.transferManagerThread.addMessage(msg);
//                lastStateTime = new Date();
//            }
//        }
//        prev_sLocal = sLocal;
//        prev_sRemote = sRemote;
    }

    protected synchronized void addMessage(Message msg) {
        messages.add(msg);
    }

    private void bootstrapJobs() {
        int jobItems = MainThread.numElements / MainThread.numJobs;
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < MainThread.numElements; i += jobItems) {
            //List<Double> list = new ArrayList<Double>(jobItems); SLOW
            Double[] arr = new Double[jobItems];
            for (int j = i; j < i + jobItems; j++) {
                //list.add(MainThread.vectorA[j]);
                arr[j - i] = MainThread.vectorA[j];
            }
            Job job = new Job(i, i + jobItems, arr);
            MainThread.jobQueue.add(job);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("Time taken = " + (t2 - t1));
        Message msg = new Message(MainThread.machineId, MessageType.JOBTRANSFER, MainThread.numJobs / 2);
        MainThread.transferManagerThread.addMessage(msg);
        synchronized (MainThread.jobInQueueLock) {
            MainThread.jobsInQueue = true;
        }
        if (MainThread.isLocal) {
            MainThread.dynamicBalancerUI.changeTransferStatus(MainThread.machineId, true);
        }
    }

    private void startWorkersAndMonitors() {
        workerThreads = new WorkerThread[MainThread.numWorkerThreads];
        for (int i = 0; i < workerThreads.length; i++) {
            workerThreads[i] = new WorkerThread(i, throttlingValue);
            workerThreads[i].start();
        }
        MainThread.hwMonitorThread.start();
    }

    @Override
    public void run() {
        Message msg = new Message(MainThread.machineId, MessageType.UITVALUE, throttlingValue);
        if (MainThread.isLocal) {
            MainThread.dynamicBalancerUI.addMessage(msg);
        } else {
            MainThread.transferManagerThread.addMessage(msg);
        }

        if (MainThread.isLocal)
            bootstrapJobs();
        if (MainThread.isLocal) {
            while (MainThread.jobsInQueue) {
                try {
                    sleep(100);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    break;
                }
            }
        } else {
            while (MainThread.jobQueue.isEmpty() || MainThread.jobsInComing) {
                try {
                    sleep(100);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    break;
                }
            }
        }

        System.out.println("Now jobs are shared. Starting Processing");

        startWorkersAndMonitors();

        while (!MainThread.STOP_SIGNAL) {
            try {
                adapterWork();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                MainThread.stop();
            } catch (SigarException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
