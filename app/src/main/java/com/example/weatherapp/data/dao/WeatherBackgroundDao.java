package com.example.weatherapp.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.weatherapp.data.entity.WeatherBackground;

import java.util.List;

@Dao
public interface WeatherBackgroundDao {
    @Query("SELECT * FROM weather_backgrounds")
    List<WeatherBackground> getAllBackgrounds();

    @Query("SELECT * FROM weather_backgrounds WHERE weatherCondition LIKE :condition LIMIT 1")
    WeatherBackground getBackgroundByCondition(String condition);

    @Query("SELECT * FROM weather_backgrounds WHERE weatherCondition IN (:conditions)")
    List<WeatherBackground> getBackgroundsByConditions(List<String> conditions);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WeatherBackground background);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<WeatherBackground> backgrounds);

    @Update
    void update(WeatherBackground background);

    @Query("DELETE FROM weather_backgrounds")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM weather_backgrounds")
    int getBackgroundCount();
} 