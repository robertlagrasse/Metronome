package com.umpquariversoftware.metronome.UI;


import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.umpquariversoftware.metronome.FireBase.FirebaseJam;
import com.umpquariversoftware.metronome.FireBase.FirebaseKit;
import com.umpquariversoftware.metronome.FireBase.FirebasePattern;
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.elements.Jam;
import com.umpquariversoftware.metronome.elements.Kit;
import com.umpquariversoftware.metronome.elements.Pattern;
import com.umpquariversoftware.metronome.kitEditor.KitEditor;
import com.umpquariversoftware.metronome.patternEditor.PatternEditor;

import java.util.ArrayList;
import java.util.Arrays;

import static com.umpquariversoftware.metronome.database.dbContract.*;

/**
 * OVERVIEW
 * <p>
 * The user interface for metronome allows the user to build their own Jam.
 * A Jam consists of three parts - all user selectable on the main screen
 * <p>
 * Tempo (Set by seekbar)
 * Kit (Selected via recyclerview)
 * Pattern (Selected via recyclerview)
 * <p>
 * Kits consist of 8 components. A component is a sound.
 * A pattern is a sequence of beats. A beat is an array of 8 boolean values. These boolean values
 * will correlate with the components in each kit.
 * <p>
 * For every tick of the timer, the app will cycle through the pattern. The beat in the pattern
 * will determine which components sound on that tick.
 * <p>
 * I chose the data structures here very specifically. Beats consist of 8 binary values, which
 * correspond to the 8 components in a kit. Any beat can be represented as a two digit hex value.
 * These values can the chained together to create patterns of arbitrary length, with the complete
 * information for any beat only taking up a single byte in the database. This facilitates both
 * efficient DB storage, and easy sharing between users.
 * <p>
 * Component values have associated 2 Digit Hexidecimal values in the database HEXID
 * All components will be supplied with the software. No user supplied sounds will be allowed.
 * Essentially, this allows me to employ the same identification and sharing technique.
 * <p>
 * Users can pass all of the information necessary to share their Jam in the space of a tweet.
 */


public class MainActivity extends AppCompatActivity {
    String TAG = "MainActivity";
    Jam mJam = new Jam();
    Boolean beatServiceRunning = false;
    static Boolean networkIsConnected;
    static Boolean userIsLoggedIn;
    private Toolbar toolbar;
    static Context mContext;
    String userID = "this_user";
    final int TEMPO_OFFSET = 30; // Seekbar starts at 0. Offset calibrates to minimum tempo.

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    ArrayList<FirebasePattern> mPatterns = new ArrayList<>();
    ArrayList<FirebasePattern> mMasterPatterns = new ArrayList<>();
    ArrayList<FirebasePattern> mUserPatterns = new ArrayList<>();
    ArrayList<FirebasePattern> mLocalPattern = new ArrayList<>();

    ArrayList<FirebaseKit> mKits = new ArrayList<>();
    ArrayList<FirebaseKit> mUserKits = new ArrayList<>();
    ArrayList<FirebaseKit> mMasterKits = new ArrayList<>();
    ArrayList<FirebaseKit> mLocalKits = new ArrayList<>();

    ArrayList<FirebaseJam> mJams = new ArrayList<>();
    ArrayList<FirebaseJam> mUserJams = new ArrayList<>();
    ArrayList<FirebaseJam> mMasterJams = new ArrayList<>();
    ArrayList<FirebaseJam> mLocalJams = new ArrayList<>();

    PatternListAdapter mPatternListAdapter;
    KitListAdapter mKitListAdapter;
    JamListAdapter mJamListAdapter;

    Boolean mMasterListSearchResultsBack = false;
    Boolean mUserListSearchResultsBack = false;
    FirebaseJam mUserListJam, mMasterListJam;

    private static final int RC_SIGN_IN = 69;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        SharedPreferences prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        if (prefs.getBoolean("firstrun", true)) {
            createComponentsTable();
            prefs.edit().putBoolean("firstrun", false).commit();
        }

        if (savedInstanceState == null) {
            launchBeatService();
            beatServiceRunning = true;
        }

        mContext = this;

