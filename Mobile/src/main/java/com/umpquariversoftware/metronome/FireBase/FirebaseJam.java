package com.umpquariversoftware.metronome.FireBase;

import com.umpquariversoftware.metronome.elements.Jam;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Class to represent Jam in Firebase
 */

public class FirebaseJam {
    private int tempo;
    private String name;
    private String kit;
    private String pattern;
    private String signature;


    @SuppressWarnings("unused")
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

    public FirebaseJam(String name, @SuppressWarnings("SameParameterValue") int tempo, @SuppressWarnings("SameParameterValue") String kit, String pattern) {
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

    @SuppressWarnings("unused")
    public void setTempo(int tempo) {
        this.tempo = tempo;
    }

    public String getKit() {
        return kit;
    }

    @SuppressWarnings("unused")
    public void setKit(String kit) {
        this.kit = kit;
    }

    public String getPattern() {
        return pattern;
    }

    @SuppressWarnings("unused")
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getSignature() {
        return signature;
    }

    private static String md5(String s) {
        try {

            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

}
