package com.example.carlh.diaryclock.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import static android.arch.persistence.room.ForeignKey.CASCADE;

/**
 * Created by carlh on 21.07.2017.
 */

@Entity(tableName = "times")
public class Time {

        public Time(String time, String name) {
                this.time = time;
                this.name = name;
                //default values
                active  = false;
                snooze = false;
                repeat = false;
                label = "";
        }

        //default
        @Ignore
        public Time() {
        }

        @PrimaryKey(autoGenerate = true)
        private long uid;

        @ColumnInfo(name = "name")
        private String name;

        @ColumnInfo(name = "time")
        private String time;

        @ColumnInfo(name = "label")
        private String label;

        @ColumnInfo(name = "active")
        private Boolean active;

        @ColumnInfo(name = "played")
        private Boolean played;

        @ColumnInfo(name = "snooze")
        private Boolean snooze;

        @ColumnInfo(name = "repeat")
        private Boolean repeat;


        public Boolean getPlayed() {
                return played;
        }

        public void setPlayed(Boolean played) {
                this.played = played;
        }

        public String getLabel() {
                return label;
        }

        public void setLabel(String label) {
                this.label = label;
        }

        public Boolean getRepeat() {
                return repeat;
        }

        public void setRepeat(Boolean repeat) {
                this.repeat = repeat;
        }

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

        public String getTime() {
                return time;
        }

        public void setTime(String time) {
                this.time = time;
        }

        public Boolean getActive() {
                return active;
        }

        public void setActive(Boolean active) {
                this.active = active;
        }

        public Boolean getSnooze() {
                return snooze;
        }

        public void setSnooze(Boolean snooze) {
                this.snooze = snooze;
        }
}

