package com.example.demo;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

public class LaunchActivity extends AppCompatActivity {
    static final int RECEIVE_MEDIA_LIST = 1;
    static final int UPDATE_MEDIA_FILES = 2;
    static final int RESET_DOWNLOAD_QUEUE = 3;

    static final int PLAY_MEDIA_LIST = 11;
    static final int UPDATE_LOADER = 12;
    static final int MAKE_TOAST = 13;

    static final String CHANNEL_ID = "DEMO_NOTIFICATION_CHANNEL";

    private SharedPreferences config;

    private HandlerThread updaterThread;
    private Handler updaterHandler;
    private Handler mainHandler;

    private TextView launchLabel;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        launchLabel = (TextView) findViewById(R.id.launchLabel);
        launchLabel.setText("Initializing app...");

        setupConfig();
        setupMainHandler();
        verifyStoragePermissions(this);

//        Intent intent = new Intent(getApplicationContext(), MediaDisplayActivity.class);
//        startActivity(intent);
    }

    protected void onDestroy() {
        if (updaterThread != null) {
            updaterThread.quit();
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {  // permissions granted.
                    whenPermissionGranted();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    Activity currentActivity = this;

                    builder.setPositiveButton(R.string.ok_button_title, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                currentActivity.finish();
                                System.exit(0);
                            }
                        })
                        .setMessage(R.string.permision_dialog_message)
                        .setTitle(R.string.permision_dialog_title);

                    AlertDialog dialog = builder.create();

                    dialog.show();
                }

                return;
            }
        }
    }

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        } else {
            whenPermissionGranted();
        }
    }

    private void makeToast(String msg) {
        mainHandler.sendMessage(mainHandler.obtainMessage(MAKE_TOAST, msg));
    }

    private void setLabel(String msg) {
        mainHandler.sendMessage(mainHandler.obtainMessage(UPDATE_LOADER, msg));
    }

    private void whenPermissionGranted() {
        setupUpdater();
    }

    protected void setupConfig() {
        config = getSharedPreferences("config", MODE_PRIVATE);
    }

    protected void setupMainHandler() {
        mainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case PLAY_MEDIA_LIST:
                        Intent intent = new Intent(getApplicationContext(), MediaDisplayActivity.class);
                        startActivity(intent);

                        break;
                    case UPDATE_LOADER:
                        launchLabel.setText(msg.obj.toString());
                        break;

                    case MAKE_TOAST:
                        Toast toast = Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT);
                        toast.show();

                        break;
                }
            }
        };
    }

    protected void setupUpdater() {
        updaterThread = new HandlerThread("UpdaterThread", HandlerThread.NORM_PRIORITY);
        updaterThread.start();

        updaterHandler = new Handler(updaterThread.getLooper());
        updaterHandler.post(new UpdateMediaListTask());
    }

    private class UpdateMediaListTask implements Runnable {

        public void run() {
            makeToast("Loading media list...");

            String jsonResponse = getJSONResponse();

            if (jsonResponse == null) {
                makeToast("An error occured. Server response is empty.");

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex ) {
                    Log.e(this.getClass().getName(), ex.getMessage());
                }

                return;
            }

            makeToast("Parsing media list...");

            String lastUpdateHash = config.getString("last_update_hash", "");
            String newHash = Tools.md5(jsonResponse);

            if (!lastUpdateHash.equals(newHash)) {
                makeToast("Updating media list...");

                try {
                    JSONObject responseObject = new JSONObject(jsonResponse);
                    JSONObject newFiles = responseObject.getJSONObject("files");
                    JSONObject currentFiles = new JSONObject(config.getString("current_files_list", "{}"));

                    Iterator<String> currentFilesIterator = currentFiles.keys();

                    while (currentFilesIterator.hasNext()) {
                        String key = currentFilesIterator.next();
                        JSONObject value = currentFiles.getJSONObject(key);

                        if (!newFiles.has(key) || value.getLong("lastSavedTime") != newFiles.getJSONObject(key).getLong("lastSavedTime")) {
                            //delete file
                        }
                    }

                    config.edit()
                        .putString("last_update_hash", newHash)
                        .putFloat("aspectRatio", (float) responseObject.getDouble("aspectRatio"))
                        .putString("current_files_list", newFiles.toString())
                        .commit();

                } catch (JSONException e) {
                    Log.e("JSON Parser", "Error parsing data " + e.toString());
                }
            } else {
                makeToast("Media list is up to date.");
            }

            setupDownload();
        }

        private void setupDownload() {
            try {
                JSONObject currentFiles = new JSONObject(config.getString("current_files_list", "{}"));
                JSONObject downloadedFiles = new JSONObject(config.getString("downloaded_files", "{}"));

                Iterator<String> currentFilesIterator = currentFiles.keys();

                while (currentFilesIterator.hasNext()) {
                    String key = currentFilesIterator.next();
                    JSONObject value = currentFiles.getJSONObject(key);

                    if (!value.has("downloaded")) {
                        updaterHandler.post(new DownloadMediaFilesTask(key, value.getInt("fileSizeKb")));
                    }
                }

                updaterHandler.post(new PlayMediaListTask());

            } catch (JSONException e) {
                Log.e("JSON Parser", "Error parsing data " + e.toString());
            }

        }

        private String getJSONResponse() {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(getResources().getString(R.string.data_url));
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                }

                return buffer.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }

    private class DownloadMediaFilesTask implements Runnable {
        String url;
        int fileSizeKb;

        public DownloadMediaFilesTask(String url, int fileSizeKb) {
            this.url = url;
            this.fileSizeKb = fileSizeKb;
        }

        public void run() {
            makeToast("Downloading file " + this.url + "...");

            String fileName = this.url.substring(this.url.lastIndexOf('/') + 1, this.url.length());
            String localFilePath = Environment.getExternalStorageDirectory().toString() + "/" + fileName;

//            File file = new File(localFilePath);
//
//            if (file.exists()) {
//                try {
//                    JSONObject currentFiles = new JSONObject(config.getString("current_files_list", "{}"));
//
//                    JSONObject fileEntry = currentFiles.getJSONObject(this.url);
//
//                    fileEntry.put("downloaded", true);
//                    fileEntry.put("localFilePath", localFilePath);
//
//                    currentFiles.put(this.url, fileEntry);
//
//                    config.edit()
//                            .putString("current_files_list", currentFiles.toString())
//                            .commit();
//
//                } catch (JSONException ex) {
//                }
//            } else {
                int count;

                try {
                    URL url = new URL(this.url);
                    URLConnection connection = url.openConnection();

                    connection.connect();

                    int lenghtOfFile = connection.getContentLength();

                    InputStream input = new BufferedInputStream(url.openStream(), 8192);
                    OutputStream output = new FileOutputStream(localFilePath);

                    byte data[] = new byte[1024];
                    long total = 0;

                    while ((count = input.read(data)) != -1) {
                        total += count;

                        setLabel("Downloading file " + fileName + "... (" + (int) ((total * 100) / lenghtOfFile) + "%)");

                        output.write(data, 0, count);
                    }

                    output.flush();
                    output.close();
                    input.close();

                    JSONObject currentFiles = new JSONObject(config.getString("current_files_list", "{}"));

                    JSONObject fileEntry = currentFiles.getJSONObject(this.url);

                    fileEntry.put("downloaded", true);
                    fileEntry.put("localFilePath", localFilePath);

                    currentFiles.put(this.url, fileEntry);

                    config.edit()
                            .putString("current_files_list", currentFiles.toString())
                            .commit();

                } catch (Exception e) {
                    Log.e("Error: ", e.getMessage());
                }
//            }
        }
    }

    private class PlayMediaListTask implements Runnable {
        public void run() {
            setLabel("Running player...");

            mainHandler.sendMessage(mainHandler.obtainMessage(PLAY_MEDIA_LIST, ""));
        }
    }

}
