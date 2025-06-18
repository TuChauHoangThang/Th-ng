package com.example.weatherapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.weatherapp.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.weatherapp.api.RetrofitClient;
import com.example.weatherapp.model.GeocodingResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import java.util.ArrayList;
import android.widget.ImageButton;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.weatherapp.utils.VoiceRecognitionHelper;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private EditText searchEditText;
    private Button searchButton, btnShowWeather;
    private ProgressBar progressBar;
    private TextView statusText;
    private String selectedCityName = null;
    private LatLng selectedLatLng = null;
    private ImageButton micButton;
    private VoiceRecognitionHelper voiceRecognitionHelper;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Bản đồ Google Map");
        }

        // Ánh xạ views
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        btnShowWeather = findViewById(R.id.btnShowWeather);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        micButton = findViewById(R.id.micButton);

        // Ẩn các elements ban đầu
        btnShowWeather.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        statusText.setVisibility(View.GONE);

        // Xử lý sự kiện tìm kiếm
        searchButton.setOnClickListener(v -> performSearch());
        
        // Xử lý sự kiện nhấn Enter trên EditText
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                hideKeyboard();
                return true;
            }
            return false;
        });

        btnShowWeather.setOnClickListener(v -> {
            if (selectedCityName != null) {
                Intent resultIntent = new Intent();
                // Chuyển đổi tên thành phố để phù hợp với API thời tiết
                String weatherCityName = convertToWeatherCityName(selectedCityName);
                
                // Hiển thị thông báo về việc chuyển đổi nếu cần
                if (!selectedCityName.equals(weatherCityName)) {
                    Toast.makeText(this, "Chuyển đổi: " + selectedCityName + " → " + weatherCityName, Toast.LENGTH_SHORT).show();
                }
                
                resultIntent.putExtra("city_name", weatherCityName);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        // Khởi tạo VoiceRecognitionHelper
        voiceRecognitionHelper = new VoiceRecognitionHelper(this, result -> {
            searchEditText.setText(result);
            performSearch();
        });

        micButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                voiceRecognitionHelper.startVoiceRecognition();
            }
        });

        // Khởi tạo map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Không cần gọi destroy() nữa vì đã chuyển sang dùng Intent
    }

    private void performSearch() {
        String city = searchEditText.getText().toString().trim();
        
        // Validation input
        if (TextUtils.isEmpty(city)) {
            showError("Vui lòng nhập tên thành phố");
            return;
        }
        
        if (city.length() < 2) {
            showError("Tên thành phố phải có ít nhất 2 ký tự");
            return;
        }

        // Kiểm tra ký tự đặc biệt
        if (city.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            showError("Tên thành phố không được chứa ký tự đặc biệt");
            return;
        }

        // Ẩn keyboard và bắt đầu tìm kiếm
        hideKeyboard();
        searchCityOnMap(city);
    }

    private void searchCityOnMap(String cityName) {
        // Hiển thị loading
        showLoading(true);
        showStatus("Đang tìm kiếm: " + cityName + "...");
        
        String apiKey = com.example.weatherapp.BuildConfig.OPENWEATHER_API_KEY;
        Log.d("MapActivity", "Tìm kiếm thành phố: " + cityName);
        
        // Thử tìm kiếm với ",VN" trước
        RetrofitClient.getInstance().getOpenWeatherApiService().getCoordinatesByCityName(cityName + ",VN", 5, apiKey)
                .enqueue(new Callback<GeocodingResponse>() {
                    @Override
                    public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                        Log.d("MapActivity", "onResponse: " + response.code());
                        
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            // Tìm thấy kết quả
                            GeocodingResponse.LocationResult location = response.body().get(0);
                            Log.d("MapActivity", "Tìm thấy: " + location.getName() + " (" + location.getLat() + ", " + location.getLon() + ")");
                            
                            selectedLatLng = new LatLng(location.getLat(), location.getLon());
                            selectedCityName = location.getName();
                            
                            if (mMap != null) {
                                mMap.clear();
                                mMap.addMarker(new MarkerOptions().position(selectedLatLng).title(selectedCityName));
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 12));
                            }
                            
                            btnShowWeather.setVisibility(View.VISIBLE);
                            showSuccess(" Đã tìm thấy: " + selectedCityName);
                            
                        } else {
                            // Không tìm thấy với ",VN", thử tìm kiếm không có country code
                            searchWithoutCountryCode(cityName, apiKey);
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                        Log.e("MapActivity", "Lỗi kết nối: " + t.getMessage());
                        showError(" Lỗi kết nối: " + t.getMessage());
                        showLoading(false);
                    }
                });
    }

    private void searchWithoutCountryCode(String cityName, String apiKey) {
        Log.d("MapActivity", "Thử tìm kiếm không có country code: " + cityName);
        showStatus("Đang tìm kiếm toàn cầu: " + cityName + "...");
        
        RetrofitClient.getInstance().getOpenWeatherApiService().getCoordinatesByCityName(cityName, 5, apiKey)
                .enqueue(new Callback<GeocodingResponse>() {
                    @Override
                    public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                        Log.d("MapActivity", "onResponse (no country): " + response.code());
                        
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            // Tìm thấy kết quả
                            GeocodingResponse.LocationResult location = response.body().get(0);
                            Log.d("MapActivity", "Tìm thấy: " + location.getName() + " (" + location.getLat() + ", " + location.getLon() + ")");
                            
                            selectedLatLng = new LatLng(location.getLat(), location.getLon());
                            selectedCityName = location.getName();
                            
                            if (mMap != null) {
                                mMap.clear();
                                mMap.addMarker(new MarkerOptions().position(selectedLatLng).title(selectedCityName));
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 12));
                            }
                            
                            btnShowWeather.setVisibility(View.VISIBLE);
                            showSuccess(" Đã tìm thấy: " + selectedCityName);
                            
                        } else {
                            // Không tìm thấy kết quả, thử với các tên phổ biến
                            searchWithCommonNames(cityName, apiKey);
                        }
                        showLoading(false);
                    }
                    
                    @Override
                    public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                        Log.e("MapActivity", "Lỗi kết nối (no country): " + t.getMessage());
                        showError(" Lỗi kết nối: " + t.getMessage());
                        showLoading(false);
                    }
                });
    }

    private void searchWithCommonNames(String cityName, String apiKey) {
        // Thử với các tên phổ biến của thành phố
        String[] commonNames = getCommonCityNames(cityName);
        
        if (commonNames.length > 0) {
            Log.d("MapActivity", "Thử tìm kiếm với tên phổ biến: " + commonNames[0]);
            showStatus("Đang thử tên phổ biến: " + commonNames[0] + "...");
            
            RetrofitClient.getInstance().getOpenWeatherApiService().getCoordinatesByCityName(commonNames[0] + ",VN", 1, apiKey)
                    .enqueue(new Callback<GeocodingResponse>() {
                        @Override
                        public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                GeocodingResponse.LocationResult location = response.body().get(0);
                                selectedLatLng = new LatLng(location.getLat(), location.getLon());
                                selectedCityName = location.getName();
                                
                                if (mMap != null) {
                                    mMap.clear();
                                    mMap.addMarker(new MarkerOptions().position(selectedLatLng).title(selectedCityName));
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 12));
                                }
                                
                                btnShowWeather.setVisibility(View.VISIBLE);
                                showSuccess(" Đã tìm thấy: " + selectedCityName + " (từ " + cityName + ")");
                            } else {
                                showError(" Không tìm thấy thành phố: " + cityName + "\n Thử: " + String.join(", ", commonNames));
                            }
                            showLoading(false);
                        }
                        
                        @Override
                        public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                            showError(" Lỗi kết nối: " + t.getMessage());
                            showLoading(false);
                        }
                    });
        } else {
            showError(" Không tìm thấy thành phố: " + cityName + "\n💡 Gợi ý: Hà Nội, TP.HCM, Đà Nẵng, Huế, Nha Trang...");
            showLoading(false);
        }
    }

    private String[] getCommonCityNames(String cityName) {
        String lowerCityName = cityName.toLowerCase().trim();
        
        // Mapping các tên phổ biến
        switch (lowerCityName) {
            case "hcm":
            case "tp.hcm":
            case "tp hcm":
            case "ho chi minh":
            case "hochiminh":
                return new String[]{"Ho Chi Minh City"};
            case "hanoi":
            case "ha noi":
            case "hà nội":
                return new String[]{"Hanoi"};
            case "danang":
            case "da nang":
            case "đà nẵng":
                return new String[]{"Da Nang"};
            case "hue":
            case "huế":
                return new String[]{"Hue"};
            case "nha trang":
            case "nhatrang":
                return new String[]{"Nha Trang"};
            case "can tho":
            case "cantho":
            case "cần thơ":
                return new String[]{"Can Tho"};
            case "hai phong":
            case "haiphong":
            case "hải phòng":
                return new String[]{"Hai Phong"};
            case "vung tau":
            case "vungtau":
            case "vũng tàu":
                return new String[]{"Vung Tau"};
            case "dalat":
            case "da lat":
            case "đà lạt":
                return new String[]{"Da Lat"};
            default:
                return new String[]{};
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        searchButton.setEnabled(!show);
    }

    private void showStatus(String message) {
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
        statusText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
    }

    private void showSuccess(String message) {
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
        statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        showLoading(false);
        
        // Tự động ẩn thông báo thành công sau 3 giây
        statusText.postDelayed(() -> {
            if (statusText.getVisibility() == View.VISIBLE) {
                statusText.setVisibility(View.GONE);
            }
        }, 3000);
    }

    private void showError(String message) {
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
        statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        showLoading(false);
        btnShowWeather.setVisibility(View.GONE);
        
        // Tự động ẩn thông báo lỗi sau 5 giây
        statusText.postDelayed(() -> {
            if (statusText.getVisibility() == View.VISIBLE) {
                statusText.setVisibility(View.GONE);
            }
        }, 5000);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        }
        searchEditText.clearFocus();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Đặt vị trí mặc định (Hà Nội)
        LatLng defaultLocation = new LatLng(21.0285, 105.8542);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
        mMap.addMarker(new MarkerOptions().position(defaultLocation).title("Hà Nội"));
        
        // Cấu hình UI của map
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        
        // Xử lý sự kiện click trên map
        mMap.setOnMapClickListener(latLng -> {
            // Ẩn nút "Xem thông tin thời tiết" khi click vào map
            btnShowWeather.setVisibility(View.GONE);
            statusText.setVisibility(View.GONE);
        });
        
        // Xử lý sự kiện click trên marker
        mMap.setOnMarkerClickListener(marker -> {
            String markerTitle = marker.getTitle();
            if (markerTitle != null && !markerTitle.isEmpty()) {
                selectedCityName = markerTitle;
                btnShowWeather.setVisibility(View.VISIBLE);
                showSuccess(" Đã chọn: " + selectedCityName);
            }
            return true;
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private String convertToWeatherCityName(String cityName) {
        if (cityName == null || cityName.isEmpty()) {
            return cityName;
        }
        
        String originalName = cityName;
        String lowerCityName = cityName.toLowerCase().trim();
        
        // Chuyển đổi các tên thành phố có "City" thành tên phù hợp cho API thời tiết
        switch (lowerCityName) {
            case "ho chi minh city":
                cityName = "Ho Chi Minh";
                break;
            case "da nang":
                cityName = "Da Nang";
                break;
            case "can tho":
                cityName = "Can Tho";
                break;
            case "hai phong":
                cityName = "Hai Phong";
                break;
            case "vung tau":
                cityName = "Vung Tau";
                break;
            case "da lat":
                cityName = "Da Lat";
                break;
            case "nha trang":
                cityName = "Nha Trang";
                break;
            case "hue":
                cityName = "Hue";
                break;
            case "hanoi":
                cityName = "Hanoi";
                break;
            case "buon ma thuot":
            case "buon ma thuột":
                cityName = "Buon Ma Thuot";
                break;
            case "quy nhon":
            case "quy nhơn":
                cityName = "Quy Nhon";
                break;
            case "thanh hoa":
            case "thanh hóa":
                cityName = "Thanh Hoa";
                break;
            case "nghe an":
            case "nghệ an":
                cityName = "Nghe An";
                break;
            case "ha tinh":
            case "hà tĩnh":
                cityName = "Ha Tinh";
                break;
            case "quang binh":
            case "quảng bình":
                cityName = "Quang Binh";
                break;
            case "quang tri":
            case "quảng trị":
                cityName = "Quang Tri";
                break;
            case "thua thien hue":
            case "thừa thiên huế":
                cityName = "Thua Thien Hue";
                break;
            case "quang nam":
            case "quảng nam":
                cityName = "Quang Nam";
                break;
            case "quang ngai":
            case "quảng ngãi":
                cityName = "Quang Ngai";
                break;
            case "binh dinh":
            case "bình định":
                cityName = "Binh Dinh";
                break;
            case "phu yen":
            case "phú yên":
                cityName = "Phu Yen";
                break;
            case "khanh hoa":
            case "khánh hòa":
                cityName = "Khanh Hoa";
                break;
            case "ninh thuan":
            case "ninh thuận":
                cityName = "Ninh Thuan";
                break;
            case "binh thuan":
            case "bình thuận":
                cityName = "Binh Thuan";
                break;
            case "lam dong":
            case "lâm đồng":
                cityName = "Lam Dong";
                break;
            case "dong nai":
            case "đồng nai":
                cityName = "Dong Nai";
                break;
            case "ba ria vung tau":
            case "bà rịa vũng tàu":
                cityName = "Ba Ria Vung Tau";
                break;
            case "binh duong":
            case "bình dương":
                cityName = "Binh Duong";
                break;
            case "binh phuoc":
            case "bình phước":
                cityName = "Binh Phuoc";
                break;
            case "tay ninh":
            case "tây ninh":
                cityName = "Tay Ninh";
                break;
            case "long an":
                cityName = "Long An";
                break;
            case "tien giang":
            case "tiền giang":
                cityName = "Tien Giang";
                break;
            case "ben tre":
            case "bến tre":
                cityName = "Ben Tre";
                break;
            case "tra vinh":
            case "trà vinh":
                cityName = "Tra Vinh";
                break;
            case "vinh long":
            case "vĩnh long":
                cityName = "Vinh Long";
                break;
            case "dong thap":
            case "đồng tháp":
                cityName = "Dong Thap";
                break;
            case "an giang":
                cityName = "An Giang";
                break;
            case "kien giang":
            case "kiên giang":
                cityName = "Kien Giang";
                break;
            case "ca mau":
            case "cà mau":
                cityName = "Ca Mau";
                break;
            case "bac lieu":
            case "bạc liêu":
                cityName = "Bac Lieu";
                break;
            case "soc trang":
            case "sóc trăng":
                cityName = "Soc Trang";
                break;
            default:
                // Nếu không có trong danh sách, loại bỏ "City" nếu có
                if (lowerCityName.endsWith(" city")) {
                    cityName = originalName.substring(0, originalName.length() - 5).trim();
                }
                // Loại bỏ các từ không cần thiết khác
                else if (lowerCityName.endsWith(" province")) {
                    cityName = originalName.substring(0, originalName.length() - 9).trim();
                }
                else if (lowerCityName.endsWith(" district")) {
                    cityName = originalName.substring(0, originalName.length() - 9).trim();
                }
                break;
        }
        
        // Log để debug
        Log.d("MapActivity", "Chuyển đổi tên thành phố: '" + originalName + "' -> '" + cityName + "'");
        
        return cityName;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                voiceRecognitionHelper.startVoiceRecognition();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền micro để sử dụng chức năng này", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (voiceRecognitionHelper != null) {
            voiceRecognitionHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }
} 