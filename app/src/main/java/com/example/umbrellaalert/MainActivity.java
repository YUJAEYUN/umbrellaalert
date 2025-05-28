package com.example.umbrellaalert;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.umbrellaalert.data.api.WeatherApiService;
import com.example.umbrellaalert.data.model.WeatherApiResponse;
import com.example.umbrellaalert.util.CoordinateConverter;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private WeatherApiService weatherApiService;
    private TextView tvWeatherInfo;
    private Button btnTestApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        initViews();
        initWeatherService();
        setupClickListeners();
    }
    
    private void initViews() {
        tvWeatherInfo = findViewById(R.id.tv_weather_info);
        btnTestApi = findViewById(R.id.btn_test_api);
    }
    
    private void initWeatherService() {
        weatherApiService = new WeatherApiService(this);
    }
    
    private void setupClickListeners() {
        btnTestApi.setOnClickListener(v -> testWeatherApi());
    }
    
    private void testWeatherApi() {
        // 서울 좌표로 테스트 (위도: 37.5665, 경도: 126.9780)
        double latitude = 37.5665;
        double longitude = 126.9780;
        
        // 위도/경도를 기상청 격자 좌표로 변환
        CoordinateConverter.GridCoordinate gridCoord = CoordinateConverter.convertToGrid(latitude, longitude);
        
        Log.d(TAG, "Seoul coordinates - Lat: " + latitude + ", Lon: " + longitude);
        Log.d(TAG, "Grid coordinates - nx: " + gridCoord.nx + ", ny: " + gridCoord.ny);
        
        tvWeatherInfo.setText("날씨 정보를 가져오는 중...");
        
        weatherApiService.getUltraShortTermForecast(gridCoord.nx, gridCoord.ny, new WeatherApiService.WeatherApiCallback() {
            @Override
            public void onSuccess(WeatherApiResponse response) {
                runOnUiThread(() -> {
                    if (response != null && response.getResponse() != null && 
                        response.getResponse().getBody() != null && 
                        response.getResponse().getBody().getItems() != null &&
                        response.getResponse().getBody().getItems().getItem() != null) {
                        
                        StringBuilder weatherInfo = new StringBuilder();
                        weatherInfo.append("서울 날씨 정보:\n\n");
                        
                        for (WeatherApiResponse.Item item : response.getResponse().getBody().getItems().getItem()) {
                            String category = item.getCategory();
                            String value = item.getFcstValue();
                            
                            switch (category) {
                                case "T1H":
                                    weatherInfo.append("기온: ").append(value).append("°C\n");
                                    break;
                                case "RN1":
                                    weatherInfo.append("1시간 강수량: ").append(value).append("mm\n");
                                    break;
                                case "SKY":
                                    String skyCondition = getSkyCondition(value);
                                    weatherInfo.append("하늘상태: ").append(skyCondition).append("\n");
                                    break;
                                case "REH":
                                    weatherInfo.append("습도: ").append(value).append("%\n");
                                    break;
                                case "PTY":
                                    String precipitationType = getPrecipitationType(value);
                                    weatherInfo.append("강수형태: ").append(precipitationType).append("\n");
                                    break;
                                case "WSD":
                                    weatherInfo.append("풍속: ").append(value).append("m/s\n");
                                    break;
                            }
                        }
                        
                        tvWeatherInfo.setText(weatherInfo.toString());
                        Toast.makeText(MainActivity.this, "날씨 정보를 성공적으로 가져왔습니다!", Toast.LENGTH_SHORT).show();
                    } else {
                        tvWeatherInfo.setText("날씨 정보가 없습니다.");
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    tvWeatherInfo.setText("오류: " + error);
                    Toast.makeText(MainActivity.this, "날씨 정보를 가져오는데 실패했습니다: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private String getSkyCondition(String code) {
        switch (code) {
            case "1": return "맑음";
            case "3": return "구름많음";
            case "4": return "흐림";
            default: return "알 수 없음";
        }
    }
    
    private String getPrecipitationType(String code) {
        switch (code) {
            case "0": return "없음";
            case "1": return "비";
            case "2": return "비/눈";
            case "3": return "눈";
            case "4": return "소나기";
            default: return "알 수 없음";
        }
    }
}