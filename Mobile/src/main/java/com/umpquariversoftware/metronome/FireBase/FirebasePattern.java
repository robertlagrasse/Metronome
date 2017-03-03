package com.umpquariversoftware.metronome.FireBase;

/**
 * Class to represent Pattern in Firebase
 */

public class FirebasePattern {

    private String name;
    private String signature;

    public FirebasePattern() {
    }

    public FirebasePattern(String name, String signature) {
        this.name = name;
        this.signature = signature;
    }

    public String getSignature() {
        return signature;
    }

    @SuppressWarnings("unused")
    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public void setName(String name) {
        this.name = name;
    }
}
