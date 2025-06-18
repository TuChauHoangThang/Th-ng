package com.example.weatherapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteCityDao {
    @Insert
    void insert(FavoriteCity favoriteCity);
    
    @Delete
    void delete(FavoriteCity favoriteCity);
    
    @Query("SELECT * FROM favorite_cities WHERE userId = :userId")
    LiveData<List<FavoriteCity>> getFavoriteCitiesForUser(String userId);
    
    @Query("SELECT * FROM favorite_cities WHERE cityName = :cityName AND userId = :userId")
    FavoriteCity getFavoriteCity(String cityName, String userId);
    
    @Query("DELETE FROM favorite_cities WHERE userId = :userId")
    void clearFavoritesForUser(String userId);
} 