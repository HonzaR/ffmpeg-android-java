package com.github.hiteshsondhi88.libffmpeg;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.File;

import static com.github.hiteshsondhi88.libffmpeg.FFmpeg.ERROR_LIB_CAN_NOT_BE_LOADED;
import static com.github.hiteshsondhi88.libffmpeg.FFmpeg.ERROR_LOAD_LIB_NOT_ENOUGH_FREE_SPACE;
import static com.github.hiteshsondhi88.libffmpeg.FFmpeg.ERROR_LOAD_LIB_NO_INTERNET_CONNECTION;
import static com.github.hiteshsondhi88.libffmpeg.FFmpeg.SUCCESS_DOWNLOADING_DONE;
import static com.github.hiteshsondhi88.libffmpeg.FFmpeg.SUCCESS_DOWNLOADING_STARTED;
import static com.github.hiteshsondhi88.libffmpeg.FFmpeg.SUCCESS_INITIALIZATION_DONE;

public class FFmpegLoadLibraryAsyncTask extends AsyncTask<Void, Void, Integer> {

    private static final long NEEDED_FREE_MEMORY_SPACE = 50L;

    private final String cpuArchName;
    private final FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler;
    private final Context context;

    FFmpegLoadLibraryAsyncTask(Context context,
                               String cpuArchNameFromAssets,
                               FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler)
    {
        this.context = context;
        this.cpuArchName = cpuArchNameFromAssets;
        this.ffmpegLoadBinaryResponseHandler = ffmpegLoadBinaryResponseHandler;
    }

    @Override
    protected Integer doInBackground(Void... params)
    {
        File ffmpegFile = new File(FileUtils.getFFmpeg(context));

        if (!ffmpegFile.exists()) {

            long externalFreeSpace = Util.BToMB(Util.getStorageFreeSpace(context.getExternalFilesDir(null)));
            long internalFreeSpace = Util.BToMB(Util.getStorageFreeSpace(context.getFilesDir()));
            long neededMemorySize = NEEDED_FREE_MEMORY_SPACE;

            if (neededMemorySize >= externalFreeSpace) {   // not enough free space
                return ERROR_LOAD_LIB_NOT_ENOUGH_FREE_SPACE;
            }
            if (neededMemorySize >= internalFreeSpace) {   // not enough free space
                return ERROR_LOAD_LIB_NOT_ENOUGH_FREE_SPACE;
            }

            String assetName = cpuArchName + File.separator + FileUtils.FFMPEG_FILE_NAME;

            boolean isFileCopied = FileUtils.checkAssetFileExists(context, assetName) && FileUtils.copyBinaryFromAssetsToData(context, cpuArchName + File.separator + FileUtils.FFMPEG_FILE_NAME, FileUtils.FFMPEG_FILE_NAME);

            if (isFileCopied && loadLibraryAndFinalize(ffmpegFile, true)) {
                return SUCCESS_INITIALIZATION_DONE;
            }

            if (!Util.checkInternetConnection(context)) {
                return ERROR_LOAD_LIB_NO_INTERNET_CONNECTION;
            }
        }

        return loadLibraryAndFinalize(ffmpegFile, true) ? SUCCESS_INITIALIZATION_DONE : ERROR_LIB_CAN_NOT_BE_LOADED;
    }

    private boolean loadLibraryAndFinalize(File ffmpegFile, boolean isCopied)
    {
        if(!ffmpegFile.canExecute()) {
            ffmpegFile.setExecutable(true);
        }

        return isCopied && ffmpegFile.exists() && ffmpegFile.canExecute();
    }

    @Override
    protected void onPostExecute(Integer state) {
        super.onPostExecute(state);

        if (ffmpegLoadBinaryResponseHandler != null) {
            ffmpegLoadBinaryResponseHandler.onLoadResult(state);
        }
    }
}
