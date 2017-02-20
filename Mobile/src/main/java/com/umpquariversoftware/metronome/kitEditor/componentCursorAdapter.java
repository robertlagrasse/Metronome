package com.umpquariversoftware.metronome.kitEditor;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.UI.CursorRecyclerViewAdapter;
import com.umpquariversoftware.metronome.database.dbContract;
import com.umpquariversoftware.metronome.elements.Component;
import com.umpquariversoftware.metronome.elements.Kit;

/**
 * Created by robert on 2/18/17.
 */

public class componentCursorAdapter extends CursorRecyclerViewAdapter<componentCursorAdapter.ViewHolder> {
private static Context mContext;

public componentCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        Log.e("componentCursorAdapter", "constructor");
        mContext = context;
        }

@Override
public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.component_chooser, parent, false);
        ViewHolder vh = new ViewHolder(itemView);
        return vh;
        }

@Override
public void onBindViewHolder(componentCursorAdapter.ViewHolder viewHolder, Cursor cursor) {

    String componentName = cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.NAME));
    String componentDbID = cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.ID));
    String componentResourceID = cursor.getString(cursor.getColumnIndex(dbContract.ComponentTable.RESOURCE));

    viewHolder.componentName.setText(componentName);
    viewHolder.componentDbID.setText(componentDbID);
    viewHolder.componentResourceID.setText(componentResourceID);
    }


public static class ViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener {
    public TextView componentName;
    public TextView componentDbID;
    public TextView componentResourceID;


    public ViewHolder(View itemView) {
        super(itemView);
        componentName = (TextView) itemView.findViewById(R.id.componentName);
        componentDbID = (TextView) itemView.findViewById(R.id.componentDbID);
        componentResourceID = (TextView) itemView.findViewById(R.id.componentResourceID);
    }

    @Override
    public void onClick(View view) {
        Log.e("componentCursorAdapter", "Viewholder onClick()");
    }
}
}