package com.bc.michal.sms_chatpc;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.Menu;
import android.telephony.SmsManager;
import android.util.Log;

/**
 * Created by Michal_ on 25.2.2016.
 */
public class SenderMsg extends Activity {
    static final String LOG_TAG = "SENDER_SMS";

    private String phoneNumber;
    private String textSms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_sms);

        Intent intent = getIntent();
        phoneNumber = intent.getStringExtra(MainActivity.PHONE_NUM);
        textSms = intent.getStringExtra(MainActivity.SMS_MSG);

        Log.i(LOG_TAG, "Odosielanie sms spravy");
        Log.i(LOG_TAG, "Phone number: " + phoneNumber);
        Log.i(LOG_TAG, "Text sms: " + textSms);
        sendSms();
    }

    private void sendSms() {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, textSms, null, null);//odoslanie sms
        Log.i(LOG_TAG, "SMS correctly sent ");
        finish();                                                          // ukoncenie tejto aktivity
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_send_sms, menu);
        return true;
    }

}
