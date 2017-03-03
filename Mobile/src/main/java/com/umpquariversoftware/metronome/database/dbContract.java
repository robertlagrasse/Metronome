package com.umpquariversoftware.metronome.database;

import android.net.Uri;
import android.provider.BaseColumns;

import com.umpquariversoftware.metronome.elements.Component;
import com.umpquariversoftware.metronome.elements.Pattern;

/**
 * Created by robert on 1/26/17.
 */

public class dbContract {
    /**
     * This section defines all things Uri related for the contract provider.
     * Any call to the CP should used one of the build*Uri() methods to define the Uri.
     */
    private static final Uri CONTENT_AUTHORITY =
            Uri.parse("content://com.umpquariversoftware.metronome");


    public static Uri buildComponentUri() {
        return CONTENT_AUTHORITY.buildUpon()
                .appendPath(ComponentTable.TABLE_NAME)
                .build();
    }

    @SuppressWarnings("unused")
    public static Uri buildKitUri() {
        return CONTENT_AUTHORITY.buildUpon()
                .appendPath(KitTable.TABLE_NAME)
                .build();
    }

    // Weakening access breaks things. LINT is wrong.
    @SuppressWarnings("WeakerAccess")
    public static Uri buildPatternUri() {
        return CONTENT_AUTHORITY.buildUpon()
                .appendPath(PatternTable.TABLE_NAME)
                .build();
    }

    @SuppressWarnings("unused")
    public static Uri buildPatternBySignatureURI(String sequence) {
        return buildPatternUri().buildUpon()
                .appendPath(PatternTable.SEQUENCE)
                .appendPath(sequence)
                .build();
    }

    @SuppressWarnings("unused")
    public static Uri buildJamUri() {
        return CONTENT_AUTHORITY.buildUpon()
                .appendPath(JamTable.TABLE_NAME)
                .build();
    }

    @SuppressWarnings("unused")
    public static Uri buildJamByAttributesUri(String tempo, String kit, String pattern) {
        return CONTENT_AUTHORITY.buildUpon().appendPath("attributes")
                .appendPath(tempo)
                .appendPath(kit)
                .appendPath(pattern)
                .build();
    }

    public static Uri buildComponentByDbIDUri(int dbID) {
        return CONTENT_AUTHORITY.buildUpon().appendPath("component_by_db_id")
                .appendPath(String.valueOf(dbID))
                .build();
    }

    public static Uri buildAllComponentsUri() {
        return CONTENT_AUTHORITY.buildUpon()
                .appendPath("all_components")
                .build();
    }

    @SuppressWarnings("unused")
    public static Uri buildKitBySignatureUri(String signature) {
        return CONTENT_AUTHORITY.buildUpon().appendPath("kit_by_signature")
                .appendPath(signature)
                .build();
    }


    /**
     * This section defines the tables in the database, and each table's associated columns.
     */
    private static final String VARCHAR_255 = " VARCHAR(255), ";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "database";

    public static final class ComponentTable implements BaseColumns {
        public static final String TABLE_NAME = "components";
        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String RESOURCE = "resource";
        public static final String HEXID = "hexid";

        public static final String CREATE_TABLE =
                "CREATE TABLE " +
                        TABLE_NAME + "(" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        NAME + VARCHAR_255 +
                        RESOURCE + VARCHAR_255 +
                        HEXID + VARCHAR_255 +
                        "UNIQUE (" + _ID + ") ON CONFLICT IGNORE);";
    }

    public static final class KitTable implements BaseColumns {
        public static final String TABLE_NAME = "kit";
        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String COMPONENTS = "components";

        public static final String CREATE_TABLE =
                "CREATE TABLE " +
                        TABLE_NAME + "(" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        NAME + VARCHAR_255 +
                        COMPONENTS + VARCHAR_255 +
                        "UNIQUE (" + _ID + ") ON CONFLICT IGNORE);";
    }

    public static final class PatternTable implements BaseColumns {
        public static final String TABLE_NAME = "pattern";
        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String SEQUENCE = "sequence";

        public static final String CREATE_TABLE =
                "CREATE TABLE " +
                        TABLE_NAME + "(" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        NAME + VARCHAR_255 +
                        SEQUENCE + VARCHAR_255 +
                        "UNIQUE (" + _ID + ") ON CONFLICT IGNORE);";
    }

    public static final class JamTable implements BaseColumns {
        public static final String TABLE_NAME = "jam";
        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String KIT_ID = "kit_id";
        public static final String PATTERN_ID = "pattern_id";
        public static final String TEMPO = "tempo";

        public static final String CREATE_TABLE =
                "CREATE TABLE " +
                        TABLE_NAME + "(" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        NAME + VARCHAR_255 +
                        KIT_ID + VARCHAR_255 +
                        PATTERN_ID + VARCHAR_255 +
                        TEMPO + VARCHAR_255 +
                        "UNIQUE (" + _ID + ") ON CONFLICT REPLACE);";
    }
}
