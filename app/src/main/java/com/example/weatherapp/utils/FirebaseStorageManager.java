package com.example.weatherapp.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.UUID;

public class FirebaseStorageManager {
    private static final String TAG = "FirebaseStorageManager";
    private static final String BACKGROUNDS_FOLDER = "backgrounds";
    private final FirebaseStorage storage;
    private final Context context;

    public FirebaseStorageManager(Context context) {
        this.context = context;
        this.storage = FirebaseStorage.getInstance();
    }

    public void uploadBackground(String weatherCondition, Uri imageUri, OnUploadListener listener) {
        try {
            // Tạo tên file duy nhất
            String fileName = weatherCondition + "_" + UUID.randomUUID().toString() + ".jpg";
            StorageReference backgroundRef = storage.getReference()
                    .child(BACKGROUNDS_FOLDER)
                    .child(fileName);

            // Upload file
            UploadTask uploadTask = backgroundRef.putFile(imageUri);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Lấy URL download
                backgroundRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Upload thành công: " + uri.toString());
                    if (listener != null) {
                        listener.onSuccess(uri.toString());
                    }
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Upload thất bại: " + e.getMessage());
                if (listener != null) {
                    listener.onFailure(e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi upload: " + e.getMessage());
            if (listener != null) {
                listener.onFailure(e);
            }
        }
    }

    public void loadBackground(String imageUrl, ImageView imageView) {
        try {
            Glide.with(context)
                    .load(imageUrl)
                    .centerCrop()
                    .into(imageView);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi tải ảnh: " + e.getMessage());
        }
    }

    public void deleteBackground(String imageUrl, OnDeleteListener listener) {
        try {
            StorageReference ref = storage.getReferenceFromUrl(imageUrl);
            ref.delete().addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Xóa ảnh thành công");
                if (listener != null) {
                    listener.onSuccess();
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Xóa ảnh thất bại: " + e.getMessage());
                if (listener != null) {
                    listener.onFailure(e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi xóa ảnh: " + e.getMessage());
            if (listener != null) {
                listener.onFailure(e);
            }
        }
    }

    public interface OnUploadListener {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }

    public interface OnDeleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }
} 