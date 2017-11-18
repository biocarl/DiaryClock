package com.example.carlh.diaryclock.injection;

import android.arch.persistence.room.Room;

import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.app.DiaryClock;
import com.example.carlh.diaryclock.data.AppDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by carlh on 13.07.2017.
 */

@Module
public class DatabaseModule {

    public DatabaseModule(){}

    // Application reference must come from AppModule.class
    @Provides
    @Singleton
    AppDatabase providesDatabase(DiaryClock application) { //This dependency (DiaryClock) is automatically injected by Dagger (mediated by the Component). If you have in the Modules different dependencies but the same type you can use the @Named()-Annotation

        return Room.databaseBuilder(application.getApplicationContext(),
                AppDatabase.class, application.getString(R.string.database_file))
                .allowMainThreadQueries() //TODO ONLY FOR TESTING!
                .build();

        //TODO ONLY FOR TESTING!
        /*
        return Room.inMemoryDatabaseBuilder(application.getApplicationContext(),AppDatabase.class)
                .allowMainThreadQueries()
                .build();
                */
    }

}
