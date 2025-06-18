package com.example.weatherapp.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.weatherapp.R;
import com.example.weatherapp.model.CurrentWeatherResponse;
import com.example.weatherapp.model.ForecastResponse;
import com.example.weatherapp.ui.adapter.ForecastAdapter;
import com.example.weatherapp.viewmodel.WeatherViewModel;
import com.example.weatherapp.viewmodel.FavoriteCityViewModel;
import com.example.weatherapp.data.FavoriteCity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.weatherapp.viewmodel.FavoriteCityViewModelFactory;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.content.Context;
import android.content.SharedPreferences;

public class WeatherFragment extends Fragment {

    private WeatherViewModel weatherViewModel;
    private FavoriteCityViewModel favoriteCityViewModel;
    private TextView locationText, dateText, temperatureText, descriptionText;
    private ImageView weatherIcon;
    private RecyclerView forecastRecView;
    private ForecastAdapter forecastAdapter;
    private ImageButton favoriteButton;
    private String currentCityName;
    private boolean isFavorite = false;
    private TextView humidityText, windSpeedText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weather, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Ánh xạ View
        locationText = view.findViewById(R.id.locationText);
        dateText = view.findViewById(R.id.dateText);
        weatherIcon = view.findViewById(R.id.weatherIcon);
        temperatureText = view.findViewById(R.id.temperatureText);
        descriptionText = view.findViewById(R.id.descriptionText);
        forecastRecView = view.findViewById(R.id.forecastRecView);
        favoriteButton = view.findViewById(R.id.favoriteButton);
        humidityText = view.findViewById(R.id.humidityText);
        windSpeedText = view.findViewById(R.id.windSpeedText);

        // Cấu hình RecyclerView
        forecastAdapter = new ForecastAdapter(new ArrayList<>());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        forecastRecView.setLayoutManager(layoutManager);
        forecastRecView.setHasFixedSize(true);
        forecastRecView.setNestedScrollingEnabled(false);
        forecastRecView.setAdapter(forecastAdapter);

        // Lấy ViewModel
        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        // Khởi tạo FavoriteCityViewModel
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user != null ? user.getUid() : "";
        FavoriteCityViewModelFactory factory = new FavoriteCityViewModelFactory(requireActivity().getApplication(), userId);
        favoriteCityViewModel = new ViewModelProvider(this, factory).get(FavoriteCityViewModel.class);

        // Quan sát dữ liệu thời tiết
        weatherViewModel.getCurrentWeather().observe(getViewLifecycleOwner(), this::updateCurrentWeatherUI);
        weatherViewModel.getForecast().observe(getViewLifecycleOwner(), this::updateForecastUI);
        
        updateCurrentDate();

        // Theo dõi thay đổi danh sách yêu thích để cập nhật icon
        favoriteCityViewModel.getAllFavoriteCities().observe(getViewLifecycleOwner(), new Observer<List<FavoriteCity>>() {
            @Override
            public void onChanged(List<FavoriteCity> favoriteCities) {
                updateFavoriteIcon();
            }
        });

        // Thêm nút yêu thích
        favoriteButton.setOnClickListener(v -> {
            if (currentCityName != null) {
                if (getActivity() instanceof MainActivity) {
                    if (!isFavorite) {
                        ((MainActivity) getActivity()).addToFavorites(currentCityName);
                    } else {
                        // Xóa khỏi danh sách yêu thích
                        for (FavoriteCity city : favoriteCityViewModel.getAllFavoriteCities().getValue()) {
                            if (city.getCityName().equalsIgnoreCase(currentCityName)) {
                                favoriteCityViewModel.delete(city);
                                break;
                            }
                        }
                    }
                }
            }
        });

        // Nếu chưa có city nào được chọn, tự động lấy vị trí hiện tại
        if (currentCityName == null) {
            requestWeatherByCurrentLocation();
        }
    }

    private void updateCurrentWeatherUI(CurrentWeatherResponse weatherData) {
        if (weatherData == null) return;
        currentCityName = weatherData.getName();
        locationText.setText(currentCityName);
        temperatureText.setText(String.format(Locale.getDefault(), "%.0f°C", weatherData.getMain().getTemp()));
        descriptionText.setText(weatherData.getWeather().get(0).getDescription());
        String iconCode = weatherData.getWeather().get(0).getIcon();
        String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
        Glide.with(this).load(iconUrl).error(R.drawable.ic_cloudy).into(weatherIcon);
        humidityText.setText(String.format(Locale.getDefault(), "Độ ẩm: %d%%", weatherData.getMain().getHumidity()));
        windSpeedText.setText(String.format(Locale.getDefault(), "Gió: %.1f m/s", weatherData.getWind().getSpeed()));
        updateFavoriteIcon();
    }

    private void updateForecastUI(ForecastResponse forecastData) {
        if (forecastData == null) return;
        forecastAdapter.updateData(forecastData.getList());
    }
    
    private void updateCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d/M", new Locale("vi"));
        dateText.setText(dateFormat.format(new Date()));
    }

    private void updateFavoriteIcon() {
        isFavorite = false;
        List<FavoriteCity> favoriteCities = favoriteCityViewModel.getAllFavoriteCities().getValue();
        if (favoriteCities != null && currentCityName != null) {
            for (FavoriteCity city : favoriteCities) {
                if (city.getCityName().equalsIgnoreCase(currentCityName)) {
                    isFavorite = true;
                    break;
                }
            }
        }
        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    private void requestWeatherByCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Xin quyền vị trí nếu chưa có
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                getCityNameFromLocation(location);
            } else {
                locationText.setText("Không lấy được vị trí");
            }
        });
    }

    private void getCityNameFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            java.util.List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String cityName = address.getLocality(); // Thành phố
                String adminArea = address.getAdminArea(); // Tỉnh/thành phố lớn
                String displayName;
                if (cityName != null && !cityName.isEmpty() && adminArea != null && !adminArea.isEmpty()) {
                    displayName = cityName + ", " + adminArea;
                } else if (adminArea != null && !adminArea.isEmpty()) {
                    displayName = adminArea;
                } else if (cityName != null && !cityName.isEmpty()) {
                    displayName = cityName;
                } else {
                    displayName = "Không xác định được vị trí";
                }
                locationText.setText(displayName);
                // Lưu tên địa điểm hiện tại vào SharedPreferences
                SharedPreferences prefs = requireContext().getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE);
                prefs.edit().putString("current_location_name", displayName).apply();
                // Gọi API thời tiết với cityName (ưu tiên city, fallback adminArea)
                String queryCity = cityName != null && !cityName.isEmpty() ? cityName : adminArea;
                if (queryCity != null && !queryCity.isEmpty()) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).searchCity(queryCity);
                    }
                }
            } else {
                locationText.setText("Không xác định được thành phố");
            }
        } catch (Exception e) {
            locationText.setText("Lỗi lấy tên thành phố");
        }
    }
} 