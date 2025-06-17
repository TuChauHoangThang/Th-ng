package com.example.weatherapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.weatherapp.R;
import com.example.weatherapp.api.RetrofitClient;
import com.example.weatherapp.model.CurrentWeatherResponse;
import com.example.weatherapp.utils.LocationHelper;
import com.example.weatherapp.BuildConfig;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LockScreenService extends Service {
    private static final String TAG = "LockScreenService";
    private static final String CHANNEL_ID = "LockScreenChannel";
    private static final int NOTIFICATION_ID = 1;

    private LocationHelper locationHelper;
    private ExecutorService executorService;
    private NotificationManager notificationManager;
    private RemoteViews remoteViews;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LockScreenService onCreate");

        locationHelper = new LocationHelper(this);
        executorService = Executors.newSingleThreadExecutor();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        remoteViews = new RemoteViews(getPackageName(), R.layout.lock_screen_widget);

        createNotificationChannel();
        // Khởi tạo notification với thông báo ban đầu
        remoteViews.setTextViewText(R.id.descriptionText, "Đang cập nhật thời tiết...");
        startForeground(NOTIFICATION_ID, createNotification());

        // Bắt đầu cập nhật thời tiết
        startWeatherUpdates();
    }

    private void startWeatherUpdates() {
        executorService.execute(() -> {
            while (!Thread.interrupted()) {
                try {
                    updateWeather();
                    Thread.sleep(300000); // Cập nhật mỗi 5 phút
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in weather update loop", e);
                }
            }
        });
    }

    private void updateWeather() {
        locationHelper.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                fetchWeatherData(location.getLatitude(), location.getLongitude());
            } else {
                Log.e(TAG, "Location is null");
                updateNotification("Không thể lấy vị trí hiện tại");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error getting location", e);
            updateNotification("Lỗi khi lấy vị trí");
        });
    }

    private void fetchWeatherData(double lat, double lon) {
        RetrofitClient.getInstance().getOpenWeatherApiService()
            .getCurrentWeather(lat, lon, BuildConfig.OPENWEATHER_API_KEY, "metric", "vi")
            .enqueue(new Callback<CurrentWeatherResponse>() {
                @Override
                public void onResponse(Call<CurrentWeatherResponse> call, Response<CurrentWeatherResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        CurrentWeatherResponse weather = response.body();
                        updateWeatherUI(weather);
                    } else {
                        Log.e(TAG, "Weather API call failed");
                        updateNotification("Không thể cập nhật thời tiết");
                    }
                }

                @Override
                public void onFailure(Call<CurrentWeatherResponse> call, Throwable t) {
                    Log.e(TAG, "Weather API call failed", t);
                    updateNotification("Lỗi kết nối");
                }
            });
    }

    private void updateWeatherUI(CurrentWeatherResponse weather) {
        try {
            // Cập nhật vị trí
            remoteViews.setTextViewText(R.id.locationText, weather.getName());

            // Cập nhật thời gian
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            remoteViews.setTextViewText(R.id.timestampText, time);

            // Cập nhật nhiệt độ
            String temp = String.format(Locale.getDefault(), "%.0f°C", weather.getMain().getTemp());
            remoteViews.setTextViewText(R.id.temperatureText, temp);

            // Cập nhật mô tả
            if (weather.getWeather() != null && !weather.getWeather().isEmpty()) {
                String description = weather.getWeather().get(0).getDescription();
                remoteViews.setTextViewText(R.id.descriptionText, description);

                // Cập nhật icon thời tiết
                String iconCode = weather.getWeather().get(0).getIcon();
                String iconUrl = String.format("https://openweathermap.org/img/wn/%s@2x.png", iconCode);
                loadWeatherIcon(iconUrl);
            }

            // Cập nhật độ ẩm
            String humidity = String.format(Locale.getDefault(), "Độ ẩm: %d%%", 
                weather.getMain().getHumidity());
            remoteViews.setTextViewText(R.id.humidityText, humidity);

            // Cập nhật tốc độ gió
            String windSpeed = String.format(Locale.getDefault(), "Gió: %.1f m/s", 
                weather.getWind().getSpeed());
            remoteViews.setTextViewText(R.id.windSpeedText, windSpeed);

            // Cập nhật notification
            updateNotification();
        } catch (Exception e) {
            Log.e(TAG, "Error updating weather UI", e);
        }
    }

    private void loadWeatherIcon(String iconUrl) {
        Glide.with(this)
            .asBitmap()
            .load(iconUrl)
            .into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                    remoteViews.setImageViewBitmap(R.id.weatherIcon, bitmap);
                    updateNotification();
                }
            });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Weather Updates",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Hiển thị thông tin thời tiết trên màn hình khóa");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateNotification() {
        try {
            Notification notification = createNotification();
            notificationManager.notify(NOTIFICATION_ID, notification);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when showing notification", e);
            Toast.makeText(this, "Không thể hiển thị thông báo: " + e.getMessage(), 
                         Toast.LENGTH_LONG).show();
        }
    }

    private void updateNotification(String message) {
        remoteViews.setTextViewText(R.id.descriptionText, message);
        updateNotification();
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent();
        notificationIntent.setClassName(getPackageName(), getPackageName() + ".MainActivity");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
            notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "LockScreenService onDestroy");
        executorService.shutdownNow();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 