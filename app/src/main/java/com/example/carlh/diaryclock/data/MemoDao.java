package com.example.carlh.diaryclock.data;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.database.Cursor;

import java.util.List;

/**
 * Created by carlh on 21.07.2017.
 */

@Dao
public interface MemoDao {

        @Query("SELECT * FROM memos ORDER BY fileId DESC")
        List<Memo> getAll();

        @Query("SELECT * FROM memos ORDER BY fileId DESC")
        LiveData<List<Memo>> getAllObservable();

        @Query("SELECT * FROM memos WHERE cached LIKE :isOffline ORDER BY RANDOM() LIMIT 1")
        Memo getByRandomOffline(Boolean isOffline);

        @Query("SELECT memos.* FROM memos "

                //1:X-Relation: Left site is preserved and many-relations are added (ID is not anymore unique)
                + "LEFT JOIN memotag ON memos.uid = memotag.memoID "
                + "LEFT JOIN tags ON memotag.tagID = tags.uid "
                + "WHERE memos.cached LIKE :isOffline AND tags.name LIKE :labelString "
                + "ORDER BY RANDOM() LIMIT 1 ")
        Memo getByLabelOffline(String labelString,Boolean isOffline);

        /*
        Get all id's with Labels in table MemoTag
        Use that id's to look for one random ID in table Memo which is cached
         */

        @Query("SELECT * FROM memos WHERE uid IN (:memoIds)")
        List<Memo> loadAllByIds(int[] memoIds);

        @Query("SELECT * FROM memos WHERE name LIKE :namePar")
        List<Memo> findByName(String namePar);

        @Query("SELECT * FROM memos WHERE last_played LIKE 1 LIMIT 1")
        Memo getLastPlayed();

        @Query("SELECT * FROM memos WHERE uid LIKE :uID")
        Memo findById(Long uID);

        @Query("SELECT * FROM recordings WHERE uid LIKE :id")
        Recording getFile(Long id);

        @Insert
        void insertAll(Memo... memos);

        @Insert
        Long insert(Memo memo);

        @Delete
        void delete(Memo memo);

        @Update
        public void updateAll(Memo... memos);

        @Update
        public void update(Memo memo);

        @Delete
        void deleteMemos(Memo... memos);

}
