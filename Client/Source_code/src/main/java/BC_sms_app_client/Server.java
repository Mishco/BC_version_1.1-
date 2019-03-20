package BC_sms_app_client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Michal_
 */
public class Server {

    protected FXMLController controller;
    
    String inputLine = null;        // odchadzajuci text       
    String outputLine = null;       // prichadzajuci text
    static String remoteAddr;       // Ip adresa vzdialeneho zariadenia    
    static int localPort;           // moj port na ktorom pocuvam
    
    protected int remotePort;          // vzdialeny port na ktory posielam

    Boolean result = false;

    boolean isListening;
    static int buf_size = 1024;

    protected Thread listenThread;
    private DatagramSocket socket;          // soket, ktory je otvoreny 
    private byte[] replyBytes; 
    
    
    protected void startToListen() {
       /* listenThread = new Thread(() -> {
            listen(remotePort);
        });
        */
        listenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                listen(remotePort);
            }
        });
        
        
        
        isListening = true;

        listenThread.start();
    }

    
    /*
        FUNKCIE PRE SPRACOVANIE PAKETU
    */
    private void processPacket(byte[] buff, int packetSize) {
        
    }
    
    private void listen(int portToListen) {
        this.remotePort = portToListen;
        DatagramPacket replyPacket;
        try {
            socket = new DatagramSocket(portToListen);
            byte[] buff = new byte[1024];
            while (isListening) {
                DatagramPacket packet = new DatagramPacket(buff, buff.length);
                socket.receive(packet);
                processPacket(buff, packet.getLength());
                replyPacket = new DatagramPacket(replyBytes, replyBytes.length, packet.getAddress(), packet.getPort());
                socket.send(replyPacket);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopListen() {
        listenThread.interrupt();
        this.isListening = false;
        socket.close();
    }

    /*
     *  Odosielanie samostanych sprav
     */
    public void sendMessage(Message message) {

        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] buf = new byte[256];

            String outputLine = message.getHead() + message.getTelNum() + message.getBody();

            buf = outputLine.getBytes();
            InetAddress address = InetAddress.getByName(remoteAddr);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, remotePort);
            System.out.println("To send message");
            socket.send(packet);
            System.out.println("Sent message");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     *  Pocuvanie a cakanie na prijatie spravy
     */
    protected void run() throws IOException {

        Thread t1 = new Thread(new Runnable() {
            public void run() {
                try {
                    DatagramPacket packet;
                    byte[] buf = new byte[256];

                    //Thread.sleep(1000);
                    packet = new DatagramPacket(buf, buf.length);

                    socket.receive(packet);
                    System.out.println("received packet: ");
                    String received = new String(packet.getData());
                    System.out.println(received);
                    setResult(true);
                    socket.close();                     // zatvor pre prvotnu komunikaciu
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t1.start();
    }

    public void setResult(Boolean recevied) {
        this.result = recevied;
    }

    public Boolean getResult() {
        return result;
    }
}
