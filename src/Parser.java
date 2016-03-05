package com.example.michal.sms_chatpc;

import android.text.AutoText;

/**
 * Created by Michal_ on 26.2.2016.
 * Trieda pre rozobratie a roztriedenie sprav
 */
public class Parser {

    public Parser() {

    }

    public Message LoadMsg(String text) {
        Message msg = new Message();
        msg.setHead(getFlagFromMsg(text));
        msg.setBody(getSMSfromMsg(text));
        msg.setTelNumber(getNumberFromMsg(text));
        msg.setReply(false);                        // zatial neodpovedane
        return msg;
    }

    public char getFlagFromMsg(String msg) {
        if (msg == null) {
            return '0';
        } else return msg.charAt(0);
    }


    public String getNumberFromMsg(String msg) {
        if (msg == null)
            return null;

        // ak ide o SMS spravu alebo telefony hovor
        if (msg.startsWith("S") || msg.startsWith("P")) {
            return msg.substring(1, 15);
        }
        // v opacnom pripade ide o technicku spravu
        return "Tech";
    }

    public String getSMSfromMsg(String msg) {
        return msg.substring(15, msg.length());
    }
}
