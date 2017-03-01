package com.umpquariversoftware.metronome.FireBase;

import com.umpquariversoftware.metronome.elements.Jam;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by robert on 2/20/17.
 */

public class FirebaseJam {
    int tempo;
    String name;
    String kit;
    String pattern;
    String signature;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public FirebaseJam(int tempo, String kit, String pattern) {
        this.tempo = tempo;
        this.kit = kit;
        this.pattern = pattern;
        this.signature = md5(String.valueOf(tempo)
                .concat("|")
                .concat(kit)
                .concat("|")
                .concat(pattern));
    }

    public FirebaseJam(String name, int tempo, String kit, String pattern) {
        this.name = name;
        this.tempo = tempo;
        this.kit = kit;
        this.pattern = pattern;
        this.signature = md5(String.valueOf(tempo)
                .concat("|")
                .concat(kit)
                .concat("|")
                .concat(pattern));
    }

    public FirebaseJam() {

    }

    public FirebaseJam(Jam jam) {
        this.tempo = jam.getTempo();
        this.kit = jam.getKit().getSignature();
        this.pattern = jam.getPattern().getPatternHexSignature();
        this.signature = md5(String.valueOf(tempo)
                .concat("|")
                .concat(kit)
                .concat("|")
                .concat(pattern));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTempo() {
        return tempo;
    }

    public void setTempo(int tempo) {
        this.tempo = tempo;
    }

    public String getKit() {
        return kit;
    }

    public void setKit(String kit) {
        this.kit = kit;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    private static String md5(String s) {
        try {

            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

}
