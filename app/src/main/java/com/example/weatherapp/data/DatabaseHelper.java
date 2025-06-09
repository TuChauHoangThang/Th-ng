package com.example.weatherapp.data;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;

import com.example.weatherapp.data.entity.WeatherBackground;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "weather_database";
    private static DatabaseHelper instance;
    private final AppDatabase database;
    private final ExecutorService executorService;

    private DatabaseHelper(Context context) {
        database = Room.databaseBuilder(context.getApplicationContext(),
                AppDatabase.class, DATABASE_NAME)
                .build();
        executorService = Executors.newSingleThreadExecutor();
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        return instance;
    }

    public void initializeBackgrounds(Runnable onComplete) {
        executorService.execute(() -> {
            try {
                if (database.weatherBackgroundDao().getBackgroundCount() == 0) {
                    List<WeatherBackground> backgrounds = List.of(
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
                if (onComplete != null) {
                    onComplete.run();
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi khởi tạo backgrounds: " + e.getMessage());
            }
        });
    }

    public void getBackgroundByCondition(String condition, DatabaseCallback<WeatherBackground> callback) {
        executorService.execute(() -> {
            try {
                WeatherBackground background = database.weatherBackgroundDao()
                        .getBackgroundByCondition(condition);
                if (callback != null) {
                    callback.onResult(background);
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi lấy background: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public void getAllBackgrounds(DatabaseCallback<List<WeatherBackground>> callback) {
        executorService.execute(() -> {
            try {
                List<WeatherBackground> backgrounds = database.weatherBackgroundDao().getAllBackgrounds();
                if (callback != null) {
                    callback.onResult(backgrounds);
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi lấy tất cả backgrounds: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public void printDatabaseInfo() {
        executorService.execute(() -> {
            try {
                // In thông tin về số lượng records
                int count = database.weatherBackgroundDao().getBackgroundCount();
                Log.d(TAG, "Tổng số backgrounds trong database: " + count);

                // In danh sách tất cả backgrounds
                List<WeatherBackground> backgrounds = database.weatherBackgroundDao().getAllBackgrounds();
                Log.d(TAG, "Danh sách backgrounds:");
                for (WeatherBackground bg : backgrounds) {
                    Log.d(TAG, String.format("- Điều kiện: %s, Background: %s, Mô tả: %s",
                        bg.getWeatherCondition(),
                        bg.getBackgroundPath(),
                        bg.getDescription()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi in thông tin database: " + e.getMessage());
            }
        });
    }

    public void cleanup() {
        executorService.shutdown();
    }

    // Interface để xử lý kết quả từ database
    public interface DatabaseCallback<T> {
        void onResult(T result);
        void onError(Exception e);
    }
} 