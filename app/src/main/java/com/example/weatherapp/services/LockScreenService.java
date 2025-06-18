package com.example.weatherapp.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;
import com.example.weatherapp.R;
import com.example.weatherapp.api.RetrofitClient;
import com.example.weatherapp.model.CurrentWeatherResponse;
import com.example.weatherapp.utils.LocationHelper;
import com.example.weatherapp.BuildConfig;
import com.example.weatherapp.data.AppDatabase;
import com.example.weatherapp.data.entity.BackgroundImage;
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
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;
import android.location.Geocoder;
import android.location.Address;
import android.location.Location;
import androidx.lifecycle.LifecycleService;
import com.example.weatherapp.viewmodel.WeatherViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;
import java.util.List;
import android.content.SharedPreferences;
import com.example.weatherapp.data.FavoriteCity;
import com.example.weatherapp.data.FavoriteCityDao;
import com.example.weatherapp.model.GeocodingResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LockScreenService extends LifecycleService {
    private static final String TAG = "LockScreenService";
    private static final String CHANNEL_ID = "LockScreenChannel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_WEATHER_UPDATED = "com.example.weatherapp.WEATHER_UPDATED";
    public static final String EXTRA_WEATHER = "weather_data";

    private LocationHelper locationHelper;
    private ExecutorService executorService;
    private NotificationManager notificationManager;
    private RemoteViews remoteViews;
    private AppDatabase database;
    private BroadcastReceiver weatherReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LockScreenService onCreate");

        locationHelper = new LocationHelper(this);
        executorService = Executors.newSingleThreadExecutor();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        remoteViews = new RemoteViews(getPackageName(), R.layout.lock_screen_widget);
        database = AppDatabase.getInstance(this);

        createNotificationChannel();
        initializeNotificationLayout();
        startForeground(NOTIFICATION_ID, createNotification());

        // Bắt đầu cập nhật thời tiết ngay khi service khởi động
        updateWeather();

        // Đăng ký BroadcastReceiver để nhận cập nhật thời tiết
        weatherReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_WEATHER_UPDATED.equals(intent.getAction())) {
                    CurrentWeatherResponse weather = intent.getParcelableExtra(EXTRA_WEATHER);
                    if (weather != null) {
                        updateWeatherUI(weather);
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(weatherReceiver, new IntentFilter(ACTION_WEATHER_UPDATED));

        // Lên lịch cập nhật thời tiết định kỳ (15 phút)
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        updateWeather();
                        Thread.sleep(15 * 60 * 1000); // 15 phút
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });

        // Sau khi cập nhật thời tiết hiện tại, kiểm tra các thành phố yêu thích
        executorService.execute(this::checkFavoriteCitiesWeatherAlert);
    }

    private void initializeNotificationLayout() {
        remoteViews.setTextViewText(R.id.locationText, "Đang tải...");
        remoteViews.setTextViewText(R.id.timestampText, getCurrentTime());
        remoteViews.setTextViewText(R.id.temperatureText, "--°C");
        remoteViews.setTextViewText(R.id.descriptionText, "Đang cập nhật...");
        remoteViews.setTextViewText(R.id.humidityText, "Độ ẩm: --%");
        remoteViews.setTextViewText(R.id.windSpeedText, "Gió: -- m/s");
        
        // Set background mặc định
        loadDefaultBackground();
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }

    private void loadDefaultBackground() {
        try {
            Log.d(TAG, "Loading default background...");
            Glide.with(this)
                .asBitmap()
                .load("https://res.cloudinary.com/dijswwhab/image/upload/v1749738562/clean_nwe6vw.jpg")
                .centerCrop()
                .override(640, 320)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                        if (bitmap != null) {
                            try {
                                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 640, 320, true);
                                remoteViews.setImageViewBitmap(R.id.backgroundImage, scaledBitmap);
                                Log.d(TAG, "Default background set successfully");
                                updateNotification();
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing default background", e);
                            }
                        } else {
                            Log.e(TAG, "Bitmap null when loading default background");
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Log.e(TAG, "Failed to load default background");
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error loading default background", e);
        }
    }

    private void updateWeather() {
        if (!checkLocationPermission()) {
            Log.e(TAG, "Location permission not granted");
            updateNotificationWithError("Cần cấp quyền truy cập vị trí");
            return;
        }

        // Tạo location request với độ chính xác cao nhất
        LocationRequest locationRequest = LocationRequest.create()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setInterval(5000)
            .setFastestInterval(3000)
            .setMaxWaitTime(2000);

        locationHelper.getCurrentLocation(locationRequest)
            .addOnSuccessListener(location -> {
                if (location != null) {
                    Log.d(TAG, "Got location: " + location.getLatitude() + ", " + location.getLongitude());
                    verifyAndFetchWeather(location);
                } else {
                    Log.e(TAG, "Location is null");
                    updateNotificationWithError("Không thể lấy vị trí hiện tại");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting location", e);
                updateNotificationWithError("Lỗi khi lấy vị trí");
            });
    }

    private void verifyAndFetchWeather(Location location) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String cityName = address.getLocality(); // Thành phố
                String adminArea = address.getAdminArea(); // Tỉnh/thành phố lớn
                String displayName;

                // Xử lý đặc biệt cho TP.HCM
                if (adminArea != null && (adminArea.contains("Hồ Chí Minh") || adminArea.contains("Ho Chi Minh"))) {
                    displayName = "TP.HCM";
                } else if (cityName != null && !cityName.isEmpty() && adminArea != null && !adminArea.isEmpty()) {
                    displayName = cityName + ", " + adminArea;
                } else if (adminArea != null && !adminArea.isEmpty()) {
                    displayName = adminArea;
                } else if (cityName != null && !cityName.isEmpty()) {
                    displayName = cityName;
                } else {
                    displayName = "Không xác định được vị trí";
                }

                Log.d(TAG, "Verified location: " + displayName);
                
                // Lưu tên thành phố
                getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("last_city", displayName)
                    .apply();

                // Gọi API thời tiết với tọa độ
                fetchWeatherData(location.getLatitude(), location.getLongitude());
            } else {
                Log.e(TAG, "No address found");
                fetchWeatherData(location.getLatitude(), location.getLongitude());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting address", e);
            fetchWeatherData(location.getLatitude(), location.getLongitude());
        }
    }

    private void updateNotificationWithError(String error) {
        remoteViews.setTextViewText(R.id.descriptionText, error);
        updateNotification();
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void fetchWeatherData(double lat, double lon) {
        Log.d(TAG, "Fetching weather data for: " + lat + ", " + lon);
        RetrofitClient.getInstance().getOpenWeatherApiService()
            .getCurrentWeather(lat, lon, BuildConfig.OPENWEATHER_API_KEY, "metric", "vi")
            .enqueue(new Callback<CurrentWeatherResponse>() {
                @Override
                public void onResponse(Call<CurrentWeatherResponse> call, Response<CurrentWeatherResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Weather data received successfully");
                        CurrentWeatherResponse weather = response.body();
                        updateWeatherUI(weather);
                    } else {
                        Log.e(TAG, "Weather API call failed: " + response.code());
                        updateNotificationWithError("Không thể cập nhật thời tiết");
                    }
                }

                @Override
                public void onFailure(Call<CurrentWeatherResponse> call, Throwable t) {
                    Log.e(TAG, "Weather API call failed", t);
                    updateNotificationWithError("Lỗi kết nối mạng");
                }
            });
    }

    private void updateWeatherUI(CurrentWeatherResponse weather) {
        try {
            Log.d(TAG, "Updating weather UI with data: " + weather.toString());
            
            // Cập nhật vị trí
            String locationName = weather.getName();
            if (locationName.contains("Ho Chi Minh") || 
                locationName.contains("Hồ Chí Minh") || 
                locationName.equals("Thành phố Hồ Chí Minh")) {
                locationName = "TP.HCM";
            } else {
                String savedCity = getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
                    .getString("last_city", null);
                if (savedCity != null) {
                    locationName = savedCity;
                }
            }
            remoteViews.setTextViewText(R.id.locationText, locationName);

            // Cập nhật thời gian
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            remoteViews.setTextViewText(R.id.timestampText, time);

            // Cập nhật nhiệt độ
            String temp = String.format(Locale.getDefault(), "%.0f°C", weather.getMain().getTemp());
            remoteViews.setTextViewText(R.id.temperatureText, temp);

            // Cập nhật mô tả và background
            if (weather.getWeather() != null && !weather.getWeather().isEmpty()) {
                String description = weather.getWeather().get(0).getDescription();
                remoteViews.setTextViewText(R.id.descriptionText, description);

                // Cập nhật icon thời tiết
                String iconCode = weather.getWeather().get(0).getIcon();
                String iconUrl = String.format("https://openweathermap.org/img/wn/%s@2x.png", iconCode);
                loadWeatherIcon(iconUrl);

                // Cập nhật background dựa trên điều kiện thời tiết
                updateBackgroundBasedOnWeather(description.toLowerCase());
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
            // Kiểm tra và gửi cảnh báo nguy hiểm
            checkAndSendWeatherAlert(weather);
            
            Log.d(TAG, "Weather UI updated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error updating weather UI", e);
            updateNotificationWithError("Lỗi khi cập nhật giao diện");
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

    private void updateBackgroundBasedOnWeather(String condition) {
        String backgroundName = findMatchingBackgroundName(condition.toLowerCase());
        if (backgroundName != null) {
            executorService.execute(() -> {
                try {
                    BackgroundImage background = database.backgroundImageDao().getBackgroundImageByName(backgroundName);
                    if (background != null) {
                        loadBackgroundImage(background.getImgURL());
                    } else {
                        loadDefaultBackground();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting background from database", e);
                    loadDefaultBackground();
                }
            });
        } else {
            loadDefaultBackground();
        }
    }

    private String findMatchingBackgroundName(String condition) {
        // Danh sách các từ khóa và background tương ứng - giống như trong WeatherBackgroundManager
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
        return "Clean"; // Default background
    }

    private void loadBackgroundImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.e(TAG, "Invalid image URL");
            loadDefaultBackground();
            return;
        }

        Log.d(TAG, "Loading background image: " + imageUrl);

        try {
            Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .centerCrop()
                .override(640, 320)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                        if (bitmap != null) {
                            try {
                                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 640, 320, true);
                                remoteViews.setImageViewBitmap(R.id.backgroundImage, scaledBitmap);
                                Log.d(TAG, "Background image set successfully");
                                updateNotification();
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing background image", e);
                                loadDefaultBackground();
                            }
                        } else {
                            Log.e(TAG, "Bitmap null when loading background image");
                            loadDefaultBackground();
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Log.e(TAG, "Failed to load background image");
                        loadDefaultBackground();
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error loading background image", e);
            loadDefaultBackground();
        }
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

    private boolean isAlertEnabled(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences("WeatherAlertPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean(key, true);
    }

    // Kiểm tra cảnh báo cho các thành phố yêu thích
    private void checkFavoriteCitiesWeatherAlert() {
        try {
            String userId = getCurrentUserId();
            if (userId == null) return;
            FavoriteCityDao dao = database.favoriteCityDao();
            List<FavoriteCity> favorites = dao.getFavoriteCitiesForUser(userId).getValue();
            if (favorites == null) return;

            // Lấy tên thành phố hiện tại từ SharedPreferences
            String currentCity = getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
                .getString("current_location_name", null);

            for (FavoriteCity city : favorites) {
                if (currentCity != null && city.getCityName().equalsIgnoreCase(currentCity)) {
                    continue; // Bỏ qua nếu trùng với vị trí hiện tại
                }
                fetchWeatherAndCheckAlertForCity(city.getCityName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi kiểm tra cảnh báo cho thành phố yêu thích", e);
        }
    }

    private void fetchWeatherAndCheckAlertForCity(String cityName) {
        String apiKey = BuildConfig.OPENWEATHER_API_KEY;
        RetrofitClient.getInstance().getOpenWeatherApiService().getCoordinatesByCityName(cityName + ",VN", 1, apiKey)
            .enqueue(new Callback<GeocodingResponse>() {
                @Override
                public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        GeocodingResponse.LocationResult location = response.body().get(0);
                        fetchCurrentWeatherForAlert(location.getLat(), location.getLon(), cityName);
                    }
                }
                @Override
                public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                    Log.e(TAG, "Lỗi lấy tọa độ thành phố yêu thích: " + cityName, t);
                }
            });
    }

    private void fetchCurrentWeatherForAlert(double lat, double lon, String cityName) {
        String apiKey = BuildConfig.OPENWEATHER_API_KEY;
        RetrofitClient.getInstance().getOpenWeatherApiService().getCurrentWeather(lat, lon, apiKey, "metric", "vi")
            .enqueue(new Callback<CurrentWeatherResponse>() {
                @Override
                public void onResponse(Call<CurrentWeatherResponse> call, Response<CurrentWeatherResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        checkAndSendWeatherAlert(response.body(), cityName);
                    }
                }
                @Override
                public void onFailure(Call<CurrentWeatherResponse> call, Throwable t) {
                    Log.e(TAG, "Lỗi lấy thời tiết thành phố yêu thích", t);
                }
            });
    }

    // Lấy userId hiện tại (nếu có)
    private String getCurrentUserId() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            return user != null ? user.getUid() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Cập nhật điều kiện cảnh báo mới và cho phép truyền tên thành phố
    private void checkAndSendWeatherAlert(CurrentWeatherResponse weather, String cityName) {
        boolean isDanger = false;
        StringBuilder alertMsg = new StringBuilder();
        double temp = weather.getMain().getTemp();
        double wind = weather.getWind().getSpeed();
        String desc = weather.getWeather().get(0).getDescription().toLowerCase();
        boolean alertTemp = isAlertEnabled(this, "alert_temp");
        boolean alertRain = isAlertEnabled(this, "alert_rain");
        boolean alertWind = isAlertEnabled(this, "alert_wind");

        if (alertTemp && temp > 30) {
            isDanger = true;
            alertMsg.append("Nhiệt độ cao: ").append((int) temp).append("°C.\n");
        }
        if (alertRain && desc.contains("mưa")) {
            isDanger = true;
            alertMsg.append("Có mưa: ").append(weather.getWeather().get(0).getDescription()).append("\n");
        }
        if (alertWind && wind >= 10) {
            isDanger = true;
            alertMsg.append("Gió mạnh: ").append(String.format("%.1f", wind)).append(" m/s.\n");
        }

        if (isDanger) {
            sendDangerNotification(alertMsg.toString(), cityName);
        }
    }

    // Sửa hàm cũ để gọi checkAndSendWeatherAlert(weather, cityName) với cityName là vị trí hiện tại
    private void checkAndSendWeatherAlert(CurrentWeatherResponse weather) {
        // Đọc tên địa điểm hiện tại từ SharedPreferences
        String cityName = getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
            .getString("current_location_name", weather.getName());
        checkAndSendWeatherAlert(weather, cityName);
    }

    private void sendDangerNotification(String message, String cityName) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "WeatherAlertChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId,
                "Cảnh báo thời tiết nguy hiểm",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo cảnh báo thời tiết nguy hiểm");
            manager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Cảnh báo thời tiết: " + cityName)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);
        manager.notify((int) System.currentTimeMillis(), builder.build());
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
        super.onBind(intent);
        return null;
    }
} 