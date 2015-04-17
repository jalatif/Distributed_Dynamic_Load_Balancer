package DLB.Utils;

import java.io.Serializable;

/**
 * Created by manshu on 4/16/15.
 */
public class State implements Serializable {
    private static final long serialVersionUID = -2545658550932513121L;
    private int queue_length;
    private double cpu_usage;
    private double bw_usage;

    public State(int queue_length, double ...usages) {
        this.queue_length = queue_length;
        if (usages.length >= 1)
            cpu_usage = usages[0];
        else
            cpu_usage = -1;
        if (usages.length >= 2)
            bw_usage = usages[1];
        else
            bw_usage = -1;
    }

    @Override
    public String toString() {
        return "State{" +
                "queue_length=" + queue_length +
                ", cpu_usage=" + cpu_usage +
                ", bw_usage=" + bw_usage +
                '}';
    }

    public int getQueueLength() {
        return queue_length;
    }

    public void setQueueLength(int queue_length) {
        this.queue_length = queue_length;
    }

    public double getCpuUsage() {
        return cpu_usage;
    }

    public void setCpuUsage(double cpu_usage) {
        this.cpu_usage = cpu_usage;
    }

    public double getBwUsage() {
        return bw_usage;
    }

    public void setBwUsage(double bw_usage) {
        this.bw_usage = bw_usage;
    }
}
