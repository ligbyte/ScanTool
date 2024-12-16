package com.hello.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.hello.scan.R;

public class SoundUtil {

    public static SoundUtil sSoundUtil;

    private MediaPlayer mMediaPlayer;

    private SoundUtil() {

    }

    public static SoundUtil get() {
        if (sSoundUtil == null) {
            sSoundUtil = new SoundUtil();
        }
        return sSoundUtil;
    }

    public void init(Context pContext) {
        if (mMediaPlayer == null) {
            Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + pContext.getPackageName() + "/raw/beep");
            mMediaPlayer = MediaPlayer.create(pContext, uri);
        }
    }

    public void playSound() {
        if (mMediaPlayer == null) {
            return;
        }
        Log.e("Hello", "SoundUtil.playSound == > 播放声音");
        mMediaPlayer.start();
    }

    public void release() {
        if (mMediaPlayer == null) {
            return;
        }
        try {
            if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
        } catch (Exception ignored) {
        }
        mMediaPlayer.release();
        sSoundUtil = null;
    }
}
