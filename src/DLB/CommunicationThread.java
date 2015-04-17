package DLB;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by manshu on 4/16/15.
 */
public class CommunicationThread extends Thread {
    private DataOutputStream dout;
    private DataInputStream din;


    public CommunicationThread() throws IOException {
        dout = null;
        din = null;
    }

    public void setUpStreams() throws IOException {
        dout = new DataOutputStream(MainThread.otherSocket.getOutputStream());
        din  = new DataInputStream(MainThread.mySocket.getInputStream());
    }

    public void sendMessage(String message) throws IOException {
        if (dout == null) return;
        dout.writeUTF(message);
    }

    public String receiveMessage() throws IOException {
        if (din == null) return "";
        String message = din.readUTF();
        System.out.println("Message = " + message);
        return message;
    }

    @Override
    public void run() {
        super.run();
    }
}
