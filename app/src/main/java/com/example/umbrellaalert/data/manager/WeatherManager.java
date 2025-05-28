package com.example.umbrellaalert.data.manager;

import android.content.Context;
import android.util.Log;

import com.example.umbrellaalert.data.api.WeatherApiService;
import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.data.database.WeatherDao;
import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.data.model.WeatherApiResponse;
import com.example.umbrellaalert.util.CoordinateConverter;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WeatherManager {

    private static final String TAG = "WeatherManager";
    private static final long CACHE_DURATION = TimeUnit.HOURS.toMillis(1); // 1시간 캐시

    private static WeatherManager instance;
    private WeatherDao weatherDao;
    private WeatherApiService apiService;

    // 싱글톤 패턴
    public static synchronized WeatherManager getInstance(Context context) {
        if (instance == null) {
            instance = new WeatherManager(context.getApplicationContext());
        }
        return instance;
    }

    private WeatherManager(Context context) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        weatherDao = new WeatherDao(dbHelper);
        apiService = new WeatherApiService(context);

        // 오래된 데이터 정리
        cleanupOldData();
    }

    // 현재 위치의 날씨 가져오기
    public void getCurrentWeather(double latitude, double longitude, WeatherCallback callback) {
        // 위치 문자열 생성
        String locationStr = latitude + "," + longitude;

        // 캐시된 데이터 확인
        Weather cachedWeather = weatherDao.getLatestWeatherByLocation(locationStr);

        // 캐시가 유효한지 확인
        if (cachedWeather != null &&
                System.currentTimeMillis() - cachedWeather.getTimestamp() < CACHE_DURATION) {
            Log.d(TAG, "Using cached weather data");
            callback.onSuccess(cachedWeather);
            return;
        }

        // 새로운 데이터 가져오기
        // 위도/경도를 기상청 격자 좌표로 변환
        CoordinateConverter.GridCoordinate gridCoord = CoordinateConverter.convertToGrid(latitude, longitude);
        int nx = gridCoord.nx;
        int ny = gridCoord.ny;

        // 초단기실황 API 호출 (현재 날씨)
        apiService.getUltraShortTermObservation(nx, ny, new WeatherApiService.WeatherApiCallback() {
            @Override
            public void onSuccess(WeatherApiResponse observationResponse) {
                // 초단기예보 API도 호출하여 강수형태 정보 보완
                apiService.getUltraShortTermForecast(nx, ny, new WeatherApiService.WeatherApiCallback() {
                    @Override
                    public void onSuccess(WeatherApiResponse forecastResponse) {
                        try {
                            // 실황과 예보 데이터를 조합하여 Weather 객체 생성
                            Weather freshWeather = combineWeatherData(observationResponse, forecastResponse, latitude, longitude);
                            
                            // 데이터베이스에 저장
                            long id = weatherDao.insertWeather(freshWeather);
                            freshWeather.setId((int) id);

                            Log.d(TAG, "Retrieved fresh weather data (observation + forecast)");
                            callback.onSuccess(freshWeather);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting combined weather data", e);
                            // 실황 데이터만으로라도 변환 시도
                            try {
                                Weather freshWeather = convertApiResponseToWeather(observationResponse, latitude, longitude);
                                long id = weatherDao.insertWeather(freshWeather);
                                freshWeather.setId((int) id);
                                callback.onSuccess(freshWeather);
                            } catch (Exception e2) {
                                Log.e(TAG, "Error converting observation data", e2);
                                handleWeatherError(callback, cachedWeather, latitude, longitude);
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Forecast API Error: " + error + ", using observation data only");
                        // 예보 API 실패 시 실황 데이터만 사용
                        try {
                            Weather freshWeather = convertApiResponseToWeather(observationResponse, latitude, longitude);
                            long id = weatherDao.insertWeather(freshWeather);
                            freshWeather.setId((int) id);
                            callback.onSuccess(freshWeather);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting observation data", e);
                            handleWeatherError(callback, cachedWeather, latitude, longitude);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Observation API Error: " + error);
                // 실황 API 실패 시 예보 API라도 시도
                apiService.getUltraShortTermForecast(nx, ny, new WeatherApiService.WeatherApiCallback() {
                    @Override
                    public void onSuccess(WeatherApiResponse response) {
                        try {
                            Weather freshWeather = convertApiResponseToWeather(response, latitude, longitude);
                            long id = weatherDao.insertWeather(freshWeather);
                            freshWeather.setId((int) id);
                            Log.d(TAG, "Retrieved weather data from forecast only");
                            callback.onSuccess(freshWeather);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting forecast data", e);
                            handleWeatherError(callback, cachedWeather, latitude, longitude);
                        }
                    }

                    @Override
                    public void onError(String forecastError) {
                        Log.e(TAG, "Both APIs failed - Observation: " + error + ", Forecast: " + forecastError);
                        handleWeatherError(callback, cachedWeather, latitude, longitude);
                    }
                });
            }
        });
    }

    // 기본 날씨 정보 생성
    private Weather createDefaultWeather(double latitude, double longitude) {
        String locationStr = latitude + "," + longitude;
        long timestamp = System.currentTimeMillis();

        // 기본 날씨 정보 생성
        Weather defaultWeather = new Weather(
                0,                  // id
                20.0f,              // 기본 온도 20도
                "Clear",            // 맑음
                0.0f,               // 강수량 없음
                50,                 // 습도 50%
                1.0f,               // 풍속 1m/s
                locationStr,        // 위치
                timestamp,          // 현재 시간
                false               // 우산 필요 없음
        );

        // 데이터베이스에 저장
        try {
            long id = weatherDao.insertWeather(defaultWeather);
            defaultWeather.setId((int) id);
        } catch (Exception e) {
            Log.e(TAG, "기본 날씨 정보 저장 실패", e);
        }

        Log.d(TAG, "Created default weather data");
        return defaultWeather;
    }

    // 오래된 데이터 정리
    private void cleanupOldData() {
        // 24시간 이상 지난 데이터 삭제
        long threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        int deletedRows = weatherDao.deleteOldWeatherData(threshold);
        Log.d(TAG, "Deleted " + deletedRows + " old weather records");
    }

    // 우산 필요 여부 판단 메시지 생성
    public String getCatMessage(Weather weather) {
        if (weather == null) {
            return "날씨 정보를 가져올 수 없다냥...";
        }

        // 강수 확률을 기반으로 메시지 생성
        if (weather.isNeedUmbrella()) {
            if (weather.getPrecipitation() > 5) {
                return "비가 많이 올 예정이다냥! 우산을 꼭 챙겨라냥!";
            } else {
                return "우산을 챙겨야 할 것 같다냥!";
            }
        } else {
            // 날씨 상태에 따른 메시지
            String condition = weather.getWeatherCondition();

            // 날씨 상태가 null인 경우 처리
            if (condition == null) {
                return "오늘은 우산이 필요 없을 것 같다냥!";
            }

            if (condition.equalsIgnoreCase("Clear")) {
                return "오늘은 맑은 하루다냥~";
            } else if (condition.equalsIgnoreCase("Clouds")) {
                return "구름이 조금 있지만 비는 안 올 것 같다냥~";
            } else {
                return "오늘은 우산이 필요 없을 것 같다냥!";
            }
        }
    }

    // 우산 알림 서비스를 위한 모든 위치에 대한 날씨 체크
    public void checkAllLocationsWeather(List<Location> locations, WeatherCheckCallback callback) {
        // TODO: 모든 위치에 대한 날씨 확인 구현
    }

    // 에러 처리 헬퍼 메서드
    private void handleWeatherError(WeatherCallback callback, Weather cachedWeather, double latitude, double longitude) {
        if (cachedWeather != null) {
            callback.onSuccess(cachedWeather);
        } else {
            callback.onSuccess(createDefaultWeather(latitude, longitude));
        }
    }
    
    // 실황과 예보 데이터를 조합
    private Weather combineWeatherData(WeatherApiResponse observationResponse, WeatherApiResponse forecastResponse, double latitude, double longitude) {
        String locationStr = latitude + "," + longitude;
        long timestamp = System.currentTimeMillis();
        
        // 기본값 설정
        float temperature = 20.0f;
        String weatherCondition = "맑음";
        float precipitation = 0.0f;
        int humidity = 50;
        float windSpeed = 1.0f;
        boolean needUmbrella = false;
        
        // 실황 데이터에서 현재 날씨 정보 추출
        if (observationResponse != null && observationResponse.getResponse() != null && 
            observationResponse.getResponse().getBody() != null && 
            observationResponse.getResponse().getBody().getItems() != null &&
            observationResponse.getResponse().getBody().getItems().getItem() != null) {
            
            List<WeatherApiResponse.Item> items = observationResponse.getResponse().getBody().getItems().getItem();
            
            for (WeatherApiResponse.Item item : items) {
                String category = item.getCategory();
                String value = item.getFcstValue();
                
                // 실황 데이터는 obsrValue 필드 사용
                String obsValue = item.getObsrValue();
                if (category != null && obsValue != null) {
                    switch (category) {
                        case "T1H": // 기온
                            try {
                                temperature = Float.parseFloat(obsValue);
                                Log.d(TAG, "Temperature: " + temperature + "°C");
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid temperature value: " + obsValue);
                            }
                            break;
                        case "RN1": // 1시간 강수량
                            if (!"강수없음".equals(obsValue)) {
                                try {
                                    precipitation = Float.parseFloat(obsValue);
                                    Log.d(TAG, "Precipitation: " + precipitation + "mm");
                                    if (precipitation > 0) {
                                        needUmbrella = true;
                                    }
                                } catch (NumberFormatException e) {
                                    Log.w(TAG, "Invalid precipitation value: " + obsValue);
                                }
                            } else {
                                precipitation = 0.0f;
                                Log.d(TAG, "No precipitation");
                            }
                            break;
                        case "REH": // 습도
                            try {
                                humidity = Integer.parseInt(obsValue);
                                Log.d(TAG, "Humidity: " + humidity + "%");
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid humidity value: " + obsValue);
                            }
                            break;
                        case "WSD": // 풍속
                            try {
                                windSpeed = Float.parseFloat(obsValue);
                                Log.d(TAG, "Wind speed: " + windSpeed + "m/s");
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid wind speed value: " + obsValue);
                            }
                            break;
                        case "PTY": // 강수형태
                            try {
                                int ptyCode = Integer.parseInt(obsValue);
                                if (ptyCode > 0) {
                                    needUmbrella = true;
                                    if (ptyCode == 1 || ptyCode == 4) {
                                        weatherCondition = "비";
                                        Log.d(TAG, "Rain detected from PTY");
                                    } else if (ptyCode == 2 || ptyCode == 3) {
                                        weatherCondition = "눈";
                                        Log.d(TAG, "Snow detected from PTY");
                                    }
                                }
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid precipitation type value: " + obsValue);
                            }
                            break;
                    }
                }
            }
        }
        
        // 예보 데이터에서 하늘상태와 강수형태 보완
        if (forecastResponse != null && forecastResponse.getResponse() != null && 
            forecastResponse.getResponse().getBody() != null && 
            forecastResponse.getResponse().getBody().getItems() != null &&
            forecastResponse.getResponse().getBody().getItems().getItem() != null) {
            
            List<WeatherApiResponse.Item> items = forecastResponse.getResponse().getBody().getItems().getItem();
            
            for (WeatherApiResponse.Item item : items) {
                String category = item.getCategory();
                String value = item.getFcstValue();
                
                if (category != null && value != null) {
                    switch (category) {
                        case "SKY": // 하늘상태
                            try {
                                int skyCode = Integer.parseInt(value);
                                if (!needUmbrella) { // 강수가 없을 때만 하늘상태로 날씨 결정
                                    if (skyCode == 1) {
                                        weatherCondition = "맑음";
                                        Log.d(TAG, "Sky condition: 맑음");
                                    } else if (skyCode <= 3) {
                                        weatherCondition = "구름많음";
                                        Log.d(TAG, "Sky condition: 구름많음");
                                    } else {
                                        weatherCondition = "흐림";
                                        Log.d(TAG, "Sky condition: 흐림");
                                    }
                                }
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid sky condition value: " + value);
                            }
                            break;
                        case "PTY": // 강수형태 (예보에서도 확인)
                            try {
                                int ptyCode = Integer.parseInt(value);
                                if (ptyCode > 0) {
                                    needUmbrella = true;
                                    if (ptyCode == 1 || ptyCode == 4) {
                                        weatherCondition = "비";
                                        Log.d(TAG, "Rain forecast detected");
                                    } else if (ptyCode == 2 || ptyCode == 3) {
                                        weatherCondition = "눈";
                                        Log.d(TAG, "Snow forecast detected");
                                    }
                                }
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid forecast precipitation type value: " + value);
                            }
                            break;
                    }
                }
            }
        }
        
        Log.d(TAG, String.format("Final weather data - Temp: %.1f°C, Condition: %s, Precipitation: %.1fmm, Humidity: %d%%, Wind: %.1fm/s, Umbrella: %s", 
                temperature, weatherCondition, precipitation, humidity, windSpeed, needUmbrella ? "Yes" : "No"));
        
        return new Weather(
                0,                  // id (will be set after database insert)
                temperature,        // 온도
                weatherCondition,   // 날씨 상태
                precipitation,      // 강수량
                humidity,           // 습도
                windSpeed,          // 풍속
                locationStr,        // 위치
                timestamp,          // 현재 시간
                needUmbrella        // 우산 필요 여부
        );
    }
    
    // API 응답을 Weather 객체로 변환 (단일 API 사용 시)
    private Weather convertApiResponseToWeather(WeatherApiResponse response, double latitude, double longitude) {
        String locationStr = latitude + "," + longitude;
        long timestamp = System.currentTimeMillis();
        
        // 기본값 설정
        float temperature = 20.0f;
        String weatherCondition = "맑음";
        float precipitation = 0.0f;
        int humidity = 50;
        float windSpeed = 1.0f;
        boolean needUmbrella = false;
        
        if (response != null && response.getResponse() != null && 
            response.getResponse().getBody() != null && 
            response.getResponse().getBody().getItems() != null &&
            response.getResponse().getBody().getItems().getItem() != null) {
            
            List<WeatherApiResponse.Item> items = response.getResponse().getBody().getItems().getItem();
            
            for (WeatherApiResponse.Item item : items) {
                String category = item.getCategory();
                String value = item.getFcstValue();
                
                if (category != null && value != null) {
                    switch (category) {
                        case "T1H": // 기온
                            try {
                                temperature = Float.parseFloat(value);
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid temperature value: " + value);
                            }
                            break;
                        case "RN1": // 1시간 강수량
                            if (!"강수없음".equals(value)) {
                                try {
                                    precipitation = Float.parseFloat(value);
                                    if (precipitation > 0) {
                                        weatherCondition = "비";
                                        needUmbrella = true;
                                    }
                                } catch (NumberFormatException e) {
                                    Log.w(TAG, "Invalid precipitation value: " + value);
                                }
                            }
                            break;
                        case "REH": // 습도
                            try {
                                humidity = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid humidity value: " + value);
                            }
                            break;
                        case "WSD": // 풍속
                            try {
                                windSpeed = Float.parseFloat(value);
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid wind speed value: " + value);
                            }
                            break;
                        case "SKY": // 하늘상태
                            try {
                                int skyCode = Integer.parseInt(value);
                                if (skyCode == 1) {
                                    weatherCondition = "맑음";
                                } else if (skyCode <= 3) {
                                    weatherCondition = "구름많음";
                                } else {
                                    weatherCondition = "흐림";
                                }
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid sky condition value: " + value);
                            }
                            break;
                        case "PTY": // 강수형태
                            try {
                                int ptyCode = Integer.parseInt(value);
                                if (ptyCode > 0) {
                                    needUmbrella = true;
                                    if (ptyCode == 1 || ptyCode == 4) {
                                        weatherCondition = "비";
                                    } else if (ptyCode == 2 || ptyCode == 3) {
                                        weatherCondition = "눈";
                                    }
                                }
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid precipitation type value: " + value);
                            }
                            break;
                    }
                }
            }
        }
        
        return new Weather(
                0,                  // id (will be set after database insert)
                temperature,        // 온도
                weatherCondition,   // 날씨 상태
                precipitation,      // 강수량
                humidity,           // 습도
                windSpeed,          // 풍속
                locationStr,        // 위치
                timestamp,          // 현재 시간
                needUmbrella        // 우산 필요 여부
        );
    }

    // 콜백 인터페이스
    public interface WeatherCallback {
        void onSuccess(Weather weather);
        void onError(String error);
    }
    
    public interface WeatherCheckCallback {
        void onWeatherCheckCompleted(boolean anyLocationNeedsUmbrella);
    }
}