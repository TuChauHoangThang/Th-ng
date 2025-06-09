package com.example.weatherapp.utils;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import com.example.weatherapp.R;

public class WeatherBackgroundManager {
    private final Context context;
    private final RelativeLayout mainLayout;

    public WeatherBackgroundManager(Context context, RelativeLayout mainLayout) {
        this.context = context;
        this.mainLayout = mainLayout;
    }

    public void updateBackground(String weatherCondition) {
        int backgroundResId;
        
        // Chuyển đổi điều kiện thời tiết thành chữ thường để so sánh
        String condition = weatherCondition.toLowerCase();
        
        if (condition.contains("rain") || condition.contains("drizzle")) {
            backgroundResId = R.drawable.rain;
        } else if (condition.contains("snow")) {
            backgroundResId = R.drawable.snow;
        } else if (condition.contains("thunderstorm") || condition.contains("storm") 
                || condition.contains("squall") || condition.contains("tornado")) {
            backgroundResId = R.drawable.storm;
        } else if (condition.contains("overcast") || condition.contains("broken clouds")) {
            // Sử dụng background mưa cho mây đen u ám
            backgroundResId = R.drawable.dark_cloud;
        } else if (condition.contains("cloud")) {
            backgroundResId = R.drawable.cloudy;
        } else if (condition.contains("mist") || condition.contains("fog") 
                || condition.contains("haze")) {
            // Sử dụng background sương mù mới
            backgroundResId = R.drawable.mist;
        } else if (condition.contains("dust") || condition.contains("smoke") 
                || condition.contains("sand") || condition.contains("ash")) {
            // Sử dụng background bụi mới cho tất cả các điều kiện liên quan đến bụi
            backgroundResId = R.drawable.dust;
        } else {
            // Mặc định là thời tiết đẹp
            backgroundResId = R.drawable.clean;
        }

        mainLayout.setBackgroundResource(backgroundResId);
    }
} 