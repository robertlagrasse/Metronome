package com.umpquariversoftware.metronome.UI;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;

import com.umpquariversoftware.metronome.database.dbContract;
import com.umpquariversoftware.metronome.elements.Beat;
import com.umpquariversoftware.metronome.elements.Component;
import com.umpquariversoftware.metronome.elements.Jam;
import com.umpquariversoftware.metronome.elements.Kit;
import com.umpquariversoftware.metronome.elements.Pattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import static com.umpquariversoftware.metronome.database.dbContract.buildJamUri;
import static com.umpquariversoftware.metronome.database.dbContract.buildKitUri;
import static com.umpquariversoftware.metronome.database.dbContract.buildPatternUri;

public class BeatService extends IntentService {

    private static Boolean isRunning = false;
    private static Context mContext;
    private static Jam mJam;
    static Timer mTimer = new Timer();
    private static long mJamID = 0;


    public BeatService() {
        super("BeatService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e("BeatService", "onHandleIntent");
        mJamID = intent.getLongExtra("jamID",0);
        mJam = buildJamFromDB(mJamID);
    }

    @Override
    public void onCreate() {
        Log.e("BeatService", "onCreate()");
        super.onCreate();

        isRunning = false;
        mContext = getApplicationContext();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("BeatService", "onDestroy");
    }

    public Jam buildJamFromDB(long id){

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

        String jamName = retCursor.getString(retCursor.getColumnIndex(dbContract.JamTable.NAME));
        int jamTempo = Integer.parseInt(retCursor.getString(retCursor.getColumnIndex(dbContract.JamTable.TEMPO)));
        int dbID = Integer.parseInt(retCursor.getString(retCursor.getColumnIndex(dbContract.JamTable.ID)));

        String kitID = retCursor.getString(retCursor.getColumnIndex(dbContract.JamTable.KIT_ID));
        String patternID = retCursor.getString(retCursor.getColumnIndex(dbContract.JamTable.PATTERN_ID));

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

        String patternName = retCursor.getString(retCursor.getColumnIndex(dbContract.PatternTable.NAME));
        String patternSequence = retCursor.getString(retCursor.getColumnIndex(dbContract.PatternTable.SEQUENCE));

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

        String kitComponents = retCursor.getString(retCursor.getColumnIndex(dbContract.KitTable.COMPONENTS));
        String kitName = retCursor.getString(retCursor.getColumnIndex(dbContract.KitTable.NAME));
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

    private static void flip(){
        Log.e("TestService", "flip()");
        if(isRunning){
            stopTimer();
        } else {
            startTimer();
        }
    }

    private static void startTimer(){
        isRunning = true;
        int PRIORITY = 1;

        /**
         * Setup some basic audio attributes
         * */
        AudioAttributes audioAttrib = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        /**
         * Setup the soundpool.
         * Attach the attributes.
         * MaxStreams indicates the maximum number of simultaneous sounds
         * */

        final SoundPool soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttrib)
                .setMaxStreams(8)
                .build();

        /**
         * When a resource is loaded into a sound pool, it returns a Sound ID.
         * The Sound ID is what is actually referenced to play the sound
         * We iterate through the resources in the Kit, and add them
         * to the sound pool. We add the SoundIDs to an arraylist.
         *
         * */

        final ArrayList<Integer> soundIDs = new ArrayList<Integer>();
        // Iterate through the kit

        for(int x=0;x<8;++x){
            soundIDs.add(soundPool.load(mContext,
                    mJam.getKit().getComponents().get(x).getResource(),
                    PRIORITY));
        }

        /**
         * Setup the timer
         * */

        TimerTask tt = null;
        tt = new TimerTask() {
            int position = 0;
            @Override
            public void run() {
                {

                    if (position == mJam.getPattern().getLength()) {
                        position = 0;
                    }

                    /**
                     * Extract the beat at this position in the pattern.
                     * Each position in the beat is just a boolean.
                     * We iterate through each position in the beat.
                     * If the beat is true, the corresponding soundID
                     * from the array list is played.
                     * */


                    for(int x=0;x<8;++x){
                        if(mJam.getPattern().getBeat(position).getPosition(x)){
                            // Play the soundpool resource in position X
                            soundPool.play(soundIDs.get(x),1,1,1,0,1);
                        }
                    }
                    position++;
                }
            }
        };

        mTimer = null;
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(tt,0,mJam.getInterval());
    }

    private static void stopTimer(){
        mTimer.cancel();
        mTimer.purge();
        mTimer=null;
        isRunning=false;
    }

    public static class startStopReceiver extends BroadcastReceiver {

        public startStopReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("startStopReceiver", "onreceive()");
            // Extract pattern signature and instantiate a new Pattern
            Pattern pattern = new Pattern("temp",intent.getStringExtra("pattern"),null);
            // Log.e("startStopReceiver", "pattern signature: " + pattern.getPatternHexSignature());

            // Extract tempo
            int tempo = intent.getIntExtra("tempo", 60);
            // Log.e("startStopReceiver", "tempo: " + tempo);

            // Build a kit
            Kit kit = new Kit();

            // Extract the resource ID's from the ArrayList
            // Build a component from each resource
            // Add that component to the kit
            ArrayList<Integer> resourceIDs = intent.getIntegerArrayListExtra("components");
            for(int x=0; x<resourceIDs.size(); ++x){
                Component component = new Component();
                component.setResource(resourceIDs.get(x));
                kit.addComponent(component);
                // Log.e("startStopReceiver", "Received Component: " + resourceIDs.get(x));
            }

            // Tempo, Pattern and Kit defined, build a Jam
            Jam jam = new Jam();
            jam.setKit(kit);
            jam.setPattern(pattern);
            jam.setTempo(tempo);
            mJam=jam;

            Boolean fab = intent.getBooleanExtra("fab", false);

            if(fab){
                flip();
            } else {
                if(isRunning){
                    stopTimer();
                }
            }

        }
    }

}
