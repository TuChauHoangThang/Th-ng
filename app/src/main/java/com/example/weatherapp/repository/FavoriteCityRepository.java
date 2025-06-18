package com.example.weatherapp.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.weatherapp.data.AppDatabase;
import com.example.weatherapp.data.FavoriteCity;
import com.example.weatherapp.data.FavoriteCityDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.util.HashMap;
import java.util.Map;

public class FavoriteCityRepository {
    private FavoriteCityDao favoriteCityDao;
    private LiveData<List<FavoriteCity>> allFavoriteCities;
    private ExecutorService executorService;

    public FavoriteCityRepository(Application application, String userId) {
        AppDatabase db = AppDatabase.getInstance(application);
        favoriteCityDao = db.favoriteCityDao();
        allFavoriteCities = favoriteCityDao.getFavoriteCitiesForUser(userId);
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<FavoriteCity>> getAllFavoriteCities() {
        return allFavoriteCities;
    }

    public void insert(FavoriteCity favoriteCity) {
        executorService.execute(() -> {
            favoriteCityDao.insert(favoriteCity);
            syncFavoriteToFirestore(favoriteCity);
        });
    }

    public void delete(FavoriteCity favoriteCity) {
        executorService.execute(() -> {
            favoriteCityDao.delete(favoriteCity);
            deleteFavoriteFromFirestore(favoriteCity);
        });
    }

    public void checkAndAddFavorite(String cityName, String userId) {
        executorService.execute(() -> {
            FavoriteCity existingCity = favoriteCityDao.getFavoriteCity(cityName, userId);
            if (existingCity == null) {
                FavoriteCity newFavorite = new FavoriteCity(cityName, userId);
                favoriteCityDao.insert(newFavorite);
                syncFavoriteToFirestore(newFavorite);
            }
        });
    }

    // --- FIRESTORE SYNC ---
    private void syncFavoriteToFirestore(FavoriteCity favoriteCity) {
        String userId = favoriteCity.getUserId();
        if (userId == null || userId.isEmpty()) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("cityName", favoriteCity.getCityName());
        data.put("timestamp", System.currentTimeMillis());
        db.collection("users")
          .document(userId)
          .collection("favorites")
          .document(favoriteCity.getCityName())
          .set(data, SetOptions.merge());
    }

    private void deleteFavoriteFromFirestore(FavoriteCity favoriteCity) {
        String userId = favoriteCity.getUserId();
        if (userId == null || userId.isEmpty()) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
          .document(userId)
          .collection("favorites")
          .document(favoriteCity.getCityName())
          .delete();
    }

    // Đồng bộ favorites từ Firestore về local Room
    public void syncFavoritesFromFirestore(String userId) {
        if (userId == null || userId.isEmpty()) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
          .document(userId)
          .collection("favorites")
          .get()
          .addOnSuccessListener(queryDocumentSnapshots -> {
              executorService.execute(() -> {
                  // Xóa local cũ
                  List<FavoriteCity> oldList = favoriteCityDao.getFavoriteCitiesForUser(userId).getValue();
                  if (oldList != null) {
                      for (FavoriteCity city : oldList) {
                          favoriteCityDao.delete(city);
                      }
                  }
                  // Lưu danh sách mới
                  for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                      String cityName = doc.getString("cityName");
                      if (cityName != null && !cityName.isEmpty()) {
                          FavoriteCity city = new FavoriteCity(cityName, userId);
                          favoriteCityDao.insert(city);
                      }
                  }
              });
          });
    }
} 