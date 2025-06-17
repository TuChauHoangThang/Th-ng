package com.example.weatherapp.api;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;

public class RetrofitClient {
    private static final String BASE_URL = "https://api.openweathermap.org/";
    private static RetrofitClient instance;
    private OpenWeatherApiService openWeatherApiService;

    private RetrofitClient() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        openWeatherApiService = retrofit.create(OpenWeatherApiService.class);
    }
    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }
    public OpenWeatherApiService getOpenWeatherApiService() {
        return openWeatherApiService;
    }

    private String extractLocationFromText(String text) {
        // Đơn giản: tìm các thành phố lớn
        String[] cities = {"Hà Nội", "TP.HCM", "Đà Nẵng", "Cần Thơ", "Hải Phòng", "Huế", "Nha Trang", "Biên Hòa", "Dĩ An"};
        for (String city : cities) {
            if (text.contains(city)) return city;
        }
        // Nếu không tìm thấy, trả về đoạn text đầu tiên
        return text.split("\\n")[0];
    }


    }

