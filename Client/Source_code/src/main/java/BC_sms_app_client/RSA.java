package BC_sms_app_client;

/**
 * 
 * @author Michal_
 * 
 * Class for encrypt and decrypt 
 * a every message to send over network
 * 
 * RSA using a math library (java.math.BigInteger)
 * to save a big value. For simple I use only small,
 * because big numbers(like 8. Mersen's number is big) are 
 * pretending to a slow running of program
 * 
 *
 */
import java.math.BigInteger;

public class RSA {

    private static BigInteger one, p, q, E, D, n, P, Q;

    /*
     *  Constructor to
     *  create a big number
     *  and inicialize other numbers
     */
    public RSA() {
        
        // First number of RSA
        p = new BigInteger("17");
        //System.out.println("Enter A's prime number: " + p);
        //q = new BigInteger("19");
        // 8 Mersenove cislo
        //2147483647 	
       
        // second number of RSA
        q = new BigInteger("19");
        //System.out.println("Enter B's prime number: " + q);
        n = p.multiply(q);
        P = p.subtract(BigInteger.ONE);
        Q = q.subtract(BigInteger.ONE);
        
        // finding a number witch is 
        int x = 0;
        do {
            // optimal number
            // optimal, because is to big to safety and to
            // small to process
            E = new BigInteger("65537"); // 65537
            //System.out.println("Enter Public key: " + E);
            if (((P.gcd(E)).equals(BigInteger.ONE)) && ((Q.gcd(E)).equals(BigInteger.ONE))) {
                x++;
            }
        } while (x == 0);
        
        for (int i = 1;; i++) {
            D = new BigInteger(String.valueOf(i));
            if (((D.multiply(E)).mod(P.multiply(Q))).equals(BigInteger.ONE)) {
                break;
            }
        }
    }

    public static String encrypt(String text) {
      //  System.out.println("Encrypting " + text);
        String in = "", out = ""; // text = t.nextLine();
        for (int i = 0; i < text.length(); i++) {
            BigInteger T = new BigInteger(String.valueOf((int) (text.charAt(i)))), O, TF;
            O = T.modPow(E, n);
            out += (char) O.intValue();
            TF = O.modPow(D, n);
            in += (char) TF.intValue();
        }
        return out;
    }

    public static String decrypt(String text) {
     //   System.out.println("Decrypting " + text);
        String in = "", out ="";
        for (int i = 0; i < text.length(); i++) {
            BigInteger T = new BigInteger(String.valueOf((int) (text.charAt(i)))), O, TF;
            O = T.modPow(E, n);
            out += (char) O.intValue();
            TF = O.modPow(D, n);
            in += (char) TF.intValue();
        }
        return out;
    }
    
    /*
     * Test method to RSA encrypt and decrypt
     * it can be run self (shift + f6)
     */
    public static void main(String args[]) {
        RSA rsa = new RSA();
        System.out.println("ahoj");
        String crypText = rsa.encrypt("ahoj");
        System.out.println(crypText);
        String orig = rsa.decrypt(crypText);
        System.out.println(orig);
    }

}
