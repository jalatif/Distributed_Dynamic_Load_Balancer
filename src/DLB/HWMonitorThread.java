package DLB;

import DLB.Utils.Message;
import DLB.Utils.MessageType;
import DLB.Utils.StateInfo;
import org.hyperic.sigar.*;

import java.io.IOException;

/**
 * Created by manshu on 4/16/15.
 */
public class HWMonitorThread extends Thread {
    private Sigar sigar;
    private long pid;
    private ProcCpu cpu;
    private NetStat net;
    private CpuPerc cpuPerc;
    private double cpu_usage = 0.0, nw_usage = 0.0;

    public HWMonitorThread() throws SigarException {
        sigar = new Sigar();
        cpu = null;
        pid = sigar.getPid(); // this one gives me the same process ID that I see in visualVM
        cpuPerc = sigar.getCpuPerc();
        System.out.println("PID of the process is " + pid);
    }

    private double getCpuUsage() {
//        try {
//            cpu = sigar.getProcCpu(pid);
//        } catch (SigarException se) {
//            se.printStackTrace();
//            return cpu_usage;
//        }
//        cpu_usage = cpu.getPercent();
        cpu_usage = cpuPerc.getCombined() * 100.0;
        return cpu_usage;
    }

    private double getNwUsage() {
        try {
            net = sigar.getNetStat();
        } catch (SigarException e) {
            e.printStackTrace();
            return nw_usage;
        }
        nw_usage = net.getAllInboundTotal() + net.getAllOutboundTotal();
        return nw_usage;
    }

    protected StateInfo getCurrentState() {
        return new StateInfo(MainThread.jobQueue.size(), getCpuUsage(), getNwUsage());
    }

    private void doMonitoring() throws IOException {
        //if (MainThread.jobsInQueue || MainThread.jobsInComing) return;
        //int queue_length = MainThread.jobQueue.size();
        StateInfo state = getCurrentState();//new StateInfo(queue_length, getCpuUsage(), getNwUsage());
        Message msg = new Message(MainThread.machineId, MessageType.HW, state);
        //MainThread.adapterThread.addMessage(msg);
        MainThread.stateManagerThread.addMessage(msg);//MainThread.communicationThread.sendMessage(msg);
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
