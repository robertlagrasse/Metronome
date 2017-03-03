package com.umpquariversoftware.metronome.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class contentProvider extends ContentProvider {
    /**
     * Garden Variety Content Provider front-ending the local DB
     * **/

    DatabaseManager databaseManager;

    final int COMPONENT = 0;
    final int ALL_KITS = 1;
    final int KIT_BY_ID = 2;
    final int ALL_PATTERNS = 3;
    final int PATTERN_BY_ID = 4;
    final int PATTERN_BY_SEQUENCE = 5;
    final int ALL_JAMS = 6;
    final int JAM_BY_ID = 7;
    final int JAM_BY_ATTRIBUTES = 8;
    final int COMPONENT_BY_DB_ID = 9;
    final int ALL_COMPONENTS = 10;
    final int KIT_BY_SIGNATURE = 11;

    public contentProvider() {
    }

    private int uriSwitcher(Uri uri) {
        switch (uri.getPathSegments().get(0)) {
            case dbContract.ComponentTable.TABLE_NAME: {
                return COMPONENT;
            }
            case dbContract.KitTable.TABLE_NAME: {
                if (uri.getLastPathSegment().equals(dbContract.KitTable.TABLE_NAME)) {
                    return ALL_KITS;
                } else {
                    return KIT_BY_ID;
                }
            }
            case dbContract.PatternTable.TABLE_NAME: {
                if (uri.getLastPathSegment().equals(dbContract.PatternTable.TABLE_NAME)) {
                    return ALL_PATTERNS;
                } else if (uri.getPathSegments().get(1).equals(dbContract.PatternTable.SEQUENCE)) {
                    return PATTERN_BY_SEQUENCE;
                } else {
                    return PATTERN_BY_ID;
                }
            }
            case dbContract.JamTable.TABLE_NAME: {
                if (uri.getLastPathSegment().equals(dbContract.JamTable.TABLE_NAME)) {
                    return ALL_JAMS;
                } else {
                    return JAM_BY_ID;
                }
            }
            case "attributes": {
                return JAM_BY_ATTRIBUTES;
            }

            case "component_by_db_id": {
                return COMPONENT_BY_DB_ID;
            }

            case "all_components": {
                return ALL_COMPONENTS;
            }

            case "kit_by_signature": {
                return KIT_BY_SIGNATURE;
            }

        }
        return -1;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = databaseManager.getWritableDatabase();
        long _id = 0;
        switch (uriSwitcher(uri)) {
            case COMPONENT: {
                _id = db.delete(dbContract.ComponentTable.TABLE_NAME, selection, selectionArgs);
            }
            case ALL_KITS: {
                _id = db.delete(dbContract.KitTable.TABLE_NAME, selection, selectionArgs);
            }
            case ALL_PATTERNS: {
                _id = db.delete(dbContract.PatternTable.TABLE_NAME, selection, selectionArgs);
            }
            case ALL_JAMS: {
                _id = db.delete(dbContract.JamTable.TABLE_NAME, selection, selectionArgs);
            }
        }
        return (int) _id;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = databaseManager.getWritableDatabase();
        long _id = 0;

        switch (uriSwitcher(uri)) {
            case COMPONENT: {
                _id = db.insert(dbContract.ComponentTable.TABLE_NAME, null, values);
                break;
            }
            case ALL_KITS: {
                _id = db.insert(dbContract.KitTable.TABLE_NAME, null, values);
                break;
            }
            case ALL_PATTERNS: {
                _id = db.insert(dbContract.PatternTable.TABLE_NAME, null, values);
                break;
            }
            case ALL_JAMS: {
                _id = db.insert(dbContract.JamTable.TABLE_NAME, null, values);
                break;
            }
        }
        return Uri.withAppendedPath(uri, String.valueOf(_id));
    }

    @Override
    public boolean onCreate() {
        databaseManager = new DatabaseManager(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Cursor retCursor = null;

        switch (uriSwitcher(uri)) {
            case COMPONENT: {
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
            case COMPONENT_BY_DB_ID: {
                retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.ComponentTable.TABLE_NAME,
                        projection,
                        dbContract.ComponentTable.ID + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );
                break;
            }
            case ALL_COMPONENTS: {
                retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.ComponentTable.TABLE_NAME,
                        projection,
                        null,
                        null,
                        null,
                        null,
                        null
                );
                break;
            }
            case ALL_KITS: {
                retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.KitTable.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            case KIT_BY_ID: {
                retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.KitTable.TABLE_NAME,
                        projection,
                        dbContract.KitTable.ID + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );
                break;
            }

            case KIT_BY_SIGNATURE: {
                retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.KitTable.TABLE_NAME,
                        projection,
                        dbContract.KitTable.COMPONENTS + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );
                break;
            }

            case ALL_PATTERNS: {
                retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.PatternTable.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            case PATTERN_BY_SEQUENCE: {
                retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.PatternTable.TABLE_NAME,
                        projection,
                        dbContract.PatternTable.SEQUENCE + " = ?",
                        new String[]{uri.getPathSegments().get(2)},
                        null,
                        null,
                        null
                );
                break;
            }
            case PATTERN_BY_ID: {
                retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.PatternTable.TABLE_NAME,
                        projection,
                        dbContract.PatternTable.ID + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );
                break;
            }
            case ALL_JAMS: {
                retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.JamTable.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case JAM_BY_ID: {
                retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.JamTable.TABLE_NAME,
                        projection,
                        dbContract.JamTable.ID + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );
                break;
            }
            case JAM_BY_ATTRIBUTES: {
                retCursor = databaseManager.getReadableDatabase().query(
                        dbContract.JamTable.TABLE_NAME,
                        projection,
                        dbContract.JamTable.TEMPO + " = ? AND " +
                                dbContract.JamTable.KIT_ID + " = ? AND " +
                                dbContract.JamTable.PATTERN_ID + " = ? ",
                        new String[]{uri.getPathSegments().get(1),
                                uri.getPathSegments().get(2),
                                uri.getPathSegments().get(3)},
                        null,
                        null,
                        null
                );
                break;
            }
        }
        return retCursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        switch (uriSwitcher(uri)) {
            case COMPONENT: {
                // update component table
            }
            case ALL_KITS: {
                // update kit table
            }
            case ALL_PATTERNS: {
                // update pattern table
            }
            case ALL_JAMS: {
                // update jam table
            }
        }
        return 0;
    }
}
