package com.example.umbrellaalert.domain.factory;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.data.model.CatMessage;
import com.example.umbrellaalert.data.model.Weather;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 고양이 캐릭터 메시지를 생성하는 팩토리 클래스
 * agentrule.md의 "고양이 캐릭터 메시지 시스템" 요구사항 구현
 */
@Singleton
public class CatMessageFactory {
    
    private final Random random;
    
    @Inject
    public CatMessageFactory() {
        this.random = new Random();
    }
    
    /**
     * 날씨 정보를 바탕으로 적절한 고양이 메시지 생성
     */
    public CatMessage createWeatherMessage(Weather weather) {
        if (weather == null) {
            return createErrorMessage();
        }
        
        List<CatMessage> possibleMessages = new ArrayList<>();
        
        // 우산 필요 여부가 최우선
        if (weather.isNeedUmbrella()) {
            possibleMessages.addAll(createUmbrellaMessages(weather));
        } else {
            // 날씨 상태별 메시지
            possibleMessages.addAll(createWeatherConditionMessages(weather));
        }
        
        // 온도별 추가 메시지
        possibleMessages.addAll(createTemperatureMessages(weather.getTemperature()));
        
        // 시간대별 특별 메시지
        possibleMessages.addAll(createTimeBasedMessages());
        
        // 우선순위가 높은 메시지 선택
        return selectBestMessage(possibleMessages);
    }
    
    /**
     * 우산 관련 메시지 생성
     */
    private List<CatMessage> createUmbrellaMessages(Weather weather) {
        List<CatMessage> messages = new ArrayList<>();
        
        if (weather.getPrecipitation() > 10) {
            // 많은 비
            messages.add(new CatMessage(
                "비가 엄청 많이 올 예정이다냥! 우산을 꼭꼭 챙겨라냥!",
                CatMessage.MessageType.UMBRELLA_NEEDED,
                CatMessage.CatMood.WORRIED,
                R.drawable.cat_rainy,
                "☔",
                10
            ));
            messages.add(new CatMessage(
                "폭우 경보다냥! 큰 우산을 준비하고 조심해서 다녀라냥!",
                CatMessage.MessageType.WARNING,
                CatMessage.CatMood.WORRIED,
                R.drawable.cat_rainy,
                "⛈️",
                10
            ));
        } else if (weather.getPrecipitation() > 5) {
            // 보통 비
            messages.add(new CatMessage(
                "비가 제법 올 것 같다냥~ 우산 챙기는 거 잊지 마라냥!",
                CatMessage.MessageType.UMBRELLA_NEEDED,
                CatMessage.CatMood.CARING,
                R.drawable.cat_rainy,
                "🌧️",
                8
            ));
            messages.add(new CatMessage(
                "오늘은 우산이 필수템이다냥! 젖지 않게 조심하라냥~",
                CatMessage.MessageType.UMBRELLA_NEEDED,
                CatMessage.CatMood.CARING,
                R.drawable.cat_rainy,
                "☂️",
                8
            ));
        } else {
            // 약한 비
            messages.add(new CatMessage(
                "살짝 비가 올 수도 있다냥~ 작은 우산이라도 챙겨라냥!",
                CatMessage.MessageType.UMBRELLA_NEEDED,
                CatMessage.CatMood.CALM,
                R.drawable.cat_cloudy,
                "🌦️",
                6
            ));
            messages.add(new CatMessage(
                "혹시 모르니 우산을 챙겨가는 게 좋겠다냥~",
                CatMessage.MessageType.UMBRELLA_NEEDED,
                CatMessage.CatMood.CALM,
                R.drawable.cat_cloudy,
                "☔",
                6
            ));
        }
        
        return messages;
    }
    
