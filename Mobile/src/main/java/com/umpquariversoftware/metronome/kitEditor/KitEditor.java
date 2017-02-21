package com.umpquariversoftware.metronome.kitEditor;

import android.app.Dialog;
import android.app.IntentService;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.UI.RecyclerViewItemClickListener;
import com.umpquariversoftware.metronome.UI.SnappyRecyclerView;
import com.umpquariversoftware.metronome.UI.kitCursorAdapter;
import com.umpquariversoftware.metronome.database.dbContract;
import com.umpquariversoftware.metronome.elements.Component;
import com.umpquariversoftware.metronome.elements.Kit;

import java.util.ArrayList;

import static com.umpquariversoftware.metronome.database.dbContract.buildAllComponentsUri;
import static com.umpquariversoftware.metronome.database.dbContract.buildComponentByDbIDUri;
import static com.umpquariversoftware.metronome.database.dbContract.buildComponentUri;
import static com.umpquariversoftware.metronome.database.dbContract.buildJamUri;
import static com.umpquariversoftware.metronome.database.dbContract.buildKitBySignatureUri;
import static com.umpquariversoftware.metronome.database.dbContract.buildKitUri;
import static com.umpquariversoftware.metronome.database.dbContract.buildPatternBySignatureURI;
import static com.umpquariversoftware.metronome.database.dbContract.buildPatternUri;

public class KitEditor extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    final int COMPONENT_LOADER_ID = 4;
    int mComponentCount = 1;
    Kit mKit;
    componentCursorAdapter mComponentCursorAdapter;
    Cursor mComponentCursor;
    Context mContext;

    AudioAttributes audioAttrib = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();

    SoundPool soundPool = new SoundPool.Builder()
            .setAudioAttributes(audioAttrib)
            .setMaxStreams(8)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kit_editor);
        mContext = this;
        /**
         * Point the cursor at the first item in the component database
         * Use that row to build a component.
         * */

        Cursor cursor = getContentResolver().query(dbContract.buildComponentByDbIDUri(1),
                null,
                null,
                null,
                null);
        Component component = new Component(cursor);

        /**
         * Build a kit, and fill all 8 slots with the same component
         * */
        mKit = new Kit();
        for(int x=0;x<8;++x){
            mKit.addComponent(component);
        }

        /**
         * Build SnappyRecyclerView for each component
         * */

        setupComponentChooser();
        setupFab();
    }

    public void setupFab(){
        FloatingActionButton fab;
        fab = (FloatingActionButton) findViewById(R.id.kitEditorButton);
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //First check to see if the kit exists in the database already
                Cursor cursor = getContentResolver().query(buildKitBySignatureUri(mKit.getSignature()),
                        null,
                        null,
                        null,
                        null);

                if(cursor.getCount()!=0) {
                    cursor.moveToFirst();
                    String kitName = cursor.getString(cursor.getColumnIndex(dbContract.KitTable.NAME));
                    cursor.close();

                    Log.e("KitEditor", "Cursor was not empty. Name returned was: " + kitName);
                    final Dialog dialog = new Dialog(mContext);

                    dialog.setContentView(R.layout.alert_dialog);
                    dialog.setTitle("EXISTS!");

                    TextView alertText = (TextView) dialog.findViewById(R.id.alertText);
                    alertText.setText(R.string.kit_exists);

                    TextView alertText2 = (TextView) dialog.findViewById(R.id.alertText2);
                    alertText2.setText(kitName);

                    Button okButton = (Button) dialog.findViewById(R.id.alertOK);
                    okButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.cancel();
                        }
                    });
                    dialog.show();
                } else {
                    new MaterialDialog.Builder(mContext).title(R.string.enter_kit_name)
                        .content(R.string.content_test)
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                ContentValues contentValues;

                                contentValues = new ContentValues();
                                contentValues.put(dbContract.KitTable.NAME, input.toString());
                                contentValues.put(dbContract.KitTable.COMPONENTS, mKit.getSignature());

                                Uri i = getContentResolver().insert(buildKitUri(), contentValues);
                                Log.e("KitEditor", "insert() Returned URI:" + i.toString());
                            }
                        })
                        .show();
                }
            }
        });


    }

    public void playSound(Component component){
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int i, int i1) {
                soundPool.play(i,1,1,1,0,1);
            }
        });
        soundPool.load(mContext, component.getResource(), 1);

    }

    public void setupComponentChooser(){
        getLoaderManager().initLoader(COMPONENT_LOADER_ID, null, this);

        mComponentCursorAdapter = new componentCursorAdapter(this, null);

        ArrayList<View> components = new ArrayList<>();
        components.add(0, findViewById(R.id.recyclerViewComponent1));
        components.add(1, findViewById(R.id.recyclerViewComponent2));
        components.add(2, findViewById(R.id.recyclerViewComponent3));
        components.add(3, findViewById(R.id.recyclerViewComponent4));
        components.add(4, findViewById(R.id.recyclerViewComponent5));
        components.add(5, findViewById(R.id.recyclerViewComponent6));
        components.add(6, findViewById(R.id.recyclerViewComponent7));
        components.add(7, findViewById(R.id.recyclerViewComponent8));

        final ArrayList<SnappyRecyclerView> snappyRecyclerViews = new ArrayList<>();
        for (int x = 0; x<8; ++x) {
            final SnappyRecyclerView srv = (SnappyRecyclerView) components.get(x);
            srv.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            srv.setLayoutManager(llm);
            srv.setAdapter(mComponentCursorAdapter);

            snappyRecyclerViews.add(x,srv);
        }

        for (int x = 0; x<8 ; ++x) {
            snappyRecyclerViews.get(x).addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                    new RecyclerViewItemClickListener.OnItemClickListener() {
                        @Override
                        public void onItemClick(View v, int position) {
                            mComponentCursor.moveToPosition(position);
                            Cursor retCursor = getContentResolver()
                                    .query(buildComponentByDbIDUri(position + 1),
                                            null,
                                            null,
                                            null,
                                            null);
                            retCursor.moveToFirst();
                            Component component = new Component(retCursor);
                            playSound(component);
                        }
                    }));
        }

        for (int x = 0; x<8 ; ++x) {
            final int finalX = x;
            snappyRecyclerViews.get(x).addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if(snappyRecyclerViews.get(finalX).getFirstVisibleItemPosition() >=0){
                        // get information from db
                        Cursor retCursor = getContentResolver()
                                .query(buildComponentByDbIDUri(snappyRecyclerViews.get(finalX).getFirstVisibleItemPosition()+1),
                                        null,
                                        null,
                                        null,
                                        null);
                        retCursor.moveToFirst();
                        // put that component in the Kit component(0)
                        Component component = new Component(retCursor);
                        Log.e("rvComponent1", "component.getResource: " + component.getResource());
                        mKit.replaceComponent(finalX,component);
                    }
                }
            });
        }

        for (int x = 0; x<8; ++x){
            snappyRecyclerViews.get(x).scrollToPosition(mComponentCount);
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.e("onCreateLoader", "int i = " + i);
        switch (i) {
            case COMPONENT_LOADER_ID:
                return new CursorLoader(this, buildAllComponentsUri(),
                        null,
                        null,
                        null,
                        null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch(loader.getId()) {
            case COMPONENT_LOADER_ID:
                mComponentCursorAdapter.swapCursor(data);
                mComponentCursor = data;
                Log.e("KitEditor", "onLoadFinished data.getCount(): " + data.getCount());
                mComponentCount = data.getCount();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}