/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BC_sms_app_client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import javax.media.CaptureDeviceInfo;
import javax.media.cdm.CaptureDeviceManager;

/**
 *
 * @author Michal_
 */
public class FXMLController implements Initializable {

    @FXML
    private Button connect;
    @FXML
    private Button send;
    @FXML
    private ComboBox<String> combobox;
    @FXML
    protected TextArea debugMsg;
    @FXML
    private TextArea smsMsg;
    @FXML
    private Button addNum;
    @FXML
    private Button help;

//    private boolean isConnected;
    Stage stage;
    //   Server server;
    Client client;
//    VOIP voip;
    RSA rsa;
    //   private String displayName;
    public static InetAddress serverIP;
    public static Object syncLock = new Object();
    StringBuilder logTime;
    FileProces fileProces;
    Set<String> AllNumbers;
    /*ObservableList<String> telNumbers = FXCollections.observableArrayList(
     "0949207257", "+421915980458", "0917383317"
     );*/
    ObservableList<String> telNumbers = FXCollections.observableArrayList();
    StopWatch stopWatch;
    ArrayList<PhoneNumber> numbersList = new ArrayList();
    StringBuilder vsetkyKontakty = new StringBuilder();

    
    int port = 2226;                    // port na ktorom pocuvam
    int hostPort = 12345;               // port na ktory posielam    
    String addr = "192.168.137.28";     // aktualna IP adresa mobilneho zariadenia

    
    String textUsage = "If you like a create connection, which is necessery\n"
                       + " 1. start app in mobile (shared mobile SMS)\n"
                       + " 2. click on connect button on mobile\n"
                       + " 3. click on connect button in here, in PC application\n"
                       + "  After few second a connection will be created, if not can be"
                       + " Error in network(both devices must be in ONE Wifi network)\n"
                       + "\n\n"
                       + "If you like Send a SMS (with PSTN or GSM)\n"
                       + " 1. create a connection\n"
                       + " 2. choose a number to whom you send a SMS\n"
                       + " 3. write a message\n"
                       + " 4. click to send button or press ENTER key\n"
                       + " in success you will be informed about a send SMS\n"
                        ;
    /*
     *   Funkcia po vytvoreni okna
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        debugMsg.appendText("Welcome in Shared Mobile - Client side\n");

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Before Start");
        alert.setHeaderText(null);
        alert.setContentText("First must run app on mobile device.\n\nClick on connect button on mobile, after than click on connect on connect here.");
        alert.showAndWait();

        // disable tel numbers 
        // because they didn't use in create connection
        combobox.setVisible(false);

        // create StopWatch to catch time of piece of code
        stopWatch = new StopWatch("Using RSA crypting");
        logTime = new StringBuilder();

        // telNumbers are loading from File
        fileProces = new FileProces();
        telNumbers = fileProces.startOperator();

        // init comboBox
        combobox.setItems(telNumbers);
        // defaultne selected Item
        combobox.getSelectionModel().select(0);

        // sending SMS are avaible after 
        // creating a connection        
        send.setDisable(true);

        // add exist numbers to SET
        AllNumbers = new HashSet<String>();
        AllNumbers.addAll(telNumbers);

        // AZ KED je vytvorene spojenie
        // lambda expression
        // to catch ENTER key after write a sms text
        smsMsg.setOnKeyPressed((event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                try {
                    SendSmsHandler();
                } catch (IOException ex) {
                    Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        // initilizacia cryptovania
        rsa = new RSA();

    }

    /*
     *  Zobrazi aktualne vybrate cislo
     */
    private void showNum() {
        debugMsg.appendText(combobox.getSelectionModel().getSelectedItem());
        debugMsg.appendText("\n");
    }

    private String getNum() {
       
        return combobox.getSelectionModel().getSelectedItem();
    }

    /*
     *  Po stlaceni tlacidla CONNECT
     *  vykonava nasledovne akcie
     */
    @FXML
    private void handlerConnect() throws InterruptedException {
        System.out.println("starting communication");
        connect.setDisable(true);
        startComunication();
    }

    private void startComunication() throws InterruptedException {

        stopWatch.start("broadcast find server");
        // BROADCAST
        // odosle spravy co celej sieti
        // a najde klienta, ten mu odpovie a ziska tak jeho adresu
        Thread discoveryThread = new Thread(DiscoveryThread.getInstance());
        discoveryThread.start();

        //wait for discover thread
        synchronized (syncLock) {
            syncLock.wait();
        }
        stopWatch.stop();

        // tel number are available
        // after create connection
        combobox.setVisible(true);

        stopWatch.start("technical message");
        //System.out.println("SKUSKA: " + getServerIp());
        addr = getServerIp().getHostAddress();
        System.out.println("adresa zariadenia: " + addr);

        client = new Client();
        client.controller = this;
        client.portToSend = hostPort;
        client.setAddressToSend(addr);
        client.startSendingData("Technical report");

        // mal by pockat na jeho dokoncenie
        while (!client.sendingThread.isInterrupted()) {
            // ..
            // caka kym vlakno nie je dokocnene
        }

        if (client.retValue == -1) {
            debugMsg.appendText("Error with create connection");
            System.out.println("err");
        } else {
            System.out.println("okej");
            send.setDisable(false);
        }
        stopWatch.stop();
    }

