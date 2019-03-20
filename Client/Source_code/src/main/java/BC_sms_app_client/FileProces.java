package BC_sms_app_client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * @author Michal_
 * 
 * Class for process with any file
 * opening, reading and writing to a file
 */
public class FileProces {
    /*
     * Constructor(numbers, count)
     * numbers - array of string's with telephone numbers
     * count - count of array of numbers
     */
    public FileProces(String[] numbers, int count) throws IOException {
        CreateFile(numbers, count);
    }

    
    /*
     * Constructor(number)
     * number - one telephony number to add to exist list
     */
    public FileProces(String number) throws IOException {
        addToFile(number);
    }

    
    /*
     * Constructor()
     * read a numbers from file
     */
    public FileProces() {
        try {
            readFile();
        } catch (IOException ex) {
            Logger.getLogger(FileProces.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // create a file if does not exist
    private void CreateFile(String[] numbers, int count) throws FileNotFoundException, IOException {
        // working with file of name 'numbers'
        // if this file does not exist, system create them
        // else write a numbers to a file, until count
        File fac = new File("numbers");
        if (!fac.exists()) {
            fac.createNewFile();
        }
        try (FileWriter wr = new FileWriter(fac)) {
            for (int i = 0; i < count; i++) {
                wr.append(numbers[i] + " \r\n");
            }
        }
    }

    /*  
     * add one number to a file 'numbers'
     * if this file does not exist 
     *  than write error
     * else write a new number to a last position
     */
    private void addToFile(String number) throws FileNotFoundException, IOException {
        File fac = new File("numbers");
        if (!fac.exists()) {
            System.err.println("Error with file");
            return;
        }
        FileWriter wr = new FileWriter(fac, true);
        try (BufferedWriter bufferedWriter = new BufferedWriter(wr)) {
            bufferedWriter.write(number + "\n");
        }
    }

    /*
     * Reading from file 
     * until a line is not null
     */
    private void readFile() throws FileNotFoundException, IOException {
        System.out.println("Core of file numbers");
        //read from a file
        BufferedReader br = new BufferedReader(new FileReader("numbers"));
        String sCurrentLine;
        while ((sCurrentLine = br.readLine()) != null) {
            System.out.println(sCurrentLine);
        }
    }

    public void fileOperator(int type, Set<String> numbers, int count, String number) {
        String[] array = numbers.toArray(new String[0]);
        try {
            switch (type) {
                case 0:
                    // read from file
                    FileProces f = new FileProces();
                    break;
                case 1:
                    // file exist and add a new number to a file
                    FileProces f2 = new FileProces(number);
                    break;
                case 2:
                    // if File does not exist
                    // create and fill it
                    FileProces f3 = new FileProces(array, count);
                    break;
            }
        } catch (IOException ex) {
            Logger.getLogger(FileProces.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /* 
     * Method startOperator()
     * return a list of telephony number's
     * value ObservableList<String>, which is 
     * need in to ComboBox
     */
    public ObservableList<String> startOperator() {
        BufferedReader br = null;
        ObservableList<String> telNumbers = FXCollections.observableArrayList();
        try {
            br = new BufferedReader(new FileReader("numbers"));
            //file don't exists
            if (!br.ready())
                return null;
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                telNumbers.add(sCurrentLine);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileProces.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileProces.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(FileProces.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return telNumbers;
    }

}
