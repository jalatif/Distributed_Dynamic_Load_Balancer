package TryThreading;

/**
 * Created by manshu on 4/16/15.
 */
/**
 * Created with IntelliJ IDEA.
 * User: jalatif
 * Date: 8/4/13
 * Time: 9:22 PM
 * To change this template use File | Settings | File Templates.
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class Server
{

    private ServerSocket ss;

    private Hashtable outputStreams = new Hashtable();
    private Hashtable socketMap = new Hashtable();

    protected static Hashtable nameMap = new Hashtable();

    protected static Hashtable portMap = new Hashtable();

    public Server( String ip, int port ) throws IOException {
        listen( ip, port );
    }

    private void showInterfaces() throws Exception
    {
        System.out.println("Host addr: " + InetAddress.getLocalHost().getHostAddress());
        Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
        for (; n.hasMoreElements();)
        {
            NetworkInterface e = n.nextElement();
            System.out.println("Interface: " + e.getName());
            Enumeration<InetAddress> a = e.getInetAddresses();
            for (; a.hasMoreElements();)
            {
                InetAddress addr = a.nextElement();
                System.out.println("  " + addr.getHostAddress());
            }
        }
    }

    private void listen( String ip, int port ) throws IOException {

        String data = "Pyaas JaltheBand";
        try {
            InetAddress locIP;
            locIP = InetAddress.getByName(ip);

            showInterfaces();
            System.out.println(Inet4Address.getLocalHost().getHostAddress());

            ss = new ServerSocket( port, 0, locIP );

            System.out.println( "Listening on "+ss );

            while (true) {

                Socket s = ss.accept();

                System.out.println( "Connection from "+s );


                DataOutputStream dout = new DataOutputStream( s.getOutputStream() );

                outputStreams.put( s, dout );
                socketMap.put(s.getPort(), dout);
                this.sendToAll(data);
                this.sendToAll("Architect sahab ki jay");
                //new ServerThread( this, s );
            }
        }
        catch (Exception e){
            System.out.print("Whoops! It didn't work!\n" + e);
        }

    }


    Enumeration getOutputStreams() {
        return outputStreams.elements();
    }

    Enumeration getSockets(){
        return outputStreams.keys();
    }

    void sendToAll( String message ) {



        synchronized( outputStreams ) {

            for (Enumeration e = getOutputStreams(); e.hasMoreElements(); ) {

                DataOutputStream dout = (DataOutputStream)e.nextElement();

                try {
                    dout.writeUTF( message );
                } catch( IOException ie ) { System.out.println( ie ); }
            }
        }
    }

    void sendTo( String user, String message ) {

        synchronized( outputStreams ) {
            DataOutputStream dout;
            int localport = (Integer) portMap.get(user);
            //int localport = Integer.parseInt(lPort);
            System.out.println("Given " + localport);
            try{
                dout = (DataOutputStream) socketMap.get(localport);
                dout.writeUTF(message);
            }
            catch(NullPointerException npe){
                npe.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            /*Socket s;
            for (Enumeration e = getSockets(); e.hasMoreElements(); ) {

                s = (Socket)e.nextElement();
                try {
                    System.out.println("Searching in " + s.getPort());
                if (s.getPort() == localport){
                    System.out.println("Ids Matched");
                    dout = (DataOutputStream) outputStreams.get(s);
                    dout.writeUTF( message );
                    }
                }
                catch( Exception ie ) { System.out.println( ie ); }

            }
            */

        }
    }



    void removeConnection( Socket s ) {


        synchronized( outputStreams ) {

            System.out.println( "Removing connection to "+s );

            outputStreams.remove( s );

            try {
                s.close();
            } catch( IOException ie ) {
                System.out.println( "Error closing "+s );
                ie.printStackTrace();
            }
        }
    }


    static public void main( String args[] ) throws Exception {

        String ip = "localhost"; //args[0];
        int port = 1234;
//        if (args.length == 2)
//            port = Integer.parseInt( args[1] );
        new Server(ip, port);
    }
}
