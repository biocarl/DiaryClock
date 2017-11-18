package com.example.carlh.diaryclock.activity;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;


import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.app.DiaryClock;
import com.example.carlh.diaryclock.cloud.Cloud;
import com.example.carlh.diaryclock.cloud.CloudActivity;
import com.example.carlh.diaryclock.cloud.tasks.AsyncSyncFolderTask;
import com.example.carlh.diaryclock.cloud.tasks.persistentAction.Action;
import com.example.carlh.diaryclock.data.AppDatabase;


import com.example.carlh.diaryclock.data.Memo;
import com.example.carlh.diaryclock.data.Recording;
import com.example.carlh.diaryclock.data.Tag;
import com.example.carlh.diaryclock.data.Time;
import com.example.carlh.diaryclock.ui.clock.edit.ClockFragment;
import com.example.carlh.diaryclock.ui.clock.list.TimeListFragment;
import com.example.carlh.diaryclock.ui.memo.list.MemoListFragment;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import omrecorder.AudioChunk;
import omrecorder.AudioRecordConfig;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.PullableSource;
import omrecorder.Recorder;
import rm.com.audiowave.AudioWaveView;

import static android.preference.PreferenceManager.setDefaultValues;
import static com.example.carlh.diaryclock.R.drawable.clock_01;
import static com.example.carlh.diaryclock.R.drawable.ic_add_alarm;
import static com.example.carlh.diaryclock.R.drawable.micro;
import static com.example.carlh.diaryclock.R.drawable.play;
import static com.example.carlh.diaryclock.R.drawable.stop;
import static com.example.carlh.diaryclock.cloud.tasks.persistentAction.ActionHelper.Type.DOWNLOAD;
import static com.example.carlh.diaryclock.cloud.tasks.persistentAction.ActionHelper.Type.UPLOAD;
import static com.example.carlh.diaryclock.ui.clock.edit.ClockFragment.ClockIntent.DEFAULT;
import static com.example.carlh.diaryclock.ui.clock.edit.ClockFragment.ClockIntent.LAST_PLAYED;
import static com.example.carlh.diaryclock.ui.clock.edit.ClockFragment.ClockIntent.WELCOME;


public class MainActivity extends CloudActivity {
    //Vars
    @Inject
    SharedPreferences prefs;
    @Inject
    AppDatabase db;
    @Inject
    Cloud cloud;

    public SectionsPagerAdapter mSectionsPagerAdapter;
    public ViewPager mViewPager;
    private boolean online = false;
    private Context context;
    private MainActivity activity;
    private DiaryClock application;
    private Toolbar toolbar;
    private TextClock clock;
    private TabLayout tabLayout;
    private FloatingActionButton actionButton;

    //Recording related
    private Recording recording;
    private boolean permissionToRecordAccepted = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private boolean isRecording;
    private Button saveButton;
    private Button reDoButton;
    private Button playButton;
    private ProgressBar progressBar;
    private Chronometer chronometer;
    private AudioWaveView waveView;
    private boolean isPlaying;
    private MediaPlayer mediaPlayer;
    final Handler mHandler = new Handler();


