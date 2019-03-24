package com.example.favoriteplacesapp;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface FavoritesDaoAccess {

    @Insert
    void insert(Favorites favorites);

    @Query("SELECT * FROM Favorites")
    List<Favorites> fetchAll();

    @Query("DELETE FROM Favorites")
    void deleteAll();
}