package com.umpquariversoftware.metronome.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class contentProvider extends ContentProvider {
    DatabaseManager databaseManager;

    public contentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = databaseManager.getWritableDatabase();
        long _id = 0;
        switch (uri.getPathSegments().get(0)){
            case dbContract.ComponentTable.TABLE_NAME:{
                // delete from component table
                _id = db.delete(dbContract.ComponentTable.TABLE_NAME, selection, selectionArgs);
            }
            case dbContract.KitTable.TABLE_NAME:{
                // delete from kit table
                _id = db.delete(dbContract.KitTable.TABLE_NAME, selection, selectionArgs);
            }
            case dbContract.PatternTable.TABLE_NAME:{
                // delete from pattern table
                _id = db.delete(dbContract.PatternTable.TABLE_NAME, selection, selectionArgs);
            }
            case dbContract.JamTable.TABLE_NAME:{
                // delete from jam table
                _id = db.delete(dbContract.JamTable.TABLE_NAME, selection, selectionArgs);
                Log.e("contentProvider","delete matched jam table");

            }
        }

        Log.e("contentProvider","delete() called with uri: " + uri.toString());
        return (int) _id;
    }

    @Override
    public String getType(Uri uri) {
        Log.e("contentProvider","getType() called with uri: " + uri.toString());
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = databaseManager.getWritableDatabase();
        long _id = 0;
        switch (uri.getPathSegments().get(0)){
            case dbContract.ComponentTable.TABLE_NAME:{
                // insert to component table
                Log.e("insert()","matched Component");
                _id = db.insert(dbContract.ComponentTable.TABLE_NAME, null, values);
                break;
            }
            case dbContract.KitTable.TABLE_NAME:{
                // insert to kit table
                Log.e("insert()","matched Kit");
                _id = db.insert(dbContract.KitTable.TABLE_NAME, null, values);
                break;
            }
            case dbContract.PatternTable.TABLE_NAME:{
                // insert to pattern table
                Log.e("insert()","matched Pattern");
                _id = db.insert(dbContract.PatternTable.TABLE_NAME, null, values);
                break;
            }
            case dbContract.JamTable.TABLE_NAME:{
                // insert to jam table
                Log.e("insert()","matched Jam");
                _id = db.insert(dbContract.JamTable.TABLE_NAME, null, values);
                break;
            }
        }
        return Uri.withAppendedPath(uri, String.valueOf(_id));
    }

    @Override
    public boolean onCreate() {
        Log.e("contentProvider","onCreate called");
        databaseManager = new DatabaseManager(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Cursor retCursor = null;

        switch (uri.getPathSegments().get(0)){
            case dbContract.ComponentTable.TABLE_NAME:{
                // query component table
//                Log.e("contentProvider", "Matched component URI via case/switch");
//                Log.e("contentProvider", uri.getPathSegments().get(1));
                retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.ComponentTable.TABLE_NAME,
                        projection,
                        dbContract.ComponentTable.HEXID + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );
                break;
            }
            case dbContract.KitTable.TABLE_NAME:{
//                // query kit table
//                Log.e("contentProvider","Matched kit URI via case/switch");
                if(uri.getLastPathSegment().equals(dbContract.KitTable.TABLE_NAME)){
//                    Log.e("contentProvider", "Requested all kits");
                    retCursor = databaseManager.getReadableDatabase().query(
                            dbContract.KitTable.TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            sortOrder
                    );
                } else {
                retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.KitTable.TABLE_NAME,
                        projection,
                        dbContract.KitTable.ID + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );
                }
                break;
            }
            case dbContract.PatternTable.TABLE_NAME:{
                // query pattern table
//                Log.e("contentProvider","Matched pattern URI via case/switch");
                if(uri.getLastPathSegment().equals(dbContract.PatternTable.TABLE_NAME)){
//                    Log.e("contentProvider", "Requested all patterns");
                    retCursor = databaseManager.getReadableDatabase().query(
                            dbContract.PatternTable.TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            sortOrder
                    );                } else {
//                    Log.e("contentProvider", "Requested a single pattern");
                    retCursor = databaseManager.getReadableDatabase().query(
                            dbContract.PatternTable.TABLE_NAME,
                            projection,
                            dbContract.PatternTable.ID + " = ?",
                            new String[]{uri.getPathSegments().get(1)},
                            null,
                            null,
                            null
                    );
                }
                break;
            }
            case dbContract.JamTable.TABLE_NAME:{
                // query jam table
//                Log.e("contentProvider","Matched jam URI via case/switch");
                if(uri.getLastPathSegment().equals(dbContract.JamTable.TABLE_NAME)){
                    retCursor = databaseManager.getReadableDatabase().query(
                            dbContract.JamTable.TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            sortOrder
                    );
                } else {

                    retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.JamTable.TABLE_NAME,
                        projection,
                        dbContract.JamTable.ID + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );}
                break;
            }
        }

//        Log.e("contentProvider","query() called with uri: " + uri.toString());
        return retCursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        switch (uri.getPathSegments().get(0)){
            case dbContract.ComponentTable.TABLE_NAME:{
                // update component table
                Log.e("contentProvider","Matched component URI via case/switch");
            }
            case dbContract.KitTable.TABLE_NAME:{
                // update kit table
                Log.e("contentProvider","Matched kit URI via case/switch");
            }
            case dbContract.PatternTable.TABLE_NAME:{
                // update pattern table
                Log.e("contentProvider","Matched pattern URI via case/switch");
            }
            case dbContract.JamTable.TABLE_NAME:{
                // update jam table
                Log.e("contentProvider","Matched jam URI via case/switch");
            }
        }

        Log.e("contentProvider","update() called with uri: " + uri.toString());
        return 0;
    }
}
