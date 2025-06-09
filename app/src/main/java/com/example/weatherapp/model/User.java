package com.example.weatherapp.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String uid;
    public String email;

    public User(String uid, String email) {
        this.uid = uid;
        this.email = email;
    }
} 