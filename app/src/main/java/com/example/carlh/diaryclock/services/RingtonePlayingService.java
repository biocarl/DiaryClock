package com.example.carlh.diaryclock.services;

/**
 * Created by carlh on 18.04.2017.
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.audiofx.LoudnessEnhancer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import android.util.Log;
import android.widget.Toast;

import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.activity.MainActivity;
import com.example.carlh.diaryclock.app.DiaryClock;
import com.example.carlh.diaryclock.data.AppDatabase;
import com.example.carlh.diaryclock.data.Memo;
import com.example.carlh.diaryclock.data.Recording;
import com.example.carlh.diaryclock.ui.clock.AlarmActivity;

import java.io.IOException;

import javax.inject.Inject;

public class RingtonePlayingService extends Service {

    MediaPlayer ringtone;
    boolean isRunning;
    String soundPath;
    Long uIDTime;
    Long uIDMemo;
    Memo memo;
    Recording recording;
    private DiaryClock application;

    @Inject
    AppDatabase db;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = this;

        //fetch the 'extra'-values
        String state = intent.getExtras().getString("extra");
        //fetch vars
        this.uIDTime = intent.getExtras().getLong("uIDTime");
        this.uIDMemo = intent.getExtras().getLong("uIDMemo");
        this.soundPath = intent.getExtras().getString("soundPath");

        //Doing injection
        this.application = ((DiaryClock) context.getApplicationContext());   //Get application
        application.getComponent().inject(this);
        this.memo = db.memoDao().findById(uIDMemo);
        this.recording = db.recordingDao().findById(memo.getUid());


        Log.e(getClass().getName(), "[ring][][fetching extras]" + state + soundPath + "uid:" + uIDTime + "startID:" + startId + "THIS WORKED: " + db.memoDao().getAll().size(), null);

        switch (state) {
            case "alarm on":
                startId = 1;
                break;
            case "alarm off":
                startId = 0;
                break;

            case "alarm off snooze":
                startId = 3;
                break;

            case "alarm on snooze":
                startId = 2;
                break;
            default:
                startId = 0;
                break;
        }


        //if else-statements
        if (this.isRunning && startId == 3) {
            ringtone.stop();
            this.isRunning = false;
            Log.e(getClass().getName(), "[ring][][stopping music //snooze run]", null);

        } else if (!this.isRunning && startId == 2) {
            Log.e(getClass().getName(), "[ring][][playing music//snooze run]", null);
            //showing the Activity screen
            Intent intentAlarmActivity = new Intent();
            intentAlarmActivity.putExtra("soundPath", soundPath);
            intentAlarmActivity.putExtra("uIDTime", uIDTime);
            intentAlarmActivity.putExtra("uIDMemo", uIDMemo);

            intentAlarmActivity.setClass(this, AlarmActivity.class);
            intentAlarmActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentAlarmActivity);
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();

            this.ringtone = new MediaPlayer();
            this.ringtone.setAudioAttributes(attributes);

            try {
                this.ringtone.setDataSource(this, Uri.parse(soundPath));
                this.ringtone.prepare();
                this.ringtone.start();

                //start and stop of recording snipped (-> Memo)
                this.ringtone.seekTo(memo.getStart().intValue()); //here is the memo defined
                Handler handler = new Handler();
                handler.postDelayed(stopPlayerTask, memo.getStop().longValue() - memo.getStart().longValue()); //here the memo stops
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.isRunning = true;
            startId = 0;
        }
        //if there is no music playing and the user pressed alarm on //music should start playing
        else if (!this.isRunning && startId == 1) {
            Toast.makeText(this, "[music...]", Toast.LENGTH_LONG).show();
            Log.e(getClass().getName(), "[ring][][playing music//normal run]", null);
            //showing the Activity screen
            Intent intentAlarmActivity = new Intent();
            intentAlarmActivity.putExtra("soundPath", soundPath);
            intentAlarmActivity.putExtra("uIDTime", uIDTime);
            intentAlarmActivity.putExtra("uIDMemo", uIDMemo);
            intentAlarmActivity.setClass(this, AlarmActivity.class);
            intentAlarmActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentAlarmActivity);

            //Playing alarm
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();

            this.ringtone = new MediaPlayer();
            this.ringtone.setAudioAttributes(attributes);

            try {
                //this.ringtone.setDataSource(this,Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.piano));
                this.ringtone.setDataSource(this, Uri.parse(soundPath));
                this.ringtone.prepare();
                this.ringtone.start();
                this.ringtone.seekTo(memo.getStart().intValue()); //here is the memo defined
                if (memo.getStop() == 0) {
                    memo.setStop((float) ringtone.getDuration());
                    db.memoDao().update(memo);

                } else {
                    Handler handler = new Handler();
                    Log.e(getClass().getName(), "Stoppin GIVEN _ : " + memo.getStop() + "|as long|" + memo.getStop().longValue(), null);
                    handler.postDelayed(stopPlayerTask, memo.getStop().longValue() - memo.getStart().longValue()); //here the memo stops
                }

                this.isRunning = true;

            } catch (IOException e) {
                e.printStackTrace();
            }

            startId = 0;

            // notification
            //set up the notification surface
            NotificationManager notificationManager = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);

            Intent intentMainActivity = new Intent(this.getApplicationContext(), MainActivity.class);
            PendingIntent pendingIntentMainActivity = PendingIntent.getActivity(this, 0, intentMainActivity, 0);
            Intent intentChancelAlarm = new Intent(this.getApplicationContext(), ChancelAlarmReceiver.class);
            PendingIntent pendingIntentChancelAlarm = PendingIntent.getActivity(this, 1, intentChancelAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

            //Notification-Parameters
            Notification notification = new Notification.Builder(this)
                    .setContentTitle("Wake up!")
                    .setContentText("Click me to stop the alarm")
                    .setContentIntent(pendingIntentMainActivity)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel Upload", pendingIntentChancelAlarm)
                    .setSmallIcon(R.drawable.clock_01)
                    .setAutoCancel(true)
                    .build();

            //set up notifcation call command
            notificationManager.notify(0, notification);

        }//if there is music playing and the user pressed alarm off //music should stop
        else if (this.isRunning && startId == 0) {
            Log.e(getClass().getName(), "[ring][][stopping music //normal run]", null);
            ringtone.stop();
            this.isRunning = false;
            startId = 0;
            Intent intentMainActivity = new Intent();
            intentMainActivity.putExtra("uIDTime", uIDTime);
            intentMainActivity.putExtra("uIDMemo", uIDMemo);
            intentMainActivity.putExtra("update-from-service", true);
            intentMainActivity.setClass(this, MainActivity.class);
            intentMainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentMainActivity);
        }

        //if there is no music playing and the user pressed 'alarm off'
        else if (!this.isRunning && startId == 0) {
            Log.e(getClass().getName(), "[ring][][stopping music //makes no sense]", null);
            //do nothing
            Toast.makeText(this, "[Makes no sense]", Toast.LENGTH_LONG).show();
            this.isRunning = false;
        }
        //if there is music playing and the user pressed 'alarm on'
        else if (this.isRunning && startId == 1) {
            //do nothing
            this.isRunning = true;
            Log.e(getClass().getName(), "[ring][][playing music //makes no sense]", null);
            Toast.makeText(this, "[There is already a alarm playing, wait!]", Toast.LENGTH_LONG).show();
        }
        //catch odd event
        else {
            Log.e("else", "Somehow you reached that");
        }

        return START_NOT_STICKY;
    }

    Runnable stopPlayerTask = new Runnable() {
        @Override
        public void run() {
            ringtone.stop();
        }
    };

    @Override
    public void onDestroy() {
        isRunning = false;
    }
}

