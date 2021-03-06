package com.umpquariversoftware.metronome.kitEditor;

import android.app.Dialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.umpquariversoftware.metronome.FireBase.FirebaseKit;
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.UI.RecyclerViewItemClickListener;
import com.umpquariversoftware.metronome.UI.SnappyRecyclerView;
import com.umpquariversoftware.metronome.database.StandardDbContract;
import com.umpquariversoftware.metronome.elements.Component;
import com.umpquariversoftware.metronome.elements.Kit;

import java.util.ArrayList;

import static com.umpquariversoftware.metronome.database.StandardDbContract.buildAllComponentsUri;
import static com.umpquariversoftware.metronome.database.StandardDbContract.buildComponentByDbIDUri;

/**
 * KitEditor is an activity that allows the user to build their own kit.
 * 8 RecyclerViews are built and populated from the components table
 * Each of these recyclerViews represents a specific component in the overall 8 piece kit.
 * Scrolling to the item assigns it to the kit. Tapping the item plays the associated sound
 *
 * Kits saved in this activity go straight to firebase. The mainActivity pulls updates
 * from Firebase.
 * **/

public class KitEditor extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final int COMPONENT_LOADER_ID = 4;
    private int mComponentCount = 1;
    private Kit mKit;
    private ComponentCursorAdapter mComponentCursorAdapter;
    private Cursor mComponentCursor;
    private Context mContext;
    private Boolean mMasterListSearchResultsBack = false;
    private Boolean mUserListSearchResultsBack = false;
    private String userID = "";

    private FirebaseKit mMasterListKit;
    private FirebaseKit mUserListKit;

    private final AudioAttributes audioAttrib = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();

    private final SoundPool soundPool = new SoundPool.Builder()
            .setAudioAttributes(audioAttrib)
            .setMaxStreams(8)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kit_editor);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        mContext = this;
        userID = getIntent().getStringExtra("userID");

        /**
         * Pull the first row from the database, and build a kit with
         * all 8 components built from the data in that row.
         * This is our starting point
         * **/

        Cursor cursor = getContentResolver().query(StandardDbContract.buildComponentByDbIDUri(1),
                null,
                null,
                null,
                null);
        Component component = null;
        if(cursor!=null){
            component = new Component(cursor);
            cursor.close();
        }

        mKit = new Kit();
        for (int x = 0; x < 8; ++x) {
            mKit.addComponent(component);
        }

        /**
         * Setup the UI
         * */
        setupToolbar();
        setupComponentChooser();
        setupFab();
        getThatMoney();

    }

    private void setupToolbar() {
        /**
         * Buttons and onClick listeners
         * */

        Toolbar toolbar = (Toolbar) findViewById(R.id.editor_toolbar);
        setSupportActionBar(toolbar);

        TextView title = (TextView) findViewById(R.id.activityDisplay);
        title.setText(getResources().getString(R.string.kit_editor));

        ImageView helpButton = (ImageView) findViewById(R.id.helpButton);
        helpButton.setFocusable(true);
        helpButton.setContentDescription(getResources().getString(R.string.help));
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, getResources().getString(R.string.placeholder), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupFab() {
        /**
         * Setup Fab/click listener
         * */
        FloatingActionButton fab;
        fab = (FloatingActionButton) findViewById(R.id.kitEditorButton);
        fab.setContentDescription(getResources().getString(R.string.save_kit_to_cloud));
        fab.setFocusable(true);

        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                check(mKit.getSignature());
            }
        });
    }

    private void getThatMoney(){
        /**
         * Setup adMobs.
         * */
        // MobileAds.initialize(getApplicationContext(), "ca-app-pub-8040545141030965~5491821531");

        AdView mAdView = (AdView) findViewById(R.id.adView);

        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("74D61A4429900485751F374428FB6C95")
                .build();
        mAdView.loadAd(adRequest);
    }

    private void setupComponentChooser() {
        /**
         * We're setting up 8 recyclerViews that all feed from through the same
         * cursor adapter. The only difference between them is the position
         * in the kit they represent.
         *
         * Rather than repeat code over and over again, I just built an arrayList
         * and built 8 of the RV's in a for loop.
         * */

        getLoaderManager().initLoader(COMPONENT_LOADER_ID, null, this);
        mComponentCursorAdapter = new ComponentCursorAdapter(this, null);

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
        for (int x = 0; x < 8; ++x) {
            final SnappyRecyclerView srv = (SnappyRecyclerView) components.get(x);
            srv.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            srv.setLayoutManager(llm);
            srv.setAdapter(mComponentCursorAdapter);
            String description = getResources().getString(R.string.swipe_left_or_right_to_change_component).concat(String.valueOf(x));
            srv.setContentDescription(description);
            srv.setFocusable(true);
            snappyRecyclerViews.add(x, srv);
        }

        for (int x = 0; x < 8; ++x) {
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
                            Component component;
                            if(retCursor!=null){
                                retCursor.moveToFirst();
                                component = new Component(retCursor);
                                retCursor.close();
                                playSound(component);
                            }
                        }
                    }));
        }

        for (int x = 0; x < 8; ++x) {
            final int finalX = x;
            snappyRecyclerViews.get(x).addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (snappyRecyclerViews.get(finalX).getFirstVisibleItemPosition() >= 0) {
                        // get information from db
                        Cursor retCursor = getContentResolver()
                                .query(buildComponentByDbIDUri(snappyRecyclerViews.get(finalX).getFirstVisibleItemPosition() + 1),
                                        null,
                                        null,
                                        null,
                                        null);
                        Component component;
                        if(retCursor!=null){
                            retCursor.moveToFirst();
                            component = new Component(retCursor);
                            retCursor.close();
                            mKit.replaceComponent(finalX, component);
                        }
                    }
                }
            });
        }

        for (int x = 0; x < 8; ++x) {
            snappyRecyclerViews.get(x).scrollToPosition(mComponentCount);
        }
    }

    private void playSound(Component component) {
        /**
         * Extract the sound resource from the component, and play it once.
         * */
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int i, int i1) {
                soundPool.play(i, 1, 1, 1, 0, 1);
            }
        });
        soundPool.load(mContext, component.getResource(), 1);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
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
        switch (loader.getId()) {
            case COMPONENT_LOADER_ID:
                mComponentCursorAdapter.swapCursor(data);
                mComponentCursor = data;
                mComponentCount = data.getCount();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void check(String signature) {
        /**
         * Looks in kits/master and kits/user to see if the kit is there.
         * Alerts if already present. Sends to askAndInsert() if not found.
         * **/
        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mMasterListSearchResultsBack = false;
        mUserListSearchResultsBack = false;
        mMasterListKit = null;
        mUserListKit = null;

        mDatabase.child("kits").child("master").child(signature)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FirebaseKit masterListKit = dataSnapshot.getValue(FirebaseKit.class);
                        if (masterListKit != null) {
                            mMasterListKit = masterListKit;
                        } else {
                            mMasterListKit = null;
                        }
                        mMasterListSearchResultsBack = true;
                        if (mUserListSearchResultsBack) {
                            if (mMasterListKit != null) {
                                alert(getString(R.string.kit_exists), mMasterListKit.getName());
                            } else if (mUserListKit != null) {
                                alert(getString(R.string.kit_exists), mUserListKit.getName());
                            } else {
                                askAndInsert();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        mDatabase.child("kits").child("users").child(userID).child(signature)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FirebaseKit userListKit = dataSnapshot.getValue(FirebaseKit.class);
                        if (userListKit != null) {
                            mUserListKit = userListKit;
                        } else {
                            mUserListKit = null;
                        }
                        mUserListSearchResultsBack = true;
                        if (mMasterListSearchResultsBack) {
                            if (mMasterListKit != null) {
                                alert(getString(R.string.kit_exists), mMasterListKit.getName());
                            } else if (mUserListKit != null) {
                                alert(getString(R.string.kit_exists), mUserListKit.getName());
                            } else {
                                askAndInsert();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void alert(String text1, String text2) {
        /**
         * Simple alert dialog
         * **/
        final Dialog dialog = new Dialog(mContext);

        dialog.setContentView(R.layout.alert_dialog);

        TextView alertText = (TextView) dialog.findViewById(R.id.alertText);
        alertText.setText(text1);

        TextView alertText2 = (TextView) dialog.findViewById(R.id.alertText2);
        alertText2.setText(text2);

        Button okButton = (Button) dialog.findViewById(R.id.alertOK);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        okButton.setFocusable(true);
        okButton.setContentDescription(getResources().getString(R.string.alertOK));
        dialog.show();
    }

    private void askAndInsert() {
        /**
         * Request Kit name, and save current kit to Firebase kits/users/user
         * */
        final DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        new MaterialDialog.Builder(mContext).title(R.string.enter_kit_name)
                .content(R.string.content_test)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        FirebaseKit fbk = new FirebaseKit(input.toString(), mKit.getSignature());
                        mDatabase.child("kits")
                                .child("users")
                                .child(userID)
//                                .child("master") // Allows me to write to master directory. (Change FB permissions first)
                                .child(fbk.getSignature())
                                .setValue(fbk);
                    }
                })
                .show();
    }
}
