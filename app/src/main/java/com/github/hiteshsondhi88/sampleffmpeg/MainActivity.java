package com.github.hiteshsondhi88.sampleffmpeg;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            FFmpeg ffmpeg = FFmpeg.getInstance();
            ffmpeg.loadBinary(null,
                    "", "");
        } catch (Exception e) {
            Log.d("ddd", "ddd");
        }
    }
}
