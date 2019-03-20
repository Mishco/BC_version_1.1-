package com.bc.michal.sms_chatpc;

/**
 * Created by Michal_ on 28.4.2016.
 */
import java.math.BigInteger;

public class RSA {
    private static BigInteger one, p, q, E, D, n, P, Q;

    // create a big number
    public RSA() {
        p = new BigInteger("17");
        //System.out.println("Enter A's prime number: " + p);
        q = new BigInteger("19");
        //System.out.println("Enter B's prime number: " + q);
        n = p.multiply(q);
        P = p.subtract(BigInteger.ONE);
        Q = q.subtract(BigInteger.ONE);
        int x = 0;
        do {
            E = new BigInteger("65537");   // 65537
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
        System.out.println("Encrypting " + text);
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
        // cutting necessary text ( rubbish )
        text = text.substring(0,27);
        System.out.println("Decrypting " + text);
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

}
