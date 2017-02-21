package com.umpquariversoftware.metronome.FireBase;

/**
 * Created by robert on 2/21/17.
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

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
