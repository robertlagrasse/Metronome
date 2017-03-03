package com.umpquariversoftware.metronome.FireBase;

/**
 * Class to represent Kit in Firebase.
 */

public class FirebaseKit {
    private String name;
    private String signature;

    public FirebaseKit() {
    }

    public FirebaseKit(String name, String signature) {
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

