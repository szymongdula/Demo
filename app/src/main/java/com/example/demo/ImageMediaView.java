package com.example.demo;

import android.graphics.BitmapFactory;
import android.os.Looper;

public class ImageMediaView extends android.support.v7.widget.AppCompatImageView implements MediaViewInterface {
    private MediaDisplayActivity mediaDisplayActivity;
    private MediaView mediaView;

    private String _fileName;
    private String _parentViewLabel;
    private String _playedFiles;

    public ImageMediaView(MediaDisplayActivity context, MediaView mediaView) {
        super(context);

        this.mediaView = mediaView;

        _parentViewLabel = mediaView.getLabel();

        mediaDisplayActivity = context;
    }

    public void setFilePath(String filePath) {
        _fileName = Tools.getFileName(filePath);

        mediaDisplayActivity.log("MediaView (" + _parentViewLabel + ") -> ImageMediaView (" + _fileName + ") -> setFilePath");
        mediaDisplayActivity.log( " already played files: " + _playedFiles);

        _playedFiles = ((_playedFiles != null) ? _playedFiles + ", " : "") + _fileName;

        this.setImageBitmap(BitmapFactory.decodeFile(filePath));
    }

    public void onPlay() {
        boolean _isMainThread = (Looper.myLooper() == Looper.getMainLooper()) ? true : false;

        mediaDisplayActivity.log("MediaView (" + _parentViewLabel + ") -> ImageMediaView (" + _fileName + ") -> onPlay");
        mediaDisplayActivity.log("  running in main thread: " + String.valueOf(_isMainThread));

        try {
            Thread.sleep(mediaDisplayActivity.IMAGE_DISPLAY_DURATION - mediaDisplayActivity.TRANSITION_DURATION);

            while(mediaView.isLocked()) {
                Thread.sleep(100);
            }

            mediaDisplayActivity.log("MediaView (" + _parentViewLabel + ") -> VideoMediaView (" + _fileName + ") -> run transition");

            mediaDisplayActivity.mainHandler.obtainMessage(mediaDisplayActivity.PLAY_NEXT).sendToTarget();
        } catch (InterruptedException ex ) {
            mediaDisplayActivity.log("MediaView (" + _parentViewLabel + ") -> VideoMediaView (" + _fileName + ") -> ERROR: " + this.getClass().getName() + ":" +ex.getStackTrace()[0].getLineNumber());
        }
    }

    public void reset() {
        mediaDisplayActivity.log("MediaView (" + _parentViewLabel + ") -> ImageMediaView (" + _fileName + ") -> reset");

        _fileName = null;
        this.setImageDrawable(null);
    }

}
