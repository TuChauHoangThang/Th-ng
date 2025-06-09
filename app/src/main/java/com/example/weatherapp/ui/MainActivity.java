package com.example.weatherapp.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.weatherapp.BuildConfig;
import com.example.weatherapp.R;
import com.example.weatherapp.api.RetrofitClient;
import com.example.weatherapp.data.DatabaseHelper;
import com.example.weatherapp.model.CurrentWeatherResponse;
import com.example.weatherapp.model.ForecastResponse;
import com.example.weatherapp.model.GeocodingResponse;
import com.example.weatherapp.ui.adapter.ViewPagerAdapter;
import com.example.weatherapp.utils.LocationHelper;
import com.example.weatherapp.utils.WeatherBackgroundManager;
import com.example.weatherapp.viewmodel.WeatherViewModel;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private LocationHelper locationHelper;
    private EditText searchInputEditText;
    private WeatherViewModel weatherViewModel;
    private final String OPENWEATHER_API_KEY = BuildConfig.OPENWEATHER_API_KEY;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;
    private WeatherBackgroundManager weatherBackgroundManager;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton menuButton;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo DatabaseHelper và in thông tin database
        DatabaseHelper.getInstance(this).printDatabaseInfo();

        // Khởi tạo ViewModel
        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        // Ánh xạ View
        searchInputEditText = findViewById(R.id.searchInputEditText);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        menuButton = findViewById(R.id.menuButton);
        RelativeLayout mainLayout = findViewById(R.id.main);

        // Khởi tạo WeatherBackgroundManager
        weatherBackgroundManager = new WeatherBackgroundManager(this, mainLayout);

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

        // Cấu hình Navigation Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        mAuth = FirebaseAuth.getInstance();
        setupAuthListener();

        // Khởi tạo các thành phần khác
        locationHelper = new LocationHelper(this);
        setupPermissionLauncher();
        checkLocationPermission();
        setupSearchInput();

        // Quan sát thời tiết để cập nhật background
        weatherViewModel.getCurrentWeather().observe(this, weather -> {
            if (weather != null && weather.getWeather() != null && !weather.getWeather().isEmpty()) {
                String weatherCondition = weather.getWeather().get(0).getDescription();
                weatherBackgroundManager.updateBackground(weatherCondition);
            }
        });
    }

    private void setupAuthListener() {
        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            updateUI(user);
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void updateUI(FirebaseUser user) {
        MenuItem loginItem = navigationView.getMenu().findItem(R.id.nav_login);
        MenuItem registerItem = navigationView.getMenu().findItem(R.id.nav_register);
        MenuItem logoutItem = navigationView.getMenu().findItem(R.id.nav_logout);
        TextView userEmailTextView = navigationView.getHeaderView(0).findViewById(R.id.textView);

        if (user != null) {
            loginItem.setVisible(false);
            registerItem.setVisible(false);
            logoutItem.setVisible(true);
            userEmailTextView.setText(user.getEmail());
        } else {
            loginItem.setVisible(true);
            registerItem.setVisible(true);
            logoutItem.setVisible(false);
            userEmailTextView.setText("Chưa đăng nhập");
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_login) {
            startActivity(new Intent(this, LoginActivity.class));
        } else if (id == R.id.nav_register) {
            startActivity(new Intent(this, RegisterActivity.class));
        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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