    /*
     *   Odoslanie spravy na
     *   konkretne cislo
     */
    @FXML
    private void SendSmsHandler() throws IOException {
        stopWatch.start("send a sms file");

        // maximum length of a SMS 
        int maxLengthSms = 160;

        // get a text from GUI
        String textSms = smsMsg.getText();
        // empty message don't send
        if (textSms.equals("")) {
            debugMsg.appendText("Sms must contains some text\n");
            return;
        }

        // One SMS is max.length == 250 
        // if text of sms is to big
        // it must be correct split
        int allLength = textSms.length();
        if (allLength > maxLengthSms) {
            // sending a many of msg's
            String[] allmessages = new String[1000];
            int pocetOpakovani = allLength / maxLengthSms;
            //  System.out.println("#znakov = " + allLength);
            smsMsg.clear();
            int i = 0;
            // list of all msg's
            List<String> tmpMsgs = splitEqually(textSms, maxLengthSms);
            for (String oneSms : tmpMsgs) {
                Message smsToSend = new Message('L', oneSms, standardOfNum(getNum()));
                allmessages[i++] = smsToSend.toString();
            }
            // posledna sprava signalizujuca koniec
            allmessages[i++] = new Message('L', "000000000000000", standardOfNum(getNum())).toString();
            System.out.println("#allmess " + i);

            // send all message in open socket 
            client.controller = this;
            client.startSendingLongData(allmessages, i);

            debugMsg.appendText("==================\n");
            debugMsg.appendText("Text spravy:\r\n");
            debugMsg.appendText(textSms);
            debugMsg.appendText("\n==================");
            debugMsg.appendText("\nSprava bola odoslana na cislo: \n");

            System.out.println("pocet " + pocetOpakovani);

        } else {

            // sending only one message
            System.out.println("Odosielam sms spravu");
            smsMsg.clear();
            String telNum = getNum();

            Message sms = new Message('S', textSms, standardOfNum(telNum));

            client.controller = this;
            client.startSendingData(sms.toString());

            debugMsg.appendText("==================\n");
            debugMsg.appendText("Text spravy:\r\n");
            debugMsg.appendText(textSms);
            debugMsg.appendText("\n==================");
            debugMsg.appendText("\nSprava bola odoslana na cislo: \n");

            showNum();
        }
        stopWatch.stop();
    }

    /*
     *   Metoda na vytvorenie standardneho 
     *   formatu cisla 00421...
     */
    private String standardOfNum(String num) {
        String PhoneNum = new String();
        PhoneNum = num;

        if (num.startsWith("+")) {
            PhoneNum = "00" + num.substring(1, num.length());
        } else if (!num.startsWith("0", 1)) {
            // pridanie predvolby 
            // zatial SLOVENSKA, neskor vyber
            PhoneNum = "00421" + num.substring(1, num.length());
        }
        return PhoneNum;
    }

    /*
     *  Metoda na overenie zadaneho cisla
     *  povolene su iba znaky
     *  0123456789+
     */
    private boolean validNumber(String result) {
        // lambda expression
        // fast and simple.
        boolean allDigit = result.chars().allMatch(x -> Character.isDigit(x));
        // if a number contains
        // '+'
        boolean signPlus = !Pattern.matches("^+[0-9]*$", result);
        int length = result.length();
        // the 'smallest' version of telephone number must have 10 or more digit 
        if (length < 10) {
            return false;
        }

        // A or B
        return allDigit | signPlus;
    }

    /*
     *   Metoda na rozdelenie spravy podla
     *   definovanej dlzky
     */
    public static List<String> splitEqually(String text, int size) {
        // Give the list the right capacity to start with. You could use an array
        // instead if you wanted.
        List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);

