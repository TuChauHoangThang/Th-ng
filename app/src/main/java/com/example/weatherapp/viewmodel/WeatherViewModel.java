package com.example.weatherapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.weatherapp.model.CurrentWeatherResponse;
import com.example.weatherapp.model.ForecastResponse;

public class WeatherViewModel extends ViewModel {
    private final MutableLiveData<CurrentWeatherResponse> currentWeather = new MutableLiveData<>();
    private final MutableLiveData<ForecastResponse> forecast = new MutableLiveData<>();

    public void setCurrentWeather(CurrentWeatherResponse weather) {
        currentWeather.setValue(weather);
    }

    public LiveData<CurrentWeatherResponse> getCurrentWeather() {
        return currentWeather;
    }

    public void setForecast(ForecastResponse forecastData) {
        forecast.setValue(forecastData);
    }

    public LiveData<ForecastResponse> getForecast() {
        return forecast;
    }
} 