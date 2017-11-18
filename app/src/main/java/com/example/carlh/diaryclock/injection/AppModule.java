package com.example.carlh.diaryclock.injection;

import android.app.Application;

import com.example.carlh.diaryclock.app.DiaryClock;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by carlh on 13.07.2017.
 */

@Module
public class AppModule {

    DiaryClock mApplication;
    public AppModule(DiaryClock application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    DiaryClock providesApplication() {
        return mApplication;
    }

}
