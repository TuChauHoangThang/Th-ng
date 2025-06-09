package com.example.weatherapp.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "weather_backgrounds")
public class WeatherBackground {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String weatherCondition; // Điều kiện thời tiết (ví dụ: "mây đen u ám")
    private String backgroundPath;    // Đường dẫn đến file ảnh
    private String description;       // Mô tả thêm về background

    public WeatherBackground(String weatherCondition, String backgroundPath, String description) {
        this.weatherCondition = weatherCondition;
        this.backgroundPath = backgroundPath;
        this.description = description;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public String getBackgroundPath() {
        return backgroundPath;
    }

    public void setBackgroundPath(String backgroundPath) {
        this.backgroundPath = backgroundPath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
} 