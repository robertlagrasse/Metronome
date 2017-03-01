package com.umpquariversoftware.metronome.UI;

/**
 * Created by robert on 2/21/17.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.umpquariversoftware.metronome.FireBase.FirebaseJam;
import com.umpquariversoftware.metronome.R;

import java.util.ArrayList;

public class JamListAdapter extends RecyclerView.Adapter<JamListAdapter.ViewHolder> {
    private Context mContext;
    private ArrayList<FirebaseJam> firebaseJams;


    public JamListAdapter(ArrayList<FirebaseJam> firebasejams, Context mContext) {
        super();
        this.mContext = mContext;
        this.firebaseJams = firebasejams;
    }

    @Override
    public JamListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.control_panel, parent, false);
        JamListAdapter.ViewHolder vh = new JamListAdapter.ViewHolder(itemView);
        return vh;
    }


    @Override
    public void onBindViewHolder(JamListAdapter.ViewHolder viewHolder, int position) {
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

