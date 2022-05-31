package com.example.demo;

import android.os.Looper;
import android.os.Message;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

import android.os.Handler;

public class MediaView extends ViewGroup {
    public String filePath;
    private String fileType;

    private VideoMediaView videoView;
    private ImageMediaView imageView;

    private MediaDisplayActivity mediaDisplayActivity;

    private int left;
    private int top;
    private int right;
    private int bottom;

    private boolean isLocked = false;

    private String _label;
    private String _playedFiles;
    private TextView _labelView;
    private TextView _labelChildView;

    public MediaView(MediaDisplayActivity context, int l, int t, int r, int b, int bg, String lb) {
        super(context);

        _label = lb;

        left = l;
        top = t;
        right = r;
        bottom = b;

        mediaDisplayActivity = context;

        videoView = new VideoMediaView(context, this);
        imageView = new ImageMediaView(context, this);

        videoView.setWillNotDraw(true);
        imageView.setWillNotDraw(true);

        addView(videoView);
        addView(imageView);

        this.setBackgroundColor(bg);

        _labelView = new TextView(context);

        _labelView.setText("View " + _label);
        _labelView.setTextColor(0xFFFFFFFF);
        _labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);

        addView(_labelView);
    }

    public void setLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setFile(String filePath) {
        mediaDisplayActivity.log("MediaView (" + _label + ") -> setFile " + Tools.getFileName(filePath));
        mediaDisplayActivity.log( " already played files: " + _playedFiles);

        _labelView.setText("View " + _label + " > " + Tools.getFileName(filePath));

        _playedFiles = ((_playedFiles != null) ? _playedFiles + ", " : "") + Tools.getFileName(filePath);

        if (fileType != null) {
            reset();
        }

        this.filePath = filePath;

        String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);

        if (extension == null) {
            skip();

            return;
        }

        fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).split("/")[0];

        switch(fileType) {
            case "image":
                videoView.setWillNotDraw(true);
                imageView.setWillNotDraw(false);
                imageView.setFilePath(filePath);
                break;
            case "video":
                imageView.setWillNotDraw(true);
                videoView.setWillNotDraw(false);
                videoView.setFilePath(filePath);
                break;
            default:
                skip();
        }
    }

    public String getLabel() {
        return _label;
    }

    public void play() {
        if (fileType == null) {
            return;
        }

        switch(fileType) {
            case "image":
                imageView.onPlay();
                break;
            case "video":
                videoView.onPlay();
                break;
        }
    }

    public void skip() {

    }

    public void reset() {
        if (fileType == null) {
            return;
        }

        switch(fileType) {
            case "image":
                imageView.reset();
                break;
            case "video":
                videoView.reset();
                break;
        }

        fileType = null;

        videoView.setWillNotDraw(true);
        imageView.setWillNotDraw(true);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();

        for(int i=0; i<childCount;i++) {
            View v = getChildAt(i);

            v.layout(left, top, right, bottom);

        }
    }
}
