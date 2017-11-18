package com.example.carlh.diaryclock.injection;

import android.content.SharedPreferences;

import com.example.carlh.diaryclock.app.DiaryClock;
import com.example.carlh.diaryclock.cloud.Cloud;
import com.example.carlh.diaryclock.cloud.DriveCloud;
import com.example.carlh.diaryclock.cloud.DropboxCloud;
import com.example.carlh.diaryclock.cloud.NoCloud;

import dagger.Module;
import dagger.Provides;

/**
 * Created by carlh on 13.07.2017.
 */

@Module
public class CloudModule {
    Cloud cloud;

    public CloudModule(){}

    // Application reference must come from AppModule.class and PreferenceModule.class
    @Provides
    Cloud providesSharedPreferences(DiaryClock application, SharedPreferences prefs) {

        String provider = prefs.getString("service-name", "");
        Boolean cloudSync = prefs.getBoolean("cloud-sync",false);

        if(cloudSync){

            if (provider.equals("dropbox")) {
                this.cloud = new DropboxCloud(application);
            } else if (provider.equals("drive")) {
                this.cloud = new DriveCloud(application);
            } else {
                //TODO report some error!
                //This shouldn't occur since there will be only a dropdown available
            }
        }else {
            //User is not using cloud service
            this.cloud = new NoCloud(application);
        }

        return this.cloud;
    }

}
