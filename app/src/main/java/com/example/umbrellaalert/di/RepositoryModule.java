package com.example.umbrellaalert.di;

import android.content.Context;

import com.example.umbrellaalert.data.api.OpenWeatherApiClient;
import com.example.umbrellaalert.data.manager.WeatherManager;
import com.example.umbrellaalert.data.repository.LocationRepositoryImpl;
import com.example.umbrellaalert.data.repository.WeatherRepositoryImpl;
import com.example.umbrellaalert.domain.repository.LocationRepository;
import com.example.umbrellaalert.domain.repository.WeatherRepository;
import com.example.umbrellaalert.weather.SimpleWeatherService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Repository 의존성 주입을 위한 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    @Provides
    @Singleton
    public OpenWeatherApiClient provideOpenWeatherApiClient(@ApplicationContext Context context) {
        return new OpenWeatherApiClient(context);
    }

    @Provides
    @Singleton
    public SimpleWeatherService provideSimpleWeatherService(@ApplicationContext Context context, OpenWeatherApiClient apiClient) {
        return new SimpleWeatherService(context, apiClient);
    }

    @Provides
    @Singleton
    public WeatherManager provideWeatherManager(@ApplicationContext Context context, SimpleWeatherService weatherService) {
        return new WeatherManager(context, weatherService);
    }

    @Provides
    @Singleton
    public WeatherRepository provideWeatherRepository(@ApplicationContext Context context, WeatherManager weatherManager) {
        return new WeatherRepositoryImpl(context, weatherManager);
    }

    @Provides
    @Singleton
    public LocationRepository provideLocationRepository(@ApplicationContext Context context) {
        return new LocationRepositoryImpl(context);
    }


}
