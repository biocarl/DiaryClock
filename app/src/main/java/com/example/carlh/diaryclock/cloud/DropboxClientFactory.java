package com.example.carlh.diaryclock.cloud;


import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;

import java.util.Locale;

/**
 * Singleton instance of {@link DbxClientV2} and friends
 */
public class DropboxClientFactory {

    private static DbxClientV2 sDbxClient;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void init(String accessToken) {

        if (sDbxClient == null) {

            Log.e("DropboxClientFactory", "Starting");

            //THIS IS A WORKAROUND
            Locale locale = Locale.GERMANY;
            DbxRequestConfig config = new DbxRequestConfig("AudioBase", locale.toLanguageTag());

            DbxClientV2 client = new DbxClientV2(config, accessToken);
            sDbxClient = client;
        }
    }

    public static DbxClientV2 getClient() {
        if (sDbxClient == null) {
            throw new IllegalStateException("Client not initialized.");
        }
        return sDbxClient;
    }
}
