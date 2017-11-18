package com.example.carlh.diaryclock.cloud;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.dropbox.core.android.Auth;
import com.example.carlh.diaryclock.app.DiaryClock;

/**
 * Created by carlh on 19.09.2017.
 */

public class CloudActivity extends AppCompatActivity {

    public String provider;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.provider = getSharedPreferences("preferences",MODE_PRIVATE).getString("service-name", "notGood");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(getClass().getName(), "Provider on resume:"+provider);
        if (provider.equals("dropbox")) {
            Log.e(getClass().getName(), "Dropbox worked!");
            SharedPreferences prefs = getSharedPreferences("Dropbox-Pref", MODE_PRIVATE);
            String accessToken = prefs.getString("access-token", null);
            if (accessToken == null) {
                accessToken = Auth.getOAuth2Token();
                if (accessToken != null) {
                    //store access-token in preferences
                    prefs.edit().putString("access-token", accessToken).apply();
                    initAndLoadData(accessToken);
                }
            } else {
                initAndLoadData(accessToken);
            }
            //Store User-Id in preferences
            String uid = Auth.getUid();
            String storedUid = prefs.getString("user-id", null);
            if (uid != null && !uid.equals(storedUid)) {
                prefs.edit().putString("user-id", uid).apply();
            }
        } else if (provider.equals("drive")) {
            //to implement
            ;;
            } else {
            //throw error
            Log.e(getClass().getName(), "NOT worked!");
            ;;
            }
    }

    private void initAndLoadData(String accessToken) {
        DropboxClientFactory.init(accessToken);
    }

    protected boolean hasToken() {
        SharedPreferences prefs = getSharedPreferences("Dropbox-Pref", MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
        return accessToken != null;
    }

}