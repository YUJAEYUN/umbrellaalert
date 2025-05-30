package com.example.umbrellaalert.data.model;

/**
 * 고양이 캐릭터 메시지 모델
 * 다양한 감정과 상황을 표현하는 메시지 시스템
 */
public class CatMessage {
    
    // 메시지 타입
    public enum MessageType {
        GREETING,           // 인사 메시지
        WEATHER_SUNNY,      // 맑은 날씨
        WEATHER_CLOUDY,     // 구름 많은 날씨
        WEATHER_RAINY,      // 비 오는 날씨
        WEATHER_SNOWY,      // 눈 오는 날씨
        UMBRELLA_NEEDED,    // 우산 필요
        UMBRELLA_NOT_NEEDED,// 우산 불필요
        TEMPERATURE_HOT,    // 더운 날씨
        TEMPERATURE_WARM,   // 따뜻한 날씨
        TEMPERATURE_COOL,   // 시원한 날씨
        TEMPERATURE_COLD,   // 추운 날씨
        DEPARTURE_TIME,     // 출발 시간 알림
        ENCOURAGEMENT,      // 격려 메시지
        WARNING,            // 주의 메시지
        SPECIAL_EVENT       // 특별한 날 메시지
    }
    
    // 고양이 감정 상태
    public enum CatMood {
        HAPPY,      // 기쁨 😸
        EXCITED,    // 신남 😻
        CALM,       // 평온 😺
        WORRIED,    // 걱정 😿
        SLEEPY,     // 졸림 😴
        PLAYFUL,    // 장난기 😼
        CARING,     // 돌봄 😽
        SURPRISED   // 놀람 🙀
    }
    
    private String message;
    private MessageType type;
    private CatMood mood;
    private int catImageResource;
    private String emoji;
    private int priority; // 메시지 우선순위 (높을수록 우선)
    
    public CatMessage(String message, MessageType type, CatMood mood, 
                     int catImageResource, String emoji, int priority) {
        this.message = message;
        this.type = type;
        this.mood = mood;
        this.catImageResource = catImageResource;
        this.emoji = emoji;
        this.priority = priority;
    }
    
    // Getters
    public String getMessage() { return message; }
    public MessageType getType() { return type; }
    public CatMood getMood() { return mood; }
    public int getCatImageResource() { return catImageResource; }
    public String getEmoji() { return emoji; }
    public int getPriority() { return priority; }
    
    // 메시지에 이모지 추가
    public String getMessageWithEmoji() {
        return message + " " + emoji;
    }
    
    // 시간대별 인사말 추가
    public String getTimeBasedMessage(int hour) {
        String timeGreeting = getTimeGreeting(hour);
        if (type == MessageType.GREETING) {
            return timeGreeting + " " + message;
        }
        return message;
    }
    
    private String getTimeGreeting(int hour) {
        if (hour >= 5 && hour < 12) {
            return "좋은 아침이다냥!";
        } else if (hour >= 12 && hour < 17) {
            return "좋은 오후다냥!";
        } else if (hour >= 17 && hour < 21) {
            return "좋은 저녁이다냥!";
        } else {
            return "늦은 시간이다냥!";
        }
    }
}
