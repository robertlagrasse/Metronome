package com.umpquariversoftware.metronome.UI;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.speech.tts.TextToSpeech;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.database.dbContract;
import com.umpquariversoftware.metronome.elements.Pattern;

/**
 * Created by robert on 1/30/17.
 */

public class patternCursorAdapter extends CursorRecyclerViewAdapter<patternCursorAdapter.ViewHolder> {
    private static Context mContext;
    public String mID;

    public patternCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        Log.e("patternCursorAdapter", "constructor");
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.pattern_chooser, parent, false);
        ViewHolder vh = new ViewHolder(itemView);
        return vh;
    }

    @Override
    public void onBindViewHolder(patternCursorAdapter.ViewHolder viewHolder, Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndex(dbContract.PatternTable.NAME));
        String signature = cursor.getString(cursor.getColumnIndex(dbContract.PatternTable.SEQUENCE));
        String id = cursor.getString(cursor.getColumnIndex(dbContract.PatternTable.ID));
        mID = id;
        Pattern tempPattern = new Pattern("temp", signature, mContext);
        PointsGraphSeries<DataPoint> series = new PointsGraphSeries<>();
        series = tempPattern.getPatternDataPoints();

        viewHolder.patternGraphView.getViewport().setXAxisBoundsManual(true);
        viewHolder.patternGraphView.getViewport().setMinX(0.5);
        viewHolder.patternGraphView.getViewport().setMaxX(tempPattern.getLength() + 0.5);

        // set manual Y bounds
        viewHolder.patternGraphView.getViewport().setYAxisBoundsManual(true);
        viewHolder.patternGraphView.getViewport().setMinY(1);
        viewHolder.patternGraphView.getViewport().setMaxY(8);

        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(viewHolder.patternGraphView);
        staticLabelsFormatter.setVerticalLabels(new String[] {"one", "two", "three", "four", "five", "six", "seven", "eight"});
        viewHolder.patternGraphView.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);

        viewHolder.patternGraphView.addSeries(series);
        viewHolder.patternName.setText(name);
    }


    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public CardView patternCardView;
        public GraphView patternGraphView;
        public TextView patternName;

        public ViewHolder(View itemView) {
            super(itemView);
            patternCardView = (CardView) itemView.findViewById(R.id.patternCardView);
            patternGraphView = (GraphView) itemView.findViewById(R.id.patternGraph);
            patternName = (TextView) itemView.findViewById(R.id.patternName);
        }

        @Override
        public void onClick(View view) {
            Log.e("patternCursorAdapter", "Viewholder onClick()");
        }
    }
}
