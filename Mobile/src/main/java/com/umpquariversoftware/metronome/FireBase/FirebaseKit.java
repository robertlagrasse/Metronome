package com.umpquariversoftware.metronome.FireBase;

/**
 * Created by robert on 2/21/17.
 */

public class FirebaseKit {
    private String Name;

    // Components will refer to the Hex ID's associated with the Components table in the
    // Local Database.
    private String signature;

    public FirebaseKit() {
    }

    public FirebaseKit(String name, String signature) {
        this.Name = name;
        this.signature = signature;
    }

    public String getName() {
        return Name;
    }

    public void setName(String Name) {
        this.signature = Name;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
