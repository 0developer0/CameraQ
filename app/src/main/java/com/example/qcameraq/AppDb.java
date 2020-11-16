package com.example.qcameraq;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(version = 1, exportSchema = false, entities = {Picture.class})
public abstract class AppDb extends RoomDatabase {
    private static volatile AppDb appDb;

    public static AppDb getAppDb(Context context){
        if(appDb == null){
            appDb = Room.databaseBuilder(context.getApplicationContext(), AppDb.class, "app_db")
                    .allowMainThreadQueries()
                    .build();
        }
        return appDb;
    }

    public abstract PictureDao getPictureDao();
}
