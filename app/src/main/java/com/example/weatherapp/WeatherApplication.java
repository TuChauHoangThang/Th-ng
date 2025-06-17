package com.example.weatherapp;

import android.app.Application;

import com.example.weatherapp.data.DatabaseInitializer;

public class WeatherApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize database with background images
        DatabaseInitializer.initializeDatabase(this);
    }
} 