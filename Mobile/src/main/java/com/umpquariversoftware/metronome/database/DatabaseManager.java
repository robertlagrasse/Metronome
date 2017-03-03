package com.umpquariversoftware.metronome.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

/**
 * Created by robert on 1/26/17.
 */

class DatabaseManager extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseManager";

    DatabaseManager(Context context) {
        //super(context, name, factory, version); // original super.
        super(context, dbContract.DATABASE_NAME, null, dbContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(dbContract.ComponentTable.CREATE_TABLE);
        db.execSQL(dbContract.KitTable.CREATE_TABLE);
        db.execSQL(dbContract.PatternTable.CREATE_TABLE);
        db.execSQL(dbContract.JamTable.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // delete the existing database
        db.execSQL("DROP TABLE IF EXISTS " + dbContract.ComponentTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + dbContract.KitTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + dbContract.PatternTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + dbContract.JamTable.TABLE_NAME);
        // call onCreate
        onCreate(db);
    }

}
