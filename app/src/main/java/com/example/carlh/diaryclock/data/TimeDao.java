package com.example.carlh.diaryclock.data;

import android.arch.lifecycle.LiveData;
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
public interface TimeDao {

        @Query("SELECT * FROM times")
        List<Time> getAll();

        @Query("SELECT * FROM times")
        LiveData<List<Time>> getAllObservable();

        @Query("SELECT * FROM times WHERE uid LIKE :uID")
        Time findById(Long uID);

        @Query("DELETE FROM times")
        public void deleteTable();

        @Insert
        void insertAll(Time... times);

        @Insert
        Long insert(Time time);

        @Delete
        void delete(Time time);

        @Update
        public void updateAll(Time... times);

        @Update
        public void update(Time time);

        @Delete
        void deleteAll(Time... times);

}
