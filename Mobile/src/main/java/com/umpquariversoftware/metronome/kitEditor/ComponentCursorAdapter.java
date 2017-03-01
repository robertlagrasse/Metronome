package com.umpquariversoftware.metronome.kitEditor;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.UI.CursorRecyclerViewAdapter;
import com.umpquariversoftware.metronome.database.dbContract;

/**
 * Created by robert on 2/18/17.
 */

public class ComponentCursorAdapter extends CursorRecyclerViewAdapter<ComponentCursorAdapter.ViewHolder> {
    private static Context mContext;

    public ComponentCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_chooser, parent, false);
        ViewHolder vh = new ViewHolder(itemView);
        return vh;
    }

    @Override
    public void onBindViewHolder(ComponentCursorAdapter.ViewHolder viewHolder, Cursor cursor) {

        String componentName = cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.NAME));
        viewHolder.componentName.setText(componentName);

    }


    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public TextView componentName;


        public ViewHolder(View itemView) {
            super(itemView);
            componentName = (TextView) itemView.findViewById(R.id.componentName);
        }

        @Override
        public void onClick(View view) {
            // Do something
        }
    }
}