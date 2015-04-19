package DLB;

import DLB.Utils.Message;
import DLB.Utils.MessageType;
import DLB.Utils.StateInfo;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.io.IOException;

/**
 * Created by manshu on 4/16/15.
 */
public class HWMonitorThread extends Thread {
    private Sigar sigar;
    private long pid;
    private ProcCpu cpu;
    private NetStat net;
    private double cpu_usage = 0.0, nw_usage = 0.0;

    public HWMonitorThread() {
        sigar = new Sigar();
        cpu = null;

        pid = sigar.getPid(); // this one gives me the same process ID that I see in visualVM
    }

    private double getCpuUsage() {
        try {
            cpu = sigar.getProcCpu(pid);
        } catch (SigarException se) {
            se.printStackTrace();
            return cpu_usage;
        }
        cpu_usage = cpu.getPercent();
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

    private void doMonitoring() throws IOException {
        int queue_length = MainThread.jobQueue.size();
        if (queue_length == 0) return;

        int machineId = 0;
        if (!MainThread.isLocal) machineId = 1;

        StateInfo state = new StateInfo(machineId, queue_length, getCpuUsage(), getNwUsage());
        Message msg = new Message(MessageType.HW, state);
        MainThread.adapterThread.addMessage(msg);
        MainThread.communicationThread.sendMessage(msg);
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
