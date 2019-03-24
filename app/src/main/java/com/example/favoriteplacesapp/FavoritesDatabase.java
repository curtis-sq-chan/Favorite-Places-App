package com.example.favoriteplacesapp;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import java.util.List;

@Database(entities = {Favorites.class}, version = 1, exportSchema = false)
public abstract class FavoritesDatabase extends RoomDatabase {
    public abstract FavoritesDaoAccess daoAccess() ;
}