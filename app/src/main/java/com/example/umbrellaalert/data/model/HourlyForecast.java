package com.example.umbrellaalert.data.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 시간별 예보 데이터 모델
 * 12시간 예보를 위한 1시간 단위 날씨 정보
 */
public class HourlyForecast {
    private String forecastDate; // 예보 일자 (yyyyMMdd)
    private String forecastTime; // 예보 시각 (HHmm)
    private float temperature; // 기온 (°C)
    private float precipitation; // 1시간 강수량 (mm)
    private int precipitationProbability; // 강수확률 (%)
    private int humidity; // 습도 (%)
    private float windSpeed; // 풍속 (m/s)
    private int precipitationType; // 강수형태 (0:없음, 1:비, 2:비/눈, 3:눈, 4:소나기)
    private String weatherCondition; // 날씨 상태 (Clear, Rain, Snow 등)
    private boolean needUmbrella; // 우산 필요 여부
    private boolean isCurrentHour; // 현재 시간 여부
    private String dataSource; // 데이터 소스 ("API", "MOCK", "DEFAULT")

    public HourlyForecast() {
        // 기본 생성자
    }

    public HourlyForecast(String forecastDate, String forecastTime, float temperature,
                         float precipitation, int precipitationProbability, int humidity,
                         float windSpeed, int precipitationType, String weatherCondition,
                         boolean needUmbrella) {
        this.forecastDate = forecastDate;
        this.forecastTime = forecastTime;
        this.temperature = temperature;
        this.precipitation = precipitation;
        this.precipitationProbability = precipitationProbability;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.precipitationType = precipitationType;
        this.weatherCondition = weatherCondition;
        this.needUmbrella = needUmbrella;
        this.isCurrentHour = checkIfCurrentHour();
    }

    // Getters and Setters
    public String getForecastDate() { return forecastDate; }
    public void setForecastDate(String forecastDate) { this.forecastDate = forecastDate; }

    public String getForecastTime() { return forecastTime; }
    public void setForecastTime(String forecastTime) { this.forecastTime = forecastTime; }

    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }

    public float getPrecipitation() { return precipitation; }
    public void setPrecipitation(float precipitation) { this.precipitation = precipitation; }

    public int getPrecipitationProbability() { return precipitationProbability; }
    public void setPrecipitationProbability(int precipitationProbability) {
        this.precipitationProbability = precipitationProbability;
    }

    public int getHumidity() { return humidity; }
    public void setHumidity(int humidity) { this.humidity = humidity; }

    public float getWindSpeed() { return windSpeed; }
    public void setWindSpeed(float windSpeed) { this.windSpeed = windSpeed; }

    public int getPrecipitationType() { return precipitationType; }
    public void setPrecipitationType(int precipitationType) { this.precipitationType = precipitationType; }

    public String getWeatherCondition() { return weatherCondition; }
    public void setWeatherCondition(String weatherCondition) { this.weatherCondition = weatherCondition; }

    public boolean isNeedUmbrella() { return needUmbrella; }
    public void setNeedUmbrella(boolean needUmbrella) { this.needUmbrella = needUmbrella; }

    public boolean isCurrentHour() { return isCurrentHour; }
    public void setCurrentHour(boolean currentHour) { isCurrentHour = currentHour; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    /**
     * 예보 시간을 가독성 있는 형식으로 변환 (예: "1500" -> "15:00")
     */
    public String getFormattedTime() {
        if (forecastTime == null || forecastTime.length() != 4) {
            return forecastTime;
        }
        return forecastTime.substring(0, 2) + ":" + forecastTime.substring(2);
    }

    /**
     * 예보 날짜를 가독성 있는 형식으로 변환 (예: "20231201" -> "12/01")
     */
    public String getFormattedDate() {
        if (forecastDate == null || forecastDate.length() != 8) {
            return forecastDate;
        }
        return forecastDate.substring(4, 6) + "/" + forecastDate.substring(6);
    }

    /**
     * 현재 시간인지 확인 (내부 메서드)
     */
    private boolean checkIfCurrentHour() {
        if (forecastDate == null || forecastTime == null) {
            return false;
        }

        try {
            Calendar now = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);

            String currentDate = dateFormat.format(now.getTime());
            String currentTime = timeFormat.format(now.getTime());

            // 날짜가 같고 시간이 현재 시간과 같으면 현재 시간
            return forecastDate.equals(currentDate) &&
                   forecastTime.substring(0, 2).equals(currentTime.substring(0, 2));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 날짜와 시간을 Date 객체로 변환
     */
    public Date getForecastDateTime() {
        if (forecastDate == null || forecastTime == null) {
            return null;
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm", Locale.KOREA);
            return format.parse(forecastDate + forecastTime);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 상대적 시간 표시 (예: "1시간 후", "3시간 후")
     */
    public String getRelativeTime() {
        Date forecastDateTime = getForecastDateTime();
        if (forecastDateTime == null) {
            return getFormattedTime();
        }

        long diffMillis = forecastDateTime.getTime() - System.currentTimeMillis();
        long diffHours = diffMillis / (1000 * 60 * 60);

        if (diffHours == 0) {
            return "지금";
        } else if (diffHours == 1) {
            return "1시간 후";
        } else if (diffHours > 1 && diffHours <= 12) {
            return diffHours + "시간 후";
        } else {
            return getFormattedTime();
        }
    }

    @Override
    public String toString() {
        return "HourlyForecast{" +
                "forecastDate='" + forecastDate + '\'' +
                ", forecastTime='" + forecastTime + '\'' +
                ", temperature=" + temperature +
                ", precipitation=" + precipitation +
                ", precipitationProbability=" + precipitationProbability +
                ", humidity=" + humidity +
                ", windSpeed=" + windSpeed +
                ", precipitationType=" + precipitationType +
                ", weatherCondition='" + weatherCondition + '\'' +
                ", needUmbrella=" + needUmbrella +
                ", isCurrentHour=" + isCurrentHour +
                '}';
    }
}
