package com.umpquariversoftware.metronome.elements;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.umpquariversoftware.metronome.database.dbContract;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * A band has a name and members (instruments)
 */

public class Kit {

    private String name;
    private ArrayList<Component> components;

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
        for(int x=0;x<signature.length();x+=2){
            String pick = new StringBuilder().append(sig[x]).append(sig[x+1]).toString();
            Cursor cursor = context.getContentResolver().query(dbContract.buildComponentUri().buildUpon().appendPath(pick).build(),
                    null,
                    null,
                    null,
                    null);
            cursor.moveToFirst();
            String componentName = cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.NAME));
            int resource = Integer.parseInt(cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.RESOURCE)));
            String hexID = cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.HEXID));
//            Log.e("kitBuilder", "" + componentName + " " + resource);
            Component component = new Component(cursor);
            components.add(component);
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

    public boolean addComponent(Component component){
        // Verify there's room to add a new member
        // add the member
        components.add(component);

        return false;
    }

    public boolean removeMember(){
        // Figure out how to identify the specific member
        // Remove the member
        return false;
    }


    public String getSignature(){
        StringBuilder stringBuilder = new StringBuilder();
        for(int x=0;x<8;++x){
            stringBuilder.append(String.valueOf(getComponents().get(x).getHexID()));
        }
        return stringBuilder.toString();
    }

    private static String md5(String s) { try {

        // Create MD5 Hash
        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        digest.update(s.getBytes());
        byte messageDigest[] = digest.digest();

        // Create Hex String
        StringBuffer hexString = new StringBuffer();
        for (int i=0; i<messageDigest.length; i++)
            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
        return hexString.toString();

    } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
    }
        return "";

    }

}
