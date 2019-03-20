/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BC_sms_app_client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Michal_ navrhovy vzor SINGLETON
 *
 * client strana aplikacie
 *
 */
public class DiscoveryThread implements Runnable {

    static final String LOG_TAG = "Discover";

    DatagramSocket c;
    
    RSA rsa = new RSA();
    StopWatch sw = new StopWatch("DiscoverThread RSA");
    
    
    @Override
    public void run() {

        // Find the server using UDP broadcast
        try {
            //Open a random port to send the package
            c = new DatagramSocket();
            c.setBroadcast(true);
    
            sw.start("RSA encrypt");
            String msg = rsa.encrypt("DISCOVER_FUIFSERVER_REQUEST");
            sw.stop();
             // String msg = "DISCOVER_FUIFSERVER_REQUEST";
// System.out.println("DISCOVER_FUIFSERVER_REQUEST".length());
            byte[] sendData = msg.getBytes("UTF-8");
          
            
             
            //Try the 255.255.255.255 first
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);
                c.send(sendPacket);
                System.out.println(LOG_TAG + " Request packet sent to: 255.255.255.255 (DEFAULT)");
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Broadcast the message over all the network interfaces
            Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue; // Don't want to broadcast to the loopback interface
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    // Send the broadcast package!
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
                        c.send(sendPacket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println(LOG_TAG + " Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                }
            }
            System.out.println(LOG_TAG + " Done looping over all network interfaces. Now waiting for a reply!");

            //Wait for a response
            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            c.receive(receivePacket);

            sw.start("RSA decrypt");
            String receiveData = rsa.decrypt(new String(receivePacket.getData()));
            //String receiveData = new String(receivePacket.getData());
            sw.stop();
                System.out.println("TIME " +sw.getTotalTimeMillis());
            //We have a response
            System.out.println(LOG_TAG + " Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

            
            
            //Check if the message is correct
            String message =  receiveData.trim();

            if (message.equals("DISCOVER_FUIFSERVER_RESPONSE")) {
                System.out.println("OK");
                //DO SOMETHING WITH THE SERVER'S IP (for example, store it in your controller)
                FXMLController.setServerIp(receivePacket.getAddress());
             
                
                synchronized (FXMLController.syncLock) {
                    FXMLController.syncLock.notifyAll();
                }

                
              
                
                
                c.close();
                return;
            }   

            //Close the port!
            c.close();

        } catch (IOException ex) {
            System.err.println(ex);
        } catch (Throwable ex) {
            Logger.getLogger(DiscoveryThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static DiscoveryThread getInstance() {
        return DiscoveryThreadHolder.INSTANCE;
    }

    private static class DiscoveryThreadHolder {
        private static final DiscoveryThread INSTANCE = new DiscoveryThread();
    }

}
