package com.umpquariversoftware.metronome.elements;

import android.database.Cursor;
import android.util.Log;

import com.umpquariversoftware.metronome.database.dbContract;

/**
 * A component links a hexID to a resource for easy reference. Components are combined to
 * build kits.
 */

public class Component {

    private String name;
    private int resource;
    private String hexID;

    public String getHexID() {
        return hexID;
    }

    public Component() {
        // Empty Constructor
    }

    public Component(Cursor cursor) {
        cursor.moveToFirst();
        this.name = cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.NAME));
        this.resource = Integer.parseInt(cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.RESOURCE)));
        this.hexID = cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.HEXID));
    }

    @SuppressWarnings("unused")
    public Component(int resource) {
        this.resource = resource;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
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
