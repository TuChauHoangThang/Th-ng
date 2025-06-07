package com.example.weatherapp.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.weatherapp.R;
import com.example.weatherapp.model.CurrentWeatherResponse;
import com.example.weatherapp.utils.ClothingSuggestion;
import com.example.weatherapp.viewmodel.WeatherViewModel;
import java.util.Locale;

public class ClothingSuggestionFragment extends Fragment {
    private WeatherViewModel weatherViewModel;
    private TextView currentTempText;
    private TextView currentWeatherText;
    private TextView humidityText;
    private TextView windSpeedText;
    private TextView clothingSuggestionText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // We need to rename the layout file to match this
        return inflater.inflate(R.layout.fragment_clothing_suggestion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các view
        currentTempText = view.findViewById(R.id.currentTempText);
        currentWeatherText = view.findViewById(R.id.currentWeatherText);
        humidityText = view.findViewById(R.id.humidityText);
        windSpeedText = view.findViewById(R.id.windSpeedText);
        clothingSuggestionText = view.findViewById(R.id.clothingSuggestionText);

        // Lấy ViewModel từ Activity
        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        // Quan sát dữ liệu thời tiết từ ViewModel
        weatherViewModel.getCurrentWeather().observe(getViewLifecycleOwner(), this::updateUI);
    }
    
    private void updateUI(CurrentWeatherResponse weatherData) {
        if (weatherData == null) {
            clothingSuggestionText.setText("Vui lòng tìm kiếm một thành phố hoặc cho phép truy cập vị trí để nhận gợi ý.");
            return;
        }

        try {
            // Cập nhật thông tin thời tiết hiện tại
            double temp = weatherData.getMain().getTemp();
            currentTempText.setText(String.format(Locale.getDefault(), "%.0f°C", temp));

            if (weatherData.getWeather() != null && !weatherData.getWeather().isEmpty()) {
                currentWeatherText.setText(weatherData.getWeather().get(0).getDescription());
            }

            humidityText.setText(String.format(Locale.getDefault(), "Độ ẩm: %d%%", weatherData.getMain().getHumidity()));
            // Chuyển đổi m/s sang km/h để nhất quán với ClothingSuggestion
            double windSpeedKmh = weatherData.getWind().getSpeed() * 3.6;
            windSpeedText.setText(String.format(Locale.getDefault(), "Gió: %.1f km/h", windSpeedKmh));

            // Lấy thông tin để tạo gợi ý
            String weatherCondition = weatherData.getWeather().get(0).getMain(); // Dùng .getMain()
            
            // Gọi lớp tiện ích để lấy gợi ý
            String suggestion = ClothingSuggestion.getClothingSuggestion(
                    temp,
                    weatherCondition,
                    weatherData.getMain().getHumidity(),
                    windSpeedKmh
            );
            clothingSuggestionText.setText(suggestion);
        } catch (Exception e) {
            clothingSuggestionText.setText("Lỗi khi cập nhật dữ liệu.");
        }
    }
} 