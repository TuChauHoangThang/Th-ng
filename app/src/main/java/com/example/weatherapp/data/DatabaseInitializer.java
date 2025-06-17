package com.example.weatherapp.data;

import android.content.Context;
import android.os.AsyncTask;

import com.example.weatherapp.data.dao.BackgroundImageDao;
import com.example.weatherapp.data.entity.BackgroundImage;

import java.util.Arrays;
import java.util.List;

public class DatabaseInitializer {
    private static final List<BackgroundImage> DEFAULT_BACKGROUNDS = Arrays.asList(
        new BackgroundImage("Mist", "https://res.cloudinary.com/dijswwhab/image/upload/v1749738564/mist_pheq1d.jpg"),
        new BackgroundImage("Dust", "https://res.cloudinary.com/dijswwhab/image/upload/v1749738563/dust_nd9dkr.jpg"),
        new BackgroundImage("Weather Background", "https://res.cloudinary.com/dijswwhab/image/upload/v1749738564/bg_weather_a1uye0.jpg"),
        new BackgroundImage("Clean", "https://res.cloudinary.com/dijswwhab/image/upload/v1749738562/clean_nwe6vw.jpg"),
        new BackgroundImage("Dark Cloud", "https://res.cloudinary.com/dijswwhab/image/upload/v1749738563/dark_cloud_yg5psx.jpg"),
        new BackgroundImage("Cloudy", "https://res.cloudinary.com/dijswwhab/image/upload/v1749738563/cloudy_aag1fn.jpg"),
        new BackgroundImage("Rain", "https://res.cloudinary.com/dijswwhab/image/upload/v1749738563/rain_gzde01.jpg")
    );

    public static void initializeDatabase(Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                AppDatabase database = AppDatabase.getInstance(context);
                BackgroundImageDao backgroundImageDao = database.backgroundImageDao();

                // Clear existing data
                backgroundImageDao.deleteAll();

                // Insert new data
                for (BackgroundImage background : DEFAULT_BACKGROUNDS) {
                    backgroundImageDao.insert(background);
                }
                return null;
            }
        }.execute();
    }
} 