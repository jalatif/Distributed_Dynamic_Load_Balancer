package DLB.Utils;

import java.io.Serializable;

/**
 * Created by manshu on 4/16/15.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1256368627620549766L;

    private MessageType msgType;
    private Object data;

    public Message(MessageType msgType, Object data) {
        this.msgType = msgType;
        this.data = data;
    }

    @Override
    public String toString() {
        return "Message{" +
                "msgType=" + msgType.name() +
                ", data=" + data.toString() +
                '}';
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
