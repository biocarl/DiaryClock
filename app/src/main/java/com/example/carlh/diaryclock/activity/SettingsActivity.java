package com.example.carlh.diaryclock.activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dropbox.core.android.Auth;
import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.app.DiaryClock;
import com.example.carlh.diaryclock.cloud.Cloud;
import com.example.carlh.diaryclock.cloud.CloudActivity;
import com.example.carlh.diaryclock.ui.memo.main.PreferenceFragment;

import javax.inject.Inject;

/**
 * Created by carlh on 18.09.2017.
 */



public class SettingsActivity extends CloudActivity {

    @Inject
    SharedPreferences prefs;
    DiaryClock application;
    private Context context;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        //Injection
        this.application = ((DiaryClock) context.getApplicationContext());   //Get application
        application.getComponent().inject(this);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PreferenceFragment())
                .commit();
        if(savedInstanceState == null){
            Bundle extras = getIntent().getExtras();
            if(extras !=null){
                if(extras.getBoolean("first_start_extra")){
                    finish();
                }
            }
        }
    }
}
