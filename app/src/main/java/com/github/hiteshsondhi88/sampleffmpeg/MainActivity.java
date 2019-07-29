package com.github.hiteshsondhi88.sampleffmpeg;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.util.Log;
import android.view.Display;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class MainActivity extends Activity {

    private FFmpeg ffmpeg;
    private File sourceFile;
    private File resultFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadFFmpegBinary("title", "message");
    }

    public void loadFFmpegBinary(String title, String message)
    {
        try {
            ffmpeg = FFmpeg.getFirstInstance();
            ffmpeg.loadBinary(
                    MainActivity.this,
                    loadBinaryResponseHandler,
                    title,
                    message);

        } catch (Exception e) {
            Log.e("FFMPEG", e.toString());
        }
    }

    private FFmpegLoadBinaryResponseHandler loadBinaryResponseHandler = new FFmpegLoadBinaryResponseHandler() {
        @Override
        public void onLoadResult(int state) {

            switch (state) {
                case FFmpeg.SUCCESS_INITIALIZATION_DONE:
                    Log.d("FFMPEG", "ffmpeg initialized successfully");

                    sourceFile = getFileFromAssets("video.mp4");
                    resultFile = new File(getExternalFilesDir(null) + "/" + "video_proceed.mp4");

                    processTestVideo();

                    break;
                case FFmpeg.SUCCESS_DOWNLOADING_STARTED:
                    Log.d("FFMPEG", "ffmpeg download started");
                    break;
                case FFmpeg.SUCCESS_DOWNLOADING_DONE:
                    Log.d("FFMPEG","ffmpeg download done");
                    break;
                case FFmpeg.ERROR_LIB_CAN_NOT_BE_LOADED:
                    Log.e("FFMPEG", "ffmpeg can not be loaded");
                    break;
                case FFmpeg.ERROR_DEVICE_NOT_SUPPORTED:
                    Log.e("FFMPEG", "ffmpeg unsupported device");
                    break;
                case FFmpeg.ERROR_LOAD_LIB_NO_INTERNET_CONNECTION:
                    Log.e("FFMPEG", "ffmpeg no internet connection");
                    break;
                case FFmpeg.ERROR_LOAD_LIB_NOT_ENOUGH_FREE_SPACE:
                    Log.e("FFMPEG", "ffmpeg not enough free space");
                    break;
            }
        }
    };

    private ExecuteBinaryResponseHandler executeBinaryResponseHandler = new ExecuteBinaryResponseHandler() {
        @Override
        public void onFailure(String s) {
            Log.e("FFMPEG","FAILED with output : " + s);
        }

        @Override
        public void onSuccess(String s) {

            if (resultFile == null || !resultFile.exists()) {
                Log.e("FFMPEG", "Corrupted file!");
                return;
            }

            Log.i("FFMPEG", "SUCCESS with output : " + s);
        }

        @Override
        public void onProgress(String s) {
            Log.i("FFMPEG", "Progress : " + s);
        }

        @Override
        public void onStart() {
            Log.i("FFMPEG", "Start compile");
        }

        @Override
        public void onFinish() {
            Log.e("FFMPEG", "Finish");
        }
    };

    private File getFileFromAssets(String assetName)
    {
        File f = new File(getExternalCacheDir() + "/" + assetName);

        if (!f.exists()) try {

            InputStream is = getAssets().open(assetName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return f;
    }

    private void processTestVideo()
    {
        if (sourceFile == null || !sourceFile.isFile() || sourceFile.length() == 0)
            return;

        String filter = getResolutionFilter();

        String[] command = {"-y", "-ss", "10.0", "-i", sourceFile.getAbsolutePath(), "-t", "5.0", "-vf", filter, "-metadata:s:v:0", "rotate=0", "-vcodec", "libx264", "-preset", "superfast", "-pix_fmt", "yuv420p", "-b:v", "40M", "-bt", "4M", "-r", "23.976", "-acodec", "aac", "-ab", "128k", "-ac", "1", "-ar", "48k", "-strict", "-2", resultFile.getAbsolutePath()};

        execFFmpegBinary(command);
    }

    private int[] getScreenResolution()
    {
        int[] resolution = new int[2];

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        resolution[0] = 1080;//size.x;
        resolution[1] = 1920;//size.y;

        return resolution;
    }

    private String getResolutionFilter()
    {
        int[] resolution = getScreenResolution();
        String resolutionString = resolution[1] + ":" + resolution[0];
        return "scale=" + resolutionString + ":force_original_aspect_ratio=decrease,pad=" + resolutionString + ":(ow-iw)/2:(oh-ih)/2";
    }

    private void execFFmpegBinary(final String[] command)
    {
        try {
            ffmpeg.execute(command, executeBinaryResponseHandler);
        } catch (Exception e) {
            Log.e("FFMPEG: ", e.toString());
        }
    }
}
