package com.example.carlh.diaryclock.cloud;

import android.content.Context;

import java.io.File;
import java.net.ConnectException;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by carlh on 11.07.2017.
 */

public class NoCloud extends Cloud<Object> {


    public NoCloud(Context context) {
        super(context);
    }

    @Override
    //TODO implement a default hashing method if the user is not using a cloud provider
    public String createHash(File file) {
        return null;
    }

    @Override
    public boolean mkdir() {
        return false;
    }

    @Override
    public boolean init() {
        //There is nothing to initialize
        return false;
    }

    @Override
    public boolean mkdir(String relativePath) {
        return false;
    }

    @Override
    public String fetchCursor(String cursor) {
        return null;
    }

    @Override
    public boolean delete(File filePath) {
        return false;
    }

    @Override
    public boolean deleteSystemFile(File filePath) throws ConnectException {
        return false;
    }

    @Override
    public void download(File path) {
    }

    @Override
    public void downloadSystemFile(File path, String tag) throws ConnectException {

    }

    @Override
    public String upload(File filepath) {
        return null;
    }

    @Override
    public String uploadSystemFile(File filepath) throws ConnectException {
        return null;
    }

    @Override
    public Iterator<Object> listEntries(String cursorString, String pathCloud) {
        return null;
    }

    @Override
    public Type getType(Object metadata) {
        return null;
    }

    @Override
    public Object getMetadata(File file) {
        return null;
    }

    @Override
    public String getPath(Object metadata) {
        return null;
    }

    @Override
    public String getName(Object metadata) {
        return null;
    }

    @Override
    public Date getDate(File file) throws ConnectException {
        return null;
    }


    @Override
    public String getHash(Object metadata) {
        return null;
    }
}
