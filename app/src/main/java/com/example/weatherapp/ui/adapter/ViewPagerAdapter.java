package com.example.weatherapp.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.weatherapp.ui.WeatherFragment;
import com.example.weatherapp.ui.fragment.ClothingSuggestionFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return new ClothingSuggestionFragment();
        }
        return new WeatherFragment();
    }

    @Override
    public int getItemCount() {
        return 2; // Chúng ta có 2 tab
    }
} 