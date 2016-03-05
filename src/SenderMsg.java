package com.example.michal.sms_chatpc;

import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import static android.widget.Toast.*;

/**
 * Created by Michal_ on 25.2.2016.
 */
public class SenderMsg {

    public Activity activity;               // atribut spojeni s hlavnou aktivitou

    SenderMsg(Activity _activity, final String msg, final String telNum) {
        this.activity = _activity;
        recevingAndSendingNextSMS(msg, telNum);
    }

    /* Precita spravu, ktoru prijal od PC
    *  a ziska cislo na ktore to ma poslat
    *  a odosle sms spravu ktora nasledovala
    *  za tel. cislom
    * */
    public void recevingAndSendingNextSMS(String msg, String telNum) {
        Log.i("Prepare SMS", "");
        //Parser par = new Parser();

       // String phoneNo = par.getNumberFromMsg(msg);
        showPhoneNo(telNum);

        //String aloneMsg = par.getSMSfromMsg(msg);
        showAloneMsg(msg);

        SendSMS(telNum, msg);
    }

    /*
    * Vykonanie samotneho odoslania spravy
    * na konkretne tel. cislo
    * */
    public void SendSMS(String Number, String msg) {
        if (msg == null)
            return;

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(Number, null, msg, null, null);
            Log.d("DEBUG", "Sms bola poslana");
            showSuccesDialog();
            // daj vediet SERVERu o uspesnom odoslani spravy
        } catch (Exception e) {
            this.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity.getApplication(), "SMS faild, please try again.", Toast.LENGTH_LONG).show();
                }
            });
            // informuj SERVER o neuspesnom odoslani spravy
            e.printStackTrace();
        }
    }

    private void showSuccesDialog() {
        final EditText editText = (EditText) this.activity.findViewById(R.id.editText2);
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity.getApplication(), "Sms bola uspesna", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showPhoneNo(final String num) {
        final EditText editText = (EditText) this.activity.findViewById(R.id.editText);
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editText.setText(num, TextView.BufferType.EDITABLE);
            }
        });
    }

    private void showAloneMsg(final String msg) {
        final EditText editText = (EditText) this.activity.findViewById(R.id.editText2);
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editText.setText(msg, TextView.BufferType.EDITABLE);
            }
        });
    }

}
