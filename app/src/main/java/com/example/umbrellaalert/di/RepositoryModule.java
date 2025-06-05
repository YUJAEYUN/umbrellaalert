package com.example.umbrellaalert.di;

import android.content.Context;


import com.example.umbrellaalert.data.repository.LocationRepositoryImpl;
import com.example.umbrellaalert.data.repository.WeatherRepositoryImpl;
import com.example.umbrellaalert.domain.repository.LocationRepository;
import com.example.umbrellaalert.domain.repository.WeatherRepository;

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
    public WeatherRepository provideWeatherRepository(@ApplicationContext Context context) {
        return new WeatherRepositoryImpl(context);
    }

    @Provides
    @Singleton
    public LocationRepository provideLocationRepository(@ApplicationContext Context context) {
        return new LocationRepositoryImpl(context);
    }


}
