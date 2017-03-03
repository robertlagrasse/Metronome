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
import android.widget.Toast;

import com.umpquariversoftware.metronome.R;
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
    /**
     * The beat service runs the timer that makes the beats happen.
     * This persists the timer beyond the normal activity lifecycle,
     * and keeps things a little more stable as the user hops in and
     * out of the UI. This is typical behavior for someone using a
     * metronome application - fire up the metronome, then flip to
     * another app to look at the music they're going to play.
     *
     * Activites that want to interact with the timer do so thorough a
     * broadcast receiver.
     * */

    private static Boolean isRunning = false;
    private static Context mContext;
    private static Jam mJam;
    private static Timer mTimer = new Timer();

    public BeatService() {
        super("BeatService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long mJamID = intent.getLongExtra("jamID", 0);
        mJam = new Jam();
        mJam.setTempo(60);
        mJam.setKit(new Kit("temp", "0102030405060708", mContext));
        mJam.setPattern(new Pattern("temp", "01", mContext));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = false;
        mContext = getApplicationContext();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private static void flip() {
        if (isRunning) {
            stopTimer();
        } else {
            startTimer();
        }
    }

    private static void startTimer() {
        /**
         * This is where the magic happens.
         * **/
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

        for (int x = 0; x < 8; ++x) {
            soundIDs.add(soundPool.load(mContext,
                    mJam.getKit().getComponents().get(x).getResource(),
                    PRIORITY));
        }

        /**
         * Setup the timer
         * */

        TimerTask tt = new TimerTask() {
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

                    for (int x = 0; x < 8; ++x) {
                        if (mJam.getPattern().getBeat(position).getPosition(x)) {
                            // Play the soundpool resource in position X
                            soundPool.play(soundIDs.get(x), 1, 1, 1, 0, 1);
                        }
                    }
                    position++;
                }
            }
        };

        mTimer = null;
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(tt, 0, mJam.getInterval());
    }

    private static void stopTimer() {
        mTimer.cancel();
        mTimer.purge();
        mTimer = null;
        isRunning = false;
    }

    public static class startStopReceiver extends BroadcastReceiver {
        /**
         * Receive jam information via intent extras.
         * Modify mJam.
         * Decide what to do with the timer based on whether or not this was a fab call.
         * */

        public startStopReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean widget = intent.getBooleanExtra("widget", false);

            if (!widget) {
                String name = intent.getStringExtra("jamName");
                if (name == null) {
                    name = mContext.getResources().getString(R.string.no_name);
                }
                Pattern pattern = new Pattern("temp", intent.getStringExtra("pattern"), null);
                int tempo = intent.getIntExtra("tempo", 60);
                Kit kit = new Kit();

                ArrayList<Integer> resourceIDs = intent.getIntegerArrayListExtra("components");
                for (int x = 0; x < resourceIDs.size(); ++x) {
                    Component component = new Component();
                    component.setResource(resourceIDs.get(x));
                    kit.addComponent(component);
                }

                // Tempo, Pattern and Kit defined, build a Jam
                Jam jam = new Jam();
                jam.setKit(kit);
                jam.setPattern(pattern);
                jam.setTempo(tempo);
                jam.setName(name);
                mJam = jam;
            }

            if (mJam == null || mJam.getKit() == null || mJam.getPattern() == null) {
                Log.e("startStopReceiver", "Service not running.");
            } else {
                String ACTION_DATA_UPDATED =
                        "android.appwidget.action.APPWIDGET_UPDATE";

                Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
                        .setPackage(context.getPackageName());
                dataUpdatedIntent.putExtra("jamName", mJam.getName());
                context.sendBroadcast(dataUpdatedIntent);

                Boolean fab = intent.getBooleanExtra("fab", false);
                if (fab) {
                    flip();
                } else {
                    if (isRunning) {
                        stopTimer();
                    }
                }
            }
        }
    }
}
