package com.example.weatherapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageCaptureManager {
    private static final String TAG = "ImageCaptureManager";
    private final Context context;
    private final File imageDir;

    public ImageCaptureManager(Context context) {
        this.context = context;
        this.imageDir = new File(context.getFilesDir(), "images");
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
    }

    public File saveImage(Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: " + imageUri);
                return null;
            }

            // Đọc ảnh và resize để giảm kích thước
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: " + imageUri);
                return null;
            }

            // Resize ảnh xuống kích thước phù hợp (ví dụ: max 1024px)
            Bitmap resizedBitmap = resizeBitmap(originalBitmap, 1024);
            originalBitmap.recycle();

            // Tạo tên file với timestamp
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File imageFile = new File(imageDir, "IMG_" + timeStamp + ".jpg");

            // Lưu ảnh đã resize
            FileOutputStream fos = new FileOutputStream(imageFile);
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();
            resizedBitmap.recycle();

            Log.d(TAG, "Image saved successfully: " + imageFile.getAbsolutePath());
            return imageFile;
        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }

    public void deleteOldImages() {
        if (!imageDir.exists()) return;

        File[] files = imageDir.listFiles();
        if (files == null) return;

        // Xóa ảnh cũ hơn 24 giờ
        long cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        for (File file : files) {
            if (file.lastModified() < cutoff) {
                if (file.delete()) {
                    Log.d(TAG, "Deleted old image: " + file.getName());
                }
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    public File[] getImages() {
        if (!imageDir.exists()) return new File[0];
        
        File[] files = imageDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
            
        return files != null ? files : new File[0];
    }
} 