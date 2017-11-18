package com.example.carlh.diaryclock.cloud.tasks;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.UnexpectedException;
import com.example.carlh.diaryclock.activity.MainActivity;
import com.example.carlh.diaryclock.app.DiaryClock;
import com.example.carlh.diaryclock.cloud.Cloud;
import com.example.carlh.diaryclock.cloud.NoCloud;
import com.example.carlh.diaryclock.cloud.tasks.persistentAction.Action;
import com.example.carlh.diaryclock.cloud.tasks.persistentAction.ActionHelper;
import com.example.carlh.diaryclock.cloud.tasks.persistentAction.XMLParser;
import com.example.carlh.diaryclock.cloud.tasks.persistentAction.XMLWriter;
import com.example.carlh.diaryclock.data.AppDatabase;
import com.example.carlh.diaryclock.data.Memo;
import com.example.carlh.diaryclock.data.Recording;
import com.example.carlh.diaryclock.data.Time;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import static com.example.carlh.diaryclock.cloud.tasks.persistentAction.ActionHelper.Type.DELETE_OFFLINE;
import static com.example.carlh.diaryclock.cloud.tasks.persistentAction.ActionHelper.Type.DELETE_ONLINE;
import static com.example.carlh.diaryclock.cloud.tasks.persistentAction.ActionHelper.Type.DOWNLOAD;
import static com.example.carlh.diaryclock.cloud.tasks.persistentAction.ActionHelper.Type.UPLOAD;

/**
 * Syncing files between cloud and offline storage
 */

public class AsyncSyncFolderTask extends AsyncTask<String, Integer, Boolean> {


    //AsyncThread
    private final AsyncSyncFolderTask.Callback mCallback;
    public boolean cloudSync;
    public boolean cacheAll;
    DiaryClock application;
    //Injected Dependencies
    @Inject
    SharedPreferences prefs;
    @Inject
    AppDatabase db;
    @Inject
    Cloud cloud; //Instance of a Cloud (Dropbox/Google Drive)
    private Context context;
    private Exception mException;
    private String PATH_SD = "";
    private String PATH_CLOUD = "";
    private AlertDialog importDialog;
    private boolean mergeDatabase;

    public AsyncSyncFolderTask(Context context,AsyncSyncFolderTask.Callback callback) {
        //Do the inject! (Dagger2)
        this.application = ((DiaryClock) context.getApplicationContext());   //Get application
        application.getComponent().inject(this);
        this.context = context;
        //AsyncThread
        this.mCallback = callback;
        //Setting up paths and vars
        this.PATH_CLOUD = application.getRootPath().getRelativePath();
        this.PATH_SD = application.getRootPath().getAbsolutePath();
        //Get booleans (prefs already injected)
        this.cloudSync = prefs.getBoolean("cloud-sync", false);
        this.cacheAll = prefs.getBoolean("cache-all", false);
        this.mergeDatabase = prefs.getBoolean("merge-db", false);
    }

