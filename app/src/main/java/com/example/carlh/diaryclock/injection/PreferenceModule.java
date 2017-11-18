package com.example.carlh.diaryclock.injection;

import android.app.Application;
import android.content.SharedPreferences;

import com.example.carlh.diaryclock.app.DiaryClock;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by carlh on 13.07.2017.
 */

@Module
public class PreferenceModule {

    private String name;
    private int mode;
    public PreferenceModule(String name, int mode){
        this.name =name;
        this.mode = mode;
    }

    // Application reference must come from AppModule.class
    @Provides
    @Singleton
    SharedPreferences providesSharedPreferences(DiaryClock application) { //This dependency (DiaryClock) is automatically injected by Dagger (mediated by the Component). If you have in the Modules different dependencies but the same type you can use the @Named()-Annotation
        return application.getSharedPreferences(name,mode);
    }

}
