package com.umpquariversoftware.metronome.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Standard DatabaseManager
 */

class DatabaseManager extends SQLiteOpenHelper {

    DatabaseManager(Context context) {
        //super(context, name, factory, version); // original super.
        super(context, StandardDbContract.DATABASE_NAME, null, StandardDbContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(StandardDbContract.ComponentTable.CREATE_TABLE);
        db.execSQL(StandardDbContract.KitTable.CREATE_TABLE);
        db.execSQL(StandardDbContract.PatternTable.CREATE_TABLE);
        db.execSQL(StandardDbContract.JamTable.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // delete the existing database
        db.execSQL("DROP TABLE IF EXISTS " + StandardDbContract.ComponentTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + StandardDbContract.KitTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + StandardDbContract.PatternTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + StandardDbContract.JamTable.TABLE_NAME);
        // call onCreate
        onCreate(db);
    }

}
