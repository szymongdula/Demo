package com.example.demo;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;

public class MediaDisplayActivity extends AppCompatActivity {
    static final int PLAY_NEXT = 1;
    static final int RESET_VIEW = 2;
    static final int SKIP = 3;
    static final int LOG = 4;
    static final int START = 5;

    static boolean DUMP_LOG_ON_SCREEN = false;
    static boolean RESIZE_MEDIA_TO_COVER_SCREEN = false;

    static int TRANSITION_DURATION = 2500;
    static int IMAGE_DISPLAY_DURATION = 10000;
    static int CROP_VIDEO_DURATION_TO = 10000;

    private SharedPreferences config;
    private JSONObject mediaList;
    private Iterator<String> mediaIterator;
    private MediaView frontView;
    private MediaView backView;

    public Handler mainHandler;

    private ScrollView scrollView;
    private TextView logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        config = getSharedPreferences("config", MODE_PRIVATE);

        File file = new File("/etc/media_codecs.xml");

        Log.d("file.canRead()", String.valueOf(file.canRead()));

        //rewrite old config
        Boolean overwriteConfig = config.getBoolean("overwriteConfig", true);

        if (overwriteConfig) {
            config.edit()
                .putInt("cropVideoDurationTo", CROP_VIDEO_DURATION_TO)
                .putInt("imageDisplayDuration", IMAGE_DISPLAY_DURATION)
                .putInt("transitionDuration", TRANSITION_DURATION)
                .putBoolean("overwriteConfig", false)
                .commit();
        }

        DUMP_LOG_ON_SCREEN = config.getBoolean("dumpLog", DUMP_LOG_ON_SCREEN);
        RESIZE_MEDIA_TO_COVER_SCREEN = config.getBoolean("resizeMediaToCoverScreen", RESIZE_MEDIA_TO_COVER_SCREEN);
        TRANSITION_DURATION = config.getInt("transitionDuration", TRANSITION_DURATION);
        IMAGE_DISPLAY_DURATION = config.getInt("imageDisplayDuration", IMAGE_DISPLAY_DURATION);
        CROP_VIDEO_DURATION_TO = config.getInt("cropVideoDurationTo", CROP_VIDEO_DURATION_TO);

        int minTransDur = (int) Math.min((CROP_VIDEO_DURATION_TO > 0) ? CROP_VIDEO_DURATION_TO : 9999999 , IMAGE_DISPLAY_DURATION) / 2;

        if (TRANSITION_DURATION > minTransDur ) {
            TRANSITION_DURATION = (int) (minTransDur * 0.7);
        }

        try {
            mediaList = new JSONObject(config.getString("current_files_list", "{}"));
        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
        }

        setupMainHandler();

        final RelativeLayout root = new RelativeLayout(this);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        setupDebugConsole(displayMetrics, root);

        log("TRANSITION_DURATION: " + MediaDisplayActivity.TRANSITION_DURATION);
        log("IMAGE_DISPLAY_DURATION: " + MediaDisplayActivity.IMAGE_DISPLAY_DURATION);
        log("CROP_VIDEO_DURATION_TO: " + MediaDisplayActivity.CROP_VIDEO_DURATION_TO);

        root.setBackgroundColor(0xFF000000);

        setupViews(displayMetrics, root);

        setContentView(root);

