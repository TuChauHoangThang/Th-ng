package com.example.weatherapp.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.weatherapp.R;
import com.example.weatherapp.data.AppDatabase;
import com.example.weatherapp.data.dao.BackgroundImageDao;
import com.example.weatherapp.data.entity.BackgroundImage;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherBackgroundManager {
    private static final String TAG = "WeatherBackgroundManager";
    private final Context context;
    private final RelativeLayout mainLayout;
    private final BackgroundImageDao backgroundImageDao;
    private final ExecutorService executorService;

    public WeatherBackgroundManager(Context context, RelativeLayout mainLayout) {
        this.context = context;
        this.mainLayout = mainLayout;
        this.backgroundImageDao = AppDatabase.getInstance(context).backgroundImageDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void updateBackground(String weatherCondition) {
        if (weatherCondition == null || weatherCondition.isEmpty()) {
            setDefaultBackground();
            return;
        }

        // Tìm background phù hợp nhất với điều kiện thời tiết
        String backgroundName = findMatchingBackgroundName(weatherCondition.toLowerCase());
        if (backgroundName != null) {
            executorService.execute(() -> {
                BackgroundImage background = backgroundImageDao.getBackgroundImageByName(backgroundName);
                if (background != null) {
                    setBackground(background.getImgURL());
                } else {
                    setDefaultBackground();
                }
            });
        } else {
            setDefaultBackground();
        }
    }

    private String findMatchingBackgroundName(String condition) {
        // Danh sách các từ khóa và background tương ứng
        if (condition.contains("sương mù") || condition.contains("mist")) {
            return "Mist";
        } else if (condition.contains("bụi") || condition.contains("dust")) {
            return "Dust";
        } else if (condition.contains("mây đen") || condition.contains("dark cloud")) {
            return "Dark Cloud";
        } else if (condition.contains("mây") || condition.contains("cloudy")) {
            return "Cloudy";
        } else if (condition.contains("mưa") || condition.contains("rain")) {
            return "Rain";
        } else if (condition.contains("nắng") || condition.contains("clear")) {
            return "Clean";
        }
        return null;
    }

    private void setBackground(String imageUrl) {
        mainLayout.post(() -> {
            try {
                Glide.with(context)
                    .load(imageUrl)
                    .centerCrop()
                    .into(new CustomTarget<android.graphics.drawable.Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource,
                                                  @Nullable Transition<? super Drawable> transition) {
                            mainLayout.setBackground(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                            setDefaultBackground();
                        }
                    });
                Log.d(TAG, "Đã set background từ URL: " + imageUrl);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi set background từ URL: " + e.getMessage());
                setDefaultBackground();
            }
        });
    }

    private void setDefaultBackground() {
        mainLayout.post(() -> {
            try {
                Glide.with(context)
                    .load("https://res.cloudinary.com/dijswwhab/image/upload/v1749738562/clean_nwe6vw.jpg")
                    .centerCrop()
                    .into(new CustomTarget<android.graphics.drawable.Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource,
                                                  @Nullable Transition<? super android.graphics.drawable.Drawable> transition) {
                            mainLayout.setBackground(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                            // Do nothing
                        }
                    });
                Log.d(TAG, "Đã set default background");
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi set default background: " + e.getMessage());
            }
        });
    }

    public void cleanup() {
        executorService.shutdown();
    }
} 