package com.example.carlh.diaryclock.cloud.tasks;


import android.os.AsyncTask;

import com.example.carlh.diaryclock.UnexpectedException;
import com.example.carlh.diaryclock.cloud.Cloud;

import java.io.File;
import java.net.ConnectException;

/**
 * Async task to list items in a folder
 */
public class DeleteSystemFileTask extends AsyncTask<String, Void, Void> {
    private Cloud cloud;
    private String tag;
    private final Callback mCallback;
    private Exception mException;
    private File db_file;


    public interface Callback {
        void onDataLoaded(String result);
        void onError(Exception e);
    }

    public DeleteSystemFileTask(Cloud cloud, File db_file, Callback callback) {
        this.cloud = cloud;
        this.tag = tag;
        this.db_file = db_file;
        mCallback = callback;
    }

    @Override
    protected Void doInBackground(String... params) {

        try {
            cloud.deleteSystemFile(db_file);
        } catch (ConnectException e) {
            e.printStackTrace();
        } catch (UnexpectedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