        for (int start = 0; start < text.length(); start += size) {
            ret.add(text.substring(start, Math.min(text.length(), start + size)));
        }
        return ret;
    }

    /*
     *   Funkcia na pridanie 
     *   noveho cisla v lubovolnom tvare
     *   kontrola iba cisel a znaku '+'  
     */
    /*
     KVOLI TEJTO METODE
     SOM MUSEL AKTUALIZOVAT JAVA_jdk_NA 1.8.0_73 A TU ZMENU NASTAVIT
     AJ V NETBEANSe DEFAULNTE NASTAVENU JAVU
    
     .Close NetBeans if it is running.
     .Find the file C:\Program Files\NetBeans 7.3\etc\netbeans.conf
     .Change the value for the parameter "netbeans_jdkhome" to the desired JDK version
     E.g.: netbeans_jdkhome="C:\Program Files\Java\jdk1.7.0_21"
     .Save the file and start NetBeans again.

    
     KTORY POMAHA SPRACOVAVAT A VYTVARAT
     DIALOGOVE OKNA
    
     */
    @FXML
    private void addNewNumber() {

        //debugMsg.appendText("Pridat nove cislo\n\r");
        TextInputDialog dialog = new TextInputDialog("0949 ...");
        dialog.setTitle("Add new number");
        dialog.setHeaderText("Add new number");
        dialog.setContentText("Please, write new number: ");

        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        stopWatch.start("add a new number");

        // check if a valid number 
        // cislo musi byt iba zo znakov[0123456789+]
        if (!validNumber(result.get())) {
            debugMsg.appendText("Invalid input number.\nMust contains only digit or '+'\n");
            stopWatch.stop();
            return;
        }

        // if this number is not in set
        if (AllNumbers.add(result.get())) {
            if (result.isPresent()) {
                System.out.println("Your number: " + result.get());
                debugMsg.appendText("Add a new number: " + result.get() + "\n");
            }

            // adding in a file
            fileProces.fileOperator(1, AllNumbers, 0, result.get());

            // kontrola spravnosti cisla
            // kontrola opakovania cisla v roznom tvare a ulozenie v jednotnom formate
            // zapis do externeho suboru cisla + vytvorenie citania zo suboru pri init()
            // http://www.tutorialspoint.com/java/java_set_interface.htm
            telNumbers.add(result.get());
            combobox.setItems(telNumbers);

            // create and select last added
            combobox.getSelectionModel().select(result.get());
            stopWatch.stop();
            return;
        }
        // new number is in set
        // return without change
        debugMsg.appendText("The number " + result.get() + " is already in list\n");

        // The Java 8 way to get the response value (with lambda expression).
        result.ifPresent(name -> System.out.println("Your name: " + name));
        stopWatch.stop();
    }

    protected void setStage(Stage stage) {
        this.stage = stage;
    }

    public static void setServerIp(InetAddress address) {
        serverIP = address;
    }

    public InetAddress getServerIp() {
        return serverIP;
    }

    @FXML
    private void showHelp() {
        // System.out.println("Autor: Michal Slovík");
        //  System.out.println("About: Klient strana aplikacie");
        //  System.out.println("       ");

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Shared Mobile");
        alert.setHeaderText("Welcome Shared Mobile app");
        alert.setContentText("Choose your option.");

        ButtonType buttonTypeOne = new ButtonType("Author");
        ButtonType buttonTypeTwo = new ButtonType("About");
        ButtonType buttonTypeThree = new ButtonType("Usage");
        ButtonType buttonTypeFour = new ButtonType("Get Contacts");
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeTwo, buttonTypeThree, buttonTypeFour, buttonTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonTypeOne) {
            // ... user chose "Author"
            alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Author");
            alert.setHeaderText(null);
            alert.setContentText("Author this app is Michal Slovik!\nIt was created in bachelor thesis");
            alert.showAndWait();
        } else if (result.get() == buttonTypeTwo) {
            // ... user chose "About"
            alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("About");
            alert.setHeaderText(null);
            alert.setContentText("App Shared Mobile-Client side\nThis app can connect to a server(mobile).\nAfter created connection can send plenty of sms to anybody.");
            alert.showAndWait();

        } else if (result.get() == buttonTypeThree) {
            // ... user chose "Usage"
            alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Usage");
            alert.setHeaderText(null);
            alert.setContentText(textUsage);
            alert.showAndWait();

                
            
        } else if (result.get() == buttonTypeFour) {
            // odosli ziadost o kontakty 
            getContants();

        } else {
            // ... user chose CANCEL or closed the dialog
        }
    }

    private void getContants() {
        if (send.isDisable() == true) {
            System.out.println("nie je spojenie");
            return;
        }

        // sending only one message
        System.out.println("Odosielam ziadost o kontakty");
     
        String textSms = "POSLI kontakty";
        Message contactM = new Message('C', textSms, "000000000000000" );

        client.controller = this;
        client.startSendingData(contactM.toString());
        debugMsg.appendText("Poslal som ziadost o kontakty\n");

    }

    @FXML
    private void exit() {
        FileWriter wr = null;
        try {
            System.out.println(stopWatch.prettyPrint());
            logTime.append(stopWatch.prettyPrint());
            // saved the log file
            File fac = new File("time.log");
            if (!fac.exists()) {
                try {
                    fac.createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            wr = new FileWriter(fac, true);
            try (BufferedWriter bufferedWriter = new BufferedWriter(wr)) {
                bufferedWriter.write(logTime + " \n");
            }
            // and exit with correct value
            Platform.exit();
            System.exit(0);
        } catch (IOException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                wr.close();
            } catch (IOException ex) {
                Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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
}
