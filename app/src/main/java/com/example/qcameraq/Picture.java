package com.example.qcameraq;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tbl_Picture")
public class Picture {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private byte[] picture;

    public Picture(byte[] picture) {
        this.picture = picture;
    }

    public int getId() { return id; }
    public byte[] getPicture() { return picture; }

    public void setId(int id) { this.id = id; }
    public void setPicture(byte[] picture) { this.picture = picture; }
}