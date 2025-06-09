package com.example.weatherapp.utils;

import android.content.Context;
import android.util.Log;
import android.widget.RelativeLayout;

import com.example.weatherapp.R;
import com.example.weatherapp.data.DatabaseHelper;
import com.example.weatherapp.data.entity.WeatherBackground;

import java.util.Arrays;
import java.util.List;

public class WeatherBackgroundManager {
    private static final String TAG = "WeatherBackgroundManager";
    private final Context context;
    private final RelativeLayout mainLayout;
    private final DatabaseHelper databaseHelper;

    public WeatherBackgroundManager(Context context, RelativeLayout mainLayout) {
        this.context = context;
        this.mainLayout = mainLayout;
        this.databaseHelper = DatabaseHelper.getInstance(context);
        
        // Khởi tạo dữ liệu
        initializeBackgrounds();
    }

    private void initializeBackgrounds() {
        databaseHelper.initializeBackgrounds(() -> 
            Log.d(TAG, "Khởi tạo backgrounds hoàn tất"));
    }

    public void updateBackground(String weatherCondition) {
        if (weatherCondition == null || weatherCondition.isEmpty()) {
            setDefaultBackground();
            return;
        }

        // Tìm background phù hợp nhất với điều kiện thời tiết
        WeatherBackground background = findMatchingBackground(weatherCondition.toLowerCase());
        if (background != null) {
            setBackground(background);
        } else {
            setDefaultBackground();
        }
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
                databaseHelper.getBackgroundByCondition(term, new DatabaseHelper.DatabaseCallback<WeatherBackground>() {
                    @Override
                    public void onResult(WeatherBackground result) {
                        if (result != null) {
                            setBackground(result);
                        } else {
                            setDefaultBackground();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Lỗi khi tìm background: " + e.getMessage());
                        setDefaultBackground();
                    }
                });
                return null; // Return null vì kết quả sẽ được xử lý trong callback
            }
        }

        return null;
    }

    private void setBackground(WeatherBackground background) {
        try {
            int backgroundResId = context.getResources().getIdentifier(
                background.getBackgroundPath(),
                "drawable",
                context.getPackageName()
            );
            mainLayout.post(() -> {
                try {
                    mainLayout.setBackgroundResource(backgroundResId);
                    Log.d(TAG, "Đã set background: " + background.getWeatherCondition());
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi khi set background: " + e.getMessage());
                    setDefaultBackground();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lấy resource ID: " + e.getMessage());
            setDefaultBackground();
        }
    }

    private void setDefaultBackground() {
        mainLayout.post(() -> {
            try {
                mainLayout.setBackgroundResource(R.drawable.clean);
                Log.d(TAG, "Đã set default background");
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi set default background: " + e.getMessage());
            }
        });
    }

    public void cleanup() {
        databaseHelper.cleanup();
    }
} 