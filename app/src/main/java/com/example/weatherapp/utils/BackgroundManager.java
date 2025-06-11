package com.example.weatherapp.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackgroundManager {
    private static final String TAG = "BackgroundManager";
    private static final String BACKGROUNDS_COLLECTION = "backgrounds";
    private final FirebaseFirestore db;
    private final FirebaseStorageManager storageManager;

    public BackgroundManager(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.storageManager = new FirebaseStorageManager(context);
    }

    public void saveBackground(String weatherCondition, Uri imageUri, OnSaveListener listener) {
        storageManager.uploadBackground(weatherCondition, imageUri, new FirebaseStorageManager.OnUploadListener() {
            @Override
            public void onSuccess(String downloadUrl) {
                // Lưu thông tin ảnh vào Firestore
                Map<String, Object> background = new HashMap<>();
                background.put("weatherCondition", weatherCondition);
                background.put("imageUrl", downloadUrl);
                background.put("timestamp", System.currentTimeMillis());

                db.collection(BACKGROUNDS_COLLECTION)
                        .add(background)
                        .addOnSuccessListener(documentReference -> {
                            Log.d(TAG, "Lưu thông tin ảnh thành công");
                            if (listener != null) {
                                listener.onSuccess(documentReference.getId());
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Lưu thông tin ảnh thất bại: " + e.getMessage());
                            if (listener != null) {
                                listener.onFailure(e);
                            }
                        });
            }

            @Override
            public void onFailure(Exception e) {
                if (listener != null) {
                    listener.onFailure(e);
                }
            }
        });
    }

    public void getBackgroundsForCondition(String weatherCondition, OnGetBackgroundsListener listener) {
        db.collection(BACKGROUNDS_COLLECTION)
                .whereEqualTo("weatherCondition", weatherCondition)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Background> backgrounds = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Background background = new Background(
                                document.getId(),
                                document.getString("weatherCondition"),
                                document.getString("imageUrl"),
                                document.getLong("timestamp")
                        );
                        backgrounds.add(background);
                    }
                    if (listener != null) {
                        listener.onSuccess(backgrounds);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lấy danh sách ảnh thất bại: " + e.getMessage());
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    public void loadBackground(String imageUrl, ImageView imageView) {
        storageManager.loadBackground(imageUrl, imageView);
    }

    public void deleteBackground(String documentId, String imageUrl, OnDeleteListener listener) {
        // Xóa ảnh từ Storage
        storageManager.deleteBackground(imageUrl, new FirebaseStorageManager.OnDeleteListener() {
            @Override
            public void onSuccess() {
                // Xóa thông tin từ Firestore
                db.collection(BACKGROUNDS_COLLECTION)
                        .document(documentId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Xóa thông tin ảnh thành công");
                            if (listener != null) {
                                listener.onSuccess();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Xóa thông tin ảnh thất bại: " + e.getMessage());
                            if (listener != null) {
                                listener.onFailure(e);
                            }
                        });
            }

            @Override
            public void onFailure(Exception e) {
                if (listener != null) {
                    listener.onFailure(e);
                }
            }
        });
    }

    public interface OnSaveListener {
        void onSuccess(String documentId);
        void onFailure(Exception e);
    }

    public interface OnGetBackgroundsListener {
        void onSuccess(List<Background> backgrounds);
        void onFailure(Exception e);
    }

    public interface OnDeleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static class Background {
        private String id;
        private String weatherCondition;
        private String imageUrl;
        private long timestamp;

        public Background(String id, String weatherCondition, String imageUrl, long timestamp) {
            this.id = id;
            this.weatherCondition = weatherCondition;
            this.imageUrl = imageUrl;
            this.timestamp = timestamp;
        }

        public String getId() {
            return id;
        }

        public String getWeatherCondition() {
            return weatherCondition;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
} 