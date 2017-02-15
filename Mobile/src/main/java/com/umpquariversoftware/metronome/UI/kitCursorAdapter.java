package com.umpquariversoftware.metronome.UI;

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
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.database.dbContract;
import com.umpquariversoftware.metronome.elements.Kit;
import com.umpquariversoftware.metronome.elements.Pattern;

/**
 * Created by robert on 1/30/17.
 */

public class kitCursorAdapter extends CursorRecyclerViewAdapter<kitCursorAdapter.ViewHolder> {
    private static Context mContext;

    public kitCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        Log.e("patternCursorAdapter", "constructor");
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.kit_chooser, parent, false);
        ViewHolder vh = new ViewHolder(itemView);
        return vh;
    }

    @Override
    public void onBindViewHolder(kitCursorAdapter.ViewHolder viewHolder, Cursor cursor) {
        String signature = cursor.getString(cursor.getColumnIndex(dbContract.KitTable.COMPONENTS));
        Kit tempKit = new Kit(cursor.getString(cursor.getColumnIndex(dbContract.KitTable.NAME)), signature, mContext);

        viewHolder.component1.setText(tempKit.getComponents().get(0).getName());
        viewHolder.component2.setText(tempKit.getComponents().get(1).getName());
        viewHolder.component3.setText(tempKit.getComponents().get(2).getName());
        viewHolder.component4.setText(tempKit.getComponents().get(3).getName());
        viewHolder.component5.setText(tempKit.getComponents().get(4).getName());
        viewHolder.component6.setText(tempKit.getComponents().get(5).getName());
        viewHolder.component7.setText(tempKit.getComponents().get(6).getName());
        viewHolder.component8.setText(tempKit.getComponents().get(7).getName());
        viewHolder.kitName.setText(tempKit.getName());
    }


    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public TextView component1;
        public TextView component2;
        public TextView component3;
        public TextView component4;
        public TextView component5;
        public TextView component6;
        public TextView component7;
        public TextView component8;
        public TextView kitName;


        public ViewHolder(View itemView) {
            super(itemView);
            component1 = (TextView) itemView.findViewById(R.id.component1);
            component2 = (TextView) itemView.findViewById(R.id.component2);
            component3 = (TextView) itemView.findViewById(R.id.component3);
            component4 = (TextView) itemView.findViewById(R.id.component4);
            component5 = (TextView) itemView.findViewById(R.id.component5);
            component6 = (TextView) itemView.findViewById(R.id.component6);
            component7 = (TextView) itemView.findViewById(R.id.component7);
            component8 = (TextView) itemView.findViewById(R.id.component8);
            kitName = (TextView) itemView.findViewById(R.id.kitName);
        }

        @Override
        public void onClick(View view) {
            Log.e("patternCursorAdapter", "Viewholder onClick()");
        }
    }
}