        new Thread(new Runnable() {
            @Override
            public void run() {
                mainHandler.obtainMessage(START).sendToTarget();
            }
        }).start();
    }

    private void setupDebugConsole(DisplayMetrics displayMetrics, RelativeLayout root) {
        if (DUMP_LOG_ON_SCREEN) {
            int deviceWidth = displayMetrics.widthPixels;
            int deviceHeight = displayMetrics.heightPixels;

            scrollView = new ScrollView(this);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(deviceWidth, deviceHeight);

            params.leftMargin = 0;
            params.topMargin = 0;
            params.width = deviceWidth;
            params.height = deviceHeight;

            scrollView.setLayoutParams(params);
            scrollView.setBackgroundColor(0xFF000000);
            scrollView.setAlpha(0.3f);

            root.addView(scrollView);

            logView = new TextView(this);

            params = new RelativeLayout.LayoutParams(deviceWidth, deviceHeight);

            params.leftMargin = 0;
            params.topMargin = 0;
            params.width = deviceWidth;
            params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;

            logView.setLayoutParams(params);
            logView.setTextColor(0xFFFFFFFF);
            logView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);

            scrollView.addView(logView);
        }
    }

    private void setupViews(DisplayMetrics displayMetrics, RelativeLayout root) {
        int deviceWidth = displayMetrics.widthPixels;
        int deviceHeight = displayMetrics.heightPixels;

        float mediaAspectRatio = config.getFloat("aspectRatio", 0.0f);
        float deviceAspectRatio = (float) deviceWidth / deviceHeight;

        int width;
        int height;
        int horizontalMargin = 0;
        int verticalMargin = 0;

        if (RESIZE_MEDIA_TO_COVER_SCREEN) {
            if (deviceAspectRatio > mediaAspectRatio) {
                width = deviceWidth;
                height = (int) (deviceWidth / mediaAspectRatio);
                verticalMargin = -1 * (height - deviceHeight) / 2;
            } else if (deviceAspectRatio < mediaAspectRatio) {
                width = (int) (deviceHeight * mediaAspectRatio);
                height = deviceHeight;
                horizontalMargin = -1 * (width - deviceWidth) / 2;
            } else {
                width = deviceWidth;
                height = deviceHeight;
            }
        } else {
            if (deviceAspectRatio > mediaAspectRatio) {
                height = deviceHeight;
                width = (int) (height * mediaAspectRatio);
                horizontalMargin = (deviceWidth - width) / 2;
            } else if (deviceAspectRatio < mediaAspectRatio) {
                width = deviceWidth;
                height = (int) (width / mediaAspectRatio);
                verticalMargin = (deviceHeight - height) / 2;
            } else {
                width = deviceWidth;
                height = deviceHeight;
            }
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(deviceWidth, deviceHeight);

        params.leftMargin = 0;
        params.topMargin = 0;
        params.width = deviceWidth;
        params.height = deviceHeight;

        log( "  backView: A");
        log( "  frontView: B");

        backView = new MediaView(this, horizontalMargin, verticalMargin, width + horizontalMargin, height + verticalMargin, 0xFF000000, "A");
        frontView = new MediaView(this, horizontalMargin, verticalMargin, width + horizontalMargin, height + verticalMargin, 0xFF000000, "B");

        root.addView(backView, params);
        root.addView(frontView, params);
    }

    private void _log(String text) {
        if (DUMP_LOG_ON_SCREEN) {
            logView.setText(logView.getText() + ">>> " + text + "\n");

            scrollView.fullScroll(View.FOCUS_DOWN);
        } else {
            Log.d("DEMO DEBUG", text);
        }
    }

    public void log(String text) {
        if (DUMP_LOG_ON_SCREEN) {
            final Message message = mainHandler.obtainMessage(LOG);
            final Bundle bundle = new Bundle();

            bundle.putString("message", text);

            message.setData(bundle);
            message.sendToTarget();
        } else {
            Log.d("DEMO DEBUG", text);
        }
    }

    private void start() {
        frontView.setFile(getNext());
        backView.setFile(getNext());
        backView.setAlpha(0);
        frontView.bringToFront();

        if (DUMP_LOG_ON_SCREEN ) {
            scrollView.bringToFront();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                frontView.play();
            }
        }).start();
    }

    private void next() {
        log("");
        log("MediaDisplayAction -> next()");

        frontView
            .animate()
            .alpha(0)
            .setDuration(TRANSITION_DURATION)
            .withStartAction(new Runnable() {
                @Override
                public void run() {
                    backView.setLocked(true);
//                    log("Mediaview " + frontView.getLabel() + " (" + Tools.getFileName(frontView.filePath) + ") -> withStartAction");
//
//                    backView.setLocked(true);
//                    backView.setAlpha(1.0f);
//
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            backView.play();
//                        }
//                    }).start();
                }
            })
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    log("Mediaview " + frontView.getLabel() + " (" + Tools.getFileName(frontView.filePath) + ") -> withEndAction");

                    frontView.reset();

                    MediaView temp = frontView;

                    frontView = backView;
                    backView = temp;
                    temp = null;

                    log( "  AFTER VIEWS SWAP");
                    log( "  backView: " + backView.getLabel());
                    log( "  frontView: " + frontView.getLabel());

                    frontView.bringToFront();

                    if (DUMP_LOG_ON_SCREEN) {
                        scrollView.bringToFront();
                    }

                    frontView.setLocked(false);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            frontView.play();
                        }
                    }).start();

                    frontView
                        .animate()
                        .alpha(1)
                        .setDuration(TRANSITION_DURATION)
                        .start();

                    backView.setFile(getNext());
                }
            })
            .start();
    }

    private void setupMainHandler() {
        mainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SKIP:
                        break;
                    case PLAY_NEXT:
                        next();
                        break;
                    case RESET_VIEW:
                        frontView.reset();
                        break;
                    case LOG:
                        _log(msg.getData().getString("message"));
                        break;
                    case START:
                        start();
                        break;
                }
            }
        };
    }

    private String getNext() {
        if (mediaIterator == null || !mediaIterator.hasNext()) {
            mediaIterator = mediaList.keys();
        }

        try {
            String filePath = mediaList.getJSONObject(mediaIterator.next()).getString("localFilePath");

            return filePath;
        } catch (JSONException ex) {
            log(ex.getMessage());
        }

        log("MediaDisplayActivity -> getNext -> ERROR: Couldn't resolve next file name");

        return null;
    }

}