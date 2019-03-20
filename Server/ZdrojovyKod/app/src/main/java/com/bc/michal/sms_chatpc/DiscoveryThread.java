package com.bc.michal.sms_chatpc;

import android.view.ContextMenu;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Michal_ on 17.4.2016.
 * <p/>
 * Pouzity navrhovy vzor SINGLETON
 * serverova strana aplikacie
 */


public class DiscoveryThread implements Runnable {
    static final String LOG_TAG = "Discover";

   // DatagramSocket socket;
    MulticastSocket socket;
    RSA rsa = new RSA();

    @Override
    public void run() {
        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            socket = new MulticastSocket(8888);//, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while (true) {

                System.out.println(LOG_TAG + "Ready to receive broadcast packets!");
                //Receive a packet
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);

                /*
                *  V pripade ze bude o toto zariadenie mat
                *  aj iny stroj zaujem
                *  stale pocuva na broadcast-e
                *
                * */

                // wait until and break
               // socket.setSoTimeout(4000);

                socket.receive(packet);
                String text = new String(packet.getData());
                //text = text.substring(0,27);
               String receiveData = RSA.decrypt(text);
                //String receiveData = text;

                //Packet received
                System.out.println(LOG_TAG + "Discovery packet received from: " + packet.getAddress().getHostAddress());
                System.out.println(LOG_TAG + "Packet received; data: " + receiveData);

                //Set ip address client PC
                MainActivity.IPaddressPC = new String(packet.getAddress().getHostAddress());

                //See if the packet holds the right command (message)
                String message = receiveData.trim();
                if (message.equals("DISCOVER_FUIFSERVER_REQUEST")) {
                    //String msg = "DISCOVER_FUIFSERVER_RESPONSE";
                    String msg = RSA.encrypt("DISCOVER_FUIFSERVER_RESPONSE");
                    byte[] sendData = msg.getBytes();
                    //Send a response
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);
                    System.out.println(LOG_TAG + "Sent packet to: " + sendPacket.getAddress().getHostAddress());


                    /*
                    *  Ak uz ziskal aspon
                    *  jednu IP adresu
                    *  teda aspon jedneho klienta
                    *  odomke zamok a zacina s nim komunikovat
                    *
                    * */
                    synchronized (MainActivity.syncLock) {
                        MainActivity.syncLock.notifyAll();
                        getInstance();
                        MainActivity.success=true;
                    }
                }
            } // end of WHILE(1)

        } catch (SocketTimeoutException e) {
            // timeout exception.
            System.out.println("Timeout reached!!! " + e);
            socket.close();
            synchronized (MainActivity.syncLock) {
                MainActivity.syncLock.notifyAll();
                MainActivity.success=false;
               // getInstance();
            }
            return;
        } catch (IOException ex) {
            Logger.getLogger(LOG_TAG).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static DiscoveryThread getInstance() {
        return DiscoveryThreadHolder.INSTANCE;
    }

    private static class DiscoveryThreadHolder {
        private static final DiscoveryThread INSTANCE = new DiscoveryThread();
    }

}