        createLocalResources();
        setupNetworkMonitor();
        authenticateUser();
        setupToolbar();
        tempoChooser();
        patternChooser();
        kitChooser();
        jamChooser();
        actionButton();
        getThatMoney();
    }

    /**
     * Core Functionality
     **/

    private void createComponentsTable() {
        /**
         * Metronome comes preloaded with 75 sound files. This method creates
         * a table of those resources in the local database. The HexID
         * associated with each resourece is used as that component's
         * signature elsewhere.
         *
         * 75 Sets of contentValues, rolled up into a single array list, and
         * sent to the local database via content provider.
         * */

        ContentValues contentValues;
        ArrayList<ContentValues> components = new ArrayList<>();

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Clap 1");
        contentValues.put(ComponentTable.RESOURCE, R.raw.clap_1);
        contentValues.put(ComponentTable.HEXID, "00");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Clap 2");
        contentValues.put(ComponentTable.RESOURCE, R.raw.clap_2);
        contentValues.put(ComponentTable.HEXID, "01");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Clap 3");
        contentValues.put(ComponentTable.RESOURCE, R.raw.clap_3);
        contentValues.put(ComponentTable.HEXID, "02");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Clap 4");
        contentValues.put(ComponentTable.RESOURCE, R.raw.clap_4);
        contentValues.put(ComponentTable.HEXID, "03");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Cymbal 1");
        contentValues.put(ComponentTable.RESOURCE, R.raw.cymbal_1);
        contentValues.put(ComponentTable.HEXID, "04");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Cymbal 2");
        contentValues.put(ComponentTable.RESOURCE, R.raw.cymbal_2);
        contentValues.put(ComponentTable.HEXID, "05");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Cymbal 3");
        contentValues.put(ComponentTable.RESOURCE, R.raw.cymbal_3);
        contentValues.put(ComponentTable.HEXID, "06");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Flam 1");
        contentValues.put(ComponentTable.RESOURCE, R.raw.flam_1);
        contentValues.put(ComponentTable.HEXID, "07");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Flam 2");
        contentValues.put(ComponentTable.RESOURCE, R.raw.flam_2);
        contentValues.put(ComponentTable.HEXID, "08");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Flam 3");
        contentValues.put(ComponentTable.RESOURCE, R.raw.flam_3);
        contentValues.put(ComponentTable.HEXID, "09");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Flam 4");
        contentValues.put(ComponentTable.RESOURCE, R.raw.flam_4);
        contentValues.put(ComponentTable.HEXID, "0A");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Flam 5");
        contentValues.put(ComponentTable.RESOURCE, R.raw.flam_5);
        contentValues.put(ComponentTable.HEXID, "0B");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Flam 6");
        contentValues.put(ComponentTable.RESOURCE, R.raw.flam_6);
        contentValues.put(ComponentTable.HEXID, "0C");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Flam 7");
        contentValues.put(ComponentTable.RESOURCE, R.raw.flam_7);
        contentValues.put(ComponentTable.HEXID, "0D");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Flam 8");
        contentValues.put(ComponentTable.RESOURCE, R.raw.flam_8);
        contentValues.put(ComponentTable.HEXID, "0E");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Hat 1");
        contentValues.put(ComponentTable.RESOURCE, R.raw.hat_1);
        contentValues.put(ComponentTable.HEXID, "0F");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Hat 2");
        contentValues.put(ComponentTable.RESOURCE, R.raw.hat_2);
        contentValues.put(ComponentTable.HEXID, "10");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Hat 3");
        contentValues.put(ComponentTable.RESOURCE, R.raw.hat_3);
        contentValues.put(ComponentTable.HEXID, "11");
        components.add(contentValues);


        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Hat 4");
        contentValues.put(ComponentTable.RESOURCE, R.raw.hat_4);
        contentValues.put(ComponentTable.HEXID, "12");
        components.add(contentValues);


        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Hat 5");
        contentValues.put(ComponentTable.RESOURCE, R.raw.hat_5);
        contentValues.put(ComponentTable.HEXID, "13");
        components.add(contentValues);


        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Hat 6");
        contentValues.put(ComponentTable.RESOURCE, R.raw.hat_6);
        contentValues.put(ComponentTable.HEXID, "14");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Hat 7");
        contentValues.put(ComponentTable.RESOURCE, R.raw.hat_7);
        contentValues.put(ComponentTable.HEXID, "15");
        components.add(contentValues);


        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Hat 8");
        contentValues.put(ComponentTable.RESOURCE, R.raw.hat_8);
        contentValues.put(ComponentTable.HEXID, "16");
        components.add(contentValues);


        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Kick 1");
        contentValues.put(ComponentTable.RESOURCE, R.raw.kick_1);
        contentValues.put(ComponentTable.HEXID, "17");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Kick 2");
        contentValues.put(ComponentTable.RESOURCE, R.raw.kick_2);
        contentValues.put(ComponentTable.HEXID, "18");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Kick 3");
        contentValues.put(ComponentTable.RESOURCE, R.raw.kick_3);
        contentValues.put(ComponentTable.HEXID, "19");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Kick 4");
        contentValues.put(ComponentTable.RESOURCE, R.raw.kick_4);
        contentValues.put(ComponentTable.HEXID, "1A");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Kick 5");
        contentValues.put(ComponentTable.RESOURCE, R.raw.kick_5);
        contentValues.put(ComponentTable.HEXID, "1B");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Kick 6");
        contentValues.put(ComponentTable.RESOURCE, R.raw.kick_6);
        contentValues.put(ComponentTable.HEXID, "1C");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Kick 7");
        contentValues.put(ComponentTable.RESOURCE, R.raw.kick_7);
        contentValues.put(ComponentTable.HEXID, "1D");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Kick 8");
        contentValues.put(ComponentTable.RESOURCE, R.raw.kick_8);
        contentValues.put(ComponentTable.HEXID, "1E");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Kick 9");
        contentValues.put(ComponentTable.RESOURCE, R.raw.kick_9);
        contentValues.put(ComponentTable.HEXID, "1F");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Kick 10");
        contentValues.put(ComponentTable.RESOURCE, R.raw.kick_10);
        contentValues.put(ComponentTable.HEXID, "20");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Kick 11");
        contentValues.put(ComponentTable.RESOURCE, R.raw.kick_11);
        contentValues.put(ComponentTable.HEXID, "21");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Kick 12");
        contentValues.put(ComponentTable.RESOURCE, R.raw.kick_12);
        contentValues.put(ComponentTable.HEXID, "22");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Kick 13");
        contentValues.put(ComponentTable.RESOURCE, R.raw.kick_13);
        contentValues.put(ComponentTable.HEXID, "23");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Rim 1");
        contentValues.put(ComponentTable.RESOURCE, R.raw.rim_1);
        contentValues.put(ComponentTable.HEXID, "24");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Rim 2");
        contentValues.put(ComponentTable.RESOURCE, R.raw.rim_2);
        contentValues.put(ComponentTable.HEXID, "25");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Rim 3");
        contentValues.put(ComponentTable.RESOURCE, R.raw.rim_3);
        contentValues.put(ComponentTable.HEXID, "26");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Rim 4");
        contentValues.put(ComponentTable.RESOURCE, R.raw.rim_4);
        contentValues.put(ComponentTable.HEXID, "27");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Rim 5");
        contentValues.put(ComponentTable.RESOURCE, R.raw.rim_5);
        contentValues.put(ComponentTable.HEXID, "28");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Rim 6");
        contentValues.put(ComponentTable.RESOURCE, R.raw.rim_6);
        contentValues.put(ComponentTable.HEXID, "29");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Rim 7");
        contentValues.put(ComponentTable.RESOURCE, R.raw.rim_7);
        contentValues.put(ComponentTable.HEXID, "2A");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Rim 8");
        contentValues.put(ComponentTable.RESOURCE, R.raw.rim_8);
        contentValues.put(ComponentTable.HEXID, "2B");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Rim 9");
        contentValues.put(ComponentTable.RESOURCE, R.raw.rim_9);
        contentValues.put(ComponentTable.HEXID, "2C");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Rim 10");
        contentValues.put(ComponentTable.RESOURCE, R.raw.rim_10);
        contentValues.put(ComponentTable.HEXID, "2D");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Rim 11");
        contentValues.put(ComponentTable.RESOURCE, R.raw.rim_11);
        contentValues.put(ComponentTable.HEXID, "2E");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Snare 1");
        contentValues.put(ComponentTable.RESOURCE, R.raw.snare_1);
        contentValues.put(ComponentTable.HEXID, "2F");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Snare 2");
        contentValues.put(ComponentTable.RESOURCE, R.raw.snare_2);
        contentValues.put(ComponentTable.HEXID, "30");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Snare 3");
        contentValues.put(ComponentTable.RESOURCE, R.raw.snare_3);
        contentValues.put(ComponentTable.HEXID, "31");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Snare 4");
        contentValues.put(ComponentTable.RESOURCE, R.raw.snare_4);
        contentValues.put(ComponentTable.HEXID, "32");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Snare 5");
        contentValues.put(ComponentTable.RESOURCE, R.raw.snare_5);
        contentValues.put(ComponentTable.HEXID, "33");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Snare 6");
        contentValues.put(ComponentTable.RESOURCE, R.raw.snare_6);
        contentValues.put(ComponentTable.HEXID, "34");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Snare 7");
        contentValues.put(ComponentTable.RESOURCE, R.raw.snare_7);
        contentValues.put(ComponentTable.HEXID, "35");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Snare 8");
        contentValues.put(ComponentTable.RESOURCE, R.raw.snare_8);
        contentValues.put(ComponentTable.HEXID, "36");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Snare 9");
        contentValues.put(ComponentTable.RESOURCE, R.raw.snare_9);
        contentValues.put(ComponentTable.HEXID, "37");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Snare 10");
        contentValues.put(ComponentTable.RESOURCE, R.raw.snare_10);
        contentValues.put(ComponentTable.HEXID, "38");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Snare 11");
        contentValues.put(ComponentTable.RESOURCE, R.raw.snare_11);
        contentValues.put(ComponentTable.HEXID, "39");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Snare 12");
        contentValues.put(ComponentTable.RESOURCE, R.raw.snare_12);
        contentValues.put(ComponentTable.HEXID, "3A");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 1");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_1);
        contentValues.put(ComponentTable.HEXID, "3B");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 2");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_2);
        contentValues.put(ComponentTable.HEXID, "3C");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 3");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_3);
        contentValues.put(ComponentTable.HEXID, "3D");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 4");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_4);
        contentValues.put(ComponentTable.HEXID, "3E");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 5");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_5);
        contentValues.put(ComponentTable.HEXID, "3F");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 6");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_6);
        contentValues.put(ComponentTable.HEXID, "40");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 7");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_7);
        contentValues.put(ComponentTable.HEXID, "41");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 8");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_8);
        contentValues.put(ComponentTable.HEXID, "42");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 9");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_9);
        contentValues.put(ComponentTable.HEXID, "43");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 10");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_10);
        contentValues.put(ComponentTable.HEXID, "44");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 11");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_11);
        contentValues.put(ComponentTable.HEXID, "45");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 12");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_12);
        contentValues.put(ComponentTable.HEXID, "46");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 13");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_13);
        contentValues.put(ComponentTable.HEXID, "47");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 14");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_14);
        contentValues.put(ComponentTable.HEXID, "48");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 15");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_15);
        contentValues.put(ComponentTable.HEXID, "49");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Tom 16");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom_16);
        contentValues.put(ComponentTable.HEXID, "4A");
        components.add(contentValues);

        for (int x = 0; x < components.size(); x++) {
            getContentResolver().insert(buildComponentUri(), components.get(x));
        }

    }

    private void launchBeatService() {
        /**
         * Launches the BeatService, which manages the timer.
         * */
        Intent i = new Intent(this, BeatService.class);
        i.putExtra("jamID", 2L);
        startService(i);
    }

    void createLocalResources() {
        /**
         * Creates 4 patterns, 1 kit, and 4 Jams which are accessible to the user offline
         * */

        mLocalPattern.add(new FirebasePattern(getResources().getString(R.string.one_beat_pattern_local), "01"));
        mLocalPattern.add(new FirebasePattern(getResources().getString(R.string.two_beat_pattern_local), "0102"));
        mLocalPattern.add(new FirebasePattern(getResources().getString(R.string.three_beat_pattern_local), "010102"));
        mLocalPattern.add(new FirebasePattern(getResources().getString(R.string.four_beat_pattern_local), "01010102"));

        mLocalKits.add(new FirebaseKit("Standard Kit (Local)", "030405060708090A"));

        mLocalJams.add(new FirebaseJam(getResources().getString(R.string.one_beat_jam_local), 90, "030405060708090A", "01"));
        mLocalJams.add(new FirebaseJam(getResources().getString(R.string.two_beat_jam_local), 90, "030405060708090A", "0102"));
        mLocalJams.add(new FirebaseJam(getResources().getString(R.string.three_beat_jam_local), 90, "030405060708090A", "010102"));
        mLocalJams.add(new FirebaseJam(getResources().getString(R.string.four_beat_jam_local), 90, "030405060708090A", "01010102"));
    }

    public void setupToolbar() {
        /**
         * Setup the toolbar and associated onClick listeners.
         * Online features disappear if the user isn't logged in
         * or the network is offline.
         * */

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageView searchForSharedJamButton = (ImageView) findViewById(R.id.searchForSharedJamButton);
        searchForSharedJamButton.setContentDescription(getResources().getString(R.string.search_for_shared_jam));
        searchForSharedJamButton.setFocusable(true);

        ImageView shareJamButton = (ImageView) findViewById(R.id.shareJamButton);
        shareJamButton.setContentDescription(getResources().getString(R.string.share_your_jam));
        shareJamButton.setFocusable(true);

        ImageView saveJamToCloud = (ImageView) findViewById(R.id.saveJamToCloud);
        saveJamToCloud.setContentDescription(getResources().getString(R.string.save_jam_to_cloud));
        saveJamToCloud.setFocusable(true);

        if (networkIsConnected && userIsLoggedIn) {
            searchForSharedJamButton.setVisibility(View.VISIBLE);
            searchForSharedJamButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new MaterialDialog.Builder(mContext).title(R.string.download_shared_jam)
                            .content(R.string.enter_jam_id)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    if (input.length() > 0) {
                                        addSharedJamFromFirebase(input.toString());
                                    }
                                }
                            })
                            .show();
                }
            });

            shareJamButton.setVisibility(View.VISIBLE);
            shareJamButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    shareJam();
                }
            });

            saveJamToCloud.setVisibility(View.VISIBLE);
            saveJamToCloud.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendJamToFirebase();
                }
            });

        } else {
            searchForSharedJamButton.setVisibility(View.INVISIBLE);
            shareJamButton.setVisibility(View.INVISIBLE);
            saveJamToCloud.setVisibility(View.INVISIBLE);
        }
    }

    public void tempoChooser() {
        /**
         * Setup the tempo slider for user input.
         * TEMPO_OFFSET adjusts range. Set this to the minimum tempo
         * */
        final int tempo = mJam.getTempo();
        SeekBar tempoBar = (SeekBar) findViewById(R.id.tempoBar);
        final TextView tempoDisplay = (TextView) findViewById(R.id.tempoDisplay);
        tempoDisplay.setFocusable(true);

        tempoBar.setProgress(tempo - TEMPO_OFFSET);
        tempoBar.setContentDescription(getResources().getString(R.string.slide_to_set_tempo));
        tempoBar.setFocusable(true);

        tempoDisplay.setText(String.valueOf(tempo));
        tempoBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // Modify the tempo of the current Jam as we slide
                mJam.setTempo(i + 30);
                tempoDisplay.setText(String.valueOf(i + TEMPO_OFFSET));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sendBeatBroadcast(false);
            }
        });
    }

    void patternChooser() {

        /**
         * Setup recyclerView that allows user to select pattern.
         * Snappy recyclerView will reports the view visible on scroll and responds there
         * User will not have to click to make changes take affect.
         * */

        final SnappyRecyclerView patternRecyclerView = (SnappyRecyclerView) findViewById(R.id.patternRecyclerView);
        patternRecyclerView.setHasFixedSize(true);
        LinearLayoutManager patternLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        patternRecyclerView.setLayoutManager(patternLinearLayoutManager);

        patternRecyclerView.setContentDescription(getResources().getString(R.string.swipe_left_or_right_to_change_pattern));
        patternRecyclerView.setFocusable(true);

        mPatternListAdapter = new PatternListAdapter(mPatterns, mContext);
        patternRecyclerView.setAdapter(mPatternListAdapter);

        patternRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        // read info, do stuff.
                    }

                }));

        patternRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (patternRecyclerView.getFirstVisibleItemPosition() >= 0) {
                    int position = patternRecyclerView.getFirstVisibleItemPosition();
                    String name = mPatterns.get(position).getName();
                    String signature = mPatterns.get(position).getSignature();

                    Pattern pattern = new Pattern(name, signature, mContext);
                    mJam.setPattern(pattern);
                    sendBeatBroadcast(false);
                }
            }
        });
    }

    void kitChooser() {
        /**
         * setup recyclerView to allow user to change kit.
         * onScrollListener responds and reports position when changed. No click required.
         * */


        final SnappyRecyclerView kitRecyclerView = (SnappyRecyclerView) findViewById(R.id.kitRecyclerView);
        kitRecyclerView.setHasFixedSize(true);
        LinearLayoutManager kitLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        kitRecyclerView.setLayoutManager(kitLinearLayoutManager);

        kitRecyclerView.setContentDescription(getResources().getString(R.string.swipe_left_or_right_to_change_kit));
        kitRecyclerView.setFocusable(true);

        mKitListAdapter = new KitListAdapter(mKits, mContext);
        kitRecyclerView.setAdapter(mKitListAdapter);

        kitRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        // read info, do stuff.
                    }

                }));

        kitRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (kitRecyclerView.getFirstVisibleItemPosition() >= 0) {
                    int position = kitRecyclerView.getFirstVisibleItemPosition();
                    String name = mKits.get(position).getName();
                    String signature = mKits.get(position).getSignature();

                    Kit kit = new Kit(name, signature, mContext);
                    mJam.setKit(kit);
                    sendBeatBroadcast(false);
                }
            }
        });

    }

    void jamChooser() {

        /**
         * setup recyclerView that allows user to select Jam.
         * Jam selected by user is parsed for Kit, Pattern, and Tempo information.
         * Kit, Pattern, and Tempo controls are adjusted to reflect newly selected kit.
         * */

        final SnappyRecyclerView jamRecyclerView = (SnappyRecyclerView) findViewById(R.id.jamRecyclerView);
        jamRecyclerView.setHasFixedSize(true);
        final LinearLayoutManager jamLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        jamRecyclerView.setLayoutManager(jamLinearLayoutManager);
        jamRecyclerView.setContentDescription(getResources().getString(R.string.swipe_left_or_right_to_change_jam));
        jamRecyclerView.setFocusable(true);

        mJamListAdapter = new JamListAdapter(mJams, mContext);
        jamRecyclerView.setAdapter(mJamListAdapter);

        jamRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        // read info, do stuff.
                    }

                }));

        jamRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (jamRecyclerView.getFirstVisibleItemPosition() >= 0) {
                    int position = jamRecyclerView.getFirstVisibleItemPosition();
                    Kit kit = new Kit("temp", mJams.get(position).getKit(), mContext);
                    Pattern pattern = new Pattern("name", mJams.get(position).getPattern(), mContext);
                    int tempo = mJams.get(position).getTempo();
                    mJam.setName(mJams.get(position).getName());
                    mJam.setKit(kit);
                    mJam.setPattern(pattern);
                    mJam.setTempo(tempo);

                    SeekBar tempoBar = (SeekBar) findViewById(R.id.tempoBar);
                    tempoBar.setProgress(mJam.getTempo() - TEMPO_OFFSET);
                    /**
                     * Figure out if the pattern associated with the Jam is
                     * a pattern in our list already. Get the index of that
                     * pattern. If we can't find it, we create the pattern
                     * from the signature, and add it to the ArrayList.
                     * Rinse and repeat for the kit
                     **/
                    int patternIndex = -1;
                    for (int x = 0; x < mPatterns.size(); ++x) {
                        if (pattern.getPatternHexSignature().equals(mPatterns.get(x).getSignature())) {
                            patternIndex = x;
                        }
                    }

                    if (patternIndex == -1) {
                        FirebasePattern fbp = new FirebasePattern(mJams.get(position).getSignature(),
                                pattern.getPatternHexSignature());
                        mPatterns.add(fbp);
                        patternIndex = mPatterns.size() - 1;
                    }

                    SnappyRecyclerView patternRecyclerView = (SnappyRecyclerView) findViewById(R.id.patternRecyclerView);
                    patternRecyclerView.scrollToPosition(patternIndex);

                    int kitIndex = -1;
                    for (int x = 0; x < mKits.size(); ++x) {
                        if (kit.getSignature().equals(mKits.get(x).getSignature())) {
                            kitIndex = x;
                        }
                    }

                    if (kitIndex == -1) {
                        FirebaseKit fbk = new FirebaseKit(mJams.get(position).getSignature(),
                                kit.getSignature());
                        mKits.add(fbk);
                        kitIndex = mKits.size() - 1;
                    }
                    SnappyRecyclerView kitRecyclerView = (SnappyRecyclerView) findViewById(R.id.kitRecyclerView);
                    kitRecyclerView.scrollToPosition(kitIndex);

                    sendBeatBroadcast(false);
                }
            }
        });
    }

    public void actionButton() {
        /**
         * Setup Floating Action Button to start/stop metronome.
         * **/
        FloatingActionButton startstop;
        startstop = (FloatingActionButton) findViewById(R.id.startStopButton);
        startstop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendBeatBroadcast(true);
            }
        });
        startstop.setFocusable(true);
        startstop.setContentDescription(getResources().getString(R.string.start_or_stop));
    }

    public void getThatMoney(){
        /**
         * Setup AdMobs
         * **/
        //         MobileAds.initialize(getApplicationContext(), "ca-app-pub-8040545141030965~5491821531");

        AdView mAdView = (AdView) findViewById(R.id.adView);

        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("74D61A4429900485751F374428FB6C95")
                .build();
        mAdView.loadAd(adRequest);
    }

    void grabData() {
        /**
         * Assemble arrayLists of Local Data, Master Data, and User Data for
         * Patterns, Kits, and Jams. Master and User data comes from Firebase.
         * */
        mPatterns.clear();
        mMasterPatterns.clear();
        mUserPatterns.clear();

        mJams.clear();
        mMasterJams.clear();
        mUserJams.clear();

        mKits.clear();

        mKits.addAll(mLocalKits);
        mPatterns.addAll(mLocalPattern);
        mJams.addAll(mLocalJams);

        Kit kit = new Kit(getResources().getString(R.string.name), mJams.get(0).getKit(), mContext);
        Pattern pattern = new Pattern(getResources().getString(R.string.name), mJams.get(0).getPattern(), mContext);
        int tempo = mJams.get(0).getTempo();
        String name = mJams.get(0).getName();
        mJam.setKit(kit);
        mJam.setPattern(pattern);
        mJam.setTempo(tempo);
        mJam.setName(name);
        sendBeatBroadcast(false);

        SeekBar tempoBar = (SeekBar) findViewById(R.id.tempoBar);
        tempoBar.setProgress(tempo - TEMPO_OFFSET);

        mJamListAdapter.notifyDataSetChanged();
        mPatternListAdapter.notifyDataSetChanged();
        mKitListAdapter.notifyDataSetChanged();


        /**
         * Download the Jams table from Firebase
         * Send the information to jamChooser to setup the RecyclerView
         * Build the Jam we'll start with.
         * **/

        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("jams").child("master")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mMasterJams.clear();
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            FirebaseJam fbj = child.getValue(FirebaseJam.class);
                            if (fbj != null) {
                                mMasterJams.add(fbj);
                            }
                        }
                        mJams.clear();
                        mJams.addAll(mLocalJams);
                        mJams.addAll(mMasterJams);
                        mJams.addAll(mUserJams);
                        mJamListAdapter.notifyDataSetChanged();

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        mMasterJams.clear();
                        mJams.clear();
                        mJams.addAll(mLocalJams);
                        mJams.addAll(mMasterJams);
                        mJams.addAll(mUserJams);
                        mJamListAdapter.notifyDataSetChanged();
                    }
                });

        mDatabase.child("jams").child("users").child(userID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mUserJams.clear();
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            FirebaseJam fbj = child.getValue(FirebaseJam.class);
                            if (fbj != null) {
                                mUserJams.add(fbj);
                            }
                        }
                        mJams.clear();
                        mJams.addAll(mLocalJams);
                        mJams.addAll(mMasterJams);
                        mJams.addAll(mUserJams);
                        mJamListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        mJams.clear();
                        mJams.addAll(mLocalJams);
                        mJams.addAll(mMasterJams);
                        mJams.addAll(mUserJams);
                        mJamListAdapter.notifyDataSetChanged();
                    }
                });

        /**
         * Download the Kits table from Firebase
         * Send the information to the kitChooser to setup the RecyclerView
         *
         * */

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("kits").child("master")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mMasterKits.clear();
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            FirebaseKit fbk = child.getValue(FirebaseKit.class);
                            if (fbk != null) {
                                mMasterKits.add(fbk);
                            }
                        }
                        mKits.clear();
                        mKits.addAll(mLocalKits);
                        mKits.addAll(mMasterKits);
                        mKits.addAll(mUserKits);
                        mKitListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        mKits.clear();
                        mKits.addAll(mLocalKits);
                        mKits.addAll(mMasterKits);
                        mKits.addAll(mUserKits);
                        mKitListAdapter.notifyDataSetChanged();
                    }
                });

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("kits").child("users").child(userID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mUserKits.clear();
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            FirebaseKit fbk = child.getValue(FirebaseKit.class);
                            if (fbk != null) {
                                mUserKits.add(fbk);
                            }
                        }
                        mKits.clear();
                        mKits.addAll(mLocalKits);
                        mKits.addAll(mMasterKits);
                        mKits.addAll(mUserKits);
                        mKitListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        mKits.clear();
                        mKits.addAll(mLocalKits);
                        mKits.addAll(mMasterKits);
                        mKits.addAll(mUserKits);
                        mKitListAdapter.notifyDataSetChanged();
                    }
                });

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("patterns").child("master")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mMasterPatterns.clear();
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            FirebasePattern fbp = child.getValue(FirebasePattern.class);
                            if (fbp != null) {
                                mMasterPatterns.add(fbp);
                            }
                        }
                        mPatterns.clear();
                        mPatterns.addAll(mLocalPattern);
                        mPatterns.addAll(mMasterPatterns);
                        mPatterns.addAll(mUserPatterns);
                        mPatternListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        mPatterns.clear();
                        mPatterns.addAll(mLocalPattern);
                        mPatterns.addAll(mMasterPatterns);
                        mPatterns.addAll(mUserPatterns);
                        mPatternListAdapter.notifyDataSetChanged();
                    }
                });

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("patterns").child("users").child(userID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mUserPatterns.clear();
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            FirebasePattern fbp = child.getValue(FirebasePattern.class);
                            if (fbp != null) {
                                mUserPatterns.add(fbp);
                            }
                        }
                        mPatterns.clear();
                        mPatterns.addAll(mLocalPattern);
                        mPatterns.addAll(mMasterPatterns);
                        mPatterns.addAll(mUserPatterns);
                        mPatternListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        mPatterns.clear();
                        mPatterns.addAll(mLocalPattern);
                        mPatterns.addAll(mMasterPatterns);
                        mPatterns.addAll(mUserPatterns);
                        mPatternListAdapter.notifyDataSetChanged();
                    }
                });
    }

    public void sendBeatBroadcast(boolean fab) {
        /**
         * Send a message to the BeatService with new parameters.
         * Boolean tells the service if the start/stop has been pushed,
         * or the parameters have just been changed.
         * */

        Intent intent = new Intent();
        intent.setAction("com.umpquariversoftware.metronome.STARTSTOP");

        intent.putExtra("jamName", mJam.getName());

        intent.putExtra("tempo", mJam.getTempo());

        intent.putExtra("pattern", mJam.getPattern().getPatternHexSignature());

        ArrayList<Integer> components = new ArrayList<>();
        for (int x = 0; x < 8; ++x) {
            components.add(mJam.getKit().getComponents().get(x).getResource());
        }
        intent.putIntegerArrayListExtra("components", components);
        intent.putExtra("fab", fab);
        sendBroadcast(intent);
    }

    /**
     * Toolbar Functions
     */

    public void shareJam() {
        /**
         * Fires off a basic share intent with the hex signature of the current Jam.
         * Receiving party can search for this jam and replicate it on their end.
         * */

        FirebaseJam fbj = new FirebaseJam(mJam);
        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("jams").child("shared").child(fbj.getSignature()).setValue(fbj);

        // Email the download signature to your friend
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", " ", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "This is the ID to download");
        emailIntent.putExtra(Intent.EXTRA_TEXT, fbj.getSignature());
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

    void sendJamToFirebase() {
        /**
         * Send the current Jam to the users's firebase account.
         * First checks to make sure the Jam isn't already there.
         * */

        String signature = new FirebaseJam(mJam).getSignature();
        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mMasterListSearchResultsBack = false;
        mUserListSearchResultsBack = false;

        mMasterListJam = null;
        for (int x = 0; x < mMasterJams.size(); ++x) {
            if (mMasterJams.get(x).getSignature().equals(signature)) {
                mMasterListJam = new FirebaseJam(mMasterJams.get(x).getTempo(),
                        mMasterJams.get(x).getKit(),
                        mMasterJams.get(x).getPattern());
                mMasterListJam.setName(mMasterJams.get(x).getName());
            }
        }
        mMasterListSearchResultsBack = true;

        mDatabase.child("jams").child("users").child(userID).child(signature)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FirebaseJam userListJam = dataSnapshot.getValue(FirebaseJam.class);
                        if (userListJam != null) {
                            mUserListJam = userListJam;
                        } else {
                            mUserListJam = null;
                        }
                        mUserListSearchResultsBack = true;
                        if (mMasterListSearchResultsBack) {
                            if (mMasterListJam != null) {
                                alert(getString(R.string.jam_exists), mMasterListJam.getName());
                            } else if (mUserListJam != null) {
                                alert(getString(R.string.jam_exists), mUserListJam.getName());
                            } else {
                                askAndInsert();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.menuPatternEditor): {
                if (networkIsConnected && userIsLoggedIn) {
                    Intent i = new Intent(this, PatternEditor.class);
                    i.putExtra("userID", userID);
                    startActivity(i);
                } else {
                    Toast.makeText(mContext, R.string.network_required, Toast.LENGTH_LONG).show();
                }
                return true;
            }
            case R.id.menuKitEditor: {
                if (networkIsConnected && userIsLoggedIn) {
                    Intent i = new Intent(this, KitEditor.class);
                    i.putExtra("userID", userID);
                    startActivity(i);
                } else {
                    Toast.makeText(mContext, R.string.network_required, Toast.LENGTH_LONG).show();
                }
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    /**
     * Utilities/Pushing data around
     */

    void alert(String text1, String text2) {
        /**
         * Basic Alert Dialog
         * */
        final Dialog dialog = new Dialog(mContext);

        dialog.setContentView(R.layout.alert_dialog);

        TextView alertText = (TextView) dialog.findViewById(R.id.alertText);
        alertText.setText(text1);

        TextView alertText2 = (TextView) dialog.findViewById(R.id.alertText2);
        alertText2.setText(text2);


        Button okButton = (Button) dialog.findViewById(R.id.alertOK);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    void askAndInsert() {
        /**
         * Queries for a name and saves jam to firebase using that name.
         * */
        final DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        new MaterialDialog.Builder(mContext).title(R.string.save_jam_to_cloud)
                .content(R.string.give_jam_a_name)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        FirebaseJam fbj = new FirebaseJam(mJam);
                        fbj.setName(input.toString());
                        mDatabase.child("jams")
                                .child("users")
                                .child(userID)
                                .child(fbj.getSignature())
                                .setValue(fbj);
                    }
                })
                .show();
    }

    public void addSharedJamFromFirebase(String signature) {
        /**
         * Writes a Jam to the shared firebase folder.
         * */
        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mDatabase.child("jams").child("shared").child(signature)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FirebaseJam newFbj = dataSnapshot.getValue(FirebaseJam.class);
                        if (newFbj != null) {
                            int tempo = newFbj.getTempo();
                            Pattern pattern = new Pattern(newFbj.getSignature(), newFbj.getPattern(), mContext);
                            Kit kit = new Kit(newFbj.getSignature(), newFbj.getKit(), mContext);

                            Jam jam = new Jam();
                            jam.setName(getResources().getString(R.string.downloaded_jam) + " " + newFbj.getSignature().substring(newFbj.getSignature().length() - 6));
                            jam.setTempo(tempo);
                            jam.setPattern(pattern);
                            jam.setKit(kit);

                            // Jam has been downloaded and instantiated here. Now, write
                            // to the user's jam table, which should kick off that
                            // change listener and update the UI

                            mJam = jam;
                            sendJamToFirebase();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("jamSignature", new FirebaseJam(mJam).getSignature());
    }

    public void setupNetworkMonitor(){
        /**
         * Watches network state. Changes member level Boolean networkIsConnected if anything
         * changes.
         * **/

        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        registerReceiver(new MainActivity.networkStatusChangeReceiver(),
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        networkIsConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    public void authenticateUser(){

        /**
         * Authenticates user, and watches for changes.
         *
         * **/

        mAuth = FirebaseAuth.getInstance();
        userIsLoggedIn = false;

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    userIsLoggedIn = true;
                    setupToolbar();
                    userID = user.getUid();
                    grabData();

                } else {
                    setupToolbar();
                    userIsLoggedIn = false;
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                }
                // ...
            }
        };
    }

    public class networkStatusChangeReceiver extends BroadcastReceiver {

        public networkStatusChangeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm =
                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            networkIsConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
            setupToolbar();
        }
    }

    public class widgetReceiver extends BroadcastReceiver {

        public widgetReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            sendBeatBroadcast(true);
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        networkIsConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        mPatternListAdapter.notifyDataSetChanged();
        mKitListAdapter.notifyDataSetChanged();
        mJamListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

}
