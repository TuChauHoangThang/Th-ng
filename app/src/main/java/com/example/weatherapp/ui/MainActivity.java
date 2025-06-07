package com.example.weatherapp.ui;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.weatherapp.BuildConfig;
import com.example.weatherapp.R;
import com.example.weatherapp.api.RetrofitClient;
import com.example.weatherapp.model.CurrentWeatherResponse;
import com.example.weatherapp.model.ForecastResponse;
import com.example.weatherapp.model.GeocodingResponse;
import com.example.weatherapp.ui.adapter.ViewPagerAdapter;
import com.example.weatherapp.utils.LocationHelper;
import com.example.weatherapp.viewmodel.WeatherViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private LocationHelper locationHelper;
    private EditText searchInputEditText;
    private WeatherViewModel weatherViewModel;
    private final String OPENWEATHER_API_KEY = BuildConfig.OPENWEATHER_API_KEY;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo ViewModel
        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        // Ánh xạ View
        searchInputEditText = findViewById(R.id.searchInputEditText);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        // Cấu hình ViewPager và Adapter
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Kết nối TabLayout với ViewPager
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("Thời tiết");
                    } else {
                        tab.setText("Gợi ý");
                    }
                }).attach();

        // Khởi tạo các thành phần khác
        locationHelper = new LocationHelper(this);
        setupPermissionLauncher();
        checkLocationPermission();
        setupSearchInput();
    }

    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    Boolean fineLocationGranted = permissions.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    if (fineLocationGranted != null && fineLocationGranted) {
                        getLocationAndFetchWeather();
                    } else {
                        Toast.makeText(this, "Cần quyền truy cập vị trí để hiển thị thời tiết.", Toast.LENGTH_SHORT).show();
                        fetchWeatherForDefaultLocation();
                    }
                });
    }

    private void checkLocationPermission() {
        if (locationHelper.checkLocationPermission()) {
            getLocationAndFetchWeather();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void setupSearchInput() {
        searchInputEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String searchQuery = v.getText().toString().trim();
                if (!searchQuery.isEmpty()) {
                    fetchWeatherByCityName(searchQuery);
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchInputEditText.getWindowToken(), 0);
        searchInputEditText.clearFocus();
    }

    private void fetchWeatherByCityName(String cityName) {
        String query = cityName + ",VN";
        RetrofitClient.getInstance().getOpenWeatherApiService().getCoordinatesByCityName(query, 1, OPENWEATHER_API_KEY)
                .enqueue(new Callback<GeocodingResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<GeocodingResponse> call, @NonNull Response<GeocodingResponse> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            GeocodingResponse.LocationResult location = response.body().get(0);
                            fetchWeatherData(location.getLat(), location.getLon());
                        } else {
                            Toast.makeText(MainActivity.this, "Không tìm thấy thành phố.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<GeocodingResponse> call, @NonNull Throwable t) {
                        Toast.makeText(MainActivity.this, "Lỗi kết nối khi tìm kiếm.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getLocationAndFetchWeather() {
        locationHelper.getCurrentLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                fetchWeatherData(location.getLatitude(), location.getLongitude());
            } else {
                fetchWeatherForDefaultLocation();
            }
        }).addOnFailureListener(this, e -> fetchWeatherForDefaultLocation());
    }

    private void fetchWeatherData(double lat, double lon) {
        // Lấy thời tiết hiện tại
        RetrofitClient.getInstance().getOpenWeatherApiService().getCurrentWeather(lat, lon, OPENWEATHER_API_KEY, "metric", "vi")
                .enqueue(new Callback<CurrentWeatherResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<CurrentWeatherResponse> call, @NonNull Response<CurrentWeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            weatherViewModel.setCurrentWeather(response.body());
                        } else {
                            Toast.makeText(MainActivity.this, "Không tải được dữ liệu hiện tại.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<CurrentWeatherResponse> call, @NonNull Throwable t) {
                        Toast.makeText(MainActivity.this, "Lỗi kết nối.", Toast.LENGTH_SHORT).show();
                    }
                });

        // Lấy dự báo
        RetrofitClient.getInstance().getOpenWeatherApiService().getForecast(lat, lon, OPENWEATHER_API_KEY, "metric", "vi")
                .enqueue(new Callback<ForecastResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ForecastResponse> call, @NonNull Response<ForecastResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            weatherViewModel.setForecast(response.body());
                        } else {
                            Toast.makeText(MainActivity.this, "Không tải được dữ liệu dự báo.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ForecastResponse> call, @NonNull Throwable t) {
                        Toast.makeText(MainActivity.this, "Lỗi kết nối.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchWeatherForDefaultLocation() {
        double defaultLat = 21.0285; // Hà Nội
        double defaultLon = 105.8542;
        fetchWeatherData(defaultLat, defaultLon);
        Toast.makeText(this, "Không thể lấy vị trí. Hiển thị thời tiết cho Hà Nội.", Toast.LENGTH_LONG).show();
    }
}
