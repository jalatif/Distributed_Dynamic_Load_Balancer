package DLB.Utils;

import java.io.Serializable;

/**
 * Created by manshu on 4/16/15.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1256368627620549766L;

    private int machineId;
    private MessageType msgType;
    private Object data;

    public Message(int machineId, MessageType msgType, Object data) {
        this.machineId = machineId;
        this.msgType = msgType;
        this.data = data;
    }

    @Override
    public String toString() {
        return "Message{" +
                "machineId=" + machineId +
                ", msgType=" + msgType +
                ", data=" + data +
                '}';
    }

    public int getMachineId() {
        return machineId;
    }

    public void setMachineId(int machineId) {
        this.machineId = machineId;
    }

    public MessageType getMsgType() {
        return msgType;
    }

    public void setMsgType(MessageType msgType) {
        this.msgType = msgType;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
