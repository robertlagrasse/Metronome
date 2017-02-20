package com.umpquariversoftware.metronome.elements;

import android.database.Cursor;
import android.util.Log;

import com.umpquariversoftware.metronome.database.dbContract;

/**
 * Created by robert on 1/26/17.
 */

public class Component {

    private String name;
    private int resource;
    private String hexID;

    public String getHexID() {
        return hexID;
    }

    public void setHexID(String hexID) {
        this.hexID = hexID;
    }

    public Component() {
        // Empty Constructor
    }


    public Component(Cursor cursor){
        cursor.moveToFirst();
        this.name = cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.NAME));
        this.resource = Integer.parseInt(cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.RESOURCE)));
        this.hexID = cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.HEXID));
    }

    public Component(int resource){
        this.resource = resource;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getResource() {
        return resource;
    }

    public void setResource(int resource) {
        this.resource = resource;
    }
}
