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
} 