    /**
     * 날씨 상태별 메시지 생성
     */
    private List<CatMessage> createWeatherConditionMessages(Weather weather) {
        List<CatMessage> messages = new ArrayList<>();
        String condition = weather.getWeatherCondition();
        
        if (condition == null || condition.equalsIgnoreCase("Clear")) {
            // 맑은 날씨
            messages.add(new CatMessage(
                "오늘은 완벽한 맑은 날이다냥! 기분 좋게 출발하라냥~",
                CatMessage.MessageType.WEATHER_SUNNY,
                CatMessage.CatMood.HAPPY,
                R.drawable.cat_sunny,
                "☀️",
                7
            ));
            messages.add(new CatMessage(
                "햇살이 따뜻하다냥~ 산책하기 딱 좋은 날씨다냥!",
                CatMessage.MessageType.WEATHER_SUNNY,
                CatMessage.CatMood.EXCITED,
                R.drawable.cat_sunny,
                "🌞",
                7
            ));
            messages.add(new CatMessage(
                "구름 한 점 없는 파란 하늘이다냥! 완벽한 하루가 될 것 같다냥~",
                CatMessage.MessageType.WEATHER_SUNNY,
                CatMessage.CatMood.HAPPY,
                R.drawable.cat_sunny,
                "🌤️",
                7
            ));
        } else if (condition.equalsIgnoreCase("Clouds") || condition.equalsIgnoreCase("Partly Cloudy")) {
            // 구름 많은 날씨
            messages.add(new CatMessage(
                "구름이 조금 있지만 괜찮은 날씨다냥~ 우산은 필요 없을 것 같다냥!",
                CatMessage.MessageType.WEATHER_CLOUDY,
                CatMessage.CatMood.CALM,
                R.drawable.cat_cloudy,
                "⛅",
                5
            ));
            messages.add(new CatMessage(
                "구름이 예쁘게 떠있다냥~ 비는 안 올 것 같으니 안심하라냥!",
                CatMessage.MessageType.WEATHER_CLOUDY,
                CatMessage.CatMood.CALM,
                R.drawable.cat_cloudy,
                "☁️",
                5
            ));
        }
        
        return messages;
    }
    
    /**
     * 온도별 메시지 생성
     */
    private List<CatMessage> createTemperatureMessages(float temperature) {
        List<CatMessage> messages = new ArrayList<>();
        
        if (temperature >= 30) {
            messages.add(new CatMessage(
                "너무 덥다냥! 시원한 음료수를 준비하고 그늘에서 쉬어라냥~",
                CatMessage.MessageType.TEMPERATURE_HOT,
                CatMessage.CatMood.WORRIED,
                R.drawable.cat_sunny,
                "🥵",
                6
            ));
        } else if (temperature >= 25) {
            messages.add(new CatMessage(
                "따뜻한 날씨다냥~ 가벼운 옷차림으로 나가면 딱 좋겠다냥!",
                CatMessage.MessageType.TEMPERATURE_WARM,
                CatMessage.CatMood.HAPPY,
                R.drawable.cat_sunny,
                "😊",
                4
            ));
        } else if (temperature <= 5) {
            messages.add(new CatMessage(
                "춥다냥! 따뜻하게 입고 나가라냥~ 감기 걸리면 안 된다냥!",
                CatMessage.MessageType.TEMPERATURE_COLD,
                CatMessage.CatMood.CARING,
                R.drawable.cat_cloudy,
                "🥶",
                6
            ));
        }
        
        return messages;
    }
    
    /**
     * 시간대별 특별 메시지
     */
    private List<CatMessage> createTimeBasedMessages() {
        List<CatMessage> messages = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        if (hour >= 6 && hour <= 8) {
            // 아침 시간
            messages.add(new CatMessage(
                "좋은 아침이다냥! 오늘도 힘내라냥~",
                CatMessage.MessageType.GREETING,
                CatMessage.CatMood.EXCITED,
                R.drawable.cat_sunny,
                "🌅",
                3
            ));
        } else if (hour >= 22 || hour <= 5) {
            // 늦은 시간
            messages.add(new CatMessage(
                "늦은 시간이다냥! 조심해서 다녀라냥~",
                CatMessage.MessageType.WARNING,
                CatMessage.CatMood.WORRIED,
                R.drawable.cat_cloudy,
                "🌙",
                5
            ));
        }
        
        return messages;
    }
    
    /**
     * 에러 메시지 생성
     */
    private CatMessage createErrorMessage() {
        return new CatMessage(
            "날씨 정보를 가져올 수 없다냥... 잠시 후 다시 확인해보라냥!",
            CatMessage.MessageType.WARNING,
            CatMessage.CatMood.WORRIED,
            R.drawable.cat_cloudy,
            "😿",
            1
        );
    }
    
    /**
     * 가장 적절한 메시지 선택 (우선순위 기반)
     */
    private CatMessage selectBestMessage(List<CatMessage> messages) {
        if (messages.isEmpty()) {
            return createErrorMessage();
        }
        
        // 우선순위가 높은 메시지들 필터링
        int maxPriority = messages.stream().mapToInt(CatMessage::getPriority).max().orElse(0);
        List<CatMessage> highPriorityMessages = messages.stream()
            .filter(msg -> msg.getPriority() == maxPriority)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        // 같은 우선순위 중에서 랜덤 선택
        return highPriorityMessages.get(random.nextInt(highPriorityMessages.size()));
    }
}
