package BC_sms_app_client;

/**
 * Model of default message
 *
 * @author Michal_
 */
public class Message {

    private char head;    // hlavicka kazdej spravy
    private String body;    // telo spravy
    private boolean reply;  // priznak na zistenie odpovede
    private String telNum;  // cislo na ktore posiela spravu / hovor
    
    /*
    *   
    *   @param head - priznak hlavicky{S , T}
    *   @param body - telo spravy
    *   @param telNum - telefonne cislo
    */
    public Message(char head, String body, String telNum) {
        this.head = head;
        this.body = body;
        this.telNum = telNum;
    }
    
     /*
    *   
    *   @param head - priznak hlavicky{S , T}
    *   @param body - telo spravy
    *   @param telNum - telefonne cislo
    *   @param reply - priznak pre prijatu alebo neprijatu odpoved
    */
    public Message(char head, String body, String telNum, boolean reply) {
        this.head = head;
        this.body = body;
        this.telNum = telNum;
        this.reply = reply; // hodnota pri vytvoreni bude false vo vacsine pripadov
    }
    
    
    
    public String getTelNum() {
        return telNum;
    }

    public void setTelNum(String telNum) {
        this.telNum = telNum;
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
    
    @Override
    public String toString() {
        String tmp = head+telNum+body;
        return tmp;
    }

}
