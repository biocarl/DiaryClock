package com.example.carlh.diaryclock.ui.clock;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.services.AlarmReceiver;

/**
 * Created by carlh on 03.11.2017.
 */

public class AlarmActivity extends Activity
{
    public Context context;
    public String soundPath;
    public Long uIDTime;
    public Long uIDMemo;

    //Dismiss-button
    private View.OnClickListener dismissClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            //create an intend to the Ringtone service
            Intent serviceIntent = new Intent(context, AlarmReceiver.class);
            serviceIntent.putExtra("extra","alarm off");
            serviceIntent.putExtra("soundPath", soundPath);
            serviceIntent.putExtra("uIDTime", uIDTime);
            serviceIntent.putExtra("uIDMemo", uIDMemo);
            context.sendBroadcast(serviceIntent);
            //close overlay
            AlarmActivity.this.finish();
        }
    };

    //Snooze-button
    private View.OnClickListener snoozeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //Stop ringtone
            Intent stopIntent = new Intent(context, AlarmReceiver.class);
            stopIntent.putExtra("extra","alarm off snooze");
            stopIntent.putExtra("soundPath", soundPath);
            stopIntent.putExtra("uIDTime", uIDTime);
            stopIntent.putExtra("uIDMemo", uIDMemo);
            context.sendBroadcast(stopIntent);
            Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
            snoozeIntent.putExtra("extra", "alarm on snooze");
            snoozeIntent.putExtra("soundPath", soundPath);
            snoozeIntent.putExtra("uIDTime", uIDTime);
            snoozeIntent.putExtra("uIDMemo", uIDMemo);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, uIDTime.intValue(), snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            Calendar currentTime = Calendar.getInstance();
            currentTime.setTimeInMillis(currentTime.getTimeInMillis() + (60*10*1000)); //ring 10 min after again
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, currentTime.getTimeInMillis(), pendingIntent);
            //close overlay
            AlarmActivity.this.finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;

        Intent intent = getIntent();
        this.uIDTime = intent.getExtras().getLong("uIDTime");
        this.uIDMemo = intent.getExtras().getLong("uIDMemo");
        this.soundPath = intent.getExtras().getString("soundPath");
        setContentView(R.layout.lockscreen_dismiss_alarm);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        Button dismissButton = (Button) findViewById(R.id.button_dismiss);
        Button snoozeButton = (Button) findViewById(R.id.button_snooze);
        dismissButton.setOnClickListener(dismissClickListener);
        snoozeButton.setOnClickListener(snoozeClickListener);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE;
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }
    }
}