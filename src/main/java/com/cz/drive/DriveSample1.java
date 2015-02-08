package com.cz.drive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ChildReference;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.*;

public class DriveSample1 {

    private static final String APPLICATION_NAME = "QA-reltio";
    private static final String FILE_NAME = "cz.cfg";
    private static final java.io.File MY_DRIVE_FILE = new java.io.File(FILE_NAME);
    private static final String UPLOAD_FILE_PATH = "C:\\Users\\Иосиф\\Downloads\\ERROR.JPG";
    private static final String DIR_FOR_DOWNLOADS = "C:\\Users\\Public\\Downloads\\";
    private static final java.io.File UPLOAD_FILE = new java.io.File(UPLOAD_FILE_PATH);
    private static final java.io.File DATA_STORE_DIR =
            new java.io.File(System.getProperty("user.home"), ".store/drive_sample");
    private static FileDataStoreFactory dataStoreFactory;
    private static HttpTransport httpTransport;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static Drive drive;
    private static final List<String> SCOPES = Arrays.asList("https://www.googleapis.com/auth/drive");
    /*private static HashMap<String, String> DriveIdEntries = new HashMap<String, String>();*/
    private static HashMap<String, String> DriveNameEntries = new HashMap<String, String>();


    private static Credential authorize() throws Exception {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(DriveSample.class.getResourceAsStream("/client_secret.json")));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets,
                Collections.singleton(DriveScopes.DRIVE_FILE)).setDataStoreFactory(dataStoreFactory)
                .setScopes(SCOPES)
                .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public static void makeFileMap(Drive service)
    {
        /*DriveIdEntries.put("root", "root");*/
        DriveNameEntries.put("root", "root");
        try {
            FileList files = service.files().list().execute();
            for (File file : files.getItems()) {
                /*DriveIdEntries.put(file.getTitle(), file.getId());*/
                DriveNameEntries.put(file.getId(), file.getTitle());
            }
        }
        catch (IOException e){e.printStackTrace();}
    }

    /**
     * Get name by ID
     * @param service
     * @param fileID
     * @return
     */
    public static  String getNameByID(Drive service, String fileID)
    {
        String fileName = null;
        try {
            FileList files = service.files().list().execute();
            for (File file : files.getItems())
                if (file.getId().equals(fileID)) return file.getTitle();
        }
        catch (IOException e){e.printStackTrace();}

        return fileName;
    }

    /**
     * Get File id by file name
     * @param service
     * @param filename
     * @return
     */
    private static String getFileID(Drive service, String filename)
    {
        String fileID = null;
        try {
            FileList files = service.files().list().execute();
            for (File file : files.getItems())
                if (file.getTitle().equals(filename)) return file.getId();
        }
        catch (IOException e){e.printStackTrace();}
        return fileID;
    }

    public static void main(String[] args) {
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
            Credential credential = authorize();
            drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(
                    APPLICATION_NAME).build();

            makeFileMap(drive);
            String fileID = getFileID("/folder2/new.txt");

            downloadFile(true, drive.files().get(fileID).execute());

        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        System.exit(1);
    }

    private static void downloadFile(boolean useDirectDownload, File uploadedFile)
            throws IOException {
        // create parent directory (if necessary)
        java.io.File parentDir = new java.io.File(DIR_FOR_DOWNLOADS);
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Unable to create parent directory");
        }
        OutputStream out = new FileOutputStream(new java.io.File(parentDir, uploadedFile.getTitle()));

        MediaHttpDownloader downloader =
                new MediaHttpDownloader(httpTransport, drive.getRequestFactory().getInitializer());
        downloader.setDirectDownloadEnabled(useDirectDownload);
        downloader.setProgressListener(new FileDownloadProgressListener());
        downloader.download(new GenericUrl(uploadedFile.getDownloadUrl()), out);
    }

    private static String getFileID(String path) {

        String[] folders = path.replaceFirst("^/", "").split("/");
        String currentEntryID = "root";
        for (int nesting = 0; nesting < folders.length; nesting++)
        {
            List<String> filesIDInCurrentDirectory = getFilesIDInFolder(currentEntryID);
            for (String fileID:filesIDInCurrentDirectory)
                if (getNameByID(drive, fileID).equals(folders[nesting])){
                    currentEntryID = fileID;
                    break;
                }
        }

        return currentEntryID;
    }

    private static List<String> getFilesIDInFolder(String fileID)
    {
        List<String> fileIDList = new ArrayList<String>();
        try
        {
            ChildList folderEntries = drive.children().list(fileID).execute();
            for (ChildReference child:folderEntries.getItems())
                fileIDList.add(child.getId() + "");
        }catch(IOException e)
              {e.printStackTrace();}
        return fileIDList;
    }

}
