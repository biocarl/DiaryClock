package com.example.carlh.diaryclock.cloud;

import android.content.Context;
import android.util.Log;

import com.dropbox.core.DbxApiException;
import com.dropbox.core.DbxException;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.RetryException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.DbxClientV2Base;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.files.WriteMode;
import com.example.carlh.diaryclock.UnexpectedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by carlh on 25.05.2017.
 */

public class DropboxCloud extends Cloud<Metadata> {

    private DbxClientV2 mDbxClient;

    public DropboxCloud(Context context) {
        super(context);
    }

    @Override
    public boolean init() {
        mDbxClient = DropboxClientFactory.getClient();
        Log.e(getClass().getName(), "starting Dropbox-Client", null);
        return (mDbxClient != null);
    }

    @Override
    public String createHash(File file) {
        MessageDigest hasher = new DropboxContentHasher();
        byte[] buf = new byte[1024];
        InputStream in = null;
        try {
            in = new FileInputStream(file.getAbsolutePath());
            while (true) {
                int n = in.read(buf);
                if (n < 0) break;  // EOF
                hasher.update(buf, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return DropboxContentHasher.hex(hasher.digest());
    }

    @Override
    public boolean mkdir() {
        try {
            mDbxClient.files().createFolder(PATH_CLOUD);
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean mkdir(String relativePath) {
        try {
            mDbxClient.files().createFolder(PATH_CLOUD + "/" + relativePath);
        } catch (DbxException e) {
           // e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean delete(File filePath) throws ConnectException,UnexpectedException {
        String dropboxPath = PATH_CLOUD + "/" + filePath.getName();
        Metadata metadata = null;
        try {
            DbxClientV2Base dbxClient = DropboxClientFactory.getClient();
            metadata = dbxClient.files().delete(dropboxPath);
            Log.e(getClass().getName(), "Delete File in Cloud: " + filePath.getName() + "metadata:" + metadata.toString(), null);
        }catch (DbxApiException e){
            throw new UnexpectedException("class:"+getClass()+"method: ");
        } catch (InvalidAccessTokenException | RetryException e) {
            throw new ConnectException("Error deleting in Dropbox:" + e.toString());
        } catch (DbxException ex) {
            ex.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean deleteSystemFile(File filePath) throws ConnectException,UnexpectedException {
        String dropboxPath = PATH_CLOUD + "/system/" + filePath.getName();
        Metadata metadata = null;
        try {
            DbxClientV2Base dbxClient = DropboxClientFactory.getClient();
            metadata = dbxClient.files().delete(dropboxPath);
            preferences.edit().putString("db-revision-number", "").apply(); //Dummy rev
            Log.e(getClass().getName(), "Delete File in Cloud: " + filePath.getName() + "metadata:" + metadata.toString(), null);
        }catch (DbxApiException e){
            throw new UnexpectedException("class:"+getClass()+"method: ");
        } catch (InvalidAccessTokenException | RetryException e) {
            throw new ConnectException("Error deleting in Dropbox:" + e.toString());
        } catch (DbxException ex) {
            ex.printStackTrace();
        }
        return true;
    }

    @Override
    public void download(File path) throws ConnectException,UnexpectedException {
        File cloudPath = new File(PATH_CLOUD + "/" + path.getName());
        File offlinePath = new File(PATH_STORAGE + "/" + path.getName());
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(offlinePath);
            FileMetadata metadata = DropboxClientFactory.getClient().files().download(cloudPath.getAbsolutePath()).download(outputStream);
        }catch (DbxApiException e){
            throw new UnexpectedException("class:"+getClass()+"method: download");
        } catch (InvalidAccessTokenException | RetryException e) {
            throw new ConnectException("Error deleting in Dropbox:" + e.toString());
        } catch (DbxException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e(getClass().getName(), "Downloaded: " + cloudPath.getName(), null);
        }
    }

    @Override
    public void downloadSystemFile(File offlinePath,String tag) throws ConnectException,UnexpectedException {
        String dropboxPath = PATH_CLOUD + "/system/" + offlinePath.getName();
        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(offlinePath+tag);
            FileMetadata metadata = DropboxClientFactory.getClient().files().download(dropboxPath).download(outputStream);
            preferences.edit().putString("db-revision-number", metadata.getRev()).apply();
        }catch (DbxApiException e){
            throw new UnexpectedException("class:"+getClass()+"method: downloadSystemFile");
        } catch (InvalidAccessTokenException | RetryException e) {
            throw new ConnectException("Error deleting in Dropbox:" + e.toString());
        } catch (DbxException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e(getClass().getName(), "Downloaded: " + dropboxPath, null);
        }
    }

    @Override
    public String fetchCursor(String cursorString) {
        String newCursor = null;
        try {
            if (cursorString != null && !(cursorString.length() <=1)) {
                newCursor = mDbxClient.files().listFolderContinue(cursorString).getCursor();
            } else {
                newCursor = mDbxClient.files().listFolderBuilder(PATH_CLOUD).withRecursive(true).start().getCursor();
            }
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return newCursor;
    }

    @Override
    public String upload(File filepath) throws ConnectException,UnexpectedException {

        File localFile = new File(PATH_STORAGE + "/" + filepath.getName());
        String dropboxPath = PATH_CLOUD + "/" + filepath.getName();
        String fileHash = null;

        try (InputStream in = new FileInputStream(localFile)) {
            DbxClientV2Base dbxClient = DropboxClientFactory.getClient();

            FileMetadata metadata = dbxClient.files().uploadBuilder(dropboxPath)
                    .withMode(WriteMode.ADD)
                    .withClientModified(new Date(localFile.lastModified()))
                    .uploadAndFinish(in);

            Log.e(getClass().getName(), "Upload File to Cloud: " + filepath.getName() + "metadata:" + metadata.toString(), null);
            //for storing Hash database
            fileHash = metadata.getContentHash();
        } catch (UploadErrorException ex) {
            throw new UnexpectedException("class:"+getClass()+"method: upload"+"||message"+ex.toString());
        }catch (DbxApiException e){
            throw new UnexpectedException("class:"+getClass()+"method: upload"+"||message"+e.toString());
        } catch (InvalidAccessTokenException | RetryException e) {
            throw new ConnectException("Error deleting in Dropbox:" + e.toString());
        } catch (DbxException ex) {
            throw new ConnectException("Error deleting in Dropbox:" + ex.toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return fileHash;
    }

    @Override
    public String uploadSystemFile(File pathAbsolute) throws ConnectException,UnexpectedException {
        //start
        updateCursor();

        //Create Folder if not existent
        mkdir("system");

        String dropboxPath = PATH_CLOUD + "/system/" + pathAbsolute.getName();
        String fileHash = null;
        String rev = preferences.getString("db-revision-number", "");

        try (InputStream in = new FileInputStream(pathAbsolute)) {
            DbxClientV2Base dbxClient = DropboxClientFactory.getClient();
            FileMetadata oldMetadata = null;
            FileMetadata metadata = null;

            try {
                oldMetadata = (FileMetadata) dbxClient.files().getMetadata(dropboxPath);
            } catch (Exception e) {}

            if (oldMetadata == null) {
                Log.e(getClass().getName(), "[db import]Old file is not found", null);
                metadata = dbxClient.files().uploadBuilder(dropboxPath)
                        .withMode(WriteMode.OVERWRITE)//tag
                        .withClientModified(new Date(pathAbsolute.lastModified()))
                        .uploadAndFinish(in);
                preferences.edit().putString("db-revision-number", metadata.getRev()).apply();

            } else { //already file existent
                if (rev.contentEquals(oldMetadata.getRev())) { //Still the same rev
                    Log.e(getClass().getName(), "[db import]Files have the same rev", null);
                    metadata = dbxClient.files().uploadBuilder(dropboxPath)
                            .withMode(WriteMode.update(oldMetadata.getRev()))//tag
                            .withClientModified(new Date(pathAbsolute.lastModified()))
                            .uploadAndFinish(in);
                    preferences.edit().putString("db-revision-number", metadata.getRev()).apply();
                } else {//different rev, probably the file was substituted -> Mode: first detected, with two different databases
                    //But this shouldn't happen -> someone has substitued the db file in the dropbox
                    Log.e(getClass().getName(), "[db import]Files have different rev - shouldn't happen", null);
                    return "upload-conflict";
                }
            }

            Log.e(getClass().getName(), "Upload File to Cloud: " + pathAbsolute.getName() + "metadata:" + metadata.toString(), null);
            fileHash = metadata.getContentHash();
            updateCursor();

        }catch (NetworkIOException e){
            e.printStackTrace();
            return "bad-connection";
        }catch (DbxApiException e){
            throw new UnexpectedException("class:"+getClass()+"method: uploadSystemfile"+"||message"+e.toString());
        } catch (InvalidAccessTokenException | RetryException e) {
            throw new ConnectException("Error deleting in Dropbox:" + e.toString());
        } catch (DbxException ex) {
            throw new ConnectException("Error deleting in Dropbox:" + ex.toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return fileHash;
    }


    @Override
    public Iterator<Metadata> listEntries(String cursorString, String pathCloud) throws ConnectException,UnexpectedException {

        ListFolderResult result = null;
        try {
            //case there is no cursor available all files for download
            if (cursorString != null && !(cursorString.length() <=1)) {
                //download everything recursively
                result = mDbxClient.files().listFolderContinue(cursorString);
            } else {
                //do comparison
                result = mDbxClient.files().listFolderBuilder(PATH_CLOUD).withRecursive(true).start();
            }

        }catch (DbxApiException e){
            throw new UnexpectedException("class:"+getClass()+"method: listEntries"+"||message"+e.toString());
        } catch (InvalidAccessTokenException | RetryException e) {
            throw new ConnectException("Error deleting in Dropbox:" + e.toString());
        } catch (DbxException ex) {
            throw new ConnectException("Error deleting in Dropbox:" + ex.toString());
        }
        return result.getEntries().iterator();
    }

    @Override
    public Type getType(Metadata metadata) {
        if (metadata instanceof FileMetadata) {
            return Type.FILE;
        } else if (metadata instanceof FolderMetadata) {
            return Type.FOLDER;
        } else if (metadata instanceof DeletedMetadata) {
            return Type.DELETED;
        } else {
            throw new IllegalStateException("Unrecognized metadata type: " + metadata.getClass());
        }
    }

    @Override
    public Metadata getMetadata(File file) throws ConnectException,UnexpectedException {
        String dropboxPath = PATH_CLOUD + "/system/" + file.getName();
        DbxClientV2Base dbxClient = DropboxClientFactory.getClient();
        FileMetadata metadata = null;
        try {
            metadata = (FileMetadata) dbxClient.files().getMetadata(dropboxPath);
        }catch (DbxApiException e){
            throw new UnexpectedException("class:"+getClass()+"method: getmetadata"+"||message"+e.toString());
        } catch (InvalidAccessTokenException | RetryException e) {
            throw new ConnectException("Error deleting in Dropbox:" + e.toString());
        } catch (DbxException ex) {
            throw new ConnectException("Error deleting in Dropbox:" + ex.toString());
        }
        return metadata;
    }

    @Override
    public String getPath(Metadata metadata) {
        String string = "";
        if ((metadata instanceof DeletedMetadata)) {
            DeletedMetadata deletedMetadata = (DeletedMetadata) metadata;
            string = deletedMetadata.getPathLower();
        }

        if ((metadata instanceof FileMetadata)) {
            FileMetadata fileMetadata = (FileMetadata) metadata;
            string = fileMetadata.getPathLower();
        }

        if ((metadata instanceof FolderMetadata)) {
            FolderMetadata folderMetadata = (FolderMetadata) metadata;
            string = folderMetadata.getPathLower();
        }

        return string.replaceAll("^/", "");
    }

    @Override
    public String getName(Metadata metadata) {
        return metadata.getName();
    }

    @Override
    public Date getDate(File file) throws ConnectException,UnexpectedException {
        File cloudPath = new File(PATH_CLOUD + "/" + file.getName());
        FileMetadata fileMetadata = null;

        try {
            fileMetadata = (FileMetadata) DropboxClientFactory.getClient().files().getMetadata(cloudPath.getAbsolutePath());

        }catch (DbxApiException e){
            throw new UnexpectedException("class:"+getClass()+"method: getdate"+"||message"+e.toString());
        } catch (InvalidAccessTokenException | RetryException e) {
            throw new ConnectException("Error deleting in Dropbox:" + e.toString());
        } catch (DbxException ex) {
            throw new ConnectException("Error deleting in Dropbox:" + ex.toString());
        }
        return fileMetadata.getServerModified();
    }

    @Override
    public String getHash(Metadata metadata) {
        if ((metadata instanceof DeletedMetadata)) {
            return null;
        }
        if (!(metadata instanceof FileMetadata))
            return null;

        FileMetadata fileMetadata = (FileMetadata) metadata;
        return fileMetadata.getContentHash();
    }
}
