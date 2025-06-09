package com.example.weatherapp.utils;

import android.content.Context;
import android.util.Log;
import android.widget.RelativeLayout;

import androidx.room.Room;

import com.example.weatherapp.R;
import com.example.weatherapp.data.AppDatabase;
import com.example.weatherapp.data.entity.WeatherBackground;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherBackgroundManager {
    private static final String TAG = "WeatherBackgroundManager";
    private final Context context;
    private final RelativeLayout mainLayout;
    private final AppDatabase database;
    private final ExecutorService executorService;

    public WeatherBackgroundManager(Context context, RelativeLayout mainLayout) {
        this.context = context;
        this.mainLayout = mainLayout;
        this.database = AppDatabase.getInstance(context);
        this.executorService = Executors.newSingleThreadExecutor();
        
        // Khởi tạo dữ liệu nếu cần
        initializeBackgrounds();
    }

    private void initializeBackgrounds() {
        executorService.execute(() -> {
            try {
                if (database.weatherBackgroundDao().getBackgroundCount() == 0) {
                    List<WeatherBackground> backgrounds = Arrays.asList(
                        new WeatherBackground("dông", "storm", "Background cho thời tiết dông bão"),
                        new WeatherBackground("bão", "storm", "Background cho thời tiết dông bão"),
                        new WeatherBackground("mưa", "rain", "Background cho thời tiết mưa"),
                        new WeatherBackground("tuyết", "snow", "Background cho thời tiết tuyết"),
                        new WeatherBackground("mây đen u ám", "dark_cloud", "Background cho mây đen u ám"),
                        new WeatherBackground("mây đen dày đặc", "dark_cloud", "Background cho mây đen dày đặc"),
                        new WeatherBackground("sương mù", "mist", "Background cho sương mù"),
                        new WeatherBackground("bụi", "dust", "Background cho thời tiết nhiều bụi"),
                        new WeatherBackground("mây", "cloudy", "Background cho thời tiết nhiều mây"),
                        new WeatherBackground("nắng", "clean", "Background cho thời tiết nắng đẹp")
                    );
                    database.weatherBackgroundDao().insertAll(backgrounds);
                    Log.d(TAG, "Đã khởi tạo " + backgrounds.size() + " background");
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi khởi tạo backgrounds: " + e.getMessage());
            }
        });
    }

    public void updateBackground(String weatherCondition) {
        if (weatherCondition == null || weatherCondition.isEmpty()) {
            setDefaultBackground();
            return;
        }

        executorService.execute(() -> {
            try {
                // Tìm background phù hợp nhất với điều kiện thời tiết
                WeatherBackground background = findMatchingBackground(weatherCondition.toLowerCase());
                
                int backgroundResId;
                if (background != null) {
                    backgroundResId = context.getResources().getIdentifier(
                        background.getBackgroundPath(),
                        "drawable",
                        context.getPackageName()
                    );
                    Log.d(TAG, "Đã tìm thấy background: " + background.getWeatherCondition());
                } else {
                    setDefaultBackground();
                    return;
                }

                // Cập nhật UI trên main thread
                mainLayout.post(() -> {
                    try {
                        mainLayout.setBackgroundResource(backgroundResId);
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi set background: " + e.getMessage());
                        setDefaultBackground();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi cập nhật background: " + e.getMessage());
                setDefaultBackground();
            }
        });
    }

    private WeatherBackground findMatchingBackground(String condition) {
        // Danh sách các từ khóa cần tìm kiếm
        List<String> searchTerms = Arrays.asList(
            "dông", "bão", "mưa", "tuyết", 
            "mây đen u ám", "mây đen dày đặc",
            "sương mù", "bụi", "mây", "nắng"
        );

        // Tìm kiếm theo thứ tự ưu tiên
        for (String term : searchTerms) {
            if (condition.contains(term)) {
                return database.weatherBackgroundDao().getBackgroundByCondition(term);
            }
        }

        return null;
    }

    private void setDefaultBackground() {
        mainLayout.post(() -> {
            try {
                mainLayout.setBackgroundResource(R.drawable.clean);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi set default background: " + e.getMessage());
            }
        });
    }

    public void cleanup() {
        executorService.shutdown();
    }
} 