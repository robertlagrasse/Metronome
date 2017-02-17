package com.umpquariversoftware.metronome.patternEditor;

import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.elements.Beat;
import com.umpquariversoftware.metronome.elements.Pattern;

import java.util.ArrayList;

public class PatternEditor extends AppCompatActivity {
    int currentBeat = 1;
    Pattern pattern = new Pattern("New Pattern", "0102030405060708", null);
    Beat beat = new Beat();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pattern_editor);


        /**
         * Build a basic pattern so you have something to display
         * Graph the pattern
         * */
        graphPattern();

        /**
         * Build buttons to scroll through beats in the pattern
         * Display the current position
         * */

        setupButtons();


    }
    private void graphPattern(){
        GraphView graph = (GraphView) findViewById(R.id.patternEditorGraph);
        graph.removeAllSeries();

        PointsGraphSeries<DataPoint> series = new PointsGraphSeries<>();
        series = pattern.getPatternDataPoints();

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(1);
        graph.getViewport().setMaxX(pattern.getLength());

        // set manual Y bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(1);
        graph.getViewport().setMaxY(8);

        ArrayList<String> horizontals = new ArrayList<>();

        for(int i=0;i<pattern.getLength();++i){
            horizontals.add(String.valueOf(i+1));
        }
        if(pattern.getLength() == 1){
            horizontals.add("");
        }

        String[] hvals = new String[horizontals.size()];
        hvals = horizontals.toArray(hvals);

        Log.e("PatternEditor","Horizontal Values: " + hvals.toString());

        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
        staticLabelsFormatter.setVerticalLabels(new String[] {"one", "two", "three", "four", "five", "six", "seven", "eight"});
        staticLabelsFormatter.setHorizontalLabels(hvals);
        graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);

        graph.addSeries(series);
    }

    private void setupButtons(){
        ImageView patternBeatLast = (ImageView) findViewById(R.id.patternBeatLast);
        ImageView patternBeatNext = (ImageView) findViewById(R.id.patternBeatNext);
        ImageView patternBeatNew = (ImageView) findViewById(R.id.patternBeatNew);
        TextView patternBeatDisplay = (TextView) findViewById(R.id.patternBeatDisplay);

        ImageView first = (ImageView) findViewById(R.id.first);
        ImageView second = (ImageView) findViewById(R.id.second);
        ImageView third = (ImageView) findViewById(R.id.third);
        ImageView fourth = (ImageView) findViewById(R.id.fourth);
        ImageView fifth = (ImageView) findViewById(R.id.fifth);
        ImageView sixth = (ImageView) findViewById(R.id.sixth);
        ImageView seventh = (ImageView) findViewById(R.id.seventh);
        ImageView eighth = (ImageView) findViewById(R.id.eighth);

        if(pattern.getLength() == 1){
            patternBeatLast.setVisibility(View.INVISIBLE);
            patternBeatNext.setVisibility(View.INVISIBLE);
        } else {
            patternBeatLast.setVisibility(View.VISIBLE);
            patternBeatNext.setVisibility(View.VISIBLE);
        }

        if(currentBeat == 1){
            patternBeatLast.setVisibility(View.INVISIBLE);
        } else {
            patternBeatLast.setVisibility(View.VISIBLE);
        }

        if(currentBeat == pattern.getLength()){
            patternBeatNext.setVisibility(View.INVISIBLE);
        } else {
            patternBeatNext.setVisibility(View.VISIBLE);
        }

        patternBeatDisplay.setText(String.valueOf(currentBeat));

        beat = pattern.getBeat(currentBeat-1);
        if(beat.getFirst()){
            first.setImageResource(R.drawable.numeric_1_box);
        }else {
            first.setImageResource(R.drawable.numeric_1_box_outline);
        }
        if(beat.getSecond()){
            second.setImageResource(R.drawable.numeric_2_box);
        }else {
            second.setImageResource(R.drawable.numeric_2_box_outline);
        }
        if(beat.getThird()){
            third.setImageResource(R.drawable.numeric_3_box);
        }else {
            third.setImageResource(R.drawable.numeric_3_box_outline);
        }
        if(beat.getFourth()){
            fourth.setImageResource(R.drawable.numeric_4_box);
        }else {
            fourth.setImageResource(R.drawable.numeric_4_box_outline);
        }
        if(beat.getFifth()){
            fifth.setImageResource(R.drawable.numeric_5_box);
        }else {
            fifth.setImageResource(R.drawable.numeric_5_box_outline);
        }
        if(beat.getSixth()){
            sixth.setImageResource(R.drawable.numeric_6_box);
        }else {
            sixth.setImageResource(R.drawable.numeric_6_box_outline);
        }
        if(beat.getSeventh()){
            seventh.setImageResource(R.drawable.numeric_7_box);
        }else {
            seventh.setImageResource(R.drawable.numeric_7_box_outline);
        }
        if(beat.getEighth()){
            eighth.setImageResource(R.drawable.numeric_8_box);
        }else {
            eighth.setImageResource(R.drawable.numeric_8_box_outline);
        }

        patternBeatLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentBeat--;
                setupButtons();
                Log.e("patternBeatLast","currentBeat: " + currentBeat);
            }
        });

        patternBeatNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentBeat++;
                setupButtons();
                Log.e("patternBeatNext","currentBeat: " + currentBeat);
            }
        });

        patternBeatNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("patternBeatNew","currentBeat: " + currentBeat);
            }
        });

        first.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat-1).setFIRST(!pattern.getBeat(currentBeat-1).getFirst());
                Log.e("onClick", "signature: " + pattern.getPatternHexSignature());
                setupButtons();
                graphPattern();
            }
        });

        second.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat-1).setSECOND(!pattern.getBeat(currentBeat-1).getSecond());
                Log.e("onClick", "signature: " + pattern.getPatternHexSignature());
                setupButtons();
                graphPattern();
            }
        });

        third.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat-1).setTHIRD(!pattern.getBeat(currentBeat-1).getThird());
                Log.e("onClick", "signature: " + pattern.getPatternHexSignature());
                setupButtons();
                graphPattern();
            }
        });

        fourth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat-1).setFOURTH(!pattern.getBeat(currentBeat-1).getFourth());
                Log.e("onClick", "signature: " + pattern.getPatternHexSignature());
                setupButtons();
                graphPattern();
            }
        });

        fifth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat-1).setFIFTH(!pattern.getBeat(currentBeat-1).getFifth());
                Log.e("onClick", "signature: " + pattern.getPatternHexSignature());
                setupButtons();
                graphPattern();
            }
        });

        sixth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat-1).setSIXTH(!pattern.getBeat(currentBeat-1).getSixth());
                Log.e("onClick", "signature: " + pattern.getPatternHexSignature());
                setupButtons();
                graphPattern();
            }
        });

        seventh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat-1).setSEVENTH(!pattern.getBeat(currentBeat-1).getSeventh());
                Log.e("onClick", "signature: " + pattern.getPatternHexSignature());
                setupButtons();
                graphPattern();
            }
        });

        eighth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pattern.getBeat(currentBeat-1).setEIGHTH(!pattern.getBeat(currentBeat-1).getEighth());
                Log.e("onClick", "signature: " + pattern.getPatternHexSignature());
                setupButtons();
                graphPattern();
            }
        });
    }

}
