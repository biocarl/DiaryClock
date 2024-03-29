package com.example.carlh.diaryclock.data;

import android.arch.persistence.room.TypeConverter;

import java.util.Date;

/**
 * Created by carlh on 24.10.2017.
 */

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
