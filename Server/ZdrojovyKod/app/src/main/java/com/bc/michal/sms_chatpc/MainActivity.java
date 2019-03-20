package com.bc.michal.sms_chatpc;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.format.Formatter;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {
    static final String LOG_TAG = "UDPchat";
    String msg = "Android : ";

    private TextView text;
    private EditText textSpravy;
    private EditText textCislo;
    private Button connect;
    private Button disconnect;
    private TextView textDebug;
    private Button exitBtn;

    private boolean LISTEN = false;                      // ak zacne pocuvat
    private boolean STARTED = false;

    private static final int LISTENER_PORT = 12345;      // port na ktorom pocuva( dostava spravy)
    private static final int BUF_SIZE = 1024;            // velkost prijmanych sprav

    public final static String PHONE_NUM = "com.exmaple.michal.sms_chatpc.SenderMsg";
    public final static String SMS_MSG = "com.example.michal.sms_chatpc.SenderMsg";
    public final static String CALL_RUN = "com.example.michal.sms_chatpc.telephony.MakeCallActivity";
    public final static String EXTRA_IP = "com.example.michal.sms_chatpc.telephony.MakeCallActivity";
    public static String IPaddressPC = "192.168.0.1";

    public static final Object syncLock = new Object();          // na riesenie synchronizacie
    public static StringBuilder longSms = new StringBuilder();
    public static boolean success = false;
    public static RSA rsa;
    // pri broadcast-e
    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.textView);
        textDebug = (TextView) findViewById(R.id.debugVypis);
        textSpravy = (EditText) findViewById(R.id.editText2);
        textCislo = (EditText) findViewById(R.id.phoneEditText);
        connect = (Button) findViewById(R.id.btnConnect);
        disconnect = (Button) findViewById(R.id.btnDisConnect);
        exitBtn = (Button) findViewById(R.id.exitBtn);

/*
        String crypText = rsa.encrypt("ahoj");
        System.out.println(crypText);
        String orig = rsa.decrypt(crypText);
        System.out.println(orig);
*/

//        displayContacts();


        disconnect.setEnabled(false);

        connect.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           Log.i(LOG_TAG, "Connect button pressed");
                                           textDebug.setText("HLADAM");
                                           STARTED = true;
                                           disconnect.setEnabled(true);
                                           connect.setEnabled(false);

                                           Thread discoveryThread = new Thread(DiscoveryThread.getInstance());
                                           discoveryThread.start();

                                           //wait for discover thread
                                           synchronized (syncLock) {
                                               try {
                                                   syncLock.wait();
                                               } catch (InterruptedException e) {
                                                   e.printStackTrace();
                                               }
                                           }

                                           // didn't find any client broadcast
                                           if (!success) {
                                               Toast.makeText(MainActivity.this, "Didn't find any client", Toast.LENGTH_LONG).show();
                                               onRestart();
                                               finish();
                                           }
                                           showAddr(); // zobrazi IP adresy s ktorymi pracuje
                                           startCallListener();
                                       }
                                   }
        );


        disconnect.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View v) {
                                              Log.i(LOG_TAG, "Disconnect button pressed");
                                              STARTED = false;
                                              stopCallListener();
                                              disconnect.setEnabled(false);
                                              connect.setEnabled(true);

                                          }
                                      }
        );

        exitBtn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           Log.i("EXIT", "goodbey");
                                           finish();
                                           return;
                                       }
                                   }
        );
          

    }
    /** Called when the activity is no longer visible. */
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(msg, "The onStop() event");
    }

    /** Called just before the activity is destroyed. */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(msg, "The onDestroy() event");
    }

    /*
     * Write all IP address with app working
     */
    public void showAddr() {
        WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        String ipAddressPhone = Formatter.formatIpAddress(ip);

        // editText3  - ip SOURCE
        // editText4  - ip DESTINATION - this phone
        TextView sourceIP = (TextView) findViewById(R.id.editText3);
        TextView destIP = (TextView) findViewById(R.id.editText4);

        if (ipAddressPhone.contains("0.0.0.0"))
            Toast.makeText(this, "Device is not connected to Wifi", Toast.LENGTH_LONG).show();

        sourceIP.setText(IPaddressPC);
        destIP.setText(ipAddressPhone);
    }

    /*
    * Call listener
    * waiting for receive packet
    * and work on
    */
    private void startCallListener() {
        LISTEN = true;
        Thread listener = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i("CALL_LISTENER:", "Prichadzajuci call listener started");
                    DatagramSocket socket = new DatagramSocket(LISTENER_PORT);
                    socket.setSoTimeout(1000);
                    byte[] buff = new byte[BUF_SIZE];
                    DatagramPacket packet = new DatagramPacket(buff, BUF_SIZE);



                    while (LISTEN) try {
                        Log.i("CALL_LISTENER", "Listening for incoming");
                        // waiting for message
                        socket.receive(packet);

                        // crypting started
                      //  rsa = new RSA();
                     //   String data = rsa.decrypt(new String(buff));
                   //     System.out.println("decrypt: " + data);
                        String data = new String(buff, 0, packet.getLength());
                        Log.i("CALL_LISTENER", "Packet receiv from " + packet.getAddress() + " with content: " + data);
                        // message starting with this sign
                        String action = data.substring(0, 1);

                        // if message starting 'P'
                        if (action.equals("P")) {
                            // prepared for phone call
                            Log.d(LOG_TAG, "This function is not supported");
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Call is not supported", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        // starting message with 'S'
                        // only one sms
                        if (action.equals("S")) {                                              // receiv sms message request
                            Log.w(LOG_TAG, packet.getAddress() + " sent sms msg: " + data);
                            Log.d("PAKET", data);

                            Parser parser = new Parser();
                            final Message receiveMsg = parser.LoadMsg(data);

                            Intent intent = new Intent(MainActivity.this, SenderMsg.class);
                            intent.putExtra(PHONE_NUM, receiveMsg.getTelNumber());
                            intent.putExtra(SMS_MSG, receiveMsg.getBody());
                            startActivity(intent);

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    TextView cislo = (TextView) findViewById(R.id.phoneEditText);
                                    cislo.setText(receiveMsg.getTelNumber());

                                    TextView sprava = (TextView) findViewById(R.id.editText2);
                                    sprava.setText(receiveMsg.getBody());

                                    Toast.makeText(MainActivity.this, "Sms bola poslana", Toast.LENGTH_SHORT).show();
                                }
                            });
                            String msg = "S: ODPOVED, poslal som sms";

                            // crypting response message
                            //msg = rsa.encrypt(msg);

                            byte[] buff2 = msg.getBytes();

                            packet.setData(buff2);
                            socket.send(packet);
                        }
                        // long SMS - many sms
                        if (action.equals("L")) {
                            //Log.w(LOG_TAG, packet.getAddress() + " sent sms msg: " + data);
                            Log.d("PAKET", data);
                            Parser parser = new Parser();
                            final Message receiveMsg = parser.LoadMsg(data);
                            // add to existing messages
                            if (!receiveMsg.getBody().equals("000000000000000"))
                                // uz prijal vsetky spravy zacne ich odosielat
                                if (receiveMsg.getBody().equals("000000000000000")) {
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            TextView cislo = (TextView) findViewById(R.id.phoneEditText);
                                            cislo.setText(receiveMsg.getTelNumber());

                                            TextView sprava = (TextView) findViewById(R.id.editText2);
                                            sprava.setText("too long Sms to show");

                                            Toast.makeText(MainActivity.this, "Sms bola poslana", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    System.out.println(longSms.toString());
                                    SmsManager smsManager = SmsManager.getDefault();
                                    ArrayList<String> parts = smsManager.divideMessage(longSms.toString());
                                    smsManager.sendMultipartTextMessage(receiveMsg.getTelNumber(), null, parts, null, null);
                                }
                            String msg = "L: ODPOVED, poslal som sms";

                            //crypt response
                            //msg = rsa.encrypt(msg);

                            packet.setData(msg.getBytes());
                            socket.send(packet);
                        }
                        // message start with 'T'
                        // technical message between server and client
                        if (action.equals("T")) {
                            Log.w(LOG_TAG, packet.getAddress() + " tech msg: " + data);

                            final String showMsg = data;
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    TextView textV = (TextView) findViewById(R.id.debugVypis);
                                    textV.setText("SPOJENE");

                                    Toast.makeText(MainActivity.this, showMsg, Toast.LENGTH_SHORT).show();
                                }
                            });
                            String msg = "T: ODPOVED, technicka sprava";
                            // crypted

                            //msg = rsa.encrypt(msg);

                            packet.setData(msg.getBytes());
                            socket.send(packet);
                        }
                        // ziadost o kontakty
                        if (action.equals("C")) {
                            ArrayList<PhoneNumber> contakty = displayContacts();
                            StringBuilder vsetkyKontakty = new StringBuilder();
                            for(int i=0; i < contakty.size(); i++) {
                                vsetkyKontakty.append(contakty.get(i).name + "," +contakty.get(i).number + ";");
                            }

                            if (vsetkyKontakty.length() < 1500) {
                                packet.setData(("C" + vsetkyKontakty).getBytes());
                                socket.send(packet);
                            } else {
                                //odoslania viacerych paketov
                                // ale najskor upozornit aby klient
                                //cakal viac paketov
                            }


                        }


                        packet = new DatagramPacket(buff, BUF_SIZE);                    // novy paket pre dalsie pocuvanie

                    } catch (Exception ignored) {
                    }
                    Log.i(LOG_TAG, "Call listener ending");
                    socket.disconnect();
                    socket.close();
                } catch (SocketException e) {
                    Log.e(LOG_TAG, "SocketException in listener " + e);
                }
            }
        });
        listener.start();
    }

    private void stopCallListener() {
        LISTEN = false;
        textDebug.setText("Odpojene");
    }

    private ArrayList<PhoneNumber>  displayContacts() {

        ArrayList<PhoneNumber> numbersList = new ArrayList();

        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (Integer.parseInt(cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        System.out.println("Name: " + name + ", Phone No: " + phoneNo);
                        numbersList.add(new PhoneNumber(name, phoneNo));
                        //Toast.makeText(MainActivity.this, "Name: " + name + ", Phone No: " + phoneNo, Toast.LENGTH_SHORT).show();
                    }
                    pCur.close();
                }
            }
        }
        return numbersList;
    }
    // inner class
    public class PhoneNumber {
        String name;  //the representation like "Call Home"
        String number;//the actual phone number like "225-5466"

        //constructor ...
        public PhoneNumber(String name, String number) {
            this.name = name;
            this.number = number;
        }
    }

}// END OF MAIN CLASS

