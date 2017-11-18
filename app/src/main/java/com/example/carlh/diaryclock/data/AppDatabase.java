package com.example.carlh.diaryclock.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

/**
 * Created by carlh on 21.07.2017.
 */
@Database(entities = {Memo.class,Recording.class,MemoTag.class,Tag.class,Time.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract MemoDao memoDao();
    public abstract RecordingDao recordingDao();
    public abstract MemoTagDao memoTagDao();
    public abstract TagDao tagDao();
    public abstract TimeDao timeDao();
}

