package com.umpquariversoftware.metronome.UI;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
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
import com.umpquariversoftware.metronome.elements.Pattern;

import java.util.ArrayList;

/**
 * Basic arraylist adapter
 */

public class PatternListAdapter extends RecyclerView.Adapter<PatternListAdapter.ViewHolder> {
    private final Context mContext;
    private final ArrayList<FirebasePattern> firebasePatterns;

    public PatternListAdapter(ArrayList<FirebasePattern> firebasePatterns, Context mContext) {
        super();
        this.mContext = mContext;
        this.firebasePatterns = firebasePatterns;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.pattern_chooser, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(PatternListAdapter.ViewHolder viewHolder, int position) {
        String name = firebasePatterns.get(position).getName();
        String signature = firebasePatterns.get(position).getSignature();

        Pattern tempPattern = new Pattern(name, signature, mContext);
        PointsGraphSeries<DataPoint> series = new PointsGraphSeries<>();
        //noinspection unchecked
        series = tempPattern.getPatternDataPoints();

        viewHolder.patternGraphView.getViewport().setXAxisBoundsManual(true);
        viewHolder.patternGraphView.getViewport().setMinX(1);
        viewHolder.patternGraphView.getViewport().setMaxX(tempPattern.getLength());

        // set manual Y bounds
        viewHolder.patternGraphView.getViewport().setYAxisBoundsManual(true);
        viewHolder.patternGraphView.getViewport().setMinY(1);
        viewHolder.patternGraphView.getViewport().setMaxY(8);

        ArrayList<String> horizontals = new ArrayList<>();

        horizontals.clear();
        for (int i = 0; i < tempPattern.getLength(); ++i) {
            horizontals.add(String.valueOf(i + 1));
        }
        if (tempPattern.getLength() == 1) {
            horizontals.add("");
        }

        String[] hvals = new String[horizontals.size()];
        hvals = horizontals.toArray(hvals);

        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(viewHolder.patternGraphView);
        staticLabelsFormatter.setVerticalLabels(mContext.getResources().getStringArray(R.array.patternGraphLabels));
        staticLabelsFormatter.setHorizontalLabels(hvals);
        viewHolder.patternGraphView.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);

        viewHolder.patternGraphView.removeAllSeries();
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
        final CardView patternCardView;
        final GraphView patternGraphView;
        final TextView patternName;

        ViewHolder(View itemView) {
            super(itemView);
            patternCardView = (CardView) itemView.findViewById(R.id.patternCardView);
            patternGraphView = (GraphView) itemView.findViewById(R.id.patternGraph);
            patternName = (TextView) itemView.findViewById(R.id.patternName);
        }

        @Override
        public void onClick(View view) {
        }
    }
}
