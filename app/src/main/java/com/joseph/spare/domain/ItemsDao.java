package com.joseph.spare.domain;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import io.reactivex.Single;

@Dao
public interface ItemsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Item item);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(Item item);

    @Delete
    void delete(Item item);

    @Query("SELECT * FROM items WHERE id=:id")
    Single<Item> find(String id);

    @Query("SELECT * FROM items")
    Single<List<Item>> findAll();
}
