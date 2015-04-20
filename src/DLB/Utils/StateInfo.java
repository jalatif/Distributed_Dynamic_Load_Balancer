package DLB.Utils;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by manshu on 4/16/15.
 */
public class StateInfo implements Serializable {
    private static final long serialVersionUID = -1885711311464708208L;

    private Date timestamp;
    private int queueLength;
    private double cpuUsage;
    private double bwUsage;

    public StateInfo(int queue_length, double ...usages) {
        this.timestamp = new Date();
        this.queueLength = queue_length;
        if (usages.length >= 1)
            cpuUsage = usages[0];
        else
            cpuUsage = -1;
        if (usages.length >= 2)
            bwUsage = usages[1];
        else
            bwUsage = -1;
    }

    @Override
    public String toString() {
        return "StateInfo{" +
                "queueLength=" + queueLength +
                ", cpuUsage=" + cpuUsage +
                ", bwUsage=" + bwUsage +
                ", timestamp=" + timestamp +
                '}';
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getQueueLength() {
        return queueLength;
    }

    public void setQueueLength(int queueLength) {
        this.queueLength = queueLength;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public double getBwUsage() {
        return bwUsage;
    }

    public void setBwUsage(double bwUsage) {
        this.bwUsage = bwUsage;
    }

}
