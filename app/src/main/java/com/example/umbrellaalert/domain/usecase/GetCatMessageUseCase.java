package com.example.umbrellaalert.domain.usecase;

import com.example.umbrellaalert.data.model.Weather;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 고양이 캐릭터 메시지를 생성하는 UseCase
 * 기획서의 "캐릭터 중심 UI/UX" 요구사항 구현
 */
@Singleton
public class GetCatMessageUseCase {

    @Inject
    public GetCatMessageUseCase() {
    }

    /**
     * 날씨 상태에 따른 고양이 메시지 생성
     * @param weather 날씨 정보
     * @return 고양이 메시지
     */
    public String execute(Weather weather) {
        if (weather == null) {
            return "날씨 정보를 가져올 수 없다냥...";
        }

        // 우산 필요 여부에 따른 메시지
        if (weather.isNeedUmbrella()) {
            if (weather.getPrecipitation() > 5) {
                return "비가 많이 올 예정이다냥! 우산을 꼭 챙겨라냥!";
            } else if (weather.getPrecipitation() > 0) {
                return "조금 비가 올 것 같다냥~ 우산을 챙겨라냥!";
            } else {
                return "우산을 챙겨야 할 것 같다냥!";
            }
        } else {
            // 날씨 상태에 따른 메시지
            String condition = weather.getWeatherCondition();
            
            if (condition == null) {
                return "오늘은 우산이 필요 없을 것 같다냥!";
            }
            
            if (condition.equalsIgnoreCase("Clear")) {
                return getRandomClearMessage();
            } else if (condition.equalsIgnoreCase("Clouds")) {
                return "구름이 조금 있지만 비는 안 올 것 같다냥~";
            } else if (condition.equalsIgnoreCase("Partly Cloudy")) {
                return "구름이 있지만 괜찮을 것 같다냥!";
            } else {
                return "오늘은 우산이 필요 없을 것 같다냥!";
            }
        }
    }

    /**
     * 맑은 날씨에 대한 다양한 메시지 중 랜덤 선택
     */
    private String getRandomClearMessage() {
        String[] clearMessages = {
            "오늘은 맑은 하루다냥~",
            "날씨가 정말 좋다냥! 산책하기 딱 좋은 날이다냥~",
            "햇살이 따뜻하다냥~ 기분 좋은 하루가 될 것 같다냥!",
            "구름 한 점 없는 맑은 하늘이다냥! 완벽한 날씨다냥~"
        };
        
        int randomIndex = (int) (Math.random() * clearMessages.length);
        return clearMessages[randomIndex];
    }

    /**
     * 온도에 따른 추가 메시지 생성
     */
    public String getTemperatureMessage(float temperature) {
        if (temperature >= 30) {
            return "너무 덥다냥! 시원한 곳에서 쉬어라냥~";
        } else if (temperature >= 25) {
            return "따뜻한 날씨다냥~ 가벼운 옷차림이 좋겠다냥!";
        } else if (temperature >= 15) {
            return "적당한 날씨다냥~ 활동하기 좋은 온도다냥!";
        } else if (temperature >= 5) {
            return "조금 쌀쌀하다냥~ 겉옷을 챙겨라냥!";
        } else {
            return "춥다냥! 따뜻하게 입고 나가라냥~";
        }
    }
}
