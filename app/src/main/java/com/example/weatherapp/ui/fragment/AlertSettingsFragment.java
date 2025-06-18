package com.example.weatherapp.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.weatherapp.R;

public class AlertSettingsFragment extends Fragment {
    private static final String TAG = "AlertSettingsFragment";
    private static final String PREFS_NAME = "WeatherAlertPrefs";
    private static final String ALERT_TEMP = "alert_temp";
    private static final String ALERT_RAIN = "alert_rain";
    private static final String ALERT_WIND = "alert_wind";

    private CheckBox cbTemp, cbRain, cbWind;
    private SharedPreferences prefs;
    private Button btnBackHome;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alert_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "AlertSettingsFragment opened");
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        cbTemp = view.findViewById(R.id.cb_temp);
        cbRain = view.findViewById(R.id.cb_rain);
        cbWind = view.findViewById(R.id.cb_wind);
        btnBackHome = view.findViewById(R.id.btn_back_home);

        cbTemp.setChecked(prefs.getBoolean(ALERT_TEMP, true));
        cbRain.setChecked(prefs.getBoolean(ALERT_RAIN, true));
        cbWind.setChecked(prefs.getBoolean(ALERT_WIND, true));

        cbTemp.setOnCheckedChangeListener(this::onCheckedChanged);
        cbRain.setOnCheckedChangeListener(this::onCheckedChanged);
        cbWind.setOnCheckedChangeListener(this::onCheckedChanged);

        btnBackHome.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    private void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences.Editor editor = prefs.edit();
        String type = "";
        if (buttonView.getId() == R.id.cb_temp) {
            editor.putBoolean(ALERT_TEMP, isChecked);
            type = "Nhiệt độ cao";
        } else if (buttonView.getId() == R.id.cb_rain) {
            editor.putBoolean(ALERT_RAIN, isChecked);
            type = "Mưa lớn/Bão/Giông";
        } else if (buttonView.getId() == R.id.cb_wind) {
            editor.putBoolean(ALERT_WIND, isChecked);
            type = "Gió mạnh";
        }
        editor.apply();
        Log.d(TAG, "Cảnh báo: " + type + " -> " + isChecked);
    }
} 