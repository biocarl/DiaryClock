package com.example.carlh.diaryclock.injection;

import com.example.carlh.diaryclock.activity.ImportActivity;
import com.example.carlh.diaryclock.activity.MainActivity;
import com.example.carlh.diaryclock.activity.SettingsActivity;
import com.example.carlh.diaryclock.activity.WaveActivity;
import com.example.carlh.diaryclock.cloud.CloudActivity;
import com.example.carlh.diaryclock.cloud.tasks.AsyncSyncFolderTask;
import com.example.carlh.diaryclock.services.RingtonePlayingService;
import com.example.carlh.diaryclock.ui.memo.main.PreferenceFragment;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by carlh on 13.07.2017.
 */

//Interface will be implemented by Dagger in the class DaggerDiaryClockComponent
@Singleton
@Component(modules = {PreferenceModule.class,AppModule.class,DatabaseModule.class,CloudModule.class})
public interface DiaryClockComponent {

    //How to:
    //[1]Here you put all the strictly-typed classes which will need an injection (is the sink of the DAG ;))
    //[2]Write the @inject-annotations and use the injecter in the constructor of the class e.g.  application.getComponent().inject(this)

    void inject(AsyncSyncFolderTask asyncSyncFolderTask);
    void inject(MainActivity mainActivity);
    void inject(SettingsActivity settingsActivity);
    void inject(PreferenceFragment preferenceFragment);
    void inject(WaveActivity waveActivity);
    void inject(RingtonePlayingService ringtonePlayingService);
    void inject(ImportActivity importActivity);

}
