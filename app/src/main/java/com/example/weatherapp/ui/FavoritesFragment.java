package com.example.weatherapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.weatherapp.R;
import com.example.weatherapp.adapter.FavoriteCityAdapter;
import com.example.weatherapp.data.FavoriteCity;
import com.example.weatherapp.viewmodel.FavoriteCityViewModel;
import com.example.weatherapp.viewmodel.FavoriteCityViewModelFactory;
import com.example.weatherapp.api.RetrofitClient;
import com.example.weatherapp.model.CurrentWeatherResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;
import com.example.weatherapp.BuildConfig;

public class FavoritesFragment extends Fragment implements FavoriteCityAdapter.FavoriteCityListener {
    private FavoriteCityViewModel viewModel;
    private FavoriteCityAdapter adapter;
    private final List<FavoriteCityAdapter.FavoriteCityWeather> cityWeatherList = new ArrayList<>();
    private final List<String> loadingCities = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.favorites_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FavoriteCityAdapter(cityWeatherList, this);
        recyclerView.setAdapter(adapter);

        // Lấy userId từ Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user != null ? user.getUid() : "";
        FavoriteCityViewModelFactory factory = new FavoriteCityViewModelFactory(requireActivity().getApplication(), userId);
        viewModel = new ViewModelProvider(this, factory).get(FavoriteCityViewModel.class);

        // ĐỒNG BỘ FIRESTORE VỀ LOCAL
        if (!userId.isEmpty()) {
            viewModel.syncFavoritesFromFirestore(userId);
        }

        // Quan sát danh sách thành phố yêu thích
        viewModel.getAllFavoriteCities().observe(getViewLifecycleOwner(), cities -> {
            cityWeatherList.clear();
            loadingCities.clear();
            if (cities != null) {
                for (FavoriteCity city : cities) {
                    loadingCities.add(city.getCityName());
                    fetchWeatherForCity(city.getCityName());
                }
            }
            adapter.setCities(new ArrayList<>(cityWeatherList));
        });
    }

    private void fetchWeatherForCity(String cityName) {
        String apiKey = BuildConfig.OPENWEATHER_API_KEY;
        RetrofitClient.getInstance().getOpenWeatherApiService().getCoordinatesByCityName(cityName + ",VN", 1, apiKey)
                .enqueue(new Callback<com.example.weatherapp.model.GeocodingResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<com.example.weatherapp.model.GeocodingResponse> call, @NonNull Response<com.example.weatherapp.model.GeocodingResponse> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            com.example.weatherapp.model.GeocodingResponse.LocationResult location = response.body().get(0);
                            fetchCurrentWeather(location.getLat(), location.getLon(), cityName);
                        } else {
                            addCityWeather(cityName, "--", "Không tìm thấy", null);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<com.example.weatherapp.model.GeocodingResponse> call, @NonNull Throwable t) {
                        addCityWeather(cityName, "--", "Lỗi mạng", null);
                    }
                });
    }

    private void fetchCurrentWeather(double lat, double lon, String cityName) {
        String apiKey = BuildConfig.OPENWEATHER_API_KEY;
        RetrofitClient.getInstance().getOpenWeatherApiService().getCurrentWeather(lat, lon, apiKey, "metric", "vi")
                .enqueue(new Callback<CurrentWeatherResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<CurrentWeatherResponse> call, @NonNull Response<CurrentWeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            CurrentWeatherResponse weather = response.body();
                            String temp = String.format("%.0f°C", weather.getMain().getTemp());
                            String desc = weather.getWeather().get(0).getDescription();
                            String icon = "https://openweathermap.org/img/wn/" + weather.getWeather().get(0).getIcon() + "@2x.png";
                            addCityWeather(cityName, temp, desc, icon);
                        } else {
                            addCityWeather(cityName, "--", "Không có dữ liệu", null);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<CurrentWeatherResponse> call, @NonNull Throwable t) {
                        addCityWeather(cityName, "--", "Lỗi mạng", null);
                    }
                });
    }

    private void addCityWeather(String cityName, String temp, String desc, String iconUrl) {
        cityWeatherList.add(new FavoriteCityAdapter.FavoriteCityWeather(cityName, temp, desc, iconUrl));
        loadingCities.remove(cityName);
        if (loadingCities.isEmpty()) {
            adapter.setCities(new ArrayList<>(cityWeatherList));
        }
    }

    @Override
    public void onCityClick(FavoriteCityAdapter.FavoriteCityWeather city) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("city_name", city.cityName);
        requireActivity().setResult(Activity.RESULT_OK, resultIntent);
        requireActivity().finish();
    }
} 