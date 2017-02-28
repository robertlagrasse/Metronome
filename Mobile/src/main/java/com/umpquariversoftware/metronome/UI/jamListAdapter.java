package com.umpquariversoftware.metronome.UI;

/**
 * Created by robert on 2/21/17.
 */
import android.content.Context;
import android.database.Cursor;
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
import com.umpquariversoftware.metronome.FireBase.FirebaseJam;
import com.umpquariversoftware.metronome.FireBase.FirebaseKit;
import com.umpquariversoftware.metronome.FireBase.FirebasePattern;
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.database.dbContract;
import com.umpquariversoftware.metronome.elements.Kit;
import com.umpquariversoftware.metronome.elements.Pattern;

import java.util.ArrayList;

public class jamListAdapter extends RecyclerView.Adapter<jamListAdapter.ViewHolder> {
    private Context mContext;
    private ArrayList<FirebaseJam> firebaseJams;


    public jamListAdapter(ArrayList<FirebaseJam> firebasejams, Context mContext) {
        super();
        this.mContext = mContext;
        this.firebaseJams = firebasejams;
    }

    @Override
    public jamListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.control_panel, parent, false);
        jamListAdapter.ViewHolder vh = new jamListAdapter.ViewHolder(itemView);
        return vh;
    }


    @Override
    public void onBindViewHolder(jamListAdapter.ViewHolder viewHolder, int position) {
        String name = firebaseJams.get(position).getName();
//        String tempo = String.valueOf(firebaseJams.get(position).getTempo());

        viewHolder.name.setText(name);
        // viewHolder.tempo.setText(tempo);
    }

    @Override
    public int getItemCount() {
        return firebaseJams.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public TextView name;
        // public TextView tempo;



        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.jamName);
            // tempo = (TextView) itemView.findViewById(R.id.jamTempo);

        }

        @Override
        public void onClick(View view) {
        }
    }

}

