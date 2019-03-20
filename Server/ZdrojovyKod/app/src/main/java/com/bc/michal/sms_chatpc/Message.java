package com.bc.michal.sms_chatpc;

/**
 * Created by Michal_ on 29.2.2016.
 * Model spravy
 *
 */
public class Message {
    private char head;
    private String body;
    private boolean reply;
    private String telNumber;

    public String getTelNumber() {
        return telNumber;
    }

    public void setTelNumber(String telNumber) {
        this.telNumber = telNumber;
    }

    public char getHead() {
        return head;
    }

    public void setHead(char head) {
        this.head = head;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isReply() {
        return reply;
    }

    public void setReply(boolean reply) {
        this.reply = reply;
    }
}
