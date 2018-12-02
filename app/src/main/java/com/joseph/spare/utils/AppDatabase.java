package com.joseph.spare.utils;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.joseph.spare.domain.Item;
import com.joseph.spare.domain.ItemsDao;
import com.joseph.spare.domain.User;
import com.joseph.spare.domain.UserDao;

@Database(entities = {User.class, Item.class}, version = 1, exportSchema = false)

public abstract class AppDatabase extends RoomDatabase {

    public static AppDatabase instance;

    public abstract UserDao userDao();

    public abstract ItemsDao itemsDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context, AppDatabase.class, "spare")
                    .fallbackToDestructiveMigration().build();
        }
        return instance;
    }


}
