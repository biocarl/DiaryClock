package com.example.carlh.diaryclock.activity;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.appyvet.materialrangebar.IRangeBarFormatter;
import com.appyvet.materialrangebar.RangeBar;
import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.app.DiaryClock;
import com.example.carlh.diaryclock.data.AppDatabase;
import com.example.carlh.diaryclock.data.Memo;
import com.example.carlh.diaryclock.data.Recording;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import rm.com.audiowave.AudioWaveView;
import rm.com.audiowave.OnProgressListener;

import static com.example.carlh.diaryclock.R.*;

/**
 * Created by carlh on 06.11.2017.
 */

public class WaveActivity extends AppCompatActivity {

    @Inject
    AppDatabase db;
    private Context context;
    private DiaryClock application;
    private Memo memo;
    private Recording recording;
    private MediaPlayer mediaPlayer;
    private Button stopButton;
    private Button loopButton;
    private Button playButton;
    private Button saveButton;
    private AudioWaveView seekBar;
    private RangeBar rangeBar;
    private Float duration;

    //Main button
    private View.OnClickListener stopClickListenerShort = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mediaPlayer.stop();
            finish();
        }
    };

    //Main button
    private View.OnClickListener playClickListenerShort = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(mediaPlayer.isPlaying()){
                mediaPlayer.pause();
                playButton.setText("Play");
            }else {
                mediaPlayer.start();
                playButton.setText("Pause");
            }
        }
    };

    //Main button
    private View.OnClickListener saveClickListenerShort = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Float start = (float) Math.min((float) rangeBar.getLeftIndex()/rangeBar.getTickCount(),1)* duration;
            Float stop = (float) Math.min((float) rangeBar.getRightIndex()/rangeBar.getTickCount(),1)*duration;
            memo.setStart(start);
            memo.setStop(stop);
            db.memoDao().update(memo);
            mediaPlayer.stop();
            finish();
        }
    };

    //Loop button
    private View.OnClickListener loopClickListenerShort = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final Float start = (float) Math.min((float)rangeBar.getLeftIndex()/rangeBar.getTickCount(),1)* duration;
            mediaPlayer.seekTo(start.intValue());
            Float stop = (float) Math.min(rangeBar.getRightIndex()/rangeBar.getTickCount(),1)*duration;
            Float difference = stop-start;
            new CountDownTimer(difference.longValue(), 2*difference.longValue()) {

                public void onTick(long millisUntilFinished) {
                    mediaPlayer.pause();
                    mediaPlayer.seekTo(start.intValue());
                }
                public void onFinish() {

                }
            }.start();


        }
    };

    private OnProgressListener onTouchListener = new OnProgressListener() {
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

    private RangeBar.OnRangeBarChangeListener onRangeBarListener =  new RangeBar.OnRangeBarChangeListener() {
        @Override
        public void onRangeChangeListener
                (RangeBar rangeBar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue) {
            Float lower = (float) leftPinIndex/rangeBar.getTickCount()*100;
            Float upper = (float) rightPinIndex/rangeBar.getTickCount()*100;
            seekBar.clearRanges();
            seekBar.setRange(0,lower,Color.GREEN);//Color.argb(20,96, 255, 38)
            seekBar.setRange(upper,100,Color.GREEN);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        //Injection
        this.application = ((DiaryClock) context.getApplicationContext());   //Get application
        application.getComponent().inject(this);
        //Get intent (Recording-Id)
        Bundle bundle = getIntent().getExtras();
        Long uIDMemo = -1L;
        if(bundle != null) {
            uIDMemo = bundle.getLong("uIDMemo", -1L);
        }
        //Getting vars
        this.memo = db.memoDao().findById(uIDMemo);
        if(memo != null)this.recording = db.recordingDao().findById(memo.getFileId());

        //Make fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.wave_slider);
        inflateViews();
        preparePlayer();
        updateGUI();
        //Set listeners
        setListeners();
    }


    private void setListeners() {
        //Update seekbar on UI-Thread
        final Handler mHandler = new Handler();
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mediaPlayer != null){
                    int mCurrentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(mCurrentPosition/duration*100); //factor = progress/100
                    //textTime.setText(mCurrentPosition + " sec/"+duration+" sec");
                }
                mHandler.postDelayed(this, 100);
            }
        });
        seekBar.setOnProgressListener(onTouchListener);
        stopButton.setOnClickListener(stopClickListenerShort);
        playButton.setOnClickListener(playClickListenerShort);
        rangeBar.setOnRangeBarChangeListener(onRangeBarListener);
        saveButton.setOnClickListener(saveClickListenerShort);
        loopButton.setOnClickListener(loopClickListenerShort);
    }
    public void inflateViews(){
        seekBar = findViewById(id.wave_slider_view);
        stopButton = findViewById(id.player_stop);
        loopButton = findViewById(id.player_loop);
        playButton = findViewById(id.player_start);
        saveButton = findViewById(id.player_save);
        rangeBar = findViewById(id.range_bar);
    }

    public void updateGUI(){
        //Set up range bar for selecting the range
        rangeBar.setTickEnd(duration/100); //one Tick equals 10ms
        rangeBar.setTickStart(0);
        rangeBar.setFormatter(new IRangeBarFormatter() {
            @Override
            public String format(String s) {
                Float aFloat  = Float.parseFloat(s);
                s = aFloat/10+"s";
                return s;
            }
        });

        //Setting Pins
        if(memo.getStart() == 0 && (memo.getStop() == 0 || Math.abs(memo.getStop()-duration)<0.00000001))   {//TODO thats how longs work!?
            //default: do the range in the middle third range
            Integer third = Math.round(rangeBar.getTickCount() / 3);
            rangeBar.setRangePinsByIndices(third, third * 2);
        }else {
            //existing saved range
            rangeBar.setRangePinsByValue(Math.min((float)memo.getStart()/duration,1)*rangeBar.getTickEnd(),Math.min((float)memo.getStop()/duration,1)*rangeBar.getTickEnd());
        }

        Float lower = rangeBar.getLeftIndex()/rangeBar.getTickEnd()*100;
        Float upper = rangeBar.getRightIndex()/rangeBar.getTickEnd()*100;
        seekBar.clearRanges();
        seekBar.setExpansionAnimated(true);
        seekBar.setChunkRadius(12);
        seekBar.setChunkWidth(20);
        seekBar.setMinChunkHeight(20);
        seekBar.setProgressColor(Color.YELLOW);
        seekBar.setRange(0,lower,Color.GREEN);//Color.argb(20,96, 255, 38)
        seekBar.setRange(upper,100,Color.GREEN);

    }

    public void preparePlayer(){

        //Check availability
        if(recording == null) {
            Toast.makeText(context, "File is not cached", Toast.LENGTH_SHORT).show();
            return;
        }

        String PATH_SD = ((DiaryClock) context.getApplicationContext()).getRootPath().getAbsolutePath();
        String mediaName = recording.getFile();
        String mediaPath = PATH_SD+"/"+mediaName;
        File mediaFile = new File(mediaPath);

        if(!mediaFile.exists()){
            Toast.makeText(context, "File is not cached", Toast.LENGTH_SHORT).show();
            return;
        }


        //Setting Mediaplayer
        Uri uri = Uri.fromFile(mediaFile);
        this.mediaPlayer = MediaPlayer.create(this, uri);
        this.duration = (float)mediaPlayer.getDuration();

        //Updating Memo if never played
        if(memo.getLength() == 0) {
            memo.setLength(duration);
            memo.setStop(duration);
            Recording file =  db.memoDao().getFile(memo.getFileId());
            file.setLength(duration);
            db.recordingDao().update(file);
            db.memoDao().update(memo);
        }

        //Start
        mediaPlayer.start();
        Log.e(getClass().getName(), "uri"+"Audio PLAYED: "+mediaPath, null);
        //populate the view with waveform
        seekBar.setRawData(populatePlayer(mediaFile,this));
    }


    public static byte[] populatePlayer (File file,Context context){
        Uri uri = Uri.fromFile(file);
        byte[] buffer = null;
        try {
            InputStream  fIn = context.getContentResolver().openInputStream(uri);
            int size = 0;
            size = fIn.available();
            buffer = new byte[size];
            fIn.read(buffer);
            fIn.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException s) {
            s.printStackTrace();
        }
        return buffer;
    }
}
