package com.example.weatherapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// Phản hồi từ API mã hóa địa lý là một mảng các địa điểm
public class GeocodingResponse extends java.util.ArrayList<GeocodingResponse.LocationResult> {

    public static class LocationResult {
        @SerializedName("name")
        private String name;
        @SerializedName("lat")
        private double lat;
        @SerializedName("lon")
        private double lon;
        @SerializedName("country")
        private String country;
        @SerializedName("state")
        private String state;

        public String getName() {
            return name;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public String getCountry() {
            return country;
        }

        public String getState() {
            return state;
        }
    }
} 