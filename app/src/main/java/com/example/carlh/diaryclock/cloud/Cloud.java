package com.example.carlh.diaryclock.cloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;

import com.dropbox.core.v2.files.Metadata;
import com.example.carlh.diaryclock.UnexpectedException;
import com.example.carlh.diaryclock.app.DiaryClock;

import java.io.File;
import java.net.ConnectException;
import java.util.Date;
import java.util.Iterator;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by carlh on 25.05.2017.
 *
 * T : Class-Type of Metadata-Class which is used in the Cloud-Api
 */

public abstract class Cloud<T> {

    protected SharedPreferences preferences;
    protected final String PATH_CLOUD;
    protected final String PATH_STORAGE;

    public Cloud(Context context){
        DiaryClock application = ((DiaryClock) context.getApplicationContext());
        this.preferences = application.getPreferences();
        this.PATH_CLOUD = application.getRootPath().getRelativePath();
        this.PATH_STORAGE = application.getRootPath().getAbsolutePath();
    }

    public enum Type{
        FILE,
        FOLDER,
        DELETED
    }

    //ABSTRACTS

    /*Creates the Hash which is used by the Cloud-API for identifying files*/
    public abstract String createHash(File file);
    /*create folder of root path*/
    public abstract boolean mkdir();

    /*Creates an instance of a client e.g. through a factory*/
    public abstract boolean init();

    /*create folder of root path*/
    public abstract boolean mkdir(String relativePath);

    /*Get Cursor from Cloud*/
    public abstract String fetchCursor(String cursor);
    /*Delete a file*/
    public abstract boolean delete(File filePath) throws ConnectException,UnexpectedException, UnexpectedException;
    public abstract boolean deleteSystemFile(File filePath) throws ConnectException,UnexpectedException;

    /*Download a file*/
    public abstract void download(File path) throws ConnectException,UnexpectedException;
    public abstract void downloadSystemFile(File path,String tag) throws ConnectException,UnexpectedException;
    /*Uploads a file and returns the generated HASH*/
    public abstract String upload(File filepath) throws ConnectException,UnexpectedException;
    public abstract String uploadSystemFile(File filepath) throws ConnectException,UnexpectedException;
    /*List Elements of List*/
    public abstract Iterator<T> listEntries(String cursorString, String pathCloud) throws ConnectException,UnexpectedException;
    /*Get the type of the metadata*/
    public abstract Type getType(T metadata);
    public abstract T getMetadata(File file) throws ConnectException,UnexpectedException;
    public abstract String getPath(T metadata);
    public abstract String getName(T metadata);

    public abstract Date getDate(File file) throws ConnectException,UnexpectedException;

    /* Watch out-only for files, in implementation you should check for Meta-Type and return null if no file */
    public abstract String getHash(T metadata);

    //FINAL METHODS

    /*Updates Settings while comparing cursors*/
    public final void updateCursor() {
        String cursorString = preferences.getString("cursor-db", null);
        String newCursor = null;
        //New Cursor is fetched by custom implementation
        newCursor = fetchCursor(cursorString);
        //storing new cursor in properties
        preferences.edit().putString("cursor-db", newCursor).apply();
    }

}
