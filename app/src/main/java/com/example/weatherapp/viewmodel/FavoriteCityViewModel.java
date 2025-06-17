package com.example.weatherapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.weatherapp.data.FavoriteCity;
import com.example.weatherapp.repository.FavoriteCityRepository;

import java.util.List;

public class FavoriteCityViewModel extends AndroidViewModel {
    private final FavoriteCityRepository repository;
    private final LiveData<List<FavoriteCity>> allFavoriteCities;
    private final String userId;

    public FavoriteCityViewModel(@NonNull Application application, String userId) {
        super(application);
        this.userId = userId;
        repository = new FavoriteCityRepository(application, userId);
        allFavoriteCities = repository.getAllFavoriteCities();
    }

    public LiveData<List<FavoriteCity>> getAllFavoriteCities() {
        return allFavoriteCities;
    }

    public void insert(FavoriteCity city) {
        repository.insert(city);
    }

    public void delete(FavoriteCity city) {
        repository.delete(city);
    }

    public void addToFavorites(String cityName) {
        repository.checkAndAddFavorite(cityName, userId);
    }
} 