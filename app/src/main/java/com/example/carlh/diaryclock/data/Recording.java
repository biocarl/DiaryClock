package com.example.carlh.diaryclock.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by carlh on 21.07.2017.
 */

@Entity(tableName = "recordings")

public class Recording implements Serializable{

    @PrimaryKey(autoGenerate = true)
    private long uid;

    //Representation of file name
    @ColumnInfo(name = "file")
    private String file;

    @ColumnInfo(name = "hash")
    private String hash;

    @ColumnInfo(name = "length")
    private Float length;

    @ColumnInfo(name = "date")
    private Date date;

    @ColumnInfo(name = "cachedFile")
    private Boolean cachedFile;

    @ColumnInfo(name = "not_synced")
    private Boolean notSynced;

    public Recording(String file){

        //default values
        this.length = 0f;
        this.cachedFile = false;
        this.notSynced = false;
        this.file = file;

        //Get date
        date = Calendar.getInstance().getTime();    //adds current date
    }

    //Getter and Setter
    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Float getLength() {
        return length;
    }

    public void setLength(Float length) {
        this.length = length;
    }

    public Boolean getCachedFile() {
        return cachedFile;
    }

    public void setCachedFile(Boolean cachedFile) {
        this.cachedFile = cachedFile;
    }

    public Boolean getNotSynced() {
        return notSynced;
    }

    public void setNotSynced(Boolean notSynced) {
        this.notSynced = notSynced;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
