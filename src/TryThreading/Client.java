package TryThreading;

/**
 * Created by manshu on 4/16/15.
 */
/**
 * Created with IntelliJ IDEA.
 * User: jalatif
 * Date: 8/4/13
 * Time: 9:35 PM
 * To change this template use File | Settings | File Templates.
 */
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
public class Client implements Runnable
{
    private String myMsg = "";
    private String mySMsg = "";

    private Socket socket;


    private DataOutputStream dout;
    private DataInputStream din;

    public Client( String host, int port ) {
        try {

            socket = new Socket( host, port );

            System.out.println( "connected to "+socket );


            din = new DataInputStream( socket.getInputStream() );
            dout = new DataOutputStream( socket.getOutputStream() );

            new Thread( this ).start();
        } catch( IOException ie ) { System.out.println( ie ); }
    }

    private void processMessage( String message ) {
        try {

            mySMsg = message;
            message = socket.getLocalPort() + " said: " + message;
            myMsg = message;
            dout.writeUTF(message);

        } catch( IOException ie ) { System.out.println( ie ); }
    }

    public void run() {
        try {

            while (true) {

                String message = din.readUTF();
                System.out.println("Message = " + message);
            }
        } catch( IOException ie ) { System.out.println( ie ); }
    }

    public static void main(String args[]){
        int port = 1234;

        String host = "localhost";
        Client c = new Client(host, port);
    }

}


