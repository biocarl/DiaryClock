package com.example.carlh.diaryclock.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by carlh on 21.07.2017.
 */

@Entity(tableName = "memotag")
public class MemoTag {

        @PrimaryKey(autoGenerate = true)
        private long uid;

        @ColumnInfo(name = "memoID")
        private Long memoID;

        @ColumnInfo(name = "tagID")
        private Long tagID;

        public MemoTag(Long memoID, Long tagID){
                this.memoID = memoID;
                this.tagID = tagID;
        }

        //Getter and Setter
        public long getUid() {
                return uid;
        }
        public void setUid(long uid) {
                this.uid = uid;
        }
        public Long getMemoID() {
                return memoID;
        }
        public void setMemoID(Long memoID) {
                this.memoID = memoID;
        }
        public Long getTagID() {
                return tagID;
        }
        public void setTagID(Long tagID) {
                this.tagID = tagID;
        }
}

