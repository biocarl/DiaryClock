package com.example.carlh.diaryclock.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

/**
 * Created by carlh on 21.07.2017.
 */

@Dao
public interface MemoTagDao {

        @Query("SELECT * FROM memotag")
        List<MemoTag> getAll();

        @Query("SELECT * FROM memotag WHERE memoID LIKE :memoid")
        List<MemoTag> getAllByMemoId(Long memoid);

        @Query("SELECT tagID FROM memotag WHERE memoID LIKE :memoid")
        List<Long> getTagIDs(Long memoid);

        @Insert
        void insertAll(MemoTag... memoTags);

        @Insert
        Long insert(MemoTag memoTag);

        @Update
        public void updateAll(MemoTag... memoTags);

        @Update
        public void update(MemoTag memoTag);

        @Delete
        void delete(MemoTag memoTag);

        @Delete
        void deleteAll(List<MemoTag> memoTags);


}
