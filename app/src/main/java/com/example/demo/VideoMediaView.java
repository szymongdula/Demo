package com.example.demo;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;

public class VideoMediaView extends TextureView implements MediaViewInterface, TextureView.SurfaceTextureListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private MediaDisplayActivity mediaDisplayActivity;
    private MediaView mediaView;
    private MediaPlayer mediaPlayer;

    private Boolean transitionTriggered = false;
    private Boolean surfaceTextureAvailable = false;

    private String _fileName;
    private String _parentViewLabel;
    private String _playedFiles;

    public VideoMediaView(MediaDisplayActivity context, MediaView mediaView) {
        super(context);

        this.mediaView = mediaView;

        _parentViewLabel = mediaView.getLabel();

        mediaDisplayActivity = context;

        initMediaPlayer();

        setSurfaceTextureListener(this);

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mediaDisplayActivity.log("ERROR: " + _fileName + ", code: " + String.valueOf(what)+ ", extra: " + String.valueOf(extra));

                return false;
            }
        });
    }

    public void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setLooping(false);

        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);

        if (surfaceTextureAvailable) {
            mediaPlayer.setSurface(new Surface(getSurfaceTexture()));
        }
    }

    public void setFilePath(String filePath) {
        _fileName = Tools.getFileName(filePath);

        mediaDisplayActivity.log("MediaView (" + _parentViewLabel + ") -> VideoMediaView (" + _fileName + ") -> setFilePath");
        mediaDisplayActivity.log( " already played files: " + _playedFiles);

        _playedFiles = ((_playedFiles != null) ? _playedFiles + ", " : "") + _fileName;

        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
//            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        surfaceTextureAvailable = true;

        mediaPlayer.setSurface(new Surface(surfaceTexture));
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaDisplayActivity.log("MediaView (" + _parentViewLabel + ") -> VideoMediaView (" + _fileName + ") -> onPrepared");

        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                if (mediaPlayer.isPlaying()) {
                    return;
                }

                mediaPlayer.seekTo(0);
            }
        }).start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        releaseMediaPlayer();

        return true;
    }

    private boolean test = false;

    private int fps = 25;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        private void tick() {
            timerHandler.postDelayed(this, 1000/fps);
        }

        @Override
        public void run() {
            if (!mediaPlayer.isPlaying()) {
                tick();

                return;
            }

            int duration;
            int currentPosition = mediaPlayer.getCurrentPosition();

            if (mediaDisplayActivity.CROP_VIDEO_DURATION_TO > 0) {
                duration = mediaDisplayActivity.CROP_VIDEO_DURATION_TO;
            } else {
                duration = mediaPlayer.getDuration();
            }

            if (transitionTriggered == false && !mediaView.isLocked() && currentPosition >= duration - mediaDisplayActivity.TRANSITION_DURATION) {
                mediaDisplayActivity.log("MediaView (" + _parentViewLabel + ") -> VideoMediaView (" + _fileName + ") -> run transition");

                transitionTriggered = true;

                mediaDisplayActivity.mainHandler.obtainMessage(mediaDisplayActivity.PLAY_NEXT).sendToTarget();
            }

            if (currentPosition <= mediaDisplayActivity.TRANSITION_DURATION ) {
                float volume = (float) currentPosition / mediaDisplayActivity.TRANSITION_DURATION;

//            mediaDisplayActivity.log(_fileName + "volumne: " + volume);

                mediaPlayer.setVolume(volume, volume);
            } else if ( !test ) {
                mediaPlayer.setVolume(1.0f, 1.0f);

                test = true;
            }

            if (transitionTriggered == true) {
                float volume = ((float) duration - mediaPlayer.getCurrentPosition()) / mediaDisplayActivity.TRANSITION_DURATION;

                if (volume > 0) {
//                mediaDisplayActivity.log(_fileName + "volumne: " + volume);

                    mediaPlayer.setVolume(volume, volume);
                } else {
                    mediaPlayer.setVolume(0.0f, 0.0f);
                }
            }

            //timerHandler.postDelayed(this, 1000/fps);
            tick();
        }
    };

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
    }

    public void onPlay() {
        try {
            boolean _isMainThread = (Looper.myLooper() == Looper.getMainLooper()) ? true : false;

            mediaDisplayActivity.log("MediaView (" + _parentViewLabel + ") -> VideoMediaView (" + _fileName + ") -> onPlay");
            mediaDisplayActivity.log("  running in main thread: " + String.valueOf(_isMainThread));

            transitionTriggered = false;

            mediaPlayer.start();
            timerHandler.postDelayed(timerRunnable, 0);
        } catch (Exception ex) {
            mediaDisplayActivity.log("ERROR: " + ex.getMessage());
        }
    }

    public void reset() {
        mediaDisplayActivity.log("MediaView (" + _parentViewLabel + ") -> VideoMediaView (" + _fileName + ") -> reset");

        timerHandler.removeCallbacks(timerRunnable);

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        transitionTriggered = false;
        test = false;

        mediaPlayer.reset();

//        releaseMediaPlayer();
//        initMediaPlayer();
    }

    private void releaseMediaPlayer() {
        mediaPlayer.release();
    }

}
