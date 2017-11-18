package com.example.carlh.diaryclock.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.Date;
import java.util.List;

/**
 * Created by carlh on 21.07.2017.
 */

@Dao
public interface RecordingDao {

        @Query("SELECT * FROM recordings")
        List<Recording> getAll();
        /*Returns all ass. Memos based on a file id: getMemos(recording1.getUID)*/
        @Query("SELECT * FROM memos WHERE fileId LIKE :fileId")
        List<Memo> getMemos(Long fileId);

        @Query("SELECT * FROM recordings WHERE hash LIKE :hashString")
        Recording findByHash(String hashString);

        @Query("SELECT * FROM recordings WHERE uid LIKE :uID")
        Recording findById(Long uID);

        @Query("SELECT * FROM recordings WHERE file LIKE :filePath")
        Recording findByPath(String filePath);

        @Insert
        void insertAll(Recording... recordings);

        @Insert
        Long insert(Recording recording);


        @Update
        public void updateAll(Recording... recordings);

        @Update
        public void update(Recording recording);

        @Delete
        void delete(Recording recording);

        @Delete
        void deleteMemos(Recording... recordings);

        @Query("DELETE FROM recordings")
        public void deleteTable();

        @Query("SELECT * FROM recordings WHERE date BETWEEN :from AND :to")
        List<Recording> findRecordingsBetweenDates(Date from, Date to);

}
