package com.example.weatherapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.weatherapp.utils.BackgroundManager;
import com.google.android.material.button.MaterialButton;

public class BackgroundUploadActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String[] WEATHER_CONDITIONS = {
            "sunny", "cloudy", "rainy", "snowy", "stormy", "foggy", "windy"
    };

    private BackgroundManager backgroundManager;
    private ImageView previewImage;
    private AutoCompleteTextView weatherConditionInput;
    private MaterialButton selectImageButton;
    private MaterialButton uploadButton;
    private ProgressBar progressBar;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    previewImage.setImageURI(selectedImageUri);
                    uploadButton.setEnabled(true);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_upload);

        backgroundManager = new BackgroundManager(this);
        initializeViews();
        setupWeatherConditionDropdown();
        setupClickListeners();
    }

    private void initializeViews() {
        previewImage = findViewById(R.id.previewImage);
        weatherConditionInput = findViewById(R.id.weatherConditionInput);
        selectImageButton = findViewById(R.id.selectImageButton);
        uploadButton = findViewById(R.id.uploadButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupWeatherConditionDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                WEATHER_CONDITIONS
        );
        weatherConditionInput.setAdapter(adapter);
    }

    private void setupClickListeners() {
        selectImageButton.setOnClickListener(v -> checkPermissionAndPickImage());
        uploadButton.setOnClickListener(v -> uploadImage());
    }

    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadImage() {
        String weatherCondition = weatherConditionInput.getText().toString().trim();
        if (weatherCondition.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn điều kiện thời tiết", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        backgroundManager.saveBackground(weatherCondition, selectedImageUri, new BackgroundManager.OnSaveListener() {
            @Override
            public void onSuccess(String documentId) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(BackgroundUploadActivity.this, "Tải lên thành công", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(BackgroundUploadActivity.this, "Tải lên thất bại: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        selectImageButton.setEnabled(!show);
        uploadButton.setEnabled(!show);
        weatherConditionInput.setEnabled(!show);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Cần quyền truy cập bộ nhớ để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }
} 