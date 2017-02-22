package com.umpquariversoftware.metronome.UI;

import android.content.Context;
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
import com.umpquariversoftware.metronome.FireBase.FirebasePattern;
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.database.dbContract;
import com.umpquariversoftware.metronome.elements.Pattern;

import java.util.ArrayList;

/**
 * Created by robert on 2/21/17.
 */

public class patternListAdapter extends RecyclerView.Adapter<patternListAdapter.ViewHolder> {
    private Context mContext;
    private ArrayList<FirebasePattern> firebasePatterns;


    public patternListAdapter(ArrayList<FirebasePattern> firebasePatterns, Context mContext) {
        super();
        this.mContext = mContext;
        this.firebasePatterns = firebasePatterns;
        Log.e("patternListAdapter", "Created. firebasePatterns.size(): "+ firebasePatterns.size());

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.pattern_chooser, parent, false);
        ViewHolder vh = new ViewHolder(itemView);
        return vh;
    }

    @Override
    public void onBindViewHolder(patternListAdapter.ViewHolder viewHolder, int position) {
        Log.e("patternListAdapter", "onBindViewHolder");

        String name = firebasePatterns.get(position).getName();
        String signature = firebasePatterns.get(position).getSignature();

        Pattern tempPattern = new Pattern(name, signature, mContext);
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

    @Override
    public int getItemCount() {
        return firebasePatterns.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        CardView patternCardView;
        GraphView patternGraphView;
        TextView patternName;

        ViewHolder(View itemView) {
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

    public ArrayList<FirebasePattern> getFirebasePatterns() {
        return firebasePatterns;
    }
}
