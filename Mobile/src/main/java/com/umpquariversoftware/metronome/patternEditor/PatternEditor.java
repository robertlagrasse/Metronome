package com.umpquariversoftware.metronome.patternEditor;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.umpquariversoftware.metronome.FireBase.FirebasePattern;
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.elements.Beat;
import com.umpquariversoftware.metronome.elements.Pattern;

import java.util.ArrayList;

/**
 * PatternEditor is an activity that allows the user to define their own pattern.
 * A pattern starts off with a single beat, which voices only component 1.
 * Each beat can be edited using numeric controls 1-8 to turn off specific components in that beat.
 * The pattern can be of any length > 0;
 *
 * The pattern, and any changes to the pattern, are immediately reflected in the graph at the
 * top of the screen.
 *
 * When the user is satisfied with their pattern, it can be pushed to firebase for use in
 * Jams in the mainActivity.
 * **/

public class PatternEditor extends AppCompatActivity {
    private int currentBeat = 1;
    private final Pattern pattern = new Pattern("New Pattern", "01");
    private Context mContext;
    private String userID = "";
    private Boolean mMasterListSearchResultsBack = false;
    private Boolean mUserListSearchResultsBack = false;
    private FirebasePattern mUserListPattern;
    private FirebasePattern mMasterListPattern;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pattern_editor);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        mContext = this;
        userID = getIntent().getStringExtra("userID");

        /**
         * Setup the UI
         * */

        graphPattern();
        setupButtons();
        setupToolbar();
        getThatMoney();
    }

    private void graphPattern() {
        /**
         * uses the graphView library to represent the pattern visually
         * */
        GraphView graph = (GraphView) findViewById(R.id.patternEditorGraph);
        graph.removeAllSeries();

        PointsGraphSeries<DataPoint> series;
        //noinspection unchecked,unchecked
        series = pattern.getPatternDataPoints();

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(1);
        graph.getViewport().setMaxX(pattern.getLength());

        // set manual Y bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(1);
        graph.getViewport().setMaxY(8);

        ArrayList<String> horizontals = new ArrayList<>();

        for (int i = 0; i < pattern.getLength(); ++i) {
            horizontals.add(String.valueOf(i + 1));
        }
        if (pattern.getLength() == 1) {
            horizontals.add("");
        }

        String[] hvals = new String[horizontals.size()];
        hvals = horizontals.toArray(hvals);

        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
        staticLabelsFormatter.setVerticalLabels(mContext.getResources().getStringArray(R.array.patternGraphLabels));
        staticLabelsFormatter.setHorizontalLabels(hvals);
        graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);

        graph.addSeries(series);
    }

    private void setupToolbar() {
        /**
         * Build buttons and onClick listeners
         * */

        Toolbar toolbar = (Toolbar) findViewById(R.id.editor_toolbar);
        setSupportActionBar(toolbar);

        TextView title = (TextView) findViewById(R.id.activityDisplay);
        title.setText(getResources().getString(R.string.pattern_editor));

        ImageView helpButton = (ImageView) findViewById(R.id.helpButton);
        helpButton.setFocusable(true);
        helpButton.setContentDescription(getResources().getString(R.string.help));
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, getResources().getString(R.string.placeholder), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupButtons() {
        /**
         * All of the controls live here. Images for beat buttons first-eighth change based
         * on the state of the component in the current beat.
         *
         * Buttons in the "remote control" section are visible based on aspects of the
         * pattern and the current position the user is editing. Basically, this removes the
         * user's ability to make bad inputs.
         * **/


        /**
         * "Remote Control" Buttons
         ***/

        FloatingActionButton patternBeatLast = (FloatingActionButton) findViewById(R.id.patternBeatLast);
        patternBeatLast.setFocusable(true);
        patternBeatLast.setContentDescription(getResources().getString(R.string.previous_beat));

        FloatingActionButton patternBeatNext = (FloatingActionButton) findViewById(R.id.patternBeatNext);
        patternBeatNext.setFocusable(true);
        patternBeatNext.setContentDescription(getResources().getString(R.string.next_beat));

        FloatingActionButton patternBeatNew = (FloatingActionButton) findViewById(R.id.patternBeatNew);
        patternBeatNew.setFocusable(true);
        patternBeatNew.setContentDescription(getResources().getString(R.string.new_beat));

        FloatingActionButton patternBeatDelete = (FloatingActionButton) findViewById(R.id.patternBeatDelete);
        patternBeatDelete.setFocusable(true);
        patternBeatDelete.setContentDescription(getResources().getString(R.string.delete_beat));

        FloatingActionButton patternSave = (FloatingActionButton) findViewById(R.id.patternSave);
        patternSave.setFocusable(true);
        patternSave.setContentDescription(getResources().getString(R.string.save_pattern_to_cloud));

        if (pattern.getLength() == 1) {
            patternBeatLast.setVisibility(View.INVISIBLE);
            patternBeatNext.setVisibility(View.INVISIBLE);
            patternBeatDelete.setVisibility(View.INVISIBLE);
        } else {
            patternBeatLast.setVisibility(View.VISIBLE);
            patternBeatNext.setVisibility(View.VISIBLE);
            patternBeatDelete.setVisibility(View.VISIBLE);
        }

        if (currentBeat == 1) {
            patternBeatLast.setVisibility(View.INVISIBLE);
        } else {
            patternBeatLast.setVisibility(View.VISIBLE);
        }

        if (currentBeat == pattern.getLength()) {
            patternBeatNext.setVisibility(View.INVISIBLE);
        } else {
            patternBeatNext.setVisibility(View.VISIBLE);
        }

        patternBeatLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentBeat--;
                setupButtons();
            }
        });

        patternBeatNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentBeat++;
                setupButtons();
            }
        });

        patternBeatNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Beat beat = new Beat("01");
                pattern.insertBeat(beat, currentBeat);
                currentBeat++;
                setupButtons();
                graphPattern();
            }
        });

        patternBeatDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.removeBeat(currentBeat - 1);
                if (pattern.getLength() < currentBeat) {
                    currentBeat = pattern.getLength();
                }
                setupButtons();
                graphPattern();
            }
        });

        patternSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                check(pattern.getPatternHexSignature());
            }
        });

        /**
         * Numeric controls for individual components in the current beat
         * */


        TextView patternBeatDisplay = (TextView) findViewById(R.id.patternBeatDisplay);
        patternBeatDisplay.setText(String.valueOf(currentBeat));

        ImageView first = (ImageView) findViewById(R.id.first);
        ImageView second = (ImageView) findViewById(R.id.second);
        ImageView third = (ImageView) findViewById(R.id.third);
        ImageView fourth = (ImageView) findViewById(R.id.fourth);
        ImageView fifth = (ImageView) findViewById(R.id.fifth);
        ImageView sixth = (ImageView) findViewById(R.id.sixth);
        ImageView seventh = (ImageView) findViewById(R.id.seventh);
        ImageView eighth = (ImageView) findViewById(R.id.eighth);

        first.setFocusable(true);
        first.setContentDescription(getResources().getString(R.string.one));
        second.setFocusable(true);
        second.setContentDescription(getResources().getString(R.string.two));
        third.setFocusable(true);
        third.setContentDescription(getResources().getString(R.string.three));
        fourth.setFocusable(true);
        fourth.setContentDescription(getResources().getString(R.string.four));
        fifth.setFocusable(true);
        fifth.setContentDescription(getResources().getString(R.string.five));
        sixth.setFocusable(true);
        sixth.setContentDescription(getResources().getString(R.string.six));
        seventh.setFocusable(true);
        seventh.setContentDescription(getResources().getString(R.string.seven));
        eighth.setFocusable(true);
        eighth.setContentDescription(getResources().getString(R.string.eight));

        Beat beat = pattern.getBeat(currentBeat - 1);
        if (beat.getFirst()) {
            first.setImageResource(R.drawable.numeric_1_box);
        } else {
            first.setImageResource(R.drawable.numeric_1_box_outline);
        }
        if (beat.getSecond()) {
            second.setImageResource(R.drawable.numeric_2_box);
        } else {
            second.setImageResource(R.drawable.numeric_2_box_outline);
        }
        if (beat.getThird()) {
            third.setImageResource(R.drawable.numeric_3_box);
        } else {
            third.setImageResource(R.drawable.numeric_3_box_outline);
        }
        if (beat.getFourth()) {
            fourth.setImageResource(R.drawable.numeric_4_box);
        } else {
            fourth.setImageResource(R.drawable.numeric_4_box_outline);
        }
        if (beat.getFifth()) {
            fifth.setImageResource(R.drawable.numeric_5_box);
        } else {
            fifth.setImageResource(R.drawable.numeric_5_box_outline);
        }
        if (beat.getSixth()) {
            sixth.setImageResource(R.drawable.numeric_6_box);
        } else {
            sixth.setImageResource(R.drawable.numeric_6_box_outline);
        }
        if (beat.getSeventh()) {
            seventh.setImageResource(R.drawable.numeric_7_box);
        } else {
            seventh.setImageResource(R.drawable.numeric_7_box_outline);
        }
        if (beat.getEighth()) {
            eighth.setImageResource(R.drawable.numeric_8_box);
        } else {
            eighth.setImageResource(R.drawable.numeric_8_box_outline);
        }

        first.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat - 1).setFIRST(!pattern.getBeat(currentBeat - 1).getFirst());
                setupButtons();
                graphPattern();
            }
        });

        second.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat - 1).setSECOND(!pattern.getBeat(currentBeat - 1).getSecond());
                setupButtons();
                graphPattern();
            }
        });

        third.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat - 1).setTHIRD(!pattern.getBeat(currentBeat - 1).getThird());
                setupButtons();
                graphPattern();
            }
        });

        fourth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat - 1).setFOURTH(!pattern.getBeat(currentBeat - 1).getFourth());
                setupButtons();
                graphPattern();
            }
        });

        fifth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat - 1).setFIFTH(!pattern.getBeat(currentBeat - 1).getFifth());
                setupButtons();
                graphPattern();
            }
        });

        sixth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat - 1).setSIXTH(!pattern.getBeat(currentBeat - 1).getSixth());
                setupButtons();
                graphPattern();
            }
        });

        seventh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat - 1).setSEVENTH(!pattern.getBeat(currentBeat - 1).getSeventh());
                setupButtons();
                graphPattern();
            }
        });

        eighth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat - 1).setEIGHTH(!pattern.getBeat(currentBeat - 1).getEighth());
                setupButtons();
                graphPattern();
            }
        });

    }

    private void getThatMoney(){
        /**
         * Setup adMobs
         * */
        // MobileAds.initialize(getApplicationContext(), "ca-app-pub-8040545141030965~5491821531");

        AdView mAdView = (AdView) findViewById(R.id.adView);

        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("74D61A4429900485751F374428FB6C95")
                .build();
        mAdView.loadAd(adRequest);
    }

    private void check(String signature) {
        /**
         * Search for the pattern in the user and master tables.
         * If found, alert
         * If not found, send to askAndInsert()
         * **/
        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mMasterListSearchResultsBack = false;
        mUserListSearchResultsBack = false;
        mMasterListPattern = null;
        mUserListPattern = null;

        mDatabase.child("patterns").child("master").child(signature)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FirebasePattern masterListPattern = dataSnapshot.getValue(FirebasePattern.class);
                        if (masterListPattern != null) {
                            mMasterListPattern = masterListPattern;
                        } else {
                            mMasterListPattern = null;
                        }
                        mMasterListSearchResultsBack = true;
                        if (mUserListSearchResultsBack) {
                            if (mMasterListPattern != null) {
                                alert(getString(R.string.pattern_exists), mMasterListPattern.getName());
                            } else if (mUserListPattern != null) {
                                alert(getString(R.string.pattern_exists), mUserListPattern.getName());
                            } else {
                                askAndInsert();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        mDatabase.child("patterns").child("users").child(userID).child(signature)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FirebasePattern userListPattern = dataSnapshot.getValue(FirebasePattern.class);
                        if (userListPattern != null) {
                            mUserListPattern = userListPattern;
                        } else {
                            mUserListPattern = null;
                        }
                        mUserListSearchResultsBack = true;
                        if (mMasterListSearchResultsBack) {
                            if (mMasterListPattern != null) {
                                alert(getString(R.string.pattern_exists), mMasterListPattern.getName());
                            } else if (mUserListPattern != null) {
                                alert(getString(R.string.pattern_exists), mUserListPattern.getName());
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

    private void alert(String text1, String text2) {
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
        okButton.setFocusable(true);
        okButton.setContentDescription(getResources().getString(R.string.alertOK));

        dialog.show();
    }

    private void askAndInsert() {
        /**
         * Prompt for Pattern name, insert to patterns/users/userID/{hex signature}
         * **/
        final DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        new MaterialDialog.Builder(mContext).title(R.string.enter_pattern_name)
                .content(R.string.content_test)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        FirebasePattern fbp = new FirebasePattern(input.toString(), pattern.getPatternHexSignature());
                        mDatabase.child("patterns")
                                .child("users")
                                .child(userID)
                                .child(fbp.getSignature())
                                .setValue(fbp);
                    }
                })
                .show();
    }
}