package com.example.carlh.diaryclock.ui.memo.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.android.Auth;
import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.app.DiaryClock;
import com.example.carlh.diaryclock.cloud.tasks.AsyncSyncFolderTask;
import com.example.carlh.diaryclock.cloud.tasks.persistentAction.Action;
import com.example.carlh.diaryclock.data.AppDatabase;
import com.example.carlh.diaryclock.data.Memo;
import com.example.carlh.diaryclock.data.Recording;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import static android.preference.PreferenceManager.setDefaultValues;
import static com.example.carlh.diaryclock.cloud.tasks.persistentAction.ActionHelper.Type.DELETE_OFFLINE;
import static com.example.carlh.diaryclock.cloud.tasks.persistentAction.ActionHelper.Type.DOWNLOAD;
import static java.lang.Math.round;

/**
 * Created by carlh on 18.09.2017.
 */

public class PreferenceFragment extends android.preference.PreferenceFragment {

    @Inject
    SharedPreferences prefs;
    @Inject
    AppDatabase db;

    private Preference importButton;
    private Preference cloudSyncSwitch;
    private Preference cacheAllSwitch;
    private PreferenceFragment context;
    private String PATH_SD;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //inject
        ((DiaryClock) getActivity().getApplicationContext()).getComponent().inject(this);
        this.PATH_SD = ((DiaryClock) getActivity().getApplicationContext()).getRootPath().getAbsolutePath();
        String sharedPropFileName = "preferences";
        getPreferenceManager().setSharedPreferencesName(sharedPropFileName);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_main);
        cloudSyncSwitch = findPreference("cloud-sync");
        cacheAllSwitch = findPreference("cache-all");
        this.context = this;


        //Dialogs
        final AlertDialog.Builder syncDialog = new AlertDialog.Builder(getContext())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Connect to cloud provider")
                .setMessage("In order to use the Sync, you need to connect with your Dropbox!")
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        Auth.startOAuth2Authentication(getActivity(), getString(R.string.app_key));
                    }

                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        prefs.edit().putBoolean("cloud-sync", false).commit();
                        prefs.edit().putString("cursor-db", " ").commit();
                        dialogInterface.cancel();
                    }
                });

        final AlertDialog.Builder cacheONDialog = new AlertDialog.Builder(getContext())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Cache all files")
                .setMessage("From now on all new files will be now also stored on your phone. Do you want to download the existent files? The files will be downloaded with the next synchronisation")
                .setPositiveButton("Yes, download all", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //Iterate through all memos and recordings, cache them and create for every recording a persistent action and mark unsynced
                        List<Recording> recs = db.recordingDao().getAll();
                        //sort descending so the user sees downloading in the beginning
                        Collections.sort(recs, new Comparator<Recording>() {
                            @Override
                            public int compare(Recording r1, Recording r2) {
                                return (-1)*Long.compare(r1.getUid(),r2.getUid());
                            }
                        });

                        for(Recording rec : recs){
                            if(!rec.getCachedFile()){
                                rec.setCachedFile(true);
                                rec.setNotSynced(true);
                                db.recordingDao().update(rec);
                                Memo.updateMemos(rec,db);
                                AsyncSyncFolderTask.addPersistentAction(getContext(),new Action(DOWNLOAD, rec.getUid()));
                            }
                        }
                        getActivity().finish();
                        dialog.cancel();
                    }

                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });


        final AlertDialog.Builder cacheOFFDialog = new AlertDialog.Builder(getContext())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Cache all files")
                .setMessage("From now on all new files will be saved only in the cloud. Do you want to delete the copy of all local files now?")
                .setPositiveButton("Yes, delete all", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //Delete all file locally, and delete cache
                        //remove all cached flags
                        List<Recording> recs =  db.recordingDao().getAll();
                        for(Recording rec : recs){
                            if(rec.getCachedFile()) {
                                //Delete file locally
                                String file = PATH_SD + "/" + rec.getFile();
                                File f = new File(file);
                                if (f.exists()){
                                    Log.e(getClass().getName(), "Deleting was successful: "+f.delete(), null);
                                }
                                //updating database
                                rec.setCachedFile(false);
                                //rec.setNotSynced(true);
                                db.recordingDao().update(rec);
                                Memo.updateMemos(rec, db);
                            }
                        }

                        getActivity().finish();
                        dialog.cancel();
                    }

                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });


        //listeners
        cacheAllSwitch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {

                if (prefs.getBoolean("cache-all", false)) {
                    //Confirmation Dialog
                    cacheONDialog.show();
                } else {
                    cacheOFFDialog.show();
                }

                return true;
            }
        });

        cloudSyncSwitch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {

                Log.e(getClass().getName(), "Switch was clicked", null);

                if (prefs.getBoolean("cloud-sync", false)) {
                    Log.e(getClass().getName(), "Switch was clicked: cache-all"+prefs.getBoolean("cache-all",false), null);
                    //Confirmation Dialog
                    syncDialog.show();
                } else {
                    prefs.edit().putBoolean("cloud-sync", false).commit();
                    prefs.edit().putString("cursor-db", " ").commit();
                }
                return true;
            }
        });
    }
}