package com.example.weatherapp.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "background_images")
public class BackgroundImage {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String imgURL;

    public BackgroundImage(String name, String imgURL) {
        this.name = name;
        this.imgURL = imgURL;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImgURL() {
        return imgURL;
    }

    public void setImgURL(String imgURL) {
        this.imgURL = imgURL;
    }
} 