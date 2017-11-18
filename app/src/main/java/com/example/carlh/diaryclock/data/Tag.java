package com.example.carlh.diaryclock.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import java.util.List;

import static android.arch.persistence.room.ForeignKey.CASCADE;

/**
 * Created by carlh on 21.07.2017.
 */

@Entity(tableName = "tags", indices = {@Index(value = "name", unique = true)})
public class Tag {

    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name = "name")
    private String name;

    //constructor
    public Tag(String name) {
        this.name = name;
    }

    //Getter and Setter
    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    //not very efficient but ok until you find a right solution
    public static Long insertUnique(Tag tag, AppDatabase db) {
        return 0l;
    }

    //Get a list of Strings with all tags based on a id of an memo-entry
    public static List<String> getTagStringsById(Long id, AppDatabase appDatabase) {
        List<Long> tagIDs = appDatabase.memoTagDao().getTagIDs(id);
        List<String> tagStrings = appDatabase.tagDao().getTagStringsById(tagIDs);
        return tagStrings;
    }

    public static void updateTagMemo(long memoId, String labelStrings, AppDatabase appDatabase) {
        //parse String array
        String[] tagStrings = labelStrings.split(",");
        Long currentTagId;
        MemoTag currentMemoTag;

        //update associations: brute approach: delete all associations of a memo and create them again
        //delete all associations
        appDatabase.memoTagDao().deleteAll(appDatabase.memoTagDao().getAllByMemoId(memoId));
        //Updating the Tag-Entities and creating the associations again
        for (String tagString : tagStrings) {
            //update Tags
            if (tagString.isEmpty())
                continue;

            //Correcting Strings (deleting leading/trailing whitespaces)
            tagString = tagString.trim();

            Tag tag = appDatabase.tagDao().findByName(tagString);
            if (tag == null) { //not yet existent, has unique constraint
                currentTagId = appDatabase.tagDao().insert(new Tag(tagString));
            } else {
                currentTagId = tag.uid;
            }
            //Create the new association
            currentMemoTag = new MemoTag(memoId, currentTagId);
            appDatabase.memoTagDao().insert(currentMemoTag);
        }

    }
}

