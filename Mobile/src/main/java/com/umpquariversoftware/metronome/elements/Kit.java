package com.umpquariversoftware.metronome.elements;

import android.content.Context;
import android.database.Cursor;

import com.umpquariversoftware.metronome.database.StandardDbContract;

import java.util.ArrayList;

/**
 * A Kit is a collection of 8 components.
 */

public class Kit {

    private String name;
    private final ArrayList<Component> components;
    private int databaseID;

    @SuppressWarnings("unused")
    public int getDatabaseID() {
        return databaseID;
    }

    @SuppressWarnings("unused")
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
            String pick = String.valueOf(sig[x]) + sig[x + 1];
            Cursor cursor = context.getContentResolver().query(StandardDbContract.buildComponentUri().buildUpon().appendPath(pick).build(),
                    null,
                    null,
                    null,
                    null);
            if (cursor != null) {
                cursor.moveToFirst();
                Component component = new Component(cursor);
                components.add(component);
                cursor.close();
            }
        }
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Component> getComponents() {
        return components;
    }

    public void addComponent(Component component) {
        components.add(component);
    }

    public void replaceComponent(int position, Component component) {
        components.remove(position);
        components.add(position, component);
    }

    public String getSignature() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int x = 0; x < 8; ++x) {
            stringBuilder.append(String.valueOf(getComponents().get(x).getHexID()));
        }
        return stringBuilder.toString();
    }
}
