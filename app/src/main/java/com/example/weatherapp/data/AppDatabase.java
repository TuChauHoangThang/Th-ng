package com.example.weatherapp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.weatherapp.data.dao.WeatherBackgroundDao;
import com.example.weatherapp.data.entity.WeatherBackground;

@Database(entities = {WeatherBackground.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public abstract WeatherBackgroundDao weatherBackgroundDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "weather_app_database"
            ).build();
        }
        return instance;
    }
} 