    //Listeners
    private View.OnClickListener addTimeAction = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            Time time1 = new Time("12.00", "Aufstehen :-)");
            db.timeDao().insert(time1);
            Toast.makeText(context, "Time added", Toast.LENGTH_SHORT).show();
        }
    };

    private View.OnClickListener addRecordingAction = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            if (permissionToRecordAccepted) {
                recordAudio(createFileName(getString(R.string.memo_tag)));
            } else {
                //Ask permission
                ActivityCompat.requestPermissions(activity, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
            }
            Toast.makeText(context, "Recording added", Toast.LENGTH_SHORT).show();
        }
    };

    private TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            int position = tab.getPosition();
            //Create Floating button (add time or record function)
            if (position == 0) {
                actionButton.setImageResource(ic_add_alarm);
                actionButton.setOnClickListener(addTimeAction);
            }
            if (position == 1) {
                actionButton.setImageResource(micro);
                actionButton.setOnClickListener(addRecordingAction);
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {}

        @Override
        public void onTabReselected(TabLayout.Tab tab) {}
    };

    private void inflateViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        mViewPager = (ViewPager) findViewById(R.id.container);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        actionButton = (FloatingActionButton) findViewById(R.id.fab_button);
        clock = (TextClock) findViewById(R.id.textClock);

        //start clock fragment for showing last played ID, check if available
        if (prefs.getLong("last_played", -1) != -1) {
            ClockFragment lastPlayed = ClockFragment.newInstance(-1, -1, LAST_PLAYED); //parameters not needed
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.info_frame, lastPlayed).commit();


        }
    }

    private void registerViewListeners() {
        //Add listeners
        tabLayout.addOnTabSelectedListener(onTabSelectedListener);
        //Default listener and image
        actionButton.setOnClickListener(addTimeAction);
        actionButton.setImageResource(ic_add_alarm);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        activity = this;

        if (savedInstanceState == null) { //substitute db file when db file existent
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                if (extras.getBoolean("import-database", false)) {
                    File db_old = new File(extras.getString("old_db_path"));
                    Log.e(getClass().getName(), "[INTENT: IMPORT DATABASE:]" + db_old.toString(), null);
                    File db_old_backup = new File(db_old.getAbsolutePath() + "_backup");
                    db_old.renameTo(db_old_backup);
                    //rename new file
                    db_old = new File(extras.getString("old_db_path"));
                    File db_new = new File(extras.getString("new_db_path"));
                    db_new.renameTo(db_old);
                    Log.e(getClass().getName(), "[INTENT: IMPORT DATABASE:]" + db_new.toString(), null);
                    //do dependency injection again to update file lock on the db (otherwise SQL crashes)
                    ((DiaryClock) context.getApplicationContext()).refreshInjection();
                }
            }
        }

        //Injection
        this.application = ((DiaryClock) context.getApplicationContext());   //Get application
        application.getComponent().inject(this);
        prefs.edit().putBoolean("is-syncing", false).apply(); //Restoring settings (in case the application is killed during sync)

        //Update seekbar on UI-Thread
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && waveView != null) {
                    waveView.setProgress((float) mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration() * 100); //factor = progress/100
                }
                mHandler.postDelayed(this, 100);
            }
        });

        //When the app is first started
        if (prefs.getBoolean("first_start", true)) {
            Log.e(getClass().getName(), "First start detected");

            ClockFragment welcome = ClockFragment.newInstance(-1, -1, WELCOME); //parameters not needed
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.info_frame, welcome).commit();

            setDefaultValues(context, "preferences", MODE_PRIVATE, R.xml.preference_main, false);

            Intent modifySettings = new Intent(MainActivity.this, SettingsActivity.class);
            modifySettings.putExtra("first_start_extra", true);
            startActivity(modifySettings);

            //Example tags
            Tag tag1 = new Tag("cool");
            Tag tag2 = new Tag("nice");
            Tag tag3 = new Tag("sad");
            Tag tag4 = new Tag("funny");
            db.tagDao().insertAll(tag1, tag2, tag3, tag4);
            //Example time to wake up
            Time time1 = new Time("12.00", "Wake - UP!");
            db.timeDao().insert(time1);
            prefs.edit().putBoolean("first_start", false).apply();
        }

        //when restart to connect to cloud
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                if (extras.getBoolean("update-from-service", false)) {
                    moveTaskToBack(true); //don't show to user

                    //Updating last played memo in preferences AND in database
                    Long uID_lastplayed_new = extras.getLong("uIDMemo", -1);
                    Log.e(getClass().getName(), "uID_lastplayed_new from Intent: " + uID_lastplayed_new);
                    if (uID_lastplayed_new != -1) {
                        Long uID_lastplayed_old = prefs.getLong("last_played", -1);
                        Log.e(getClass().getName(), "old uIDTime from sharedProperties:" + uID_lastplayed_old);
                        Memo lastPlayed = db.memoDao().findById(uID_lastplayed_old);
                        if (lastPlayed != null) {
                            lastPlayed.setLastPlayed(false);
                            db.memoDao().update(lastPlayed);
                        }
                        lastPlayed = db.memoDao().findById(uID_lastplayed_new);
                        if (lastPlayed != null) {
                            Log.e(getClass().getName(), "id of new database entry:" + lastPlayed.getUid());
                            lastPlayed.setLastPlayed(true);
                            lastPlayed.setPlays(lastPlayed.getPlays() + 1);
                            db.memoDao().update(lastPlayed);
                            //updating preferences
                            prefs.edit().putLong("last_played", uID_lastplayed_new).apply();
                        }
                    }

                    //Updating time
                    Long uidTime = extras.getLong("uIDTime", -1);
                    Time time = db.timeDao().findById(uidTime);
                    if (!time.getRepeat()) {
                        time.setActive(false);
                        db.timeDao().update(time);
                        Log.e(getClass().getName(), "Updating successful" + db.timeDao().findById(uidTime).getActive());
                    }

                    //checking
                    /*
                    int counter = 0;
                    for (Memo memo : db.memoDao().getAll()) {
                        if (memo.getLastPlayed())
                            counter++;
                    }
                    assert counter == 1;
                    */

                    //start ClockFragment (if repeated alarm it will reschedule the alarm)
                    ClockFragment fragment = ClockFragment.newInstance(time.getUid(), uID_lastplayed_new, DEFAULT);
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.add(R.id.info_frame, fragment).commit();
                }
            }
        }

        //inflate views
        //Hack to position clock
        final View view = getLayoutInflater().inflate(R.layout.activity_main, null);
        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Integer widthClock = clock.getWidth();

                View parentView = (View) clock.getParent();
                clock.setPadding((parentView.getWidth() / 2) - widthClock / 2 + widthClock / 4 + widthClock / 16, 0, 0, 0);
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        setContentView(view);
        inflateViews();
        createTabLayout();
        registerViewListeners();


        /*
        //Testing

        String filename = "DR_0000";
        File file = new File(SD_PATH + "/" + filename);
        String hash = "sadhsfjkdaksajdsh";
        recording = new Recording(filename);
        recording.setHash(hash);
        Long fileId = db.recordingDao().insert(recording);
        recording.setUid(fileId);

        String filename= "DR_00001";
        File file = new File(SD_PATH+"/"+filename);
        String hash = "sadhsfjkdaksajdsh";

        Recording recording = new Recording();
        recording.setFile(filename);
        recording.setHash(hash);
        Long fileId = db.recordingDao().insert(recording);
        recording.setUid(fileId);

        //Setting up database
        //Setting up memo1
        Memo memo1 = new Memo(recording);
        memo1.setName("My first Memo");
        //Setting up memo2
        Memo memo2 = new Memo(recording);
        memo2.setName("My second Memo");
        //Setting up memo3
        Recording file2 = new Recording();
        fileId = db.recordingDao().insert(file2);
        file2.setUid(fileId);
        Memo memo3 = new Memo(file2);
        memo3.setName("Memo3");
        memo3.setCached(false);
        //Inserting
        db.memoDao().insertAll(memo1,memo2,memo3);
        */

    }

    //Fragment-Pages
    private void createTabLayout() {
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        tabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Do injection again after changing settings
        application.getComponent().inject(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Toast.makeText(this, "Option: ~Setting~ clicked - ", Toast.LENGTH_SHORT).show();
            Intent modifySettings = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(modifySettings);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Helpers
    public boolean isOnline() {
        return online;
    }
    public void setOnline(boolean online) {
        this.online = online;
    }
    public AppDatabase getDatabase() {
        return db;
    }

    public String createFileName(String tag) {
        String filename = getString(R.string.recording_tag);
        filename += "_" + tag + "_";
        Date date = Calendar.getInstance().getTime();
        DateFormat formatter = new SimpleDateFormat("MM-dd-HH-mm-ss");
        String today = formatter.format(date);
        filename += today;
        return filename;
    }

    public void recordAudio(String fileName) {
        String PATH_SD = ((DiaryClock) context.getApplicationContext()).getRootPath().getAbsolutePath();
        File file = new File(PATH_SD + "/" + fileName + ".wav");

        final Recorder recorder = OmRecorder.wav(
                new PullTransport.Default(mic(), new PullTransport.OnAudioChunkPulledListener() {
                    @Override
                    public void onAudioChunkPulled(AudioChunk audioChunk) {
                        animateVoice((float) (audioChunk.maxAmplitude() / 200.0));
                    }
                }), file);
        recorder.startRecording();

        //Show recording interface
        showRecordingDialog(recorder, file);
    }

    private void showRecordingDialog(final Recorder recorder, final File file) {
        //Variables
        isRecording = true;
        final Dialog editDialog = new Dialog(this, android.R.style.Theme_Holo_Light_Dialog);
        editDialog.setContentView(R.layout.recording_dialog);
        editDialog.setCancelable(true);
        editDialog.setTitle("Recording");
        //inflate
        saveButton = (Button) editDialog.findViewById(R.id.store_button);
        reDoButton = (Button) editDialog.findViewById(R.id.redo_button);
        playButton = (Button) editDialog.findViewById(R.id.play_button);
        progressBar = (ProgressBar) editDialog.findViewById(R.id.record_progress);
        chronometer = (Chronometer) editDialog.findViewById(R.id.record_time);
        waveView = (AudioWaveView) editDialog.findViewById(R.id.wave_play_view);

        chronometer.start();
        //listeners
        View.OnClickListener saveButtonOnClick = new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (isRecording) {
                    saveButton.setText("Save?");
                    reDoButton.setVisibility(View.VISIBLE);
                    chronometer.stop();
                    playButton.setVisibility(View.VISIBLE);
                    try {
                        recorder.stopRecording();
                        isRecording = false;
                        waveView.setVisibility(View.VISIBLE);
                        waveView.setRawData(WaveActivity.populatePlayer(file, context));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else { //save modus

                    //Create file entry
                    Recording recording = new Recording(file.getName());
                    recording.setCachedFile(true);
                    if (prefs.getBoolean("cloud-sync", false))
                        recording.setNotSynced(true); //No update of memos needed because it is directly passed in the constructor
                    long fileId = db.recordingDao().insert(recording);
                    recording.setUid(fileId);
                    //Create default memo entry
                    Memo memo = new Memo(recording);
                    memo.setName(getString(R.string.MemoTag) + file.getName());
                    db.memoDao().insert(memo);
                    //Upload file if sync available
                    if (prefs.getBoolean("cloud-sync", false))
                        AsyncSyncFolderTask.addPersistentAction(context, new Action(UPLOAD, fileId));
                    //Close dialog
                    editDialog.dismiss(); //optional show clock fragment
                }
            }

        };

        //listeners
        View.OnClickListener deDoButtonOnClick = new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (!isRecording) {
                    //back_to_Default
                    waveView.setVisibility(View.GONE);
                    reDoButton.setVisibility(View.GONE);
                    editDialog.dismiss();
                    recordAudio(createFileName(getString(R.string.memo_tag)));
                }
            }

        };

        //listeners
        View.OnClickListener playButtonOnClick = new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (!isRecording && !isPlaying) {
                    //Setting Mediaplayer
                    Uri uri = Uri.fromFile(file);
                    mediaPlayer = MediaPlayer.create(activity, uri);
                    mediaPlayer.start();
                    isPlaying = true;
                    playButton.setBackground(getDrawable(stop));
                } else if (!isRecording && isPlaying) {
                    mediaPlayer.pause();
                    isPlaying = false;
                    playButton.setBackground(getDrawable(play));
                }
            }
        };

        DialogInterface.OnKeyListener backKeyClick = new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if (i == KeyEvent.KEYCODE_BACK) {
                    if (isRecording) {
                        try {
                            recorder.stopRecording();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    editDialog.dismiss();
                }
                return true;
            }
        };

        //select listeners
        editDialog.setOnKeyListener(backKeyClick);
        saveButton.setOnClickListener(saveButtonOnClick);
        reDoButton.setOnClickListener(deDoButtonOnClick);
        playButton.setOnClickListener(playButtonOnClick);
        waveView.setOnProgressListener(null);
        editDialog.show();
    }

    //Permission Boilerplate
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();
    }

    //Recording lib boilerplate:
    private PullableSource mic() {
        return new PullableSource.Default(
                new AudioRecordConfig.Default(
                        MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT,
                        AudioFormat.CHANNEL_IN_MONO, 44100
                )
        );
    }

    private void animateVoice(final float maxPeak) {
        //recordButton.animate().scaleX(1 + maxPeak).scaleY(1 + maxPeak).setDuration(10).start();
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        @Override
        //Specifies the fragment by position, beginning with 0!
        public Fragment getItem(int position) {

            //current -2- fragments:
            switch (position) {
                case 0:
                    return new TimeListFragment();
                case 1:
                    return new MemoListFragment();
                default:
                    return null;
            }
        }

        @Override
        //Number of pages
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Clock";
                case 1:
                    return "Database";
            }
            return null;
        }
    }

    public TabLayout getTabLayout() {
        return tabLayout;
    }
}