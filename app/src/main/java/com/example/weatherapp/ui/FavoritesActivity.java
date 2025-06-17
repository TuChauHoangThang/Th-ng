package com.example.weatherapp.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.weatherapp.R;

public class FavoritesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // Hiển thị nút back trên ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Danh sách yêu thích");
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.favorites_fragment_container, new FavoritesFragment())
                .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
} 