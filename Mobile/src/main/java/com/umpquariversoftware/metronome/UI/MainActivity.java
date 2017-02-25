package com.umpquariversoftware.metronome.UI;


import android.app.Dialog;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.umpquariversoftware.metronome.FireBase.FirebaseJam;
import com.umpquariversoftware.metronome.FireBase.FirebaseKit;
import com.umpquariversoftware.metronome.FireBase.FirebasePattern;
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.database.dbContract;
import com.umpquariversoftware.metronome.elements.Component;
import com.umpquariversoftware.metronome.elements.Jam;
import com.umpquariversoftware.metronome.elements.Kit;
import com.umpquariversoftware.metronome.elements.Pattern;
import com.umpquariversoftware.metronome.kitEditor.KitEditor;
import com.umpquariversoftware.metronome.patternEditor.PatternEditor;

import java.util.ArrayList;

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


public class MainActivity extends AppCompatActivity{
    String TAG = "MainActivity";
    Jam mJam = new Jam();
    Boolean beatServiceRunning = false;
    static Boolean networkIsConnected;
    private Toolbar toolbar;
    static Context mContext;
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


    patternListAdapter mPatternListAdapter;
    kitListAdapter mKitListAdapter;
    jamListAdapter mJamListAdapter;

    Boolean mMasterListSearchResultsBack = false;
    Boolean mUserListSearchResultsBack = false;
    FirebaseJam mUserListJam, mMasterListJam;

    @Override
    public void onResume() {
        super.onResume();

        ConnectivityManager cm =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };

        mContext = this;

        ConnectivityManager cm =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        registerReceiver(new MainActivity.networkStatusChangeReceiver(),
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        networkIsConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        SharedPreferences prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        if(prefs.getBoolean("firstrun", true)){
            createComponentsTable();
            prefs.edit().putBoolean("firstrun", false).commit();
        }

        if (savedInstanceState == null){
            launchBeatService();
            beatServiceRunning = true;
        }

        /***
         * Setup some local resources for use offline
         * Setup the UI
         * Grab online data
         ***/

        createLocalResources();

        setupToolbar();
        tempoChooser();
        patternChooser();
        kitChooser();
        jamChooser();
        actionButton();

        grabData();

        /**
         * Get that money!
         * */

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-8040545141030965/9922021136");
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    /**
     * Core Functionality
     * **/

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

    private void launchBeatService(){
        Intent i = new Intent(this, BeatService.class);
        i.putExtra("jamID", 2L);
        startService(i);
    }

    void createLocalResources(){
        mLocalPattern.add(new FirebasePattern("1 Beat (Local)","01"));
        mLocalPattern.add(new FirebasePattern("2 Beat (Local)","0102"));
        mLocalPattern.add(new FirebasePattern("3 Beat (Local)", "010102"));
        mLocalPattern.add(new FirebasePattern("4 Beat (Local)", "01010102"));

        mLocalKits.add(new FirebaseKit("Standard Kit (Local)", "030405060708090A"));

        mLocalJams.add(new FirebaseJam("1 Beat Jam (Local)", 90, "030405060708090A", "01"));
        mLocalJams.add(new FirebaseJam("2 Beat Jam (Local)", 90, "030405060708090A", "0102"));
        mLocalJams.add(new FirebaseJam("3 Beat Jam (Local)", 90, "030405060708090A", "010102"));
        mLocalJams.add(new FirebaseJam("4 Beat Jam (Local)", 90, "030405060708090A", "01010102"));
    }

    public void setupToolbar(){
        /**
         * Setup the toolbar and its buttons
         * */

        toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageView searchForSharedJamButton = (ImageView) findViewById(R.id.searchForSharedJamButton);
        ImageView shareJamButton = (ImageView) findViewById(R.id.shareJamButton);
        ImageView saveJamToCloud = (ImageView) findViewById(R.id.saveJamToCloud);

        if(networkIsConnected){
            searchForSharedJamButton.setVisibility(View.VISIBLE);
            searchForSharedJamButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new MaterialDialog.Builder(mContext).title(R.string.enter_string)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    addSharedJamFromFirebase(input.toString());
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

    public void tempoChooser(){
        final int tempo = mJam.getTempo();
        SeekBar tempoBar = (SeekBar) findViewById(R.id.tempoBar);
        final TextView tempoDisplay = (TextView) findViewById(R.id.tempoDisplay);
        tempoBar.setProgress(tempo-TEMPO_OFFSET);
        tempoDisplay.setText(String.valueOf(tempo));
        tempoBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // Modify the tempo of the current Jam as we slide
                mJam.setTempo(i+30);
                tempoDisplay.setText(String.valueOf(i+TEMPO_OFFSET));
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

    void patternChooser(){

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
                Intent i = new Intent(getApplicationContext(), PatternEditor.class);
                startActivity(i);
                return true;
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
                    mJam.setName(mJams.get(0).getName());
                    mJam.setKit(kit);
                    mJam.setPattern(pattern);
                    mJam.setTempo(tempo);

                    SeekBar tempoBar = (SeekBar) findViewById(R.id.tempoBar);
                    tempoBar.setProgress(mJam.getTempo()-TEMPO_OFFSET);
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

                    sendBeatBroadcast(false);
                }
            }
        });
    }

