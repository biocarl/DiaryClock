package com.example.carlh.diaryclock.ui.memo.list;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.arch.lifecycle.LifecycleFragment;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.RecyclerView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.activity.MainActivity;
import com.example.carlh.diaryclock.activity.WaveActivity;
import com.example.carlh.diaryclock.app.DiaryClock;
import com.example.carlh.diaryclock.cloud.tasks.AsyncSyncFolderTask;
import com.example.carlh.diaryclock.cloud.tasks.persistentAction.Action;
import com.example.carlh.diaryclock.cloud.tasks.persistentAction.ActionHelper;
import com.example.carlh.diaryclock.cloud.tasks.persistentAction.XMLWriter;
import com.example.carlh.diaryclock.data.AppDatabase;
import com.example.carlh.diaryclock.data.Memo;
import com.example.carlh.diaryclock.data.MemoDao;
import com.example.carlh.diaryclock.data.Recording;
import com.example.carlh.diaryclock.data.Tag;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rm.com.audiowave.AudioWaveView;
import rm.com.audiowave.OnProgressListener;

import static com.example.carlh.diaryclock.R.drawable.abc_list_selector_background_transition_holo_dark;
import static com.example.carlh.diaryclock.R.drawable.abc_list_selector_holo_dark;
import static com.example.carlh.diaryclock.R.drawable.clock_01;
import static com.example.carlh.diaryclock.R.drawable.failed;
import static com.example.carlh.diaryclock.R.drawable.micro;
import static com.example.carlh.diaryclock.cloud.tasks.persistentAction.ActionHelper.Type.DELETE_OFFLINE;
import static com.example.carlh.diaryclock.cloud.tasks.persistentAction.ActionHelper.Type.DELETE_ONLINE;
import static com.example.carlh.diaryclock.cloud.tasks.persistentAction.ActionHelper.Type.DOWNLOAD;

/**
 * Created by carlh on 11.09.2017.
 */

public class MemoListFragment extends LifecycleFragment implements SwipeRefreshLayout.OnRefreshListener {

    //Variables
    private String PATH_SD;
    public MemoAdapter adapter;
    private AppDatabase appDatabase;
    private SharedPreferences prefs;
    private MemoDao memoDao;
    //refresh-action on swipe up in list
    private SwipeRefreshLayout swipeLayout;
    //Current selected memo entry in context menue
    private Memo memoInContext;

    //short click on a sub element in an entry: change to checkbox-click
    private View.OnClickListener checkBoxClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Memo memo = (Memo) view.getTag();
            Recording recording = appDatabase.recordingDao().findById(memo.getFileId());

            Toast.makeText(getContext(), "Clicked [checkbox]:", Toast.LENGTH_SHORT).show();
            CheckBox checkBox = (CheckBox) view;

