package com.umpquariversoftware.metronome.elements;

import android.util.Log;

import java.util.Arrays;

/**
 * A beat determines whether or not each of the eight instruments should sound.
 * 8 Booleans get treated like any other 8 bits, rolled up into a single hex value,
 * and used to represent the beat in a compact way.
 */

public class Beat {

    private static int FIRST = 0;
    private static int SECOND = 1;
    private static int THIRD = 2;
    private static int FOURTH = 3;
    private static int FIFTH = 4;
    private static int SIXTH = 5;
    private static int SEVENTH = 6;
    private static int EIGHTH = 7;

    private Boolean[] beat = new Boolean[8];

    /**
     * Constructor turns all instruments off, except getFirst
     */
    public Beat() {
        Arrays.fill(this.beat, Boolean.FALSE);
        this.beat[FIRST] = true;
    }

    public Beat(String signature) {
        int value = Integer.parseInt(signature, 16);
        String binval = Integer.toBinaryString(value);
        char[] key = binval.toCharArray();

        Arrays.fill(this.beat, Boolean.FALSE);
        for (int i = 0; i < key.length; i++) {
            if (key[i] == '1') {
                this.beat[key.length - 1 - i] = true;
            } else {
                this.beat[key.length - 1 - i] = false;
            }
        }
    }

    @Override
    public String toString() {
        return "Beat{}";
    }

    public Boolean getFirst() {
        return this.beat[FIRST];
    }

    public Boolean getSecond() {
        return this.beat[SECOND];
    }

    public Boolean getThird() {
        return this.beat[THIRD];
    }

    public Boolean getFourth() {
        return this.beat[FOURTH];
    }

    public Boolean getFifth() {
        return this.beat[FIFTH];
    }

    public Boolean getSixth() {
        return this.beat[SIXTH];
    }

    public Boolean getSeventh() {
        return this.beat[SEVENTH];
    }

    public Boolean getEighth() {
        return this.beat[EIGHTH];
    }

    public Boolean getPosition(int position) {
        return this.beat[position];
    }

    public void setFIRST(Boolean value) {
        this.beat[FIRST] = value;
    }

    public void setSECOND(Boolean value) {
        this.beat[SECOND] = value;
    }

    public void setTHIRD(Boolean value) {
        this.beat[THIRD] = value;
    }

    public void setFOURTH(Boolean value) {
        this.beat[FOURTH] = value;
    }

    public void setFIFTH(Boolean value) {
        this.beat[FIFTH] = value;
    }

    public void setSIXTH(Boolean value) {
        this.beat[SIXTH] = value;
    }

    public void setSEVENTH(Boolean value) {
        this.beat[SEVENTH] = value;
    }

    public void setEIGHTH(Boolean value) {
        this.beat[EIGHTH] = value;
    }


}