    public void actionButton(){
        FloatingActionButton startstop;
        startstop = (FloatingActionButton) findViewById(R.id.startStopButton);
        startstop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendBeatBroadcast(true);
            }
        });
    }

    void grabData(){
        /**
         * Initialize the ArrayLists
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

        Kit kit = new Kit("name", mJams.get(0).getKit(), mContext);
        Pattern pattern = new Pattern("name", mJams.get(0).getPattern(), mContext);
        int tempo = mJams.get(0).getTempo();
        mJam.setKit(kit);
        mJam.setPattern(pattern);
        mJam.setTempo(tempo);

        SeekBar tempoBar = (SeekBar) findViewById(R.id.tempoBar);
        tempoBar.setProgress(tempo-TEMPO_OFFSET);

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
                        for (DataSnapshot child: dataSnapshot.getChildren()) {
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

        mDatabase.child("jams").child("users").child("this_user")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mUserJams.clear();
                        for(DataSnapshot child: dataSnapshot.getChildren()){
                            FirebaseJam fbj = child.getValue(FirebaseJam.class);
                            if (fbj!=null){
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
                        for (DataSnapshot child: dataSnapshot.getChildren()) {
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

        mDatabase.child("kits").child("users").child("this_user")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mUserKits.clear();
                        for(DataSnapshot child: dataSnapshot.getChildren()){
                            FirebaseKit fbk = child.getValue(FirebaseKit.class);
                            if(fbk!=null){
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
                        for (DataSnapshot child: dataSnapshot.getChildren()) {
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
        mDatabase.child("patterns").child("users").child("this_user")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mUserPatterns.clear();
                        for (DataSnapshot child: dataSnapshot.getChildren()) {
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

    public void sendBeatBroadcast(boolean fab){
        Intent intent = new Intent();
        intent.setAction("com.umpquariversoftware.metronome.STARTSTOP");

        intent.putExtra("tempo", mJam.getTempo());

        intent.putExtra("pattern", mJam.getPattern().getPatternHexSignature());

        ArrayList<Integer> components = new ArrayList<>();
        for(int x = 0; x<8; ++x){
            components.add(mJam.getKit().getComponents().get(x).getResource());
        }
        intent.putIntegerArrayListExtra("components", components);
        intent.putExtra("fab", fab);
        sendBroadcast(intent);
    }

    /**
     * Toolbar Functions
     * */

    public void shareJam(){
        // Write to the shared jams folder everyone has access to

        FirebaseJam fbj = new FirebaseJam(mJam);
        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("jams").child("shared").child(fbj.getSignature()).setValue(fbj);

        // Email the download signature to your friend
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto"," ", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "This is the ID to download");
        emailIntent.putExtra(Intent.EXTRA_TEXT, fbj.getSignature());
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

    void sendJamToFirebase(){
        /**
         * First, iterate through the mJam arrayList to see if the
         * jam signature shows up there.
         *
         * Next, query Firebase for the jam in both master and user table
         * */

        String signature = new FirebaseJam(mJam).getSignature();
        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mMasterListSearchResultsBack = false;
        mUserListSearchResultsBack = false;

        mMasterListJam = null;
        for(int x=0; x<mMasterJams.size(); ++x){
            if(mMasterJams.get(x).getSignature().equals(signature)){
                mMasterListJam = new FirebaseJam(mMasterJams.get(x).getTempo(),
                        mMasterJams.get(x).getKit(),
                        mMasterJams.get(x).getPattern());
                mMasterListJam.setName(mMasterJams.get(x).getName());
            }
        }
        mMasterListSearchResultsBack = true;

        mDatabase.child("jams").child("users").child("this_user").child(signature)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FirebaseJam userListJam  = dataSnapshot.getValue(FirebaseJam.class);
                        if (userListJam != null) {
                            mUserListJam = userListJam;
                        } else {
                            mUserListJam = null;
                        }
                        mUserListSearchResultsBack = true;
                        if(mMasterListSearchResultsBack){
                            if(mMasterListJam!=null){
                                alert(getString(R.string.jam_exists), mMasterListJam.getName());
                            } else if(mUserListJam!=null){
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
            case (R.id.menuPatternEditor):{
                if(networkIsConnected){
                    Intent i = new Intent(this, PatternEditor.class);
                    startActivity(i);
                }else{
                    Toast.makeText(mContext, R.string.network_required, Toast.LENGTH_LONG).show();
                }
                return true;
            }
            case R.id.menuKitEditor:{
                if(networkIsConnected){
                    Intent i = new Intent(this, KitEditor.class);
                    startActivity(i);
                }else{
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
     * */

    void alert(String text1, String text2){
        final Dialog dialog = new Dialog(mContext);

        dialog.setContentView(R.layout.alert_dialog);
        dialog.setTitle("EXISTS!");

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

    void askAndInsert(){ // that's what she said.
        final DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        new MaterialDialog.Builder(mContext).title(R.string.enter_pattern_name)
                .content(R.string.content_test)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        FirebaseJam fbj = new FirebaseJam(mJam);
                        fbj.setName(input.toString());
                        mDatabase.child("jams")
                                .child("users")
                                .child("this_user")
                                .child(fbj.getSignature())
                                .setValue(fbj);
                    }
                })
                .show();
    }

    public void addSharedJamFromFirebase(String signature){
        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mDatabase.child("jams").child("shared").child(signature)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FirebaseJam newFbj = dataSnapshot.getValue(FirebaseJam.class);
                        if (newFbj != null) {
                            int tempo = newFbj.getTempo();
                            Pattern pattern = new Pattern(newFbj.getSignature(),newFbj.getPattern(),mContext);
                            Kit kit = new Kit(newFbj.getSignature(),newFbj.getKit(),mContext);

                            Jam jam = new Jam();
                            jam.setName("Downloaded Jam | " + newFbj.getSignature().substring(newFbj.getSignature().length()-6));
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

    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString("jamSignature", new FirebaseJam(mJam).getSignature());
    }

    public class networkStatusChangeReceiver extends BroadcastReceiver {

        public networkStatusChangeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm =
                    (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            networkIsConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
            setupToolbar();
        }
    }


}
