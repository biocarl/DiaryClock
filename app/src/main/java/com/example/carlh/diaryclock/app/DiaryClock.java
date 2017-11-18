package com.example.carlh.diaryclock.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.injection.AppModule;
import com.example.carlh.diaryclock.injection.CloudModule;
import com.example.carlh.diaryclock.injection.DaggerDiaryClockComponent;
import com.example.carlh.diaryclock.injection.DatabaseModule;
import com.example.carlh.diaryclock.injection.DiaryClockComponent;
import com.example.carlh.diaryclock.injection.PreferenceModule;

import java.io.File;
import java.net.URI;

import static android.preference.PreferenceManager.setDefaultValues;

/**
 * Created by carlh on 12.07.2017.
 */
public class DiaryClock extends Application {

    //Inner class
    public class CFile {
        private String file;
        public CFile(String file){
            //Setting file
            this.file = file;
        }
        public String getRelativePath(){
            return file;
        }
        public String getAbsolutePath(){
            return prefix+"/"+file;
        }
        public String getName(){return null;}

    }
    // only for static immutabile data, otherwise something like sharedPreferences
    //Paths
    private SharedPreferences preferences;
    private String prefix;
    private DiaryClockComponent component;

    //Getter method for component (Dagger2)
    public DiaryClockComponent getComponent() {
        return component;
    }

    private DiaryClockComponent createComponent(){
        return DaggerDiaryClockComponent.builder()
                .appModule(new AppModule(this))
                .preferenceModule(new PreferenceModule("preferences",MODE_PRIVATE)) //this Module is dependend of the application instance which creates the AppModule
                .databaseModule(new DatabaseModule())
                .cloudModule(new CloudModule())
                .build();
    }

    //create all modules/singletons again since they depend on the sharedPreferences and they can change, avoiding app restart
    public void refreshInjection(){
        this.component = createComponent();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.component = createComponent();

        //Creating paths
        this.preferences = getSharedPreferences("preferences", MODE_PRIVATE);
        //Setting prefix for paths

        if(preferences.getBoolean("use-sd", true)){
            String state = Environment.getExternalStorageState();
            if(Environment.MEDIA_MOUNTED.equals(state)) {
                File[] roots = ContextCompat.getExternalFilesDirs(getApplicationContext(), null);
                //non-removable internal and sd-card (both considered as external)
                if(roots.length == 2){
                    this.prefix = roots[1].getAbsolutePath();
                }else {
                    this.prefix = roots[0].getAbsolutePath();
                }
                Log.e(getClass().getName(), "SD-MODE:"+this.prefix, null);
                Log.e(getClass().getName(), "SD-MODE:size:"+roots.length, null);
            }else {
                this.prefix = getFilesDir().getAbsolutePath();
                Log.e(getClass().getName(), "NO-SD-MODE(on error):"+this.prefix, null);
            }
        }else {
            this.prefix = getFilesDir().getAbsolutePath();
            Log.e(getClass().getName(), "NO-SD-MODE:"+this.prefix, null);
        }
    }

    //System Path is always internal and not accessible
    public CFile getSystemPath(){
        return new CFile("system"){
            @Override
            public String getAbsolutePath(){
              return getFilesDir().getAbsolutePath()+"/"+super.file; //uses as prefix the internal storage!
            }
            };
    }

    public CFile getMusicPath(){
        return new CFile("music");
    }
    public CFile getRecordingsPath(){
        return new CFile("recordings");
    }
    public CFile getRootPath(){
        return new CFile("");
    }

    //Prefs
    public SharedPreferences getPreferences() {
        return preferences;
    }
}

