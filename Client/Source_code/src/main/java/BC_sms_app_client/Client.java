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
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Michal_
 */
public class Client {

    protected int portToSend = 12345;
    protected FXMLController controller;
    protected int retValue = 0;          // navratova hodnota
    private InetAddress addressToSend;
    private byte[] buff;

    DatagramSocket socket;
    DatagramSocket socketKeepAlive;
    int fragmentSize;
    byte[] responseBytes = new byte[15000];

    protected Thread sendingThread;
    protected Thread keepAliveThread;
    boolean SENDING = false;
    int tmp;
    RSA rsa = new RSA();
 
     protected void setAddressToSend(String hostname) {
        try {
            addressToSend = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    protected void startSendingData(final String data) {
        System.out.print("Adress to send: " + addressToSend);
        System.out.println(" Port: " + portToSend);

        // lambda expression
        // Send a message in a new Thread
        sendingThread = new Thread(() -> {
            sendData(portToSend, data);
        });

        /*sendingThread = new Thread(new Runnable() {
         @Override
         public void run() {
         sendData(portToSend, data);
         }
         });*/
        SENDING = true;
        sendingThread.start();
    }

    protected void startSendingLongData(final String[] data, int countOfdata) {
        sendingThread = new Thread(() -> {
            sendLongData(data, countOfdata);
        });
        sendingThread.start();
    }

    private void sendData(int port, String msg) {
        try {
            socket = new DatagramSocket(portToSend);

            System.out.print("That was send: ");
            System.out.println(msg);
            
            //crypt the message
         //   msg = rsa.encrypt(msg);
         //   System.out.println("crypted : " + msg);
            
            buff = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(buff, buff.length, addressToSend, portToSend);
            socket.send(packet);
            socket.setSoTimeout(10000);
            System.out.println("ziadost");
            controller.debugMsg.appendText("klient odoslal ziadost\n");

            DatagramPacket response = new DatagramPacket(responseBytes, responseBytes.length);
            socket.receive(response);
            if (response.getLength() != 0) {
                stopSending();
            }
            
            
            // decrypt msg
         //   String receiveMsg = rsa.decrypt(new String(response.getData()));
            String receiveMsg = new String(response.getData());

            if (receiveMsg.contains("C")) {
                controller.vsetkyKontakty.append(receiveMsg);
                controller.debugMsg.appendText("✔ kontaky prijate\n");
                retValue = 0;
                
                int tmp = controller.vsetkyKontakty.length();
                while (tmp > 0) {
                    //meno
                    controller.vsetkyKontakty.indexOf(";");
                    
                    //cislo
                    
                }
                        
            }
            
            if (receiveMsg.contains("ODPOVED")) {
                System.out.println("ziadost bola potvrdena");
                controller.debugMsg.appendText("✔ ziadost bola potvrdena\n");
                retValue = 0;
            } else {
                System.out.println("ziadost neuspesna");
                controller.debugMsg.appendText("✖ ziadost zamietnuta\n");
                retValue = -1;
            }
            socket.close();

        } catch (SocketException ex) {
            System.err.println("Chyba client.java: " + ex.getMessage());
            retValue = -1;
            controller.debugMsg.appendText("✖ ziadost zamietnuta\n");
            sendingThread.interrupt();
            //System.exit(1);
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            System.err.println("Chyba client.java: " + ex.getMessage());
            retValue = -1;
            controller.debugMsg.appendText("✖ ziadost zamietnuta\n");
            sendingThread.interrupt();            
            //System.exit(1);
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void stopSending() {
        sendingThread.interrupt();
        this.SENDING = false;
        socket.close();
    }

    private void sendLongData(String[] data, int count) {
        int i;
        try {
            // socket stay open until all messages was sent
            socket = new DatagramSocket(portToSend);
            controller.debugMsg.appendText("klient odoslal ziadost\n");
            for (i = 0; i < count; i++) {
                //encrypt
                //data[i] = rsa.encrypt(data[i]);
                
                buff = data[i].getBytes();
                DatagramPacket packet = new DatagramPacket(buff, buff.length, addressToSend, portToSend);
                socket.send(packet);
                socket.setSoTimeout(10000);
                DatagramPacket response = new DatagramPacket(responseBytes, responseBytes.length);
                socket.receive(response);
                // decrypt and control 
                //String receiveMsg = rsa.decrypt(new String(response.getData()));
                String receiveMsg = new String(response.getData());
                
                if(receiveMsg.contains("ODPOVED")) {
                    // pokracuje dalej
                } else {
                    System.err.println("bad receive part:  " + i + " long sms");
                    socket.close();
                    return;
                }
            }
            controller.debugMsg.appendText("✔ ziadost bola potvrdena\n");
            controller.debugMsg.appendText("All message was sent\n");
                   
            // after sending all messages
            socket.close();
        } catch (SocketException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
