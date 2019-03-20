package com.bc.michal.sms_chatpc.telephony;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.bc.michal.sms_chatpc.MainActivity;
import com.bc.michal.sms_chatpc.R;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Created by Michal_ on 16.3.2016.
 */
public class MakeCallActivity extends Activity {
    static final String LOG_TAG = "MAKE_CALL";

    private String phoneNumber;
    private String call_run;
    private String contactIp;

    private boolean IN_CALL = true;
    private boolean LISTEN = true;

    private static final int BUF_SIZE = 1024;
    private static final int BROADCAST_PORT = 2226;//50002;

    private AudioCall call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_sms);

        Intent intent = getIntent();
        phoneNumber = intent.getStringExtra(MainActivity.PHONE_NUM);
        call_run = intent.getStringExtra(MainActivity.CALL_RUN);
        contactIp = intent.getStringExtra(MainActivity.EXTRA_IP);
       // IN_CALL = Boolean.parseBoolean(call_run);

        Log.i(LOG_TAG, "hovor bude prebiehat -> " + call_run);
        Log.i(LOG_TAG, "s cislom: " + phoneNumber);

        startListener();
        makeCall();
    }

    private void startListener() {
        LISTEN = true;

        final Thread listenThread = new Thread(new Runnable() {

            @Override
            public void run() {

                while (IN_CALL) {
                    Log.i(LOG_TAG, "Listener started!");
                    DatagramSocket socket = null;
                    try {
                      socket = new DatagramSocket(BROADCAST_PORT);
                      socket.setSoTimeout(2000);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                    byte[] buffer = new byte[BUF_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
                    while(LISTEN) {
                        try {
                            Log.i(LOG_TAG, "Listening for packets");
                            socket.receive(packet);
                            String data = new String(buffer, 0, packet.getLength());
                            Log.i(LOG_TAG, "Packet received from "+ packet.getAddress() +" with contents: " + data);
                            String action = data.substring(0, 4);
                            if(action.equals("REQ:")) {
                                // Accept notification received. Start call
                                call = new AudioCall(packet.getAddress());
                                call.startCall();
                                IN_CALL = true;
                            }
                            else if(action.equals("REJ:")) {
                                // Reject notification received. End call
                                endCall();
                            }
                            else if(action.equals("END:")) {
                                // End call notification received. End call
                                endCall();
                            }
                            else {
                                // Invalid notification received
                                Log.w(LOG_TAG, packet.getAddress() + " sent invalid message: " + data);
                            }
                        }
                        catch(SocketTimeoutException e) {
                            if(!IN_CALL) {
                                Log.i(LOG_TAG, "No reply from contact. Ending call");
                                endCall();
                                return;
                            }
                        }
                        catch(IOException e) { }
                    }
                }
            }
        });
        listenThread.start();
    }

    private void makeCall() {
// Send a request to start a call
        sendMessage("CAL: ", 50000);
    }

    private void endCall() {
        // Ends the chat sessions
        //stopListener();
        LISTEN = false;
        if(IN_CALL) {

            call.endCall();
        }
        sendMessage("END:", BROADCAST_PORT);
        finish();
    }

    private void sendMessage(final String message, final int port) {
        // Creates a thread used for sending notifications
        Thread replyThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    InetAddress address = InetAddress.getByName(contactIp);
                    byte[] data = message.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                    socket.send(packet);
                    Log.i(LOG_TAG, "Sent message( " + message + " ) to " + contactIp);
                    socket.disconnect();
                    socket.close();
                }
                catch(UnknownHostException e) {

                    Log.e(LOG_TAG, "Failure. UnknownHostException in sendMessage: " + contactIp);
                }
                catch(SocketException e) {

                    Log.e(LOG_TAG, "Failure. SocketException in sendMessage: " + e);
                }
                catch(IOException e) {

                    Log.e(LOG_TAG, "Failure. IOException in sendMessage: " + e);
                }
            }
        });
        replyThread.start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_send_sms, menu);
        return true;
    }


}
