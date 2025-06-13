# 🌧️ 아 맞다 우산! (Umbrella Alert)

<div align="center">
  <img src="app/src/main/res/drawable/cat_sunny.png" alt="우산 알림 고양이" width="120" height="120">
  
  **통학하는 대학생을 위한 스마트 날씨 & 교통 알림 앱**
  
  [![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
  [![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
  [![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
</div>

## 📱 프로젝트 소개

[프로젝트 소개 PPT](https://www.miricanvas.com/v/14q8m0i)

"아 맞다 우산!"은 매일 반복되는 통학 루틴에서 **"우산을 챙길지"**, **"언제 출발할지"** 를 고민하는 대학생들을 위한 생활 밀착형 알림 앱입니다.

단순한 날씨 정보 제공을 넘어서, 귀여운 고양이 캐릭터가 사용자의 위치, 일정, 날씨, 교통 정보를 종합 분석하여 **개인화된 행동 가이드**를 제공합니다.

### ✨ 핵심 가치
- 🎯 **정보 → 행동**: 단순 정보 제공이 아닌 구체적 행동 유도
- 🐱 **감성적 UX**: 고양이 캐릭터를 통한 친근하고 재미있는 인터페이스  
- ⚡ **통합 서비스**: 날씨 + 교통 + 시간 관리를 하나의 앱에서
- 🎨 **개인화**: 사용자의 통학 패턴과 선호도를 학습하여 맞춤형 알림

## 🚀 주요 기능

### 🌦️ 스마트 날씨 알림
- **OpenWeather API** 연동으로 정확한 실시간 날씨 정보
- **우산 필요도 분석**: 강수확률, 강수량, 사용자 이동거리 종합 고려
- **12시간 상세 예보**: 3시간 간격 날씨 변화 추이
- **고양이 날씨 분석가**: 날씨 데이터를 분석하여 재미있는 메시지와 추천사항 제공

### 🚌 실시간 교통 정보
- **공공데이터 버스 API** 연동 (세종시, 대전시 지원)
- **도보 시간 계산**: 현재 위치에서 정류장까지의 실시간 도보 시간
- **최적 출발 시점 알림**: 버스 도착시간 - 도보시간 - 버퍼시간 계산
- **버스 등록 시스템**: 자주 이용하는 정류장 + 노선 조합 저장

### 🗺️ 위치 기반 서비스
- **네이버 클라우드 플랫폼 지도** 연동
- **GPS 기반 위치 추적**: 실시간 위치 기반 맞춤 정보
- **지도 기반 위치 설정**: 직관적인 지도 인터페이스로 집/학교 위치 등록
- **장소 검색**: 네이버 Geocoding API를 통한 정확한 장소 검색
- **주소 변환**: Reverse Geocoding으로 좌표를 읽기 쉬운 주소로 변환

### 🔔 스마트 알림 시스템
- **위젯 지원**: 홈 화면에서 바로 확인 가능한 날씨 + 버스 정보
- **푸시 알림**: 중요한 시점에 자동 알림 발송
- **상태바 지속 알림**: 실시간 날씨 + 버스 정보 표시 (선택사항)

### 🎨 사용자 경험
- **다크/라이트 모드**: 시스템 설정 연동 및 수동 선택
- **고양이 캐릭터**: 날씨와 상황에 따른 다양한 표정과 메시지
- **직관적 네비게이션**: 홈, 날씨, 버스, 설정 4개 탭 구조

## 🛠️ 기술 스택

### 아키텍처
- **MVVM Pattern**: Model-View-ViewModel 아키텍처
- **Dependency Injection**: Dagger Hilt
- **Repository Pattern**: 데이터 레이어 추상화

### 주요 라이브러리
- **UI**: Material Design 3, ConstraintLayout, RecyclerView
- **Navigation**: Navigation Component, Fragment
- **Lifecycle**: ViewModel, LiveData
- **Network**: OkHttp3, Gson
- **Database**: Room (SQLite)
- **Location**: Google Play Services Location
- **Maps**: Naver Cloud Platform Maps SDK

### API 연동
- **OpenWeather API**: 실시간 날씨 정보 및 5일 예보
- **공공데이터 버스정보 API**: 실시간 버스 도착 정보
- **네이버 클라우드 플랫폼**: 지도 서비스, Geocoding, Reverse Geocoding, Places API

## 📋 시스템 요구사항

- **Android API Level**: 24+ (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **권한**: 위치, 알림, 네트워크 접근
- **지원 지역**: 세종시, 대전시 (버스 정보)

## 🚀 설치 및 실행

### 1. 저장소 클론
```bash
git clone https://github.com/YUJAEYUN/umbrellaalert.git
cd umbrellaalert
```

### 2. API 키 설정
프로젝트 루트에 `local.properties` 파일을 생성하고 다음 API 키들을 추가하세요:

```properties
# OpenWeather API 키 (날씨 정보)
weather.api.service.key=YOUR_OPENWEATHER_API_KEY

# 버스정보 공공데이터포털 API 키
bus.api.service.key=YOUR_BUS_API_KEY

# 네이버 클라우드 플랫폼 API 키
naver.map.client.id=YOUR_NAVER_MAP_CLIENT_ID
naver.map.client.secret=YOUR_NAVER_MAP_CLIENT_SECRET
```

### 3. API 키 발급 방법

#### 🌤️ OpenWeather API (날씨 정보)
1. [OpenWeather](https://openweathermap.org/api) 회원가입
2. "Current Weather Data" 및 "5 Day / 3 Hour Forecast" API 선택
3. 무료 플랜으로 API 키 발급 (월 1,000회 호출 제한)
4. `weather.api.service.key`에 입력

#### 🚌 버스 API (교통 정보)
1. [공공데이터포털](https://www.data.go.kr/) 접속
2. "버스도착정보" 관련 API 검색 (지역별)
   - 세종시: "세종시 버스도착정보 서비스"
   - 대전시: "대전광역시 버스도착정보 서비스"
3. 활용신청 → 승인 후 인증키 발급
4. `bus.api.service.key`에 입력

#### 🗺️ 네이버 클라우드 플랫폼 (지도 및 위치 서비스)
1. [네이버 클라우드 플랫폼](https://www.ncloud.com/) 회원가입
2. Console → Services → AI·Application Service → Maps
3. Application 등록 → Mobile Dynamic Map 선택
4. 추가로 다음 서비스들도 활성화:
   - **Geocoding**: 주소/장소명을 좌표로 변환
   - **Reverse Geocoding**: 좌표를 주소로 변환
   - **Places**: POI(관심지점) 검색
5. 클라이언트 ID와 클라이언트 Secret 발급
6. `naver.map.client.id`와 `naver.map.client.secret`에 입력

### 4. 빌드 및 실행
```bash
# Android Studio에서 프로젝트 열기
# 또는 명령줄에서:
./gradlew assembleDebug
```

## 📱 사용법

### 첫 실행 시 설정
1. **위치 권한 허용**: GPS 기반 날씨 정보를 위해 필요
2. **알림 권한 허용**: 우산 알림과 버스 알림을 위해 필요
3. **위치 설정**: 집/학교 위치를 지도에서 선택
4. **버스 등록**: 자주 이용하는 정류장과 노선 등록

### 일상 사용
1. **홈 탭**: 오늘의 날씨 요약과 고양이 메시지 확인
2. **날씨 탭**: 12시간 상세 예보와 우산 필요도 분석
3. **버스 탭**: 등록한 버스의 실시간 도착 정보
4. **설정 탭**: 테마, 알림, 위치 등 개인화 설정

## 🎯 사용자 시나리오

```
07:30 - 사용자가 자취방에서 아침 준비 중

07:32 - "아 맞다 우산!" 위젯에서 '비 소식 있음' 안내 + 캐릭터 알림 팝업
       → "비가 온다냥! 오늘은 우산 챙기는 게 좋아요 ☔️🐱"

07:34 - 슬슬 나가자 알림 도착  
       → "버스가 10분 후 도착해요. 지금 출발하면 딱 맞아요!"
```

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 👨‍💻 개발자

**YUJAEYUN** - [GitHub](https://github.com/YUJAEYUN)

---

<div align="center">
  <strong>매일 아침, 고양이와 함께 완벽한 하루를 준비하세요! 🐱☂️</strong>
</div>
