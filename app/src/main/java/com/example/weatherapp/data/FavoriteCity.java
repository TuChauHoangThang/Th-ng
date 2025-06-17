package com.example.weatherapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_cities")
public class FavoriteCity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String cityName;
    private String userId; // Để liên kết với người dùng đã đăng nhập
    
    public FavoriteCity(String cityName, String userId) {
        this.cityName = cityName;
        this.userId = userId;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getCityName() {
        return cityName;
    }
    
    public void setCityName(String cityName) {
        this.cityName = cityName;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
} 