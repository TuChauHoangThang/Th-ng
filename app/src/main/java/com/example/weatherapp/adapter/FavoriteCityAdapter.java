package com.example.weatherapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.weatherapp.R;

import java.util.List;

public class FavoriteCityAdapter extends RecyclerView.Adapter<FavoriteCityAdapter.FavoriteCityViewHolder> {
    public interface FavoriteCityListener {
        void onCityClick(FavoriteCityWeather cityWeather);
    }

    public static class FavoriteCityWeather {
        public String cityName;
        public String temperature;
        public String description;
        public String iconUrl;
        public FavoriteCityWeather(String cityName, String temperature, String description, String iconUrl) {
            this.cityName = cityName;
            this.temperature = temperature;
            this.description = description;
            this.iconUrl = iconUrl;
        }
    }

    private List<FavoriteCityWeather> cities;
    private final FavoriteCityListener listener;

    public FavoriteCityAdapter(List<FavoriteCityWeather> cities, FavoriteCityListener listener) {
        this.cities = cities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavoriteCityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_city, parent, false);
        return new FavoriteCityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteCityViewHolder holder, int position) {
        FavoriteCityWeather city = cities.get(position);
        holder.cityNameTextView.setText(city.cityName);
        holder.temperatureTextView.setText(city.temperature);
        holder.descriptionTextView.setText(city.description);
        Glide.with(holder.itemView.getContext())
                .load(city.iconUrl)
                .error(R.drawable.ic_cloudy)
                .into(holder.weatherIcon);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCityClick(city);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cities.size();
    }

    public void setCities(List<FavoriteCityWeather> cities) {
        this.cities = cities;
        notifyDataSetChanged();
    }

    static class FavoriteCityViewHolder extends RecyclerView.ViewHolder {
        TextView cityNameTextView, temperatureTextView, descriptionTextView;
        ImageView weatherIcon;
        FavoriteCityViewHolder(View itemView) {
            super(itemView);
            cityNameTextView = itemView.findViewById(R.id.city_name);
            temperatureTextView = itemView.findViewById(R.id.temperature_text);
            descriptionTextView = itemView.findViewById(R.id.description_text);
            weatherIcon = itemView.findViewById(R.id.weather_icon);
        }
    }
} 