package com.github.hiteshsondhi88.sampleffmpeg;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;

public class MainActivity extends Activity {

    private FFmpeg ffmpeg;
    private IFFmpegLoadListener callbacksLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadFFmpegBinary(iFFmpegLoadListener, "title", "message");
    }

    public void loadFFmpegBinary(IFFmpegLoadListener callbacksLoad, String title, String message)
    {
        this.callbacksLoad = callbacksLoad;
        try {
            ffmpeg = FFmpeg.getInstance();
            ffmpeg.loadBinary(
                    MainActivity.this,
                    loadBinaryResponseHandler,
                    title,
                    message);

        } catch (Exception e) {
            Log.e("FFMPEG", e.toString());
            callbacksLoad.onFFmpegCanNotBeLoaded();
        }
    }

    private FFmpegLoadBinaryResponseHandler loadBinaryResponseHandler = new FFmpegLoadBinaryResponseHandler() {
        @Override
        public void onLoadResult(int state) {

            switch (state) {
                case FFmpeg.SUCCESS_INITIALIZATION_DONE:
                    callbacksLoad.onFFmpegLoadDone();
                    break;
                case FFmpeg.SUCCESS_DOWNLOADING_STARTED:
                    callbacksLoad.onFFmpegDownloadStart();
                    break;
                case FFmpeg.SUCCESS_DOWNLOADING_DONE:
                    callbacksLoad.onFFmpegDownloadDone();
                    break;
                case FFmpeg.ERROR_LIB_CAN_NOT_BE_LOADED:
                    callbacksLoad.onFFmpegCanNotBeLoaded();
                    break;
                case FFmpeg.ERROR_DEVICE_NOT_SUPPORTED:
                    callbacksLoad.onFFmpegDeviceUnsupported();
                    break;
                case FFmpeg.ERROR_LOAD_LIB_NO_INTERNET_CONNECTION:
                    callbacksLoad.onFFmpegNoInternet();
                    break;
                case FFmpeg.ERROR_LOAD_LIB_NOT_ENOUGH_FREE_SPACE:
                    callbacksLoad.onFFmpegMemoryFull();
                    break;
            }
        }
    };

    IFFmpegLoadListener iFFmpegLoadListener = new IFFmpegLoadListener() {

        @Override
        public void onFFmpegLoadDone() {
            Log.d("FFMPEG","ffmpeg loaded");
        }

        @Override
        public void onFFmpegDownloadStart()
        {
            Log.d("FFMPEG", "ffmpeg download started");
        }

        @Override
        public void onFFmpegDownloadDone()
        {
            Log.d("FFMPEG","ffmpeg download done");
        }

        @Override
        public void onFFmpegCanNotBeLoaded()
        {
            Log.d("FFMPEG", "ffmpeg can not be loaded");
        }

        @Override
        public void onFFmpegDeviceUnsupported()
        {
            Log.d("FFMPEG", "ffmpeg unsupported device");
        }

        @Override
        public void onFFmpegNoInternet()
        {
            Log.d("FFMPEG", "ffmpeg no internet connection");
        }

        @Override
        public void onFFmpegMemoryFull()
        {
            Log.d("FFMPEG", "ffmpeg not enough free space");
        }
    };

    public interface IFFmpegLoadListener {
        void onFFmpegLoadDone();
        void onFFmpegDownloadStart();
        void onFFmpegDownloadDone();
        void onFFmpegCanNotBeLoaded();
        void onFFmpegDeviceUnsupported();
        void onFFmpegNoInternet();
        void onFFmpegMemoryFull();
    }
}
