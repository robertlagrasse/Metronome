package com.umpquariversoftware.metronome.UI;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.umpquariversoftware.metronome.FireBase.FirebaseKit;
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.elements.Kit;

import java.util.ArrayList;

/**
 * Basic ArrayList Adapter
 */

public class KitListAdapter extends RecyclerView.Adapter<KitListAdapter.ViewHolder> {
    private final Context mContext;
    private final ArrayList<FirebaseKit> firebaseKits;

    public KitListAdapter(ArrayList<FirebaseKit> firebaseKits, Context mContext) {
        super();
        this.mContext = mContext;
        this.firebaseKits = firebaseKits;
    }

    @Override
    public KitListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.kit_chooser, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(KitListAdapter.ViewHolder viewHolder, int position) {
        String name = firebaseKits.get(position).getName();
        String signature = firebaseKits.get(position).getSignature();

        Kit tempKit = new Kit(name, signature, mContext);

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

    @Override
    public int getItemCount() {
        return firebaseKits.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public final TextView component1;
        public final TextView component2;
        public final TextView component3;
        public final TextView component4;
        public final TextView component5;
        public final TextView component6;
        public final TextView component7;
        public final TextView component8;
        public final TextView kitName;

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
        }
    }
}
