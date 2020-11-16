package com.example.qcameraq;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import java.util.ArrayList;

@Dao
public interface PictureDao {

    @Insert
    long addPic(Picture picture);

    @Query("SELECT * FROM tbl_Picture")
    List<Picture> getPictures();
}
