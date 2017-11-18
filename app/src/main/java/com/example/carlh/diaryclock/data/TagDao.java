package com.example.carlh.diaryclock.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.Date;
import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.FAIL;
import static android.arch.persistence.room.OnConflictStrategy.IGNORE;
import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

/**
 * Created by carlh on 21.07.2017.
 */

@Dao
public interface TagDao {

        @Query("SELECT * FROM tags")
        List<Tag> getAll();

        @Query("SELECT name FROM tags")
        List<String> getTagsAsString();

        @Query("SELECT name FROM tags WHERE uid IN (:memoIds)")
        List<String> getTagStringsById(List<Long> memoIds);

        @Query("SELECT * FROM tags WHERE name LIKE :namePar")
        Tag findByName(String namePar);

        @Insert
        void insertAll(Tag... tags);

        @Insert(onConflict = FAIL)
        Long insert(Tag tag);

        @Update
        public void updateAll(Tag... tags);

        @Update
        public void update(Tag tag);

        @Delete
        void delete(Tag tag);

}
