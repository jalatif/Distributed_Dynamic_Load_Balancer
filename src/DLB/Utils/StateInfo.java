package DLB.Utils;

import DLB.MainThread;

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
    private double timePerJob;
    private double throttlingValue;
    private int numJobsDone;
    private boolean jobsInQueue, jobsInComing;


    public StateInfo(int queue_length, int numJobsDone, boolean jobsInQueue, boolean jobsInComing, double ...usages) {
        this.timestamp = new Date();
        this.queueLength = queue_length;
        this.numJobsDone = numJobsDone;
        this.jobsInQueue = jobsInQueue;
        this.jobsInComing = jobsInComing;

        if (usages.length >= 1)
            cpuUsage = usages[0];
        else
            cpuUsage = -1;
        if (usages.length >= 2)
            bwUsage = usages[1];
        else
            bwUsage = -1;
        if (usages.length >= 3)
		    timePerJob = usages[2];
	    else
		    timePerJob = 0.0;
        if (usages.length >= 4)
	        throttlingValue = usages[3];
	    else
		    throttlingValue = 0.1;
    }

    @Override
    public String toString() {
        return "StateInfo{" +
                "timestamp=" + timestamp +
                ", queueLength=" + queueLength +
                ", numJobsDone=" + numJobsDone +
                ", jobsInQueue=" + jobsInQueue +
                ", jobsInComing=" + jobsInComing +
                ", cpuUsage=" + cpuUsage +
                ", bwUsage=" + bwUsage +
                ", timePerJob=" + timePerJob +
                ", throttlingValue=" + throttlingValue +
                '}';
    }

    public int getNumJobsDone() {
        return numJobsDone;
    }

    public void setNumJobsDone(int numJobsDone) {
        this.numJobsDone = numJobsDone;
    }

    public String[] getFormattedKeys() {
        return new String[]{"Queue Length", "JobsDone", "InQueue", "InComing", "CPU Usage", "BW Usage",
                "time per job", "throttling value", "Timestamp"};
    }

    public String[] getFormattedValues() {
        return new String[]{String.valueOf(queueLength), String.valueOf(numJobsDone),
                String.valueOf(jobsInQueue), String.valueOf(jobsInComing), String.valueOf(cpuUsage),
                String.valueOf(bwUsage), String.valueOf(timePerJob), String.valueOf(throttlingValue),
                timestamp.toString()};
    }

    public void setTimePerJob(double timePerJob) {
        this.timePerJob = timePerJob;
    }

    public void setThrottlingValue(double throttlingValue) {
        this.throttlingValue = throttlingValue;
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

    public double getTimePerJob() {
        return timePerJob;
    }
    public double getThrottlingValue() {
        return throttlingValue;
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
