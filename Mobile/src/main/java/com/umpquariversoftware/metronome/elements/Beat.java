package com.umpquariversoftware.metronome.elements;

import android.util.Log;

import java.util.Arrays;

/**
 * A beat determines whether or not each of the eight instruments should sound.
 * 8 Booleans get treated like any other 8 bits, rolled up into a single hex value,
 * and used to represent the beat in a compact way.
 */

public class Beat {

    private static final int FIRST = 0;
    private static final int SECOND = 1;
    private static final int THIRD = 2;
    private static final int FOURTH = 3;
    private static final int FIFTH = 4;
    private static final int SIXTH = 5;
    private static final int SEVENTH = 6;
    private static final int EIGHTH = 7;

    private final Boolean[] beat = new Boolean[8];

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
            this.beat[key.length - 1 - i] = key[i] == '1';
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

