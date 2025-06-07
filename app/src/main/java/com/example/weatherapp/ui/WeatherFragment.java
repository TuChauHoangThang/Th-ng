package com.example.weatherapp.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.weatherapp.R;
import com.example.weatherapp.model.CurrentWeatherResponse;
import com.example.weatherapp.model.ForecastResponse;
import com.example.weatherapp.ui.adapter.ForecastAdapter;
import com.example.weatherapp.viewmodel.WeatherViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class WeatherFragment extends Fragment implements OnMapReadyCallback {

    private WeatherViewModel weatherViewModel;
    private TextView locationText, dateText, temperatureText, descriptionText;
    private ImageView weatherIcon;
    private RecyclerView forecastRecView;
    private ForecastAdapter forecastAdapter;
    private GoogleMap gMap;

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

        // Cấu hình RecyclerView
        forecastAdapter = new ForecastAdapter(new ArrayList<>());
        forecastRecView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        forecastRecView.setAdapter(forecastAdapter);

        // Khởi tạo bản đồ
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Lấy ViewModel
        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        // Quan sát dữ liệu thời tiết
        weatherViewModel.getCurrentWeather().observe(getViewLifecycleOwner(), this::updateCurrentWeatherUI);
        weatherViewModel.getForecast().observe(getViewLifecycleOwner(), this::updateForecastUI);
        
        updateCurrentDate();
    }

    private void updateCurrentWeatherUI(CurrentWeatherResponse weatherData) {
        if (weatherData == null) return;
        locationText.setText(weatherData.getName());
        temperatureText.setText(String.format(Locale.getDefault(), "%.0f°C", weatherData.getMain().getTemp()));
        descriptionText.setText(weatherData.getWeather().get(0).getDescription());
        String iconCode = weatherData.getWeather().get(0).getIcon();
        String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
        Glide.with(this).load(iconUrl).error(R.drawable.ic_cloudy).into(weatherIcon);
        updateMap(weatherData.getCoord().getLat(), weatherData.getCoord().getLon(), weatherData.getName());
    }

    private void updateForecastUI(ForecastResponse forecastData) {
        if (forecastData == null) return;
        forecastAdapter.updateData(forecastData.getList());
    }
    
    private void updateCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d/M", new Locale("vi"));
        dateText.setText(dateFormat.format(new Date()));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        gMap.getUiSettings().setZoomControlsEnabled(true);
        gMap.getUiSettings().setCompassEnabled(true);
        LatLng vietnamCenter = new LatLng(16.047079, 108.206230);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vietnamCenter, 5f));
    }

    private void updateMap(double lat, double lon, String cityName) {
        if (gMap == null) return;
        LatLng location = new LatLng(lat, lon);
        gMap.clear();
        gMap.addMarker(new MarkerOptions().position(location).title(cityName));
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12));
    }
} 