            if (checkBox.isChecked()) {

                Toast.makeText(getContext(), "File will be downloaded", Toast.LENGTH_SHORT).show();
                //Download ->add as persistent action and sync
                AsyncSyncFolderTask.addPersistentAction(getContext(),new Action(DOWNLOAD, recording.getUid()));
                //toggle out-of-sync-flag
                recording.setNotSynced(true);
                //update isCached-Bool in Memo and Recording
                recording.setCachedFile(true);
                appDatabase.recordingDao().update(recording);
                Memo.updateMemos(recording,appDatabase);
                doSync();
            } else {
                Toast.makeText(getContext(), "File will be locally deleted", Toast.LENGTH_SHORT).show();
                //Delete file locally
                String file = PATH_SD + "/" + recording.getFile();
                File f = new File(file);
                if (f.exists()){
                    Log.e(getClass().getName(), "Deleting was successful: "+f.delete(), null);
                }

                //update isCached-Bool in Memo and Recording
                recording.setCachedFile(false);
                appDatabase.recordingDao().update(recording);
                Memo.updateMemos(recording,appDatabase);
            }
        }
    };

    //Short click on an entry which is an area without a specific listener (e.g. checkbox): PLAY Memo
    private View.OnClickListener itemClickListenerShort = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Memo memo = (Memo) view.getTag();
            Recording recording = appDatabase.recordingDao().findById(memo.getFileId());
            Toast.makeText(getContext(), "Clicked [Short]:" + memo.getName()+ "| created: "+recording.getDate().toString(), Toast.LENGTH_SHORT).show();
            playMemo(memo);
        }
    };

    //Short click on an entry which is an area without a specific listener (e.g. checkbox): PLAY Memo
    private View.OnClickListener tagClickListenerShort = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showEditDialog((Memo) view.getTag());
        }
    };

    //helper method for determining the current selection when opening in the context menue
    private View.OnLongClickListener itemClickListenerLong = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            memoInContext = (Memo) view.getTag();
            return false;
        }
    };

    //Context menu for an entry when doing a long-press
    @SuppressLint("SetTextI18n")
    public boolean onContextItemSelected(MenuItem item) {
        //ass. file with memo
        Recording file = appDatabase.recordingDao().findById(memoInContext.getFileId());
        switch (item.getItemId()) {
            case R.id.delete_memo:
                memoDao.delete(memoInContext);
                //if no memo is ass anymore with file, delete the file TODO: but a warning message before deleting it!
                if (appDatabase.recordingDao().getMemos(file.getUid()).size() == 0) {
                    try {
                        Log.e(getClass().getName(), "Writing ser Recording ...", null);
                        FileOutputStream fos = getContext().openFileOutput(String.valueOf("deleted_"+file.getUid()), Context.MODE_PRIVATE);
                        ObjectOutputStream os = new ObjectOutputStream(fos);
                        os.writeObject(file);
                        os.close();
                        fos.close();
                    }catch (Exception e){
                        Log.e(getClass().getName(),e.toString());
                    }

                    appDatabase.recordingDao().delete(file);
                    //delete in Cloud also
                    AsyncSyncFolderTask.addPersistentAction(getContext(),new Action(DELETE_ONLINE, file.getUid()));
                    if(file.getCachedFile()){
                        AsyncSyncFolderTask.addPersistentAction(getContext(),new Action(DELETE_OFFLINE, file.getUid()));
                    }
                }

                return true;
            case R.id.delete_file:
                //serialize Recording to use for eventual persistentActions which refer to that entity
                try {
                    Log.e(getClass().getName(), "Writing ser Recording ...", null);
                    FileOutputStream fos = getContext().openFileOutput(String.valueOf("deleted_"+file.getUid()), Context.MODE_PRIVATE);
                    ObjectOutputStream os = new ObjectOutputStream(fos);
                    os.writeObject(file);
                    os.close();
                    fos.close();
                }catch (Exception e){
                    Log.e(getClass().getName(),e.toString());
                }

                //Delete Recording from database
                appDatabase.recordingDao().delete(file);//all ass. Memos are deleted automatically (foreignKeys)
                //add persistentAction, delete Offline
                AsyncSyncFolderTask.addPersistentAction(getContext(),new Action(DELETE_ONLINE, file.getUid()));
                if(file.getCachedFile()){
                    AsyncSyncFolderTask.addPersistentAction(getContext(),new Action(DELETE_OFFLINE, file.getUid()));
                }

                return true;
            case R.id.clone_memo:
                memoInContext.setUid(0); //is treated as unset by Room
                memoDao.insert(memoInContext);
                return true;
            case R.id.edit_memo:

                //Create a simple dialog to change fields of selected entry
                showEditDialog(memoInContext);



                return true;
            case R.id.download_memo:
                memoInContext.setCached(true);
                memoInContext.setNotSynced(true);
                memoDao.update(memoInContext);
                //cached-flag and download
                AsyncSyncFolderTask.addPersistentAction(getContext(),new Action(DOWNLOAD, file.getUid()));

                //automatically sync
                doSync();
                return true;
            case R.id.play_memo:

                //create dialog for playing
                playMemo(memoInContext);

                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void playMemo(Memo memo) {

        //Variables
        //Picking memo

        String mediaName = appDatabase.recordingDao().findById(memo.getFileId()).getFile();
        String mediaPath = PATH_SD+"/"+mediaName;

        File mediaFile = new File(mediaPath);

        if(!mediaFile.exists()){
            Toast.makeText(getContext(), "File is not cached", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.e(getClass().getName(), "Audio PLAYED: "+mediaPath, null);
        final MediaPlayer mediaPlayer = MediaPlayer.create(getActivity(), Uri.parse(mediaPath));
        double startTime = 0;
        double finalTime = 0;
        final boolean isPlaying= true;
        Handler myHandler = new Handler();
        final Dialog playDialog = new Dialog(getActivity());
        playDialog.setContentView(R.layout.fragment_play_memo);
        playDialog.setCancelable(false);

        final Button pauseButton = (Button) playDialog.findViewById(R.id.button_pause);
        final AudioWaveView seekBar = (AudioWaveView)playDialog.findViewById(R.id.seekbar_player);
        seekBar.setClickable(true);
        final TextView textTime,textDuration,textTitle;
        //Setup Player
        final float duration = (float) mediaPlayer.getDuration();
        seekBar.setRawData(WaveActivity.populatePlayer(mediaFile, getContext()));
        seekBar.setWaveColor(Color.BLUE);
        seekBar.setProgressColor(Color.RED);

        //Listeners::

        //|1|event: user pressed back button to not change the fields
        playDialog.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                // TODO Auto-generated method stub
                if (i == KeyEvent.KEYCODE_BACK) {
                    playDialog.dismiss();
                    mediaPlayer.stop();
                }
                return true;
            }
        });

        //|2|Update Seekbar on UI-Thread
        final Handler mHandler = new Handler();
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if(mediaPlayer != null){
                    int mCurrentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress((mCurrentPosition)/duration*100);
                    //textTime.setText(mCurrentPosition + " sec/"+duration+" sec :::"+(mCurrentPosition)/duration*100);
                }
                mHandler.postDelayed(this, 1000);
            }
        });

        //|3|dragging for audio play
        OnProgressListener onTouchListener = new OnProgressListener() {
            @Override
            public void onStartTracking(float progress) {
            }
            @Override
            public void onStopTracking(float progress) {
                if(mediaPlayer != null){
                    mediaPlayer.seekTo(Math.round(Math.min((progress/100),1)*duration)); //1000(sec-ms))/100(factor)
                }
            }
            @Override
            public void onProgressChanged(float progress, boolean byUser) {}
        };

        seekBar.setOnProgressListener(onTouchListener);

        //|4|after audio was played
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                playDialog.cancel();
            }
        });

        //|5|pause button was pressed
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    pauseButton.setText("Start");
                }else {
                    mediaPlayer.start();
                    pauseButton.setText("Pause");
                }
            }
        });

        //Start audio
        mediaPlayer.start();

        //Show Dialog-Fragment
        playDialog.show();
    }

    private void showEditDialog(final Memo memo) {

        final Dialog editDialog = new Dialog(getActivity(),android.R.style.Theme_Holo_Light_Dialog);
        editDialog.setContentView(R.layout.fragment_edit_memo);
        editDialog.setCancelable(false);

        editDialog.setTitle(memo.getName());
        Button saveButton = (Button) editDialog.findViewById(R.id.save_entry);
        Button rangeButton = (Button) editDialog.findViewById(R.id.edit_range);

        TextView uid_field = (TextView) editDialog.findViewById(R.id.uid_field);
        TextView fileId_field = (TextView) editDialog.findViewById(R.id.fileId_field);
        TextView cached_field = (TextView) editDialog.findViewById(R.id.cached_field);
        TextView notSynced_field = (TextView) editDialog.findViewById(R.id.notSynced_field);
        TextView labelViewShow_field = (TextView) editDialog.findViewById(R.id.labelViewShow);


        final EditText name_field = (EditText) editDialog.findViewById(R.id.name_field);
        final EditText length_field = (EditText) editDialog.findViewById(R.id.length_field);
        final EditText plays_field = (EditText) editDialog.findViewById(R.id.plays_field);
        final EditText start_field = (EditText) editDialog.findViewById(R.id.start_field);
        final EditText stop_field = (EditText) editDialog.findViewById(R.id.stop_field);
        final MultiAutoCompleteTextView label_field = (MultiAutoCompleteTextView) editDialog.findViewById(R.id.labelView);

        //parse and set entry
        //show only
        uid_field.setText(String.valueOf(memo.getUid()));
        fileId_field.setText(memo.getFileId().toString());
        cached_field.setText(memo.getCached().toString());
        notSynced_field.setText(memo.getNotSynced().toString());
        labelViewShow_field.setText(Html.fromHtml(adapter.createTags(Tag.getTagStringsById(memo.getUid(),appDatabase))));


        //setting existing labels
        final List<String> tagStrings = Tag.getTagStringsById(memo.getUid(),appDatabase);

        label_field.setText(TextUtils.join(",",tagStrings));
        //Set tokenizer
        label_field.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer()); //seperation is through comma
        //suggestion threshold in # char
        label_field.setThreshold(2);
        //set adapter for suggestion box
        //Get all set labels
        List<String> labels = appDatabase.tagDao().getTagsAsString();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getContext(),
                android.R.layout.simple_dropdown_item_1line,labels);
        label_field.setAdapter(adapter);

        //editable
        name_field.setText(memo.getName());
        length_field.setText(memo.getLength().toString());
        length_field.setTransformationMethod(null);
        plays_field.setText(memo.getPlays().toString());
        plays_field.setTransformationMethod(null);
        start_field.setText(memo.getStart().toString());
        start_field.setTransformationMethod(null);
        stop_field.setText(memo.getStop().toString());
        stop_field.setTransformationMethod(null);
        editDialog.show();

        //Click listeners

        //event: user pressed back button to not change the fields
        editDialog.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {

                // TODO Auto-generated method stub
                if (i == KeyEvent.KEYCODE_BACK) {
                    editDialog.dismiss();
                }
                return true;
            }
        });

        //Save to database
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //parse text fields and save to database
                //save tags (only update if there is a difference)
                if(!label_field.getText().toString().contentEquals(TextUtils.join(",",tagStrings))){
                    Log.e(getClass().getName(), "Text was changed", null);
                    //wrapper method for saving new tags to TAG, making association with Memo
                    Tag.updateTagMemo(memo.getUid(),label_field.getText().toString(),appDatabase);
                }else {
                    Log.e(getClass().getName(), "Text was not changed", null);
                }
                memo.setName(name_field.getText().toString());
                memo.setLength(Float.parseFloat(length_field.getText().toString()));
                memo.setStart(Float.parseFloat(start_field.getText().toString()));
                memo.setStop(Float.parseFloat(stop_field.getText().toString()));
                memo.setPlays(Integer.parseInt(plays_field.getText().toString()));
                memoDao.update(memo);

                editDialog.cancel();
            }
        });

        //start editRange activity

        rangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), WaveActivity.class);
                intent.putExtra("uIDMemo",memo.getUid());
                startActivity(intent);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //get dependencies through activity
        this.appDatabase = ((MainActivity) getActivity()).getDatabase();
        this.memoDao = appDatabase.memoDao();
        this.PATH_SD = ((DiaryClock) getActivity().getApplicationContext()).getRootPath().getAbsolutePath();   //Get application
        this.prefs = ((DiaryClock) getActivity().getApplicationContext()).getPreferences();
        View v = inflater.inflate(R.layout.fragment_list_memos, container, false);
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.recycler_view_list_memos);
        setupRecyclerView(v);

        swipeLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipeRefreshLayout);
        swipeLayout.setOnRefreshListener(this);

        //if an existing database was downloaded sync immediately  (see mergeDatabase in AsyncSyncFolderTask)
        if(prefs.getBoolean("merge-db", false)){

            if (swipeLayout != null) {
                swipeLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeLayout.setRefreshing(true);
                        onRefresh();
                    }
                });
            }
        }

        //Observe the live data and update adapter if change occured
        memoDao.getAllObservable().observe(this, new Observer<List<Memo>>() {
            @Override
            public void onChanged(@Nullable List<Memo> memos) {
                adapter.setItems(memos);
            }
        });

        return v;
    }

    private void setupRecyclerView(View v) {
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.recycler_view_list_memos);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        //Creating view for context menue
        MenuInflater contextMenuInflater = this.getActivity().getMenuInflater();

        HashMap<Integer, Integer> colorMap = new HashMap<>();
        adapter = new MemoAdapter(new ArrayList<Memo>(), getContext(), itemClickListenerShort, itemClickListenerLong, checkBoxClickListener, tagClickListenerShort,contextMenuInflater,appDatabase,colorMap);
        recyclerView.setAdapter(adapter);

        final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
    }


    @Override
    public void onRefresh() {
        Toast.makeText(getContext(), "refresh called", Toast.LENGTH_SHORT).show();

        Drawable icon = getContext().getResources().getDrawable(R.drawable.sync);
        //Updating GUI (tab color)
        TabLayout tabLayout = ((MainActivity)getActivity()).getTabLayout();
        if (tabLayout != null){
            TabLayout.Tab tab=tabLayout.getTabAt(1);

            TextView newTab = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.tab_indicator_database, null);
            newTab.setText(R.string.tab_description_database);

            newTab.setCompoundDrawablesWithIntrinsicBounds(icon,null,null,null);
            tab.setCustomView(newTab);
        }

        //Doing sync between Cloud and database
        doSync();
    }

    public void doSync(){
        if (!prefs.getBoolean("is-syncing", false)) {

            Log.e(getClass().getName(), "Starting Sync...", null);

            new AsyncSyncFolderTask(getContext(),new AsyncSyncFolderTask.Callback() {
                @Override
                public void onDataLoaded(Boolean result) {

                    //Sync finished
                    swipeLayout.setRefreshing(false);
                    swipeLayout.setBackgroundColor(Color.WHITE);

                    //failed:
                    Integer color = getContext().getResources().getColor(R.color.memo_sync_fail);
                    if (result)
                        color = getContext().getResources().getColor(R.color.memo_sync_successful);

                    Drawable icon = getContext().getResources().getDrawable(R.drawable.failed);
                    if (result) icon = getContext().getResources().getDrawable(R.drawable.success);

                    //Updating GUI (tab color)
                    TabLayout tabLayout = ((MainActivity) getActivity()).getTabLayout();
                    if (tabLayout != null) {
                        TabLayout.Tab tab = tabLayout.getTabAt(1);
                        TextView newTab = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.tab_indicator_database, null);
                        newTab.setText(R.string.tab_description_database);
                        newTab.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                        tab.setCustomView(newTab);
                    }
                    prefs.edit().putBoolean("is-syncing", false).apply();
                }

                @Override
                public void onError(Exception e) {
                    //Cloud-Client was not Found
                    if (e instanceof IllegalStateException) {
                        Toast.makeText(getContext(), "[Error]Client was probably not connected, connect first with Cloud-Service", Toast.LENGTH_SHORT).show();
                    }

                    if (e != null) {
                        Toast.makeText(getContext(), "[Error]Sync was not successful" + e.toString(), Toast.LENGTH_SHORT).show();
                    }

                    //Sync finished
                    swipeLayout.setRefreshing(false);
                    swipeLayout.setBackgroundColor(Color.RED);
                    prefs.edit().putBoolean("is-syncing", false).apply();
                }
            }).execute();
        }else {
            swipeLayout.setRefreshing(false);
            Toast.makeText(getContext(), "ALREADY syncing!...", Toast.LENGTH_SHORT).show();
        }
    }
}

