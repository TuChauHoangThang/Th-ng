package com.example.weatherapp.utils;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VoiceRecognitionHelper {
    private final Activity activity;
    private final OnVoiceResultListener listener;
    private final Map<String, String> cityCorrections;
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    public interface OnVoiceResultListener {
        void onVoiceResult(String result);
    }

    public VoiceRecognitionHelper(Activity activity, OnVoiceResultListener listener) {
        this.activity = activity;
        this.listener = listener;
        
        // Khởi tạo map sửa lỗi tên thành phố
        cityCorrections = new HashMap<>();
        initializeCityCorrections();
    }

    private void initializeCityCorrections() {
        // Thêm các mapping phổ biến
        cityCorrections.put("be quiet", "biên hòa");
        cityCorrections.put("bien hoa", "biên hòa");
        cityCorrections.put("be in ho", "biên hòa");
        cityCorrections.put("ho chi minh", "thành phố hồ chí minh");
        cityCorrections.put("hanoi", "hà nội");
        cityCorrections.put("ha noi", "hà nội");
        cityCorrections.put("da nang", "đà nẵng");
        cityCorrections.put("nha trang", "nha trang");
        cityCorrections.put("can tho", "cần thơ");
        cityCorrections.put("vung tau", "vũng tàu");
        cityCorrections.put("quy nhon", "quy nhơn");
        cityCorrections.put("buon ma thuot", "buôn ma thuột");
        cityCorrections.put("dong nai", "đồng nai");
        cityCorrections.put("ba ria", "bà rịa");
        cityCorrections.put("phu quoc", "phú quốc");
        cityCorrections.put("hai phong", "hải phòng");
        cityCorrections.put("long xuyen", "long xuyên");
    }

    public void startVoiceRecognition() {
        try {
            // Tạo Intent cho voice recognition
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN");
            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói tên thành phố...");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            
            // Bắt đầu activity nhận diện giọng nói
            activity.startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(activity, "Thiết bị của bạn không hỗ trợ nhận diện giọng nói", Toast.LENGTH_SHORT).show();
        }
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String rawResult = matches.get(0).toLowerCase().trim();
                String correctedResult = correctCityName(rawResult);
                listener.onVoiceResult(correctedResult);
            }
        }
    }

    private String correctCityName(String rawInput) {
        rawInput = rawInput.toLowerCase().trim();
        rawInput = rawInput.replaceAll("\\s+", " ");
        rawInput = removeVietnameseAccents(rawInput);
        
        // Kiểm tra trong map corrections
        for (Map.Entry<String, String> entry : cityCorrections.entrySet()) {
            String key = removeVietnameseAccents(entry.getKey().toLowerCase());
            if (rawInput.contains(key)) {
                return entry.getValue();
            }
        }
        
        // Nếu không tìm thấy trong map, thử phục hồi dấu
        return restoreVietnameseAccents(rawInput);
    }
    
    private String removeVietnameseAccents(String str) {
        String[] accents = {
            "à", "á", "ả", "ã", "ạ", "ă", "ằ", "ắ", "ẳ", "ẵ", "ặ", "â", "ầ", "ấ", "ẩ", "ẫ", "ậ",
            "è", "é", "ẻ", "ẽ", "ẹ", "ê", "ề", "ế", "ể", "ễ", "ệ",
            "ì", "í", "ỉ", "ĩ", "ị",
            "ò", "ó", "ỏ", "õ", "ọ", "ô", "ồ", "ố", "ổ", "ỗ", "ộ", "ơ", "ờ", "ớ", "ở", "ỡ", "ợ",
            "ù", "ú", "ủ", "ũ", "ụ", "ư", "ừ", "ứ", "ử", "ữ", "ự",
            "ỳ", "ý", "ỷ", "ỹ", "ỵ",
            "đ"
        };
        String[] nonAccents = {
            "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a",
            "e", "e", "e", "e", "e", "e", "e", "e", "e", "e", "e",
            "i", "i", "i", "i", "i",
            "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o",
            "u", "u", "u", "u", "u", "u", "u", "u", "u", "u", "u",
            "y", "y", "y", "y", "y",
            "d"
        };
        
        for (int i = 0; i < accents.length; i++) {
            str = str.replace(accents[i], nonAccents[i]);
        }
        return str;
    }
    
    private String restoreVietnameseAccents(String input) {
        // Map các từ thông dụng trong tên thành phố
        Map<String, String> commonWords = new HashMap<>();
        commonWords.put("thanh pho", "thành phố");
        commonWords.put("ha noi", "hà nội");
        commonWords.put("ho chi minh", "hồ chí minh");
        commonWords.put("da nang", "đà nẵng");
        commonWords.put("bien hoa", "biên hòa");
        commonWords.put("nha trang", "nha trang");
        commonWords.put("can tho", "cần thơ");
        commonWords.put("vung tau", "vũng tàu");
        commonWords.put("quy nhon", "quy nhơn");
        commonWords.put("buon ma thuot", "buôn ma thuột");
        commonWords.put("dong nai", "đồng nai");
        commonWords.put("ba ria", "bà rịa");
        commonWords.put("phu quoc", "phú quốc");
        commonWords.put("hai phong", "hải phòng");
        commonWords.put("long xuyen", "long xuyên");
        
        String result = input;
        for (Map.Entry<String, String> entry : commonWords.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            result = result.replace(key, value);
        }
        
        return result;
    }
} 