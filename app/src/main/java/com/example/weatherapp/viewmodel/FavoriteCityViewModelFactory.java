package com.example.weatherapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class FavoriteCityViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;
    private final String userId;

    public FavoriteCityViewModelFactory(Application application, String userId) {
        this.application = application;
        this.userId = userId;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(FavoriteCityViewModel.class)) {
            return (T) new FavoriteCityViewModel(application, userId);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
} 