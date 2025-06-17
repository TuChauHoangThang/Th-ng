package com.example.weatherapp.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.weatherapp.data.AppDatabase;
import com.example.weatherapp.data.FavoriteCity;
import com.example.weatherapp.data.FavoriteCityDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteCityRepository {
    private FavoriteCityDao favoriteCityDao;
    private LiveData<List<FavoriteCity>> allFavoriteCities;
    private ExecutorService executorService;

    public FavoriteCityRepository(Application application, String userId) {
        AppDatabase db = AppDatabase.getInstance(application);
        favoriteCityDao = db.favoriteCityDao();
        allFavoriteCities = favoriteCityDao.getFavoriteCitiesForUser(userId);
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<FavoriteCity>> getAllFavoriteCities() {
        return allFavoriteCities;
    }

    public void insert(FavoriteCity favoriteCity) {
        executorService.execute(() -> favoriteCityDao.insert(favoriteCity));
    }

    public void delete(FavoriteCity favoriteCity) {
        executorService.execute(() -> favoriteCityDao.delete(favoriteCity));
    }

    public void checkAndAddFavorite(String cityName, String userId) {
        executorService.execute(() -> {
            FavoriteCity existingCity = favoriteCityDao.getFavoriteCity(cityName, userId);
            if (existingCity == null) {
                FavoriteCity newFavorite = new FavoriteCity(cityName, userId);
                favoriteCityDao.insert(newFavorite);
            }
        });
    }
} 