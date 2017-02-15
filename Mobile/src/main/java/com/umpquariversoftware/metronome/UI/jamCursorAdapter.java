package com.umpquariversoftware.metronome.UI;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.database.dbContract;
import com.umpquariversoftware.metronome.elements.Kit;

/**
 * Created by robert on 1/30/17.
 */

public class jamCursorAdapter extends CursorRecyclerViewAdapter<jamCursorAdapter.ViewHolder> {
    private static Context mContext;

    public jamCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        Log.e("jamCursorAdapter", "constructor");
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.control_panel, parent, false);
        ViewHolder vh = new ViewHolder(itemView);
        return vh;
    }

    @Override
    public void onBindViewHolder(jamCursorAdapter.ViewHolder viewHolder, Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndex(dbContract.JamTable.NAME));
        String tempo = cursor.getString(cursor.getColumnIndex(dbContract.JamTable.TEMPO));

        viewHolder.name.setText(name);
        viewHolder.tempo.setText(tempo);
    }


    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public TextView name;
        public TextView tempo;



        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.jamName);
            tempo = (TextView) itemView.findViewById(R.id.jamTempo);

        }

        @Override
        public void onClick(View view) {
            Log.e("patternCursorAdapter", "Viewholder onClick()");
        }
    }
}