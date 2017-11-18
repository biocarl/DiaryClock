package com.example.carlh.diaryclock.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.util.TableInfo;

import static android.arch.persistence.room.ForeignKey.CASCADE;

/**
 * Created by carlh on 21.07.2017.
 */

@Entity(tableName = "memos",
        foreignKeys = {
                @ForeignKey(entity = Recording.class,
                        parentColumns = "uid",
                        childColumns = "fileId",
                        onDelete = CASCADE //whenever recording is deleted, this entity is also deleted)
                )
        }
)
public class Memo {
    public Memo(Recording recording) {
        this.fileId = (long) recording.getUid();
        //Copy the fields (that is very bad! since nothing is updated.) TODO
        this.cached = recording.getCachedFile();
        this.notSynced = recording.getNotSynced();
        this.start = 0f;
        this.plays = 0;
        this.stop = 0f;
        this.length = stop - start;
        this.rating = 0;
        this.lastPlayed = false;
        this.selected = false;
    }

    //default
    public Memo() {
    }

    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "fileId")
    private Long fileId;

    @ColumnInfo(name = "length")
    private Float length;

    @ColumnInfo(name = "plays")
    private Integer plays;

    @ColumnInfo(name = "rating")
    private Integer rating;

    @ColumnInfo(name = "start")
    private Float start;

    @ColumnInfo(name = "stop")
    private Float stop;

    @ColumnInfo(name = "cached")
    private Boolean cached;

    @ColumnInfo(name = "not_synced")
    private Boolean notSynced;

    @ColumnInfo(name = "last_played")
    private Boolean lastPlayed;

    @ColumnInfo(name = "selected")
    private Boolean selected;

    //Getter and Setter
    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public Boolean getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(Boolean lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getLength() {
        return stop - start;
    }

    public void setLength(Float length) {
        this.length = length;
    }

    public Integer getPlays() {
        return plays;
    }

    public void setPlays(Integer plays) {
        this.plays = plays;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Float getStart() {
        return start;
    }

    public void setStart(Float start) {
        this.start = start;
    }

    public Float getStop() {
        return stop;
    }

    public void setStop(Float stop) {
        this.stop = stop;
    }

    public Boolean getNotSynced() {
        return notSynced;
    }

    public void setNotSynced(Boolean notSynced) {
        this.notSynced = notSynced;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Boolean getCached() {
        return cached;
    }

    public void setCached(Boolean cached) {
        this.cached = cached;
    }

    //not very efficient but ok until you find a right solution
    public static void updateMemos(Recording recording, AppDatabase db) {
        //updating all relevant parameters of recording with all ass. Memos
        for (Memo memo : db.recordingDao().getMemos(recording.getUid())) {
            memo.setNotSynced(recording.getNotSynced());
            memo.setCached(recording.getCachedFile());
            db.memoDao().update(memo);
        }
    }

}

