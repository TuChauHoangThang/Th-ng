package com.example.weatherapp.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.weatherapp.model.User;

@Dao
public interface UserDao {
    @Insert
    void insert(User user);

    @Query("SELECT * FROM users WHERE uid = :uid")
    User findByUid(String uid);
} 