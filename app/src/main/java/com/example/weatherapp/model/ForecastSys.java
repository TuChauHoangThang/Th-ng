package com.example.weatherapp.model;
import com.google.gson.annotations.SerializedName;

public class ForecastSys {
    @SerializedName("pod")
    private String pod; // 'd' (day) or 'n' (night)
    public String getPod() { return pod; }
} 