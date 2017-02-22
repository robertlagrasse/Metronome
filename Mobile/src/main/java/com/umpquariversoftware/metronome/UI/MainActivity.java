package com.umpquariversoftware.metronome.UI;


import android.app.Dialog;
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
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.umpquariversoftware.metronome.FireBase.FirebaseJam;
import com.umpquariversoftware.metronome.FireBase.FirebaseKit;
import com.umpquariversoftware.metronome.FireBase.FirebasePattern;
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.database.dbContract;
import com.umpquariversoftware.metronome.elements.Beat;
import com.umpquariversoftware.metronome.elements.Component;
import com.umpquariversoftware.metronome.elements.Jam;
import com.umpquariversoftware.metronome.elements.Kit;
import com.umpquariversoftware.metronome.elements.Pattern;
import com.umpquariversoftware.metronome.kitEditor.KitEditor;
import com.umpquariversoftware.metronome.patternEditor.PatternEditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import static com.umpquariversoftware.metronome.R.id.kitRecyclerView;
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
    Context mContext;


    /**
     *  Three cursor adapters and their associated loaders.
     */
    patternCursorAdapter mPatternCursorAdapter;
    Cursor mPatternCursor;

    kitCursorAdapter mKitCursorAdapter;
    Cursor mKitCursor;

    jamCursorAdapter mJamCursorAdapter;
    Cursor mJamCursor;

    ArrayList<FirebaseKit> mKits = new ArrayList<>();
    ArrayList<FirebasePattern> mPatterns = new ArrayList<>();
    ArrayList<FirebaseJam> mJams = new ArrayList<>();

    patternListAdapter mPatternListAdapter;
    kitListAdapter mKitListAdapter;
    jamListAdapter mJamListAdapter;

    private static final int PATTERN_LOADER_ID = 0;
    private static final int KIT_LOADER_ID = 1;
    private static final int JAM_LOADER_ID = 2;

    @Override
    public void onResume() {
        super.onResume();
        resetLoaders();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
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

        // mJam = buildJamFromDB(lastLoadedJamID);
        prefs.edit().putInt("jamID", mJam.getDbID()).commit();

        /**
         * Start the beat service. Do this before you stand up the UI components
         * so they have someplace to send events.
         * **/

        if(!beatServiceRunning){
            launchBeatService();
            beatServiceRunning = true;
        }


        /**
         *
         * Populate the UI
         *
         */
        setupToolbar();
//        setupPatternChooser();

//        setupKitChooser();
//        setupJamChooser();
//        setupStartStopFAB();

        /**
         * There is already data in firebase. Even if there wasn't, it would be created
         * when you run this the first time.
         * */

        populateArrayListsFromFirebase();
        setupTempoChooser();
        setupStartStopFAB();

    }

    void populateArrayListsFromFirebase(){
        mJams.clear();
        mPatterns.clear();
        mKits.clear();

        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("jams").child("master")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child: dataSnapshot.getChildren()) {
                            FirebaseJam fbj = child.getValue(FirebaseJam.class);
                            if (fbj != null) {
                                mJams.add(fbj);
                                Log.e("MainActivity", "Added jam to ArrayList with signature: "
                                        + fbj.getSignature()
                                + " at position: " + mJams.size());
                            }
                        }
                        jamChooser();
                        Kit kit = new Kit("temp", mJams.get(0).getKit(), mContext);
                        Pattern pattern = new Pattern("name", mJams.get(0).getPattern(), mContext);
                        int tempo = mJams.get(0).getTempo();
                        mJam.setKit(kit);
                        mJam.setPattern(pattern);
                        mJam.setTempo(tempo);

                        /**
                         * See if the pattern is in the pattern list. If not, add it.
                         * Move the PatternRecyclerView to the position that reflects
                         * the pattern
                         *
                         * */

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("kits").child("master")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child: dataSnapshot.getChildren()) {
                            FirebaseKit fbk = child.getValue(FirebaseKit.class);
                            if (fbk != null) {
                                mKits.add(fbk);
                                Log.e("MainActivity", "Added Kit to ArrayList with signature: "
                                        + fbk.getSignature()
                                        + " at position: " + mKits.size());
                            }
                        }
                        kitChooser();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("patterns").child("master")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child: dataSnapshot.getChildren()) {
                            FirebasePattern fbp = child.getValue(FirebasePattern.class);
                            if (fbp != null) {
                                mPatterns.add(fbp);
                                Log.e("MainActivity", "Added pattern to ArrayList with signature: "
                                        + fbp.getSignature()
                                        + " at position: " + mPatterns.size());
                            }
                        }
                        patternChooser();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    void patternChooser(){
        Log.e("patternChooser", "called ");

        final SnappyRecyclerView patternRecyclerView = (SnappyRecyclerView) findViewById(R.id.patternRecyclerView);
        patternRecyclerView.setHasFixedSize(true);
        LinearLayoutManager patternLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        patternRecyclerView.setLayoutManager(patternLinearLayoutManager);

        final SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(patternRecyclerView);

        mPatternListAdapter = new patternListAdapter(mPatterns, mContext);
        patternRecyclerView.setAdapter(mPatternListAdapter);

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
                    int position = patternRecyclerView.getFirstVisibleItemPosition();
                    String name = mPatterns.get(position).getName();
                    String signature = mPatterns.get(position).getSignature();

                    Pattern pattern = new Pattern(name, signature, mContext);
                    mJam.setPattern(pattern);
                    sendBeatBroadcast(false);
                }
            }
        });

        patternRecyclerView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return false;
            }
        });
    }

    void kitChooser(){
        final SnappyRecyclerView kitRecyclerView = (SnappyRecyclerView) findViewById(R.id.kitRecyclerView);
        kitRecyclerView.setHasFixedSize(true);
        LinearLayoutManager kitLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        kitRecyclerView.setLayoutManager(kitLinearLayoutManager);

        final SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(kitRecyclerView);

        mKitListAdapter = new kitListAdapter(mKits, mContext);
        kitRecyclerView.setAdapter(mKitListAdapter);

        kitRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        // read info, do stuff.
                        Log.e("recyclerview", "clicked " + position);
                    }

                }));

        kitRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(kitRecyclerView.getFirstVisibleItemPosition() >=0){
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

    void jamChooser(){
        final SnappyRecyclerView jamRecyclerView = (SnappyRecyclerView) findViewById(R.id.jamRecyclerView);
        jamRecyclerView.setHasFixedSize(true);
        final LinearLayoutManager jamLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        jamRecyclerView.setLayoutManager(jamLinearLayoutManager);

        mJamListAdapter = new jamListAdapter(mJams, mContext);
        jamRecyclerView.setAdapter(mJamListAdapter);

        jamRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        // read info, do stuff.
                        Log.e("recyclerview", "clicked " + position);
                    }

                }));

        jamRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(jamRecyclerView.getFirstVisibleItemPosition() >=0){
                    int position = jamRecyclerView.getFirstVisibleItemPosition();
                    Kit kit = new Kit("temp", mJams.get(position).getKit(), mContext);
                    Pattern pattern = new Pattern("name", mJams.get(position).getPattern(), mContext);
                    int tempo = mJams.get(0).getTempo();
                    mJam.setKit(kit);
                    mJam.setPattern(pattern);
                    mJam.setTempo(tempo);
                    /**
                     * Figure out if the pattern associated with the Jam is
                     * a pattern in our list already. Get the index of that
                     * pattern. If we can't find it, we create the pattern
                     * from the signature, and add it to the ArrayList.
                     * Rinse and repeat for the kit
                    **/
                    int patternIndex = -1;
                    for(int x = 0; x<mPatterns.size(); ++x){
                        if(pattern.getPatternHexSignature().equals(mPatterns.get(x).getSignature())){
                            patternIndex = x;
                        }
                    }

                    if(patternIndex==-1){
                        FirebasePattern fbp = new FirebasePattern(mJams.get(position).getSignature(),
                                pattern.getPatternHexSignature());
                        mPatterns.add(fbp);
                        patternIndex=mPatterns.size()-1;
                    }

                    SnappyRecyclerView patternRecyclerView = (SnappyRecyclerView) findViewById(R.id.patternRecyclerView);
                    patternRecyclerView.scrollToPosition(patternIndex);

                    int kitIndex = -1;
                    for(int x = 0; x<mKits.size(); ++x){
                        if(kit.getSignature().equals(mKits.get(x).getSignature())){
                            kitIndex = x;
                        }
                    }

                    if(kitIndex==-1){
                        FirebaseKit fbk = new FirebaseKit(mJams.get(position).getSignature(),
                                kit.getSignature());
                        mKits.add(fbk);
                        kitIndex=mKits.size()-1;
                    }
                    SnappyRecyclerView kitRecyclerView = (SnappyRecyclerView) findViewById(R.id.kitRecyclerView);
                    kitRecyclerView.scrollToPosition(kitIndex);

                    SeekBar tempoBar = (SeekBar) findViewById(R.id.tempoBar);
                    tempoBar.setProgress(mJam.getTempo() - 30);

                    sendBeatBroadcast(false);
                }
            }
        });
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

    /**
     * Service Methods
     * */

    private void launchBeatService(){
        Intent i = new Intent(this, BeatService.class);
        i.putExtra("jamID", 2L);
        startService(i);
    }

    /**
     * UI Methods
     * */

    public void setupToolbar(){
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

        ImageView kitEditorButton = (ImageView) findViewById(R.id.kitEditorButton);
        kitEditorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), KitEditor.class);
                startActivity(i);
            }
        });

        ImageView jamSaveButton = (ImageView) findViewById(R.id.saveJamButton);

        jamSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveJam(false);
                // resetLoaders();
            }
        });

        ImageView databaseButton = (ImageView) findViewById(R.id.databaseButton);
        databaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // writeJamtoFirebase();
                // checkFirebaseForJam();
                shareJam();
            }
        });

        ImageView searchButton = (ImageView) findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialDialog.Builder(mContext).title(R.string.enter_string)
                        .content(R.string.content_test)
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                checkFirebaseForJam(input.toString());
                            }
                        })
                        .show();
            }
        });
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
                sendBeatBroadcast(false);
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
                    pattern.setDatabaseID(Integer.parseInt(patternID));
                    mJam.setPattern(pattern);

                    // Then alert the BeatService
                    sendBeatBroadcast(false);
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
                    kit.setDatabaseID(Integer.parseInt(kitID));
                    mJam.setKit(kit);
                    // Then alert the BeatService
                    sendBeatBroadcast(false);
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

                    SnappyRecyclerView kit = (SnappyRecyclerView) findViewById(kitRecyclerView);
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

                    sendBeatBroadcast(false);
                    // getLoaderManager().getLoader(JAM_LOADER_ID).onContentChanged();
                    // resetLoaders();
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
                sendBeatBroadcast(true);
            }
        });
    }

    /**
     * Loaders and Data Source related methods
     * */

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
             //   mPatternCursorAdapter.swapCursor(data);
                mPatternCursor = data;
                break;
            case KIT_LOADER_ID:
             //   mKitCursorAdapter.swapCursor(data);
                mKitCursor = data;
                break;
            case JAM_LOADER_ID:
             //   mJamCursorAdapter.swapCursor(data);
                mJamCursor = data;
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void resetLoaders(){
        Log.e("resetLoaders", "resetLoaders");
        getLoaderManager().restartLoader(PATTERN_LOADER_ID, null, this);
        getLoaderManager().restartLoader(KIT_LOADER_ID, null, this);
        getLoaderManager().restartLoader(JAM_LOADER_ID, null, this);
    }


    /**
     * Database Prepopulation/Setup
     * */

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

        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        for(int x=0;x<kits.size();x++) {
            String name = kits.get(x).getAsString(KitTable.NAME);
            String signature = kits.get(x).getAsString(KitTable.COMPONENTS);
            FirebaseKit fbk = new FirebaseKit(name, signature);

            mDatabase.child("kits")
                    .child("master")
                    .child(fbk.getName())
                    .setValue(fbk);
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

        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        for(int x=0;x<patterns.size();x++) {
            String name = patterns.get(x).getAsString(PatternTable.NAME);
            String signature = patterns.get(x).getAsString(PatternTable.SEQUENCE);
            FirebasePattern fbp = new FirebasePattern(name, signature);
            Log.e("CreatePatternTable", "name: " + name);
            Log.e("CreatePatternTable", "signature: " + signature);

            mDatabase.child("patterns")
                    .child("master")
                    .child(fbp.getName())
                    .setValue(fbp);
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

        ArrayList<ContentValues> FirebaseJams = new ArrayList<>();

        contentValues = new ContentValues();
        contentValues.put(JamTable.NAME, "Jam 1");
        contentValues.put(JamTable.KIT_ID, "0102030405060708");
        contentValues.put(JamTable.PATTERN_ID, "01");
        contentValues.put(JamTable.TEMPO, "180");
        FirebaseJams.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(JamTable.NAME, "Jam 2");
        contentValues.put(JamTable.KIT_ID, "0C0A0B040508090A");
        contentValues.put(JamTable.PATTERN_ID, "0102");
        contentValues.put(JamTable.TEMPO, "180");
        FirebaseJams.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(JamTable.NAME, "Jam 3");
        contentValues.put(JamTable.KIT_ID, "05040609080A060C");
        contentValues.put(JamTable.PATTERN_ID, "01010201");
        contentValues.put(JamTable.TEMPO, "180");
        FirebaseJams.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(JamTable.NAME, "Jam 4");
        contentValues.put(JamTable.KIT_ID, "04080A0C06090703");
        contentValues.put(JamTable.PATTERN_ID, "010103");
        contentValues.put(JamTable.TEMPO, "180");
        FirebaseJams.add(contentValues);

        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        for(int x=0;x<FirebaseJams.size();x++) {
            String name = FirebaseJams.get(x).getAsString(JamTable.NAME);
            String kitSignature = FirebaseJams.get(x).getAsString(JamTable.KIT_ID);
            String patternSignature = FirebaseJams.get(x).getAsString(JamTable.PATTERN_ID);
            int tempo = FirebaseJams.get(x).getAsInteger(JamTable.TEMPO);

            FirebaseJam fbj = new FirebaseJam(tempo, kitSignature ,patternSignature);

            mDatabase.child("jams")
                    .child("master")
                    .child(fbj.getSignature())
                    .setValue(fbj);
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
        pattern.setDatabaseID(retCursor.getInt(retCursor.getColumnIndex(PatternTable.ID)));

        Log.e("buildJamFromDB", "pattern database ID set to: " + pattern.getDatabaseID());

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

        Kit kit = new Kit(kitName, kitComponents, this);
        kit.setName(kitName);
        kit.setDatabaseID(retCursor.getInt(retCursor.getColumnIndex(KitTable.ID)));

        Log.e("buildJamFromDB", "kit database ID set to: " + kit.getDatabaseID());
        retCursor.close();

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

    private void readFireBaseJamTable(){
        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("jams")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child: dataSnapshot.getChildren()) {
                            FirebaseJam newFbj = child.getValue(FirebaseJam.class);
                            if (newFbj != null) {
                                Log.e("MainActivity", "checkFirebaseForJam() newFbj was populated by Firebase call. Signature: " + newFbj.getSignature());
                                Log.e("MainActivity", "checkFirebaseForJam() newFbj.getTempo(): " + newFbj.getTempo());
                                Log.e("MainActivity", "checkFirebaseForJam() newFbj.getPattern(): " + newFbj.getPattern());
                                Log.e("MainActivity", "checkFirebaseForJam() newFbj.getKit(): " + newFbj.getKit());
                            }
                        }

                        // Test routine - instantiates a jam and pushes to the service
                        // Entirely pulled from Firebase. (works!)
//                        mJam.setTempo(mJams.get(1).getTempo());
//                        mJam.setKit(new Kit("name",mJams.get(1).getKit(),mContext));
//                        mJam.setPattern(new Pattern("pattern", mJams.get(1).getPattern(), mContext));
//                        sendBeatBroadcast(true);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

    }

    public void sendBeatBroadcast(boolean fab){
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
        intent.putExtra("fab", fab);
        sendBroadcast(intent);
    }

    public void saveJam(final Boolean writeToFirebase){
        // search for a jam that matches the current setup (tempo, pattern id, and kit id)
        // build a uri specific to the task, send it to the content provider
        // If there's no Jam like this one, pop up a dialog box that asks for a name
        // assign that name to mJam
        // wrap up contentValues, insert()

        ContentValues contentValues;

        contentValues = new ContentValues();
        contentValues.put(JamTable.NAME, mJam.getName());
        contentValues.put(JamTable.KIT_ID, mJam.getKit().getDatabaseID());
        contentValues.put(JamTable.PATTERN_ID, mJam.getPattern().getDatabaseID());
        contentValues.put(JamTable.TEMPO, mJam.getTempo());

        Uri uri = buildJamByAttributesUri(String.valueOf(mJam.getTempo()),
                String.valueOf(mJam.getKit().getDatabaseID()),
                String.valueOf(mJam.getPattern().getDatabaseID()));

        Cursor cursor = getContentResolver().query(uri,null,null,null,null);


        Log.e("saveJam", "mJam.getName()" + mJam.getName());
        Log.e("saveJam", "mJam.getKit().getDatabaseID()" + mJam.getKit().getDatabaseID());
        Log.e("saveJam", "mJam.getPattern().getDatabaseID()" + mJam.getPattern().getDatabaseID());
        Log.e("saveJam", "mJam.getTempo()" + mJam.getTempo());
        Log.e("saveJam", "uri: " + uri);
        Log.e("saveJam", "cursor.getCount(): " + cursor.getCount());

        if(cursor.getCount()!=0){
            cursor.moveToFirst();
            String JamName = cursor.getString(cursor.getColumnIndex(dbContract.JamTable.NAME));
            cursor.close();

            // Tell the user the pattern already exists. Show name.
            final Dialog dialog = new Dialog(this);

            dialog.setContentView(R.layout.alert_dialog);
            dialog.setTitle("EXISTS!");

            TextView alertText = (TextView) dialog.findViewById(R.id.alertText);
            alertText.setText(R.string.jam_exists);

            TextView alertText2 = (TextView) dialog.findViewById(R.id.alertText2);
            alertText2.setText(JamName);


            Button okButton = (Button) dialog.findViewById(R.id.alertOK);
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.cancel();
                }
            });
            dialog.show();

        } else {
            new MaterialDialog.Builder(this).title(R.string.enter_pattern_name)
                    .content(R.string.content_test)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(MaterialDialog dialog, CharSequence input) {
                            ContentValues contentValues;

                            contentValues = new ContentValues();
                            contentValues.put(dbContract.JamTable.NAME, input.toString());
                            contentValues.put(JamTable.TEMPO, mJam.getTempo());
                            contentValues.put(JamTable.KIT_ID, mJam.getKit().getDatabaseID());
                            contentValues.put(JamTable.PATTERN_ID, mJam.getPattern().getDatabaseID());

                            Uri i = getContentResolver().insert(buildJamUri(), contentValues);
                            Log.e("CreatePatternTable", "insert() Returned URI:" + i.toString());
                            getLoaderManager().getLoader(JAM_LOADER_ID).onContentChanged();
                            if(writeToFirebase){
                                writeJamtoFirebase();
                            }

                        }
                    })
                    .show();
        }
        cursor.close();
    }

    public void shareJam(){

        if(isJamInLocalDB(mJam)){
            writeJamtoFirebase();
        } else {
            saveJam(true);
        }

        FirebaseJam fbj = new FirebaseJam(mJam);
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto"," ", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "This is the ID to download");
        emailIntent.putExtra(Intent.EXTRA_TEXT, fbj.getSignature());
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

    void writeJamtoFirebase(){

        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseJam fbj = new FirebaseJam(mJam);
        mDatabase.child("jams").child(fbj.getSignature()).setValue(fbj);

    }

    void checkFirebaseForJam(String signature){

        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mDatabase.child("jams").child(signature)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FirebaseJam newFbj = dataSnapshot.getValue(FirebaseJam.class);
                        if (newFbj != null) {
                            Log.e("MainActivity", "checkFirebaseForJam() newFbj was populated by Firebase call. Signature: " + newFbj.getSignature());
                            Log.e("MainActivity", "checkFirebaseForJam() newFbj.getTempo(): " + newFbj.getTempo());
                            Log.e("MainActivity", "checkFirebaseForJam() newFbj.getPattern(): " + newFbj.getPattern());
                            Log.e("MainActivity", "checkFirebaseForJam() newFbj.getKit(): " + newFbj.getKit());


                            int tempo = newFbj.getTempo();
                            Pattern pattern = new Pattern(newFbj.getSignature(),newFbj.getPattern(),mContext);
                            Kit kit = new Kit(newFbj.getSignature(),newFbj.getKit(),mContext);

                            Jam jam = new Jam();
                            jam.setName(newFbj.getSignature());
                            jam.setTempo(tempo);
                            jam.setPattern(pattern);
                            jam.setKit(kit);

                            Uri uri = buildPatternBySignatureURI(pattern.getPatternHexSignature());
                            Cursor cursor = getContentResolver().query(uri,null,null,null,null);
                            if(cursor != null && cursor.getCount()!=0){
                                cursor.moveToFirst();
                                pattern.setDatabaseID(Integer.parseInt(cursor.getString(cursor.getColumnIndex(PatternTable.ID))));
                            } else {
                                pattern.setDatabaseID(666);
                            }
                            cursor.close();

                            uri = buildKitBySignatureUri(kit.getSignature());
                            cursor = getContentResolver().query(uri,null,null,null,null);
                            if(cursor != null && cursor.getCount()!=0){
                                cursor.moveToFirst();
                                kit.setDatabaseID(Integer.parseInt(cursor.getString(cursor.getColumnIndex(KitTable.ID))));
                            } else {
                                kit.setDatabaseID(666);
                            }
                            cursor.close();

                            if(isJamInLocalDB(jam)){
                                Toast.makeText(mContext, R.string.jam_already_downloaded, Toast.LENGTH_LONG).show();
                            } else {
                                ContentValues contentValues;

                                contentValues = new ContentValues();
                                contentValues.put(JamTable.NAME, jam.getName());
                                contentValues.put(JamTable.KIT_ID, jam.getKit().getDatabaseID());
                                contentValues.put(JamTable.PATTERN_ID, jam.getPattern().getDatabaseID());
                                contentValues.put(JamTable.TEMPO, jam.getTempo());

                                Uri i = getContentResolver().insert(buildJamUri(), contentValues);
                                Log.e("checkFirebaseForJam()", "Database insert returned: " + i.toString());
                            }

                        } else {
                            Toast.makeText(mContext, R.string.signature_not_found, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private Boolean isJamInLocalDB(Jam jam){

        Uri uri = buildJamByAttributesUri(String.valueOf(jam.getTempo()),
                String.valueOf(jam.getKit().getDatabaseID()),
                String.valueOf(jam.getPattern().getDatabaseID()));

        Cursor cursor = getContentResolver().query(uri,null,null,null,null);
        if(cursor.getCount()!=0){
            return true;
        } else {
            return false;
        }
    }

}
