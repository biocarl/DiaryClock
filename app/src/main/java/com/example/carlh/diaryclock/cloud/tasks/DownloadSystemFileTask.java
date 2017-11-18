package com.example.carlh.diaryclock.cloud.tasks;


import android.os.AsyncTask;

import com.example.carlh.diaryclock.UnexpectedException;
import com.example.carlh.diaryclock.cloud.Cloud;

import java.io.File;
import java.net.ConnectException;

/**
 * Async task to download system file
 */
public class DownloadSystemFileTask extends AsyncTask<String, Void, String> {
    private Cloud cloud;
    private String tag;
    private final Callback mCallback;
    private Exception mException;
    private File db_file;


    public interface Callback {
        void onDataLoaded(String result);
        void onError(Exception e);
    }

    public DownloadSystemFileTask(Cloud cloud, File db_file, String tag, Callback callback) {
        this.cloud = cloud;
        this.tag = tag;
        this.db_file = db_file;
        mCallback = callback;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onDataLoaded(result);
        }
    }

    @Override
    protected String doInBackground(String... params) {


        String rev = null;
        try {
            cloud.downloadSystemFile(db_file, tag);
        } catch (ConnectException e) {
            e.printStackTrace();
        } catch (UnexpectedException e) {
            e.printStackTrace();
        }

        return rev;
    }
}
