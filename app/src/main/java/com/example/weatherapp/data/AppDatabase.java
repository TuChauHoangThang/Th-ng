package com.example.weatherapp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.weatherapp.data.dao.BackgroundImageDao;
import com.example.weatherapp.data.dao.WeatherBackgroundDao;
import com.example.weatherapp.data.entity.BackgroundImage;
import com.example.weatherapp.data.entity.WeatherBackground;
import com.example.weatherapp.data.FavoriteCity;
import com.example.weatherapp.data.FavoriteCityDao;

@Database(entities = {WeatherBackground.class, BackgroundImage.class, FavoriteCity.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public abstract WeatherBackgroundDao weatherBackgroundDao();
    public abstract BackgroundImageDao backgroundImageDao();
    public abstract FavoriteCityDao favoriteCityDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "weather_app_database"
            ).fallbackToDestructiveMigration().build();
        }
        return instance;
    }
} 