    public static void addPersistentAction(Context context, Action action) {
        //Write actions as entries to xml-file
        XMLWriter writer = new XMLWriter(context);
        ArrayList<Action> persistentAction = new ArrayList<>();
        persistentAction.add(action);
        writer.writeEntries(persistentAction);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onDataLoaded(result);
        }
    }

    public ArrayList<Action> parsePersistentActionsFromCloud() throws ConnectException {


        ArrayList<Action> persistentAction = new ArrayList<>();

        //Get cursor ~ files to download
        String cursorString = prefs.getString("cursor-db", null);

        Boolean firstStart = (cursorString == null || cursorString.length() <= 1);
        if ( !mergeDatabase && firstStart) { //when database is merged it is very possible that the cursor is 0 (when installing the application again and using the backup from the cloud)
            final File db_file = context.getDatabasePath(context.getResources().getString(R.string.database_file));
            Log.e(getClass().getName(), "First sync", null);
            //Uploading all offline files
            List<Recording> recs =  db.recordingDao().getAll();
            for(Recording rec : recs){
                if(rec.getCachedFile()){
                    rec.setNotSynced(true);
                    db.recordingDao().update(rec);
                    Memo.updateMemos(rec,db);
                    AsyncSyncFolderTask.addPersistentAction(context,new Action(UPLOAD, rec.getUid()));
                }
            }
            /*
            Object metadata = null;
            try {
                metadata = cloud.getMetadata(db_file);
            } catch (Exception e) {}

            if (metadata != null && cloud.getType(metadata) == Cloud.Type.FILE) {
                publishProgress(-1);
                return null;
            }*/
        }

        //If existent database is detected in cloud storage
        if(mergeDatabase){

            String cursorTest = prefs.getString("cursor-db", null);
            //refresh cursor (so no new files are downloaded)
            cloud.updateCursor();
            cursorTest = prefs.getString("cursor-db", null);

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
                    db.recordingDao().update(rec);
                    Memo lastPlayed = db.memoDao().getLastPlayed();
                    if(lastPlayed != null){ //removing existing flag for last played
                        lastPlayed.setLastPlayed(false);
                        db.memoDao().update(lastPlayed);
                    }
                    Memo.updateMemos(rec, db);
                }
            }
            //delete all times and create a default time
            db.timeDao().deleteTable();
            Time time1 = new Time("12.00", "Welcome back ;-)");
            db.timeDao().insert(time1);
            prefs.edit().putBoolean("merge-db", false).apply();
        }

        //Default sync procedure

        //Getting new cursor
        cursorString = prefs.getString("cursor-db", null);

        //Iterating through file changes
        Iterator metadataList = null;
        try {
            metadataList = cloud.listEntries(cursorString, PATH_CLOUD);
        } catch (UnexpectedException e) {
            e.printStackTrace();
        }
        while (metadataList.hasNext()) {
            Object metadata = metadataList.next();
            Cloud.Type type = cloud.getType((metadata));
            switch (type) {
                case FILE: {
                    //Check if File is in root dir
                    if (!isRoot(cloud.getPath(metadata))) {
                        Log.e(getClass().getName(), "Files in subdirs are not supported :" + "path: " + cloud.getPath(metadata), null);
                        break;
                    }

                    //Create file entry
                    Recording recording = new Recording(cloud.getPath(metadata));
                    recording.setCachedFile(cacheAll);
                    recording.setHash(cloud.getHash(metadata));

                    if (cacheAll) {
                        recording.setNotSynced(true); //No update of memos needed because it is directly passed in the constructor
                    }
                    long fileId = db.recordingDao().insert(recording);
                    recording.setUid(fileId);

                    //Create default memo entry
                    Memo memo = new Memo(recording);
                    memo.setName(cloud.getName(metadata));
                    db.memoDao().insert(memo);

                    //Download file if desired
                    if (cacheAll) {
                        persistentAction.add(new Action(DOWNLOAD, fileId));
                    }
                    break;
                }
                case FOLDER:
                    Log.e(getClass().getName(), "[cloud] Creating Folders is not supported  " + "Path:" + cloud.getPath(metadata), null);
                    prefs.edit().putBoolean("folder-error", true).apply();  //this state signals that the user has to remove folder structures in Dropbox!
                    break;
                case DELETED:
                    Log.e(getClass().getName(), "[cloud|delete-Event] Metadata :: name" + cloud.getName(metadata) + "path::" + cloud.getPath(metadata), null);

                    //Ignoring Folders
                    if (!cloud.getName(metadata).contains(".")) {
                        Log.e(getClass().getName(), "[cloud|delete-Event] Metadata :: is a folder ->ignore", null);
                        break;
                    }

                    //Ignoring files in subdirs
                    if (!isRoot(cloud.getPath(metadata))) {
                        Log.e(getClass().getName(), "[cloud|delete-Event]Files in subdirs are not supported :" + "path: " + cloud.getPath(metadata), null);
                        break;
                    }

                    Recording recording = db.recordingDao().findByPath(cloud.getPath(metadata));
                    if (recording == null) {
                        Log.e(getClass().getName(), "Error: " + cloud.getName(metadata) + ":: File entry is already deleted, shouldn't be! It is supposed that the actual file is also already deleted", null);
                        break;
                    } else {
                        persistentAction.add(new Action(DELETE_OFFLINE, recording.getUid()));
                        recording.setNotSynced(true);
                        db.recordingDao().update(recording);
                        Memo.updateMemos(recording, db);
                    }
                    break;
                default:
                    Log.e(getClass().getName(), "[cloud] Type not covered" + type, null);
            }
        }

        //Update Cursor
        cloud.updateCursor();

        return persistentAction;
    }

    public boolean isRoot(String path) {
        int count = path.length() - path.replace("/", "").length();
        return (count == 0);
    }

    public void initSync() {
        //Init syncing process
        prefs.edit().putBoolean("is-syncing", true).apply();  //during this state no user activity is allowed!
    }

    public Boolean doInBackground(String... params) {
        //Only sync if not other process is already running, this should happen because you should check this already before.
        if (prefs.getBoolean("is-syncing", false)) {
            return false;
        }

        //State to show sync process and init database
        initSync();

        //Starting sync
        publishProgress(0);


        /*for (Recording rec : db.recordingDao().getAll()) {
            Log.e(getClass().getName(), "database (1):" + "file:" + rec.getFile() + "UID:" + rec.getUid(), null);
        }

        {
            XMLParser par = new XMLParser(context);
            if (par.hasActions()) {
                for (Action ac : par.getActions()) {
                    Log.e(getClass().getName(), "xml (1):" + "id:" + ac.getId() + "action:" + ac.getType(), null);
                }
            }
        }*/

        //Synchronisation was successful
        Boolean stackFinished = false;  //of no internet or working of Stack fails the bool stays false

        try {
            //Init Cloud
            if (cloudSync) {
                //Looking for cloud
                publishProgress(1);

                //[A] Save Cursor results as persistent Actions to Stack
                ArrayList<Action> persistentAction;

                //Compare cursors, create persistent actions and work of persistent (incl. existent) actions
                if (isOnline()) {
                    //set client instance of cloud, if null return null
                    try {
                        if (cloud instanceof NoCloud) {
                            Log.e(getClass().getName(), "You are using NoCloud although cloudSync is activated", null);
                            throw new AssertionError();
                        }

                        cloud.init();

                    } catch (IllegalStateException e) {
                        Log.e(getClass().getName(), "Client was probably not connected, connect first with Cloud-Service" + e.toString(), null);
                        mException = e;
                        return false;
                    }

                    persistentAction = parsePersistentActionsFromCloud();

                    //file not created == empty (first sync)
                    if (persistentAction == null)
                        return true;

                    //connecting to cloud
                    publishProgress(2);

                    /*{
                        XMLParser par = new XMLParser(context);
                        if (par.hasActions()) {
                            for (Action ac : par.getActions()) {
                                Log.e(getClass().getName(), "xml (2):" + "id:" + ac.getId() + "action:" + ac.getType(), null);
                            }
                        }
                    }*/

                    //Write actions as entries to xml-file
                    XMLWriter writer = new XMLWriter(context);
                    writer.writeEntries(persistentAction);

                    //Work of stack, including previous saved actions and action which are added during the sync process.
                    XMLParser parser = new XMLParser(context);
                    do {
                        stackFinished = doStack(parser);
                        if (!stackFinished)
                            break;
                        parser = new XMLParser(context);
                    }while (parser.hasActions());

                    if (stackFinished) {
                        //everything downloaded
                        publishProgress(3);
                        Log.e(getClass().getName(), "do Stack was successful: " + stackFinished, null);
                        persistentAction.clear();

                        /*{
                            XMLParser par = new XMLParser(context);
                            if (par.hasActions()) {
                                for (Action ac : par.getActions()) {
                                    Log.e(getClass().getName(), "xml (3):" + "id:" + ac.getId() + "action:" + ac.getType(), null);
                                }
                            }
                        }*/
                    }
                }
            } else {
                stackFinished = true; //if there is nothing to sync (no Cloud Service connected) then Sync is always successful
            }
        } catch (ConnectException e) {
            Log.e(getClass().getName(), "[upper]Fetching persistent actions from Cloud failed: " + e.toString(), null);
            mException = e;
        } finally {
            //End of Synchronisation-------------------
            //db.close();
        }

        if (!stackFinished) {
            mException = new Exception("Stack was not completed");
        } else {
            //delete all serialized Recording entries, since you can be sure that they are not anymore needed
            // (Stack completed and therefore no reference to not existing entry possible)
            final File folder = context.getFilesDir();
            final File[] files = folder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File dir,
                                      final String name) {
                    return name.startsWith("deleted_");
                }
            });
            for (final File file : files) {
                if (!file.delete()) {
                    System.err.println("Can't remove " + file.getAbsolutePath());
                }
            }
        }
        return stackFinished;
    }

    public HashMap<String, String> hashingFolder(String folder) {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        File dir = new File(PATH_SD + "/" + folder);
        File[] directoryListing = dir.listFiles();

        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.isFile()) {
                    String hash = cloud.createHash(child);
                    //add to hashMap
                    hashMap.put(folder + "/" + child.getName(), hash);
                }
            }
        }

        /*//Logs
        for (String name : hashMap.keySet()) {
            String key = name.toString();
            String value = hashMap.get(name).toString();
            Log.e(getClass().getName(), key + " = " + value, null);
        }*/
        return hashMap;
    }

    //parses persistent actions (once a time) from file and works them off
    public boolean doStack(XMLParser parser) {
        Boolean performed = true; //Loop breaks if one action couldn't be performed (important for the Stack-prop)
        if (parser.hasActions()) {
            /*Log.e(getClass().getName(), "The following persistent actions where stored");
            for (Action action : parser.getActions()) {
                Log.e(getClass().getName(), "persistent: " + action.getType() + "|id: " + action.getId());
            }*/

            for (Action action : parser.getActions()) {
                //Get database entry
                Recording recording = db.recordingDao().findById(action.getId());
                if (recording == null) {
                    if (!(action.getType() == DELETE_ONLINE || (action.getType() == DELETE_OFFLINE))) {
                        Log.e(getClass().getName(), "------Invalid persistentAction was skipped-----", null);
                        parser.removeFirst();
                        continue;
                    } else {
                        try {
                            Log.e(getClass().getName(), "Reading from serialized recording ...", null);
                            FileInputStream fis = context.openFileInput("deleted_" + action.getId());
                            ObjectInputStream is = new ObjectInputStream(fis);
                            recording = (Recording) is.readObject();
                            if (recording == null) {
                                parser.removeFirst();
                                continue;
                            }
                            is.close();
                            fis.close();
                        } catch (Exception e) {
                            Log.e(getClass().getName(), "Problems with reading recording.ser!" + e.toString());
                            //This shouldn't happen
                            throw new AssertionError();
                        }
                    }
                }

                String fileName = recording.getFile();
                switch (action.getType()) {
                    case DOWNLOAD:
                        try {
                            File file = new File(PATH_CLOUD + "/" + fileName);
                            cloud.download(file);
                            recording.setDate(cloud.getDate(file));

                        } catch (ConnectException e) {
                            Log.e(getClass().getName(), "[1upper]Download failed: " + e.toString(), null);
                            mException = e;
                            performed = false;
                        } catch (UnexpectedException e) {
                            db.recordingDao().delete(recording);
                            e.printStackTrace();
                        }
                        if (performed) {
                            //toggle out-of-sync-flag
                            recording.setNotSynced(false);
                            db.recordingDao().update(recording);
                            Memo.updateMemos(recording, db);

                        }
                        break;
                    case DELETE_OFFLINE:
                        if (recording.getCachedFile()) {

                            File fileOffline = new File(PATH_SD + "/" + fileName);
                            performed = fileOffline.delete();
                            if (!performed)
                                Log.e(getClass().getName(), "[offline]File deleted (incl Entry): " + performed, null);

                            performed = true;
                        }

                        //with foreign key all ass. Memos are also deleted
                        if (performed) {

                            //serialize recording to use for eventual persistentActions which refer to that entity
                            try {
                                //get deleted Recording
                                Log.e(getClass().getName(), "Writing ser Recording ...", null);
                                FileOutputStream fos = context.openFileOutput("deleted_" + action.getId().toString(), Context.MODE_PRIVATE);
                                ObjectOutputStream os = new ObjectOutputStream(fos);
                                os.writeObject(recording);
                                os.close();
                                fos.close();
                            } catch (Exception e) {
                                Log.e(getClass().getName(), e.toString());
                            }

                            //delete recording entity of database (incl. all ass. Memos)
                            db.recordingDao().delete(recording);
                        }
                        break;

                    case DELETE_ONLINE:
                        //Delete online
                        try {
                            cloud.delete(new File(PATH_CLOUD + "/" + fileName));
                        } catch (ConnectException e) {
                            Log.e(getClass().getName(), "[upper]Delete online failed,for: " + fileName + "(->removing Action): Try again! " + "Error: " + e.toString(), null);
                            mException = e;
                            performed = false;
                            parser.removeFirst(); //should you continue with stack? Otherwise it will be stack forever there
                        } catch (UnexpectedException e) {
                            e.printStackTrace();
                        }

                        if (performed) {
                            cloud.updateCursor();
                        }
                        break;

                    case UPLOAD:

                        String hash = "";
                        try {
                            File file = new File(PATH_SD + "/" + fileName);
                            hash = cloud.upload(file);
                        } catch (ConnectException e) {
                            mException = e;
                            performed = false;
                        } catch (UnexpectedException e) {
                            e.printStackTrace();
                        }

                        if (performed) {
                            recording.setNotSynced(false);
                            recording.setHash(hash);
                            db.recordingDao().update(recording);
                            Memo.updateMemos(recording, db);
                            cloud.updateCursor();
                        }
                        break;
                    default:
                        Log.e(getClass().getName(), "Not catched action-type", null);
                        performed = false;
                }

                if (performed) {
                    //If action was performed, delete from Stack
                    parser.removeFirst();
                } else {
                    //rest of actions have to be de-serialized again and worked off when connection is available again
                    break;
                }
            }
        }

        //Every action was performed successfully
        File currentDBPath = context.getDatabasePath(context.getResources().getString(R.string.database_file));
        try {

            switch (cloud.uploadSystemFile(currentDBPath)) {
                case "upload-conflict":
                    publishProgress(-1);
                    return false;
                case "bad-connection" : //this can be pretty bad because the rev can be changed -> tactic: delete online file, and upload it automatically
                    Log.e(getClass().getName(), "Rev seem to be damaged, but no new import was detected. Deleting in cloud an reupload", null);
                    cloud.deleteSystemFile(currentDBPath);
                    cloud.uploadSystemFile(currentDBPath);
                    publishProgress(5);
                    return false;
                default:
                    ;;
            }
        } catch (ConnectException e) {
            mException = e;
            Log.e(getClass().getName(), "Uploading database not successful: " + context.getResources().getString(R.string.database_file), null);
            Log.e(getClass().getName(), "Uploading database not successful: " + currentDBPath + "bool: " + currentDBPath.exists(), null);
            Log.e(getClass().getName(), "Uploading database not successful: " + e.toString(), null);
            performed = false;
        } catch (UnexpectedException e) {
            e.printStackTrace();
        }
        return performed;
    }

    //Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public boolean isOnline() {
        return true;
    }
    public AppDatabase getDb() {
        return db;
    }
    public SharedPreferences getPrefs() {
        return prefs;
    }

    @Override
    /* inform main activity about progress  */
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        switch (values[0]) {

            case -1:
                AlertDialog.Builder importDialogBuilder = new AlertDialog.Builder(context)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Import existing file")
                        .setMessage("A existing database file was detected on your Cloud-Service. Do you want to import it? Note: Your existing entries will be deleted!")
                        .setPositiveButton("Import", yesButtonListener)
                        .setNegativeButton("No", noButtonListener);
                importDialog = importDialogBuilder.create();
                importDialog.show();

                break;
            case 0:
                Toast.makeText(context, "[sync] Starting sync", Toast.LENGTH_SHORT).show();
                break;

            case 1:
                Toast.makeText(context, "[sync] Searching for cloud", Toast.LENGTH_SHORT).show();
                break;

            case 2:
                Toast.makeText(context, "[sync] Connecting to cloud", Toast.LENGTH_SHORT).show();
                break;
            case 3:
                Toast.makeText(context, "[sync] Synchronisation finished", Toast.LENGTH_SHORT).show();
                break;

            case 5:
                Toast.makeText(context, "[error] Rev is damaged, pls report to me :-)", Toast.LENGTH_LONG).show();
            default:
                break;

        }
    }

    public interface Callback {
        void onDataLoaded(Boolean result);

        void onError(Exception e);
    }

    DialogInterface.OnClickListener yesButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(final DialogInterface dialog, int which) {
            //deleting database file
            final File db_file = context.getDatabasePath(context.getResources().getString(R.string.database_file));
            final String tag = "_new";
            //Restart Activity
            importDialog.setMessage("Waiting to download ...");
            prefs.edit().putBoolean("merge-db", true).apply();

            new DownloadSystemFileTask(cloud, db_file, tag, new DownloadSystemFileTask.Callback() {
                @Override
                public void onDataLoaded(String result) {
                    //Restarting activity:
                    dialog.cancel();
                    //showing the Activity screen
                    Intent intentMainActivity = new Intent();
                    intentMainActivity.putExtra("old_db_path", db_file.getAbsolutePath());
                    intentMainActivity.putExtra("new_db_path", db_file.getAbsolutePath() + tag);
                    intentMainActivity.putExtra("import-database", true);
                    intentMainActivity.setClass(context, MainActivity.class);
                    intentMainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intentMainActivity);
                }

                @Override
                public void onError(Exception e) {
                    importDialog.setMessage("Download failed, using your original database");
                }
            }).execute();
        }

        ;
    };

    DialogInterface.OnClickListener noButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(final DialogInterface dialogInterface, int i) {
            importDialog.setMessage("Discard found database. Doing the first sync, this might take a while");
            final File db_file = context.getDatabasePath(context.getResources().getString(R.string.database_file));

            new DeleteSystemFileTask(cloud, db_file, new DeleteSystemFileTask.Callback() {

                @Override
                public void onDataLoaded(String result) {
                    Log.e(getClass().getName(), "Delete System File task was successful.", null);
                    new AsyncSyncFolderTask(context, new Callback() {
                        @Override
                        public void onDataLoaded(Boolean result) {
                            dialogInterface.cancel();
                            prefs.edit().putBoolean("is-syncing", false).apply();
                        }

                        @Override
                        public void onError(Exception e) {
                            importDialog.setMessage("An error occured while syncing ... :-(" + "\n" + e.toString());
                            prefs.edit().putBoolean("is-syncing", false).apply();
                        }
                    }).execute();
                    prefs.edit().putString("cursor-db", " ").commit();
                }

                @Override
                public void onError(Exception e) {
                    Log.e(getClass().getName(), "Delete System File task was  NOT successful: "+e.toString(), null);
                    prefs.edit().putBoolean("is-syncing", false).apply();

                }
            }).execute();

        }
    };
}