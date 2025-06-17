package com.example.weatherapp.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.weatherapp.data.entity.BackgroundImage;

import java.util.List;

@Dao
public interface BackgroundImageDao {
    @Insert
    void insert(BackgroundImage backgroundImage);

    @Query("SELECT * FROM background_images")
    List<BackgroundImage> getAllBackgroundImages();

    @Query("SELECT * FROM background_images WHERE name = :name LIMIT 1")
    BackgroundImage getBackgroundImageByName(String name);

    @Query("DELETE FROM background_images")
    void deleteAll();
} 