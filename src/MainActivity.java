package com.example.michal.sms_chatpc;

import android.app.Activity;
import android.app.ActivityManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.content.*;

import com.example.michal.sms_chatpc.UDPCommunicator.AsyncTaskActivity;
import com.example.michal.sms_chatpc.UDPCommunicator.CallerTest;
import com.example.michal.sms_chatpc.telephony.*;

import org.w3c.dom.Text;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    String IPaddressPC = "192.168.137.1";
    private boolean mustStop = false;       // pre vytvorene spojenie
    private ProgressBar progress;
    private TextView text;
    private EditText textSpravy;
    private EditText textCislo;
    private Button connect;


    @Override
    public void onPause() {
        super.onPause();
        mustStop=true;  // zastavi nekonecne slucku
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progress = (ProgressBar) findViewById(R.id.progressBar1);
        text = (TextView) findViewById(R.id.textView);
        textSpravy = (EditText) findViewById(R.id.editText2);
        textCislo = (EditText) findViewById(R.id.editText);
        connect = (Button) findViewById(R.id.btnConnect);

        showAddr(); // zobrazi IP adresy s ktorymi pracuje


    }

    public void onClick(View view) {
        AsyncTaskExample task = new AsyncTaskExample();
        task.execute();
    }


    public void showToast(final String msg) {

        // najskor prijatu spravu rozoberie a zisti
        // co s nou bude robit

        //SenderMsg instance = new SenderMsg(this, msg);
        //Phoning call = new Phoning(this, msg);


        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
     * Funkcia na vypisanie vsetkych IP adries s akymi pracuje
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

        sourceIP.setText(IPaddressPC);
        destIP.setText(ipAddressPhone);
    }

    public void startProgress(View view) {
        // do something long
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <= 10; i++) {
                    final int value = i;


                    doFakeWork();


                    progress.post(new Runnable() {
                        @Override
                        public void run() {
                            text.setText("Updating");
                            progress.setProgress(value);
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }

    // Simulating something timeconsuming
    private void doFakeWork() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

/*
*  Asynchronna trieda na riesenie
*  synchronizacneho problemu - nevedel som zaroven udrziavat komunikaciu
*  a este k tomu aktualizovat GUI ( text sms spravy, tel.cislo na ktore posielam
*   spravu
*
* To use AsyncTask you must subclass it. AsyncTask uses generics and varargs.
* The parameters are the following AsyncTask <TypeOfVarArgParams , ProgressValue , ResultValue> .
*
*  An AsyncTask is started via the execute() method.
* The execute() method calls the doInBackground() and the onPostExecute() method.
* TypeOfVarArgParams is passed into the doInBackground() method as input,
* ProgressValue is used for progress information and ResultValue must be returned from doInBackground() method
* and is passed to onPostExecute() as a parameter.
* The doInBackground() method contains the coding instruction which should be performed in a background thread.
* This method runs automatically in a separate Thread.
* The onPostExecute() method synchronizes itself again with the user interface thread and allows it to be updated.
* This method is called by the framework once the doInBackground() method finishes.
* */

    private class AsyncTaskExample extends AsyncTask<Void, Integer, Message> {

        private final String TAG = AsyncTaskExample.class.getName();
        private int idx =0;
        final CallerTest call = new CallerTest();

        protected void onPreExecute() {
            Log.d(TAG, "On preExceute...");
        }

        protected Message doInBackground(Void... arg0) {
            final Message[] receive = {null};
            Log.d(TAG, "On doInBackground...");
            mustStop = false;
            while (!mustStop)   {
                try {
                    Log.d(TAG, "Starting receiving...");
                    Thread t = new Thread(new Runnable(){
                        public void run() {
                            try {
                                receive[0] = call.caller();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    t.start();
                    t.join();


                    Log.d(TAG, "Receive message from PC...");
                    final String textSMS = receive[0].getBody();
                    final String telPhone = receive[0].getTelNumber();

                    Log.d("HEAD: " + receive[0].getHead(), "SPRAVA: " + receive[0].getBody());
                    char sign = receive[0].getHead();
                    switch (sign) {
                        case 'S':
                            try {
                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(receive[0].getTelNumber(), null, receive[0].getBody(), null, null);
                                Log.d("DEBUG", "Sms bola poslana");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case 'T':
                            break;
                        case 'P':
                            Log.d("CALL_DEBUG", "Starting ringing");
                            try {
                                Intent callIntent = new Intent(Intent.ACTION_CALL);
                                String telNumber = receive[0].getTelNumber();
                                callIntent.setData(Uri.parse("tel:"+telNumber));
                                startActivity(callIntent);
                            } catch (ActivityNotFoundException e) {
                                e.printStackTrace();
                            }


                        break;
                    } // odpovedanie na spravu
                    call.callerResponse();
                } catch (Exception e) {
                   e.printStackTrace();
                }
                publishProgress(idx++);
            }
            return receive[0]; //spravu ktoru si prijal
        }

        protected void onProgressUpdate(Integer... a) {
            Log.d(TAG, "You are in progress update ... " + a[0]);
        }

        protected void onPostExecute(Message result) {
            textSpravy.setText(result.getBody());
            textCislo.setText(result.getTelNumber());
            Log.d(TAG, "Tuto spravu som poslal: " + result.getBody());
        }
    }



}// END OF MAIN CLASS

