package DLB.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by manshu on 4/16/15.
 */
public class Job <T> implements Serializable {
    private static final long serialVersionUID = 3522803485757903861L;

    private int startIndex;
    private int endIndex;
    //private List<T> data;
    private T[] data;

    public Job(int startIndex, int endIndex, T[] data) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.data = (T[]) new Object[data.length];
//        for (T id : data) {
//            this.data.add(id); // should be id.clone();
//        }
        for (int i = 0; i < data.length; i++)
            this.data[i] = data[i]; //should be clone();
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public T[] getData() {
        return data;
    }

    public void setData(T[] data) {
        this.data = (T[]) new Object[data.length];
        for (int i = 0; i < data.length; i++)
            this.data[i] = data[i]; //should be clone();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("FI = " + startIndex).append(", ");
        stringBuilder.append("EI = " + endIndex).append(", ");
        stringBuilder.append("Items = ");
        for (int i = 0; i < data.length; i++) {
            stringBuilder.append(data[i].toString()).append(" ");
        }
        return stringBuilder.toString();
    }
}
