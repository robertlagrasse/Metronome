package com.umpquariversoftware.metronome.UI;


import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.database.dbContract;
import com.umpquariversoftware.metronome.elements.Beat;
import com.umpquariversoftware.metronome.elements.Component;
import com.umpquariversoftware.metronome.elements.Jam;
import com.umpquariversoftware.metronome.elements.Kit;
import com.umpquariversoftware.metronome.elements.Pattern;
import com.umpquariversoftware.metronome.patternEditor.PatternEditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import static com.umpquariversoftware.metronome.database.dbContract.*;

/** OVERVIEW
 *
 * The user interface for metronome allows the user to build their own Jam.
 * A Jam consists of three parts - all user selectable on the main screen
 *
 * Tempo (Set by seekbar)
 * Kit (Selected via recyclerview)
 * Pattern (Selected via recyclerview)
 *
 * Kits consist of 8 components. A component is a sound.
 * A pattern is a sequence of beats. A beat is an array of 8 boolean values. These boolean values
 * will correlate with the components in each kit.
 *
 * For every tick of the timer, the app will cycle through the pattern. The beat in the pattern
 * will determine which components sound on that tick.
 *
 * I chose the data structures here very specifically. Beats consist of 8 binary values, which
 * correspond to the 8 components in a kit. Any beat can be represented as a two digit hex value.
 * These values can the chained together to create patterns of arbitrary length, with the complete
 * information for any beat only taking up a single byte in the database. This facilitates both
 * efficient DB storage, and easy sharing between users.
 *
 * Component values have associated 2 Digit Hexidecimal values in the database HEXID
 * All components will be supplied with the software. No user supplied sounds will be allowed.
 * Essentially, this allows me to employ the same identification and sharing technique.
 *
 * Users can pass all of the information necessary to share their Jam in the space of a tweet.
 *
 */


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    String TAG = "MainActivity";
    Jam mJam = new Jam();
    String mKitID, mPatternID;
    Boolean beatServiceRunning = false;
    private Toolbar toolbar;

    /**
     *  Three cursor adapters and their associated loaders.
     */
    patternCursorAdapter mPatternCursorAdapter;
    Cursor mPatternCursor;

    kitCursorAdapter mKitCursorAdapter;
    Cursor mKitCursor;

    jamCursorAdapter mJamCursorAdapter;
    Cursor mJamCursor;


    private static final int PATTERN_LOADER_ID = 0;
    private static final int KIT_LOADER_ID = 1;
    private static final int JAM_LOADER_ID = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Are we already running? No need to start the service if so.
         * */

        if (savedInstanceState != null){
            // Already running.
        } else {
            beatServiceRunning = false;
        }

        /**
         * Build the layout, and lock down the orientation.
         * */

        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        /**
         * Setup the toolbar and its buttons
         * */

        toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageView patternEditorButton = (ImageView) findViewById(R.id.patternEditorButton);


        patternEditorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), PatternEditor.class);
                startActivity(i);
            }
        });

        /**
         * Is this the first time we've ever run?
         * */

        int lastLoadedJamID;
        SharedPreferences prefs = null;
        prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);

        if(prefs.getBoolean("firstrun", true)){
            /**
             *  If this is the first time the app's been run, we'll need to do some
             *  basic setup. The database tables will be created and populated,
             *  and the default Jam will be selected.
             */
            createComponentsTable();
            createKitTable();
            createPatternTable();
            createJamTable();

            lastLoadedJamID = 1;
            prefs.edit().putBoolean("firstrun", false).commit();
        } else {
            /**
             * We've been here before. Load the last jam we worked with
             */
            lastLoadedJamID = prefs.getInt("jamID", 1);
        }

        /**
         * Build the Jam the UI will present to the user.
         * */

        Log.e("MainAcivity","Building JamID: " + lastLoadedJamID + " from database.");

        mJam = buildJamFromDB(lastLoadedJamID);
        prefs.edit().putInt("jamID", mJam.getDbID()).commit();

        /**
         * Start the beat service. Do this before you stand up the UI components
         * so they have someplace to send events.
         * **/

        if(!beatServiceRunning){
            Log.e("onCreate","beatServiceRunning = false. launchBeatService()...");
            launchBeatService();
            beatServiceRunning = true;
        }


        /**
         *
         * Populate the UI
         *
         */
        setupTempoChooser();
        setupPatternChooser();
        setupKitChooser();
        setupJamChooser();
        setupStartStopFAB();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void setupTempoChooser(){
        int tempo = mJam.getTempo();
        SeekBar tempoBar = (SeekBar) findViewById(R.id.tempoBar);
        tempoBar.setProgress(tempo-30);
        tempoBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // Modify the tempo of the current Jam as we slide
                mJam.setTempo(i+30);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // When we stop sliding, send the jam to the database
                // Then alert the BeatService to pull the new Jam.
                sendBeatBroadcast();
            }
        });
    }

    public void setupPatternChooser(){
        getLoaderManager().initLoader(PATTERN_LOADER_ID, null, this);

        final SnappyRecyclerView patternRecyclerView = (SnappyRecyclerView) findViewById(R.id.patternRecyclerView);
        patternRecyclerView.setHasFixedSize(true);
        final LinearLayoutManager patternLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        patternRecyclerView.setLayoutManager(patternLinearLayoutManager);

        final SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(patternRecyclerView);

        mPatternCursorAdapter = new patternCursorAdapter(this, null);
        patternRecyclerView.setAdapter(mPatternCursorAdapter);

        patternRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        // read info, do stuff.
                        Log.e("recyclerview", "clicked " + position);
                    }

                }));

        patternRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(patternRecyclerView.getFirstVisibleItemPosition() >=0){
                    // get information from db
                    String patternID = String.valueOf(patternRecyclerView.getFirstVisibleItemPosition() + 1);
                    mPatternID = patternID;
                    Cursor retCursor = getContentResolver().query(buildPatternUri().buildUpon().appendPath(patternID).build(),
                            null,
                            null,
                            null,
                            null);
                    retCursor.moveToFirst();

                    String patternName = retCursor.getString(retCursor.getColumnIndex(PatternTable.NAME));
                    String patternSequence = retCursor.getString(retCursor.getColumnIndex(PatternTable.SEQUENCE));
                    Pattern pattern = new Pattern(patternName, patternSequence, getApplicationContext());
                    mJam.setPattern(pattern);

                    // Then alert the BeatService
                    sendBeatBroadcast();
                }
            }
        });


    }

    public void setupKitChooser(){
        getLoaderManager().initLoader(KIT_LOADER_ID, null, this);

        final SnappyRecyclerView kitRecyclerView = (SnappyRecyclerView) findViewById(R.id.kitRecyclerView);
        kitRecyclerView.setHasFixedSize(true);
        LinearLayoutManager kitLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        kitRecyclerView.setLayoutManager(kitLinearLayoutManager);

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(kitRecyclerView);

        mKitCursorAdapter = new kitCursorAdapter(this, null);
        kitRecyclerView.setAdapter(mKitCursorAdapter);

        kitRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        mKitCursor.moveToPosition(position);
                        Log.e("recyclerview", "click!");
                        // read info, do stuff.
                    }
                }));

        kitRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(kitRecyclerView.getFirstVisibleItemPosition() >=0){
                    // get information from db
                    String kitID = String.valueOf(kitRecyclerView.getFirstVisibleItemPosition() + 1);
                    mKitID = kitID;
                    Cursor retCursor = getContentResolver().query(buildKitUri().buildUpon().appendPath(kitID).build(),
                            null,
                            null,
                            null,
                            null);
                    retCursor.moveToFirst();

                    String kitName = retCursor.getString(retCursor.getColumnIndex(KitTable.NAME));
                    String kitSequence = retCursor.getString(retCursor.getColumnIndex(KitTable.COMPONENTS));
                    Kit kit = new Kit(kitName, kitSequence, getApplicationContext());
                    mJam.setKit(kit);
                    // Then alert the BeatService
                    sendBeatBroadcast();
                }
            }
        });
    }

    public void setupJamChooser(){
        /**
         *  Setup UI and Loader
         */

        getLoaderManager().initLoader(JAM_LOADER_ID, null, this);

        final SnappyRecyclerView jamRecyclerView = (SnappyRecyclerView) findViewById(R.id.jamRecyclerView);
        jamRecyclerView.setHasFixedSize(true);
        final LinearLayoutManager jamLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        jamRecyclerView.setLayoutManager(jamLinearLayoutManager);

        mJamCursorAdapter = new jamCursorAdapter(this, null);
        jamRecyclerView.setAdapter(mJamCursorAdapter);

        /**
         *  Listen for Clicks
         */

        jamRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        mJamCursor.moveToPosition(position);
                        Log.e("recyclerview", "click!");
                        // read info, do stuff.
                    }
                }));

        /**
         * Listen for Scroll Events
         */

        jamRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(jamRecyclerView.getFirstVisibleItemPosition() >=0){

                    mJamCursor.moveToPosition(jamRecyclerView.getFirstVisibleItemPosition());
                    long id = mJamCursorAdapter.getItemId(jamRecyclerView.getFirstVisibleItemPosition());
                    mJam = buildJamFromDB(id);

                    /**
                     *  Identify the corresponding kit by searching for its signature in the DB
                     *  Once located, move the recyclerview to that position
                     */

                    Cursor retCursor = getContentResolver().query(buildKitUri(),
                            null,
                            KitTable.COMPONENTS + " = ?",
                            new String[]{mJam.getKit().getSignature()},
                            null);
                    retCursor.moveToFirst();

                    int kitID = Integer.parseInt(retCursor.getString(retCursor.getColumnIndex(KitTable.ID)));

                    for(int x=0;x<mKitCursorAdapter.getItemCount();++x){
                        if (mKitCursorAdapter.getItemId(x) == kitID){
                            id = x;
                        }
                    }

                    SnappyRecyclerView kit = (SnappyRecyclerView) findViewById(R.id.kitRecyclerView);
                    kit.scrollToPosition((int) id);

                    /**
                     * Identify the corresponding pattern by searching for its signature in the DB
                     * Once located, move the recyclerview to that position.
                     */

                    retCursor = getContentResolver().query(buildPatternUri(),
                            null,
                            PatternTable.SEQUENCE + " = ?",
                            new String[]{mJam.getPattern().getPatternHexSignature()},
                            null);
                    retCursor.moveToFirst();

                    int patternID = Integer.parseInt(retCursor.getString(retCursor.getColumnIndex(PatternTable.ID)));

                    for(int x=0;x<mPatternCursorAdapter.getItemCount();++x){
                        if (mPatternCursorAdapter.getItemId(x) == patternID){
                            id = x;
                        }
                    }
                    SnappyRecyclerView pattern = (SnappyRecyclerView) findViewById(R.id.patternRecyclerView);
                    pattern.scrollToPosition((int) id);

                    /**
                     * The Jam has been changed. Stop start the timer in response if it's running.
                     */

                    sendBeatBroadcast();
                }
            }
        });

    }

    public void setupStartStopFAB(){
        FloatingActionButton startstop;
        startstop = (FloatingActionButton) findViewById(R.id.startStopButton);
        startstop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.e("MainActivity", "startStop()");
                sendBeatBroadcast();
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.e("onCreateLoader", "int i = " + i);
        switch (i) {
            case PATTERN_LOADER_ID:
                return new CursorLoader(this, buildPatternUri(),
                        null,
                        null,
                        null,
                        null);
            case KIT_LOADER_ID:
                return new CursorLoader(this, buildKitUri(),
                        null,
                        null,
                        null,
                        null);
            case JAM_LOADER_ID:
                return new CursorLoader(this, buildJamUri(),
                        null,
                        null,
                        null,
                        null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch(loader.getId()) {
            case PATTERN_LOADER_ID:
                mPatternCursorAdapter.swapCursor(data);
                mPatternCursor = data;
                break;
            case KIT_LOADER_ID:
                mKitCursorAdapter.swapCursor(data);
                mKitCursor = data;
                break;
            case JAM_LOADER_ID:
                mJamCursorAdapter.swapCursor(data);
                mJamCursor = data;
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public class SoundPoolPlayer {
        private SoundPool mShortPlayer= null;
        private HashMap mSounds = new HashMap();


        public SoundPoolPlayer(Context pContext, Kit kit)
        {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.mShortPlayer = new SoundPool.Builder().build();
            } else {
                this.mShortPlayer = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
                // Deprecated constructor.
            }

            // Get components from kit
            ArrayList<Component> components = kit.getComponents();

            // Iterate through components, extract resource ID, add to soundpool
            for(int x=0;x<components.size();x++){
                mSounds.put(components.get(x).getResource(), this.mShortPlayer.load(pContext, components.get(x).getResource(),1));
            }
        }

        public void playShortResource(int piResource) {
            int iSoundId = (Integer) mSounds.get(piResource);
            this.mShortPlayer.play(iSoundId, 0.99f, 0.99f, 0, 0, 1);
        }

        // Cleanup
        public void release() {
            // Cleanup
            this.mShortPlayer.release();
            this.mShortPlayer = null;
        }
    }

    private Jam buildTestJam(){
        /**
         * buildTestJam()
         *
         * Build Some Individual Beats
         * Put Beats into a Pattern
         *
         * Build Some Components
         * Put up to 8 components into a Kit
         *
         * Build a Jam
         * Add the Kit, Pattern, and the Tempo
         * */
        // Instantiate Beats
        Beat beat1 = new Beat();
        Beat beat2 = new Beat();
        Beat beat3 = new Beat();
        Beat beat4 = new Beat();
        Beat beat5 = new Beat();
        Beat beat6 = new Beat();
        Beat beat7 = new Beat();
        Beat beat8 = new Beat();


        beat3.setSECOND(true);
        beat7.setSECOND(true);
        beat8.setSECOND(true);
        beat4.setEIGHTH(true);


        // Instantiate Pattern, add beat
        Pattern pattern = new Pattern();
        pattern.addBeat(beat1);
        pattern.addBeat(beat2);
        pattern.addBeat(beat3);
        pattern.addBeat(beat4);
        pattern.addBeat(beat5);
        pattern.addBeat(beat6);
        pattern.addBeat(beat7);
        pattern.addBeat(beat8);
        pattern.setName("Default Pattern");

        // Build components and assemble kit from signature
        Kit kit = new Kit("slick kit name", "05040609080A060C", this);

        // Instantiate Jam - Add Pattern, Kit, Tempo
        Jam jam = new Jam();
        jam.setKit(kit);
        jam.setPattern(pattern);
        jam.setTempo(120);
        jam.setName("Default Jam");

        return jam;
    }

    private Jam buildDefaultJam(){
        /**
         * buildTestJam()
         *
         * Build Some Individual Beats
         * Put Beats into a Pattern
         *
         * Build Some Components
         * Put up to 8 components into a Kit
         *
         * Build a Jam
         * Add the Kit, Pattern, and the Tempo
         * */
        // Instantiate Beats
        Beat beat1 = new Beat();


        // Instantiate Pattern, add beat
        Pattern pattern = new Pattern();
        pattern.addBeat(beat1);


        // Instantiate Components
        Component componentOne = new Component(R.raw.default_kick);
        Component componentTwo = new Component(R.raw.default_snare);
        Component componentThree = new Component(R.raw.default_crash);
        Component componentFour = new Component(R.raw.default_ride);
        Component componentFive = new Component(R.raw.default_highhat);
        Component componentSix = new Component(R.raw.default_tom1);
        Component componentSeven = new Component(R.raw.default_tom2);
        Component componentEight = new Component(R.raw.default_tom3);

        componentOne.setName("Default Kick");
        componentTwo.setName("Default Snare");
        componentThree.setName("Default Crash");
        componentFour.setName("Default Ride");
        componentFive.setName("Default High Hat");
        componentSix.setName("Default Tom 1");
        componentSeven.setName("Default Tom 2");
        componentEight.setName("Default Tom 3");

        // Instantiate Kit, add instruments
        Kit kit = new Kit();
        kit.addComponent(componentOne);
        kit.addComponent(componentTwo);
        kit.addComponent(componentThree);
        kit.addComponent(componentFour);
        kit.addComponent(componentFive);
        kit.addComponent(componentSix);
        kit.addComponent(componentSeven);
        kit.addComponent(componentEight);
        kit.setName("Kit name");

        // Instantiate Jam - Add Pattern, Kit, Tempo
        Jam jam = new Jam();
        jam.setKit(kit);
        jam.setPattern(pattern);
        jam.setTempo(120);
        jam.setName("Jam Name");

        return jam;
    }

    private void showKit(Kit kit){

        /***
         * showKit() is a simple method to populate the Kit Cardview
         * items with data from the jam.
         */
        TextView component1 = (TextView) findViewById(R.id.component1);
        TextView component2 = (TextView) findViewById(R.id.component2);
        TextView component3 = (TextView) findViewById(R.id.component3);
        TextView component4 = (TextView) findViewById(R.id.component4);
        TextView component5 = (TextView) findViewById(R.id.component5);
        TextView component6 = (TextView) findViewById(R.id.component6);
        TextView component7 = (TextView) findViewById(R.id.component7);
        TextView component8 = (TextView) findViewById(R.id.component8);
        TextView kitName = (TextView) findViewById(R.id.kitName);

        component1.setText(kit.getComponents().get(0).getName());
        component2.setText(kit.getComponents().get(1).getName());
        component3.setText(kit.getComponents().get(2).getName());
        component4.setText(kit.getComponents().get(3).getName());
        component5.setText(kit.getComponents().get(4).getName());
        component6.setText(kit.getComponents().get(5).getName());
        component7.setText(kit.getComponents().get(6).getName());
        component8.setText(kit.getComponents().get(7).getName());
        kitName.setText(kit.getName());
    }


    private void createComponentsTable(){
        ContentValues contentValues;
        ArrayList<ContentValues> components = new ArrayList<>();

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default Bass");
        contentValues.put(ComponentTable.RESOURCE, R.raw.bass);
        contentValues.put(ComponentTable.HEXID, "00");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default Button1");
        contentValues.put(ComponentTable.RESOURCE, R.raw.button1);
        contentValues.put(ComponentTable.HEXID, "01");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default Button3");
        contentValues.put(ComponentTable.RESOURCE, R.raw.button3);
        contentValues.put(ComponentTable.HEXID, "02");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default Crash");
        contentValues.put(ComponentTable.RESOURCE, R.raw.default_crash);
        contentValues.put(ComponentTable.HEXID, "03");

        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default HiHat");
        contentValues.put(ComponentTable.RESOURCE, R.raw.default_highhat);
        contentValues.put(ComponentTable.HEXID, "04");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default Kick");
        contentValues.put(ComponentTable.RESOURCE, R.raw.default_kick);
        contentValues.put(ComponentTable.HEXID, "05");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default Ride");
        contentValues.put(ComponentTable.RESOURCE, R.raw.default_ride);
        contentValues.put(ComponentTable.HEXID, "06");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default Snare");
        contentValues.put(ComponentTable.RESOURCE, R.raw.default_snare);
        contentValues.put(ComponentTable.HEXID, "07");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default Tom1");
        contentValues.put(ComponentTable.RESOURCE, R.raw.default_tom1);
        contentValues.put(ComponentTable.HEXID, "08");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default Tom2");
        contentValues.put(ComponentTable.RESOURCE, R.raw.default_tom2);
        contentValues.put(ComponentTable.HEXID, "09");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default Tom3");
        contentValues.put(ComponentTable.RESOURCE, R.raw.default_tom3);
        contentValues.put(ComponentTable.HEXID, "0A");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default HiHat");
        contentValues.put(ComponentTable.RESOURCE, R.raw.hihat);
        contentValues.put(ComponentTable.HEXID, "0B");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default Snare");
        contentValues.put(ComponentTable.RESOURCE, R.raw.snare);
        contentValues.put(ComponentTable.HEXID, "0C");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(ComponentTable.NAME, "Default Tom");
        contentValues.put(ComponentTable.RESOURCE, R.raw.tom);
        contentValues.put(ComponentTable.HEXID, "0D");
        components.add(contentValues);

        for(int x=0;x<components.size();x++){
            getContentResolver().insert(buildComponentUri(), components.get(x));
        }

    }

    private void createKitTable(){
        ArrayList<ContentValues> kits = new ArrayList<>();
        ContentValues contentValues;

        contentValues = new ContentValues();
        contentValues.put(KitTable.NAME, "Default Kit");
        contentValues.put(KitTable.COMPONENTS, "0102030405060708");
        kits.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(KitTable.NAME, "Another Kit");
        contentValues.put(KitTable.COMPONENTS, "0C0A0B040508090A");
        kits.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(KitTable.NAME, "A New Kit");
        contentValues.put(KitTable.COMPONENTS, "05040609080A060C");
        kits.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(KitTable.NAME, "Unique Kit Kit");
        contentValues.put(KitTable.COMPONENTS, "04080A0C06090703");
        kits.add(contentValues);

        for(int x=0;x<kits.size();x++){
            Uri i = getContentResolver().insert(buildKitUri(), kits.get(x));
            Log.e("CreateKitTable", "insert() Returned URI:" + i.toString());
        }
    }

    private void createPatternTable(){
        ArrayList<ContentValues> patterns = new ArrayList<>();
        ContentValues contentValues;

        contentValues = new ContentValues();
        contentValues.put(PatternTable.NAME, "Default Pattern");
        contentValues.put(PatternTable.SEQUENCE, "01");
        patterns.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(PatternTable.NAME, "2 Beat");
        contentValues.put(PatternTable.SEQUENCE, "0102");
        patterns.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(PatternTable.NAME, "4 Beat");
        contentValues.put(PatternTable.SEQUENCE, "01010201");
        patterns.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(PatternTable.NAME, "3 Beat");
        contentValues.put(PatternTable.SEQUENCE, "010103");
        patterns.add(contentValues);

        for(int x=0;x<patterns.size();x++){
            Uri i = getContentResolver().insert(buildPatternUri(), patterns.get(x));
            Log.e("CreatePatternTable", "insert() Returned URI:" + i.toString());
        }
    }

    private void createJamTable(){
        ArrayList<ContentValues> jams = new ArrayList<>();
        ContentValues contentValues;

        contentValues = new ContentValues();
        contentValues.put(JamTable.NAME, "Default Jam");
        contentValues.put(JamTable.KIT_ID, "1");
        contentValues.put(JamTable.PATTERN_ID, "1");
        contentValues.put(JamTable.TEMPO, "60");
        jams.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(JamTable.NAME, "Jam Two");
        contentValues.put(JamTable.KIT_ID, "2");
        contentValues.put(JamTable.PATTERN_ID, "2");
        contentValues.put(JamTable.TEMPO, "90");
        jams.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(JamTable.NAME, "Jam 3");
        contentValues.put(JamTable.KIT_ID, "3");
        contentValues.put(JamTable.PATTERN_ID, "3");
        contentValues.put(JamTable.TEMPO, "120");
        jams.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(JamTable.NAME, "Jam 4");
        contentValues.put(JamTable.KIT_ID, "4");
        contentValues.put(JamTable.PATTERN_ID, "4");
        contentValues.put(JamTable.TEMPO, "180");
        jams.add(contentValues);

        for(int x=0;x<jams.size();x++){
            Uri i = getContentResolver().insert(buildJamUri(), jams.get(x));
        }
    }

    private Jam buildJamFromDB(long id){

        /**
         * A Jam has to have the following parts:
         *
         * Name
         * Tempo
         * Kit
         * Pattern
         *
         * We'll pull the Name and Tempo from the DB directly, along with references
         * to the Kit and the Pattern info, then build the kit and the pattern from there.
         *
         */


        // For now, just grab the first Jam. I'll build a way to track the last jam
        // and pick that one specifically.

        Cursor retCursor = getContentResolver().query(buildJamUri().buildUpon().appendPath(String.valueOf(id)).build(),
                null,
                null,
                null,
                null);
        retCursor.moveToFirst();

        String jamName = retCursor.getString(retCursor.getColumnIndex(JamTable.NAME));
        int jamTempo = Integer.parseInt(retCursor.getString(retCursor.getColumnIndex(JamTable.TEMPO)));
        int dbID = Integer.parseInt(retCursor.getString(retCursor.getColumnIndex(JamTable.ID)));

        String kitID = retCursor.getString(retCursor.getColumnIndex(JamTable.KIT_ID));
        String patternID = retCursor.getString(retCursor.getColumnIndex(JamTable.PATTERN_ID));

        retCursor.close();

        /**
         *
         * Now we'll build the pattern. We'll use the pattern ID to get the pattern sequence
         * from the database, then use that sequence to create the beats.
         *
         * A pattern is a name and an array list of beats.
         *
         * The Pattern class has a constructor that will build a pattern directly from a
         * signature. It leverages a Beat constructor that creates a beat from an individual
         * Hex value.
         *
         */

        retCursor = getContentResolver().query(buildPatternUri().buildUpon().appendPath(patternID).build(),
                null,
                null,
                null,
                null);
        retCursor.moveToFirst();

        String patternName = retCursor.getString(retCursor.getColumnIndex(PatternTable.NAME));
        String patternSequence = retCursor.getString(retCursor.getColumnIndex(PatternTable.SEQUENCE));

        Pattern pattern = new Pattern(patternName, patternSequence, this);


        /**
         *
         * Next we build the Kit. A kit is a name and an array list of components.
         *
         * We'll use the KitID to get the Kit sequence from the DB.
         *
         * The Kit class has a constructor that will build a kit directly from a signature.
         *
         */

        retCursor = getContentResolver().query(buildKitUri().buildUpon().appendPath(kitID).build(),
                null,
                null,
                null,
                null);
        retCursor.moveToFirst();

        String kitComponents = retCursor.getString(retCursor.getColumnIndex(KitTable.COMPONENTS));
        String kitName = retCursor.getString(retCursor.getColumnIndex(KitTable.NAME));
        retCursor.close();

        Kit kit = new Kit(kitName, kitComponents, this);
        kit.setName(kitName);

        /**
         * Finally, we bring all of the pieces together and create the jam.
         */

        Jam jam = new Jam();

        jam.setName(jamName);
        jam.setTempo(jamTempo);
        jam.setKit(kit);
        jam.setPattern(pattern);
        jam.setDbID(dbID);

        return jam;
    }

    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putInt("jamID", mJam.getDbID());
        Log.e("onSaveInstanceState", "writing...");
    }

    private void launchBeatService(){
        Intent i = new Intent(this, BeatService.class);
        i.putExtra("jamID", 2L);
        startService(i);
    }

    public void sendBeatBroadcast(){
        Intent intent = new Intent();
        intent.setAction("com.umpquariversoftware.metronome.STARTSTOP");

        Log.e("sendBeatBroadcast", "sending this tempo value: " + mJam.getTempo());
        intent.putExtra("tempo", mJam.getTempo());

        Log.e("sendBeatBroadcast", "sending this pattern Signature: " + mJam.getPattern().getPatternHexSignature());
        intent.putExtra("pattern", mJam.getPattern().getPatternHexSignature());

        ArrayList<Integer> components = new ArrayList<>();
        for(int x = 0; x<8; ++x){
            Log.e("sendBeatBroadcast", "adding this component: " + mJam.getKit().getComponents().get(x).getResource());
            components.add(mJam.getKit().getComponents().get(x).getResource());
        }
        intent.putIntegerArrayListExtra("components", components);
        sendBroadcast(intent);
    }


}
