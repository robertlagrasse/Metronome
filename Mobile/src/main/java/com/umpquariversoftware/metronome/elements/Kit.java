package com.umpquariversoftware.metronome.elements;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.umpquariversoftware.metronome.database.dbContract;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * A Kit is a collection of 8 components.
 */

public class Kit {

    private String name;
    private ArrayList<Component> components;
    private int databaseID;

    public int getDatabaseID() {
        return databaseID;
    }

    public void setDatabaseID(int databaseID) {
        this.databaseID = databaseID;
    }

    public Kit() {
        // Empty Constructor
        components = new ArrayList<>();
        components.clear();
    }

    public Kit(String name, String signature, Context context) {
        components = new ArrayList<>();
        components.clear();

        this.name = name;

        char[] sig = signature.toCharArray();
        for (int x = 0; x < signature.length(); x += 2) {
            String pick = new StringBuilder().append(sig[x]).append(sig[x + 1]).toString();
            Cursor cursor = context.getContentResolver().query(dbContract.buildComponentUri().buildUpon().appendPath(pick).build(),
                    null,
                    null,
                    null,
                    null);
            cursor.moveToFirst();
            String componentName = cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.NAME));
            int resource = Integer.parseInt(cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.RESOURCE)));
            String hexID = cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.HEXID));
            Component component = new Component(cursor);
            components.add(component);
            cursor.close();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Component> getComponents() {
        return components;
    }

    public boolean addComponent(Component component) {
        components.add(component);
        return false;
    }

    public boolean replaceComponent(int position, Component component) {
        components.remove(position);
        components.add(position, component);
        return false;
    }

    public String getSignature() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int x = 0; x < 8; ++x) {
            stringBuilder.append(String.valueOf(getComponents().get(x).getHexID()));
        }
        return stringBuilder.toString();
    }
}
