package com.umpquariversoftware.metronome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends WearableActivity {

    private int TEMPO_MIN = 30;
    private int TEMPO_MAX = 330;
    private int BEATS_MIN = 1;
    private int BEATS_MAX = 8;

    int tempo = 60;
    int beats = BEATS_MIN;

    Timer mTimer;
    Boolean isRunning;

    ImageView upButton;
    ImageView downButton;
    ImageView leftButton;
    ImageView rightButton;

    FrameLayout buttonCenter;
    LinearLayout statusContainer;

    TextView majorStatus;
    TextView minorStatus;

    @Override
    protected void onPause() {
        super.onPause();
        if(isRunning){
            stopTimer();
        }
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("tempo", tempo);
        editor.putInt("beats", beats);
        editor.commit();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState!=null){
            tempo = savedInstanceState.getInt("tempo");
            beats = savedInstanceState.getInt("beats");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        tempo = prefs.getInt("tempo", 60);
        beats = prefs.getInt("beats", 1);

        isRunning = false;

        upButton = (ImageView) findViewById(R.id.buttonUp);
        downButton = (ImageView) findViewById(R.id.buttonDown);
        leftButton = (ImageView) findViewById(R.id.buttonLeft);
        rightButton = (ImageView) findViewById(R.id.buttonRight);

        buttonCenter = (FrameLayout) findViewById(R.id.buttonCenter);
        statusContainer = (LinearLayout) findViewById(R.id.statusContainer);

        majorStatus = (TextView) findViewById(R.id.majorStatus);
        minorStatus = (TextView) findViewById(R.id.minorStatus);ImageView upButton = (ImageView) findViewById(R.id.buttonUp);

        setAmbientEnabled();


        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tempo++;
                if(tempo>TEMPO_MAX){
                    tempo = TEMPO_MAX;
                }
                setupStatusWindow();
                if(isRunning){
                    stopTimer();
                    startTimer();
                }
            }
        });

        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tempo--;
                if(tempo<TEMPO_MIN){
                    tempo = TEMPO_MIN;
                }
                setupStatusWindow();
                if(isRunning){
                    stopTimer();
                    startTimer();
                }
            }
        });

        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beats--;
                if(beats<BEATS_MIN){
                    beats = BEATS_MIN;
                }
                setupStatusWindow();
            }
        });

        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beats++;
                if(beats>BEATS_MAX){
                    beats = BEATS_MAX;
                }
                setupStatusWindow();
            }
        });

        buttonCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRunning){
                    stopTimer();
                } else {
                    startTimer();
                }
            }
        });
    }
    private void startTimer(){
        isRunning = true;
        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);


        /**
         * Setup the timer
         * */

        TimerTask tt = null;
        tt = new TimerTask() {
            int position = beats;
            @Override
            public void run() {
                {
                    if (position == beats) {
                        position = 0;
                        vibrator.vibrate(150);
                    } else{
                        vibrator.vibrate(50);
                    }
                    position++;
                }
            }
        };

        mTimer = null;
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(tt,0,60000/tempo);
    }

    private  void stopTimer(){
        mTimer.cancel();
        mTimer.purge();
        mTimer=null;
        isRunning=false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tempo", tempo);
        outState.putInt("beats", beats);
    }

    protected void setupStatusWindow(){
        buttonCenter.setBackground(null);
        statusContainer.setVisibility(View.VISIBLE);
        majorStatus.setText(String.valueOf(tempo));
        minorStatus.setText(String.valueOf(beats));

        Runnable r = new Runnable() {
            @Override
            public void run(){
                statusContainer.setVisibility(View.GONE);
                buttonCenter.setBackgroundResource(R.drawable.play);
            }
        };

        Handler h = new Handler();
        h.postDelayed(r, 2000);
    }
}
