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
        super(context, DbContract.DATABASE_NAME, null, DbContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DbContract.ComponentTable.CREATE_TABLE);
        db.execSQL(DbContract.KitTable.CREATE_TABLE);
        db.execSQL(DbContract.PatternTable.CREATE_TABLE);
        db.execSQL(DbContract.JamTable.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // delete the existing database
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.ComponentTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.KitTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.PatternTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.JamTable.TABLE_NAME);
        // call onCreate
        onCreate(db);
    }

}
