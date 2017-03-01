package com.umpquariversoftware.metronome.elements;

/**
 * Created by robert on 1/26/17.
 */

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.umpquariversoftware.metronome.database.dbContract;

import java.util.ArrayList;

public class Pattern {
    private String name;
    private ArrayList<Beat> beats;
    private int databaseID;

    public int getDatabaseID() {
        return databaseID;
    }

    public void setDatabaseID(int databaseID) {
        this.databaseID = databaseID;
    }

    public Pattern() {
        this.name = "New Pattern";
        this.beats = new ArrayList<>();
        this.beats.clear();
    }

    public Pattern(String name, String signature, Context context) {
        beats = new ArrayList<>();
        beats.clear();

        this.name = name;

        char[] sig = signature.toCharArray();
        for (int x = 0; x < signature.length(); x += 2) {
            String pick = new StringBuilder().append(sig[x]).append(sig[x + 1]).toString();
            Beat beat = new Beat(pick);
            beats.add(beat);
        }
    }

    @Override
    public String toString() {
        return getPatternHexSignature();
    }

    public void addBeat(Beat beat) {
        this.beats.add(beat);
    }

    public String getName() {
        return name;
    }

    public Beat getBeat(int number) {
        return beats.get(number);

    }

    public int getLength() {
        return this.beats.size();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPatternHexSignature() {

        String pattern = new String();

        for (int x = 0; x < getLength(); ++x) {
            int total = 0;
            if (getBeat(x).getFirst()) {
                total += 1;
            }
            if (getBeat(x).getSecond()) {
                total += 2;
            }
            if (getBeat(x).getThird()) {
                total += 4;
            }
            if (getBeat(x).getFourth()) {
                total += 8;
            }
            if (getBeat(x).getFifth()) {
                total += 16;
            }
            if (getBeat(x).getSixth()) {
                total += 32;
            }
            if (getBeat(x).getSeventh()) {
                total += 64;
            }
            if (getBeat(x).getEighth()) {
                total += 128;
            }
            pattern += String.format("%02X", total);
        }
        return pattern;
    }

    public PointsGraphSeries getPatternDataPoints() {

        PointsGraphSeries<DataPoint> series = new PointsGraphSeries<>();

        for (int x = 0; x < getLength(); ++x) {
            if (getBeat(x).getFirst()) {
                series.appendData(new DataPoint(x + 1, 1), false, 8192, false);
            }
            if (getBeat(x).getSecond()) {
                series.appendData(new DataPoint(x + 1, 2), false, 8192, false);
            }
            if (getBeat(x).getThird()) {
                series.appendData(new DataPoint(x + 1, 3), false, 8192, false);
            }
            if (getBeat(x).getFourth()) {
                series.appendData(new DataPoint(x + 1, 4), false, 8192, false);
            }
            if (getBeat(x).getFifth()) {
                series.appendData(new DataPoint(x + 1, 5), false, 8192, false);
            }
            if (getBeat(x).getSixth()) {
                series.appendData(new DataPoint(x + 1, 6), false, 8192, false);
            }
            if (getBeat(x).getSeventh()) {
                series.appendData(new DataPoint(x + 1, 7), false, 8192, false);
            }
            if (getBeat(x).getEighth()) {
                series.appendData(new DataPoint(x + 1, 8), false, 8192, false);
            }
        }
        return series;
    }

    public void insertBeat(Beat beat, int position) {
        this.beats.add(position, beat);
    }

    public void removeBeat(int position) {

        this.beats.remove(position);
    }

}
