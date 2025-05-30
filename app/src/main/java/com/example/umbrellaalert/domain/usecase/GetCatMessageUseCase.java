package com.example.umbrellaalert.domain.usecase;

import com.example.umbrellaalert.data.model.CatMessage;
import com.example.umbrellaalert.data.model.Weather;
import com.example.umbrellaalert.domain.factory.CatMessageFactory;

import java.util.Calendar;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 고양이 캐릭터 메시지를 생성하는 UseCase
 * agentrule.md의 "고양이 캐릭터 메시지 시스템" 요구사항 구현
 *
 * 개선사항:
 * - 다양한 감정 표현
 * - 시간대별 메시지 변화
 * - 우선순위 기반 메시지 선택
 * - 이모지와 함께 표현
 */
@Singleton
public class GetCatMessageUseCase {

    private final CatMessageFactory catMessageFactory;

    @Inject
    public GetCatMessageUseCase(CatMessageFactory catMessageFactory) {
        this.catMessageFactory = catMessageFactory;
    }

    /**
     * 날씨 상태에 따른 고양이 메시지 생성
     * @param weather 날씨 정보
     * @return 고양이 메시지 (이모지 포함)
     */
    public String execute(Weather weather) {
        CatMessage catMessage = catMessageFactory.createWeatherMessage(weather);

        // 시간대별 인사말 추가
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        return catMessage.getTimeBasedMessage(hour) + " " + catMessage.getEmoji();
    }

    /**
     * 고양이 메시지 객체 반환 (UI에서 추가 정보 필요시)
     */
    public CatMessage getCatMessageObject(Weather weather) {
        return catMessageFactory.createWeatherMessage(weather);
    }

    /**
     * 온도에 따른 추가 메시지 생성 (개선된 버전)
     */
    public String getTemperatureMessage(float temperature) {
        if (temperature >= 35) {
            return "위험할 정도로 덥다냥! 🥵 실내에 있는 게 좋겠다냥!";
        } else if (temperature >= 30) {
            return "너무 덥다냥! 🌡️ 시원한 음료수를 준비하고 그늘에서 쉬어라냥~";
        } else if (temperature >= 25) {
            return "따뜻한 날씨다냥~ 😊 가벼운 옷차림으로 나가면 딱 좋겠다냥!";
        } else if (temperature >= 20) {
            return "완벽한 온도다냥! 🌤️ 활동하기 정말 좋은 날씨다냥~";
        } else if (temperature >= 15) {
            return "적당한 날씨다냥~ 🍃 가볍게 겉옷 하나 정도면 충분할 것 같다냥!";
        } else if (temperature >= 10) {
            return "조금 쌀쌀하다냥~ 🧥 겉옷을 챙겨라냥!";
        } else if (temperature >= 5) {
            return "춥다냥! 🧣 따뜻하게 입고 나가라냥~";
        } else if (temperature >= 0) {
            return "정말 춥다냥! ❄️ 두꺼운 옷을 입고 조심해서 다녀라냥!";
        } else {
            return "얼어붙을 정도로 춥다냥! 🥶 최대한 따뜻하게 입고 외출을 자제하라냥!";
        }
    }

    /**
     * 특별한 상황에 대한 메시지 생성
     */
    public String getSpecialMessage(String situation) {
        switch (situation) {
            case "morning_rush":
                return "아침 러시아워다냥! ⏰ 평소보다 일찍 출발하는 게 좋겠다냥~";
            case "weekend":
                return "주말이다냥! 🎉 날씨 좋으니 어디 놀러가는 거냥?";
            case "holiday":
                return "오늘은 휴일이다냥! 🎊 여유롭게 즐기라냥~";
            case "late_night":
                return "늦은 시간이다냥! 🌙 조심해서 다녀라냥~";
            default:
                return "오늘도 좋은 하루 보내라냥! 😸";
        }
    }

    /**
     * 격려 메시지 생성
     */
    public String getEncouragementMessage() {
        String[] encouragements = {
            "오늘도 화이팅이다냥! 💪",
            "넌 할 수 있다냥! ✨",
            "좋은 일이 생길 거다냥! 🍀",
            "힘내라냥! 내가 응원한다냥! 📣",
            "오늘은 특별한 날이 될 것 같다냥! ⭐"
        };

        int randomIndex = (int) (Math.random() * encouragements.length);
        return encouragements[randomIndex];
    }
}
