package com.umpquariversoftware.metronome.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

public class ContentProvider extends android.content.ContentProvider {
    /**
     * Garden Variety Content Provider front-ending the local DB
     * **/

    private DatabaseManager databaseManager;

    private final int COMPONENT = 0;
    private final int ALL_KITS = 1;
    private final int KIT_BY_ID = 2;
    private final int ALL_PATTERNS = 3;
    private final int PATTERN_BY_ID = 4;
    private final int PATTERN_BY_SEQUENCE = 5;
    private final int ALL_JAMS = 6;
    private final int JAM_BY_ID = 7;
    private final int JAM_BY_ATTRIBUTES = 8;
    private final int COMPONENT_BY_DB_ID = 9;
    private final int ALL_COMPONENTS = 10;
    private final int KIT_BY_SIGNATURE = 11;

    public ContentProvider() {
    }

    private int uriSwitcher(Uri uri) {
        switch (uri.getPathSegments().get(0)) {
            case DbContract.ComponentTable.TABLE_NAME: {
                return COMPONENT;
            }
            case DbContract.KitTable.TABLE_NAME: {
                if (uri.getLastPathSegment().equals(DbContract.KitTable.TABLE_NAME)) {
                    return ALL_KITS;
                } else {
                    return KIT_BY_ID;
                }
            }
            case DbContract.PatternTable.TABLE_NAME: {
                if (uri.getLastPathSegment().equals(DbContract.PatternTable.TABLE_NAME)) {
                    return ALL_PATTERNS;
                } else if (uri.getPathSegments().get(1).equals(DbContract.PatternTable.SEQUENCE)) {
                    return PATTERN_BY_SEQUENCE;
                } else {
                    return PATTERN_BY_ID;
                }
            }
            case DbContract.JamTable.TABLE_NAME: {
                if (uri.getLastPathSegment().equals(DbContract.JamTable.TABLE_NAME)) {
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

    @SuppressWarnings("UnusedAssignment")
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = databaseManager.getWritableDatabase();
        long _id = 0;
        switch (uriSwitcher(uri)) {
            case COMPONENT: {
                _id = db.delete(DbContract.ComponentTable.TABLE_NAME, selection, selectionArgs);
            }
            case ALL_KITS: {
                _id = db.delete(DbContract.KitTable.TABLE_NAME, selection, selectionArgs);
            }
            case ALL_PATTERNS: {
                _id = db.delete(DbContract.PatternTable.TABLE_NAME, selection, selectionArgs);
            }
            case ALL_JAMS: {
                _id = db.delete(DbContract.JamTable.TABLE_NAME, selection, selectionArgs);
            }
        }
        return (int) _id;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        final SQLiteDatabase db = databaseManager.getWritableDatabase();
        long _id = 0;

        switch (uriSwitcher(uri)) {
            case COMPONENT: {
                _id = db.insert(DbContract.ComponentTable.TABLE_NAME, null, values);
                break;
            }
            case ALL_KITS: {
                _id = db.insert(DbContract.KitTable.TABLE_NAME, null, values);
                break;
            }
            case ALL_PATTERNS: {
                _id = db.insert(DbContract.PatternTable.TABLE_NAME, null, values);
                break;
            }
            case ALL_JAMS: {
                _id = db.insert(DbContract.JamTable.TABLE_NAME, null, values);
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
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Cursor retCursor = null;

        switch (uriSwitcher(uri)) {
            case COMPONENT: {
                retCursor = databaseManager.getReadableDatabase().query(
                        DbContract.ComponentTable.TABLE_NAME,
                        projection,
                        DbContract.ComponentTable.HEXID + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );
                break;
            }
            case COMPONENT_BY_DB_ID: {
                retCursor = databaseManager.getReadableDatabase().query(
                        DbContract.ComponentTable.TABLE_NAME,
                        projection,
                        DbContract.ComponentTable.ID + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );
                break;
            }
            case ALL_COMPONENTS: {
                retCursor = databaseManager.getReadableDatabase().query(
                        DbContract.ComponentTable.TABLE_NAME,
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
                        DbContract.KitTable.TABLE_NAME,
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
                        DbContract.KitTable.TABLE_NAME,
                        projection,
                        DbContract.KitTable.ID + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );
                break;
            }

            case KIT_BY_SIGNATURE: {
                retCursor = databaseManager.getReadableDatabase().query(
                        DbContract.KitTable.TABLE_NAME,
                        projection,
                        DbContract.KitTable.COMPONENTS + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );
                break;
            }

            case ALL_PATTERNS: {
                retCursor = databaseManager.getReadableDatabase().query(
                        DbContract.PatternTable.TABLE_NAME,
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
                        DbContract.PatternTable.TABLE_NAME,
                        projection,
                        DbContract.PatternTable.SEQUENCE + " = ?",
                        new String[]{uri.getPathSegments().get(2)},
                        null,
                        null,
                        null
                );
                break;
            }
            case PATTERN_BY_ID: {
                retCursor = databaseManager.getReadableDatabase().query(
                        DbContract.PatternTable.TABLE_NAME,
                        projection,
                        DbContract.PatternTable.ID + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );
                break;
            }
            case ALL_JAMS: {
                retCursor = databaseManager.getReadableDatabase().query(
                        DbContract.JamTable.TABLE_NAME,
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
                        DbContract.JamTable.TABLE_NAME,
                        projection,
                        DbContract.JamTable.ID + " = ?",
                        new String[]{uri.getPathSegments().get(1)},
                        null,
                        null,
                        null
                );
                break;
            }
            case JAM_BY_ATTRIBUTES: {
                retCursor = databaseManager.getReadableDatabase().query(
                        DbContract.JamTable.TABLE_NAME,
                        projection,
                        DbContract.JamTable.TEMPO + " = ? AND " +
                                DbContract.JamTable.KIT_ID + " = ? AND " +
                                DbContract.JamTable.PATTERN_ID + " = ? ",
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
    public int update(@NonNull Uri uri, ContentValues values, String selection,
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
