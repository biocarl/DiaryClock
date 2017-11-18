package com.example.carlh.diaryclock.services;

/**
 * Created by carlh on 18.04.2017.
 */

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class AlarmReceiver extends WakefulBroadcastReceiver {


    @Override
    public void onReceive(final Context context, Intent intent) {
        String extraString = intent.getExtras().getString("extra");
        Intent serviceIntent = new Intent(context, RingtonePlayingService.class);
        //pass the extra string from main activity to Ringtone playing service
        serviceIntent.putExtra("extra",extraString);
        serviceIntent.putExtra("uIDTime",intent.getExtras().getLong("uIDTime"));
        Log.e(getClass().getName(), "uIDTime from time,in AlarmReceiver: " + intent.getExtras().getLong("uIDTime"));
        serviceIntent.putExtra("uIDMemo",intent.getExtras().getLong("uIDMemo"));
        serviceIntent.putExtra("soundPath",intent.getExtras().getString("soundPath"));

        //start
        context.startService(serviceIntent);
    }
}
