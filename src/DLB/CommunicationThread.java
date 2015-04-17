package DLB;

import java.io.*;

/**
 * Created by manshu on 4/16/15.
 */
public class CommunicationThread extends Thread {
    private ObjectOutputStream dout;
    private ObjectInputStream din;


    public CommunicationThread() throws IOException {
        dout = null;
        din = null;
    }

    public void setUpStreams() throws IOException {
        dout = new ObjectOutputStream(MainThread.otherSocket.getOutputStream());
        din  = new ObjectInputStream(MainThread.mySocket.getInputStream());
    }

    public void sendMessage(Object message) throws IOException {
        if (dout == null) return;
        //dout.writeUTF(message);
        dout.writeObject(message);
    }

    public Object receiveMessage() throws IOException, ClassNotFoundException {
        if (din == null) return "";
        Object message = din.readObject();
        System.out.println("Message = " + message.toString());
        return message;
    }


    @Override
    public void run() {
        while (!MainThread.STOP_SIGNAL) {
            try {
                receiveMessage();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Cannot continue w/o connection");
                MainThread.stop();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
