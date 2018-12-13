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

    //private static final String DOWNLOAD_LIB_X86 = "https://drive.google.com/uc?authuser=0&id=0B6oNTFuzvl9ncm4yd0x1Y1pKZEU&export=download";
    //private static final String DOWNLOAD_LIB_ARM = "https://drive.google.com/uc?authuser=0&id=0B6oNTFuzvl9nZ1d2NnRydHpwc1U&export=download";

    private static final String DOWNLOAD_LIB_X86 = "https://firebasestorage.googleapis.com/v0/b/everyday-c1985.appspot.com/o/public%2Flibs%2Fffmpeg_x86?alt=media&token=7725b97e-0e30-4efe-a41e-434226e42e27";
    private static final String DOWNLOAD_LIB_ARM = "https://firebasestorage.googleapis.com/v0/b/everyday-c1985.appspot.com/o/public%2Flibs%2Fffmpeg_armeabi-v7a?alt=media&token=05faea50-56b7-43b1-96b4-4093da95c7e8";

    private final String cpuArchName;
    private final FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler;
    private final String loadingTitle;
    private final String loadingMsg;
    private final Context context;
    private Long downloadReference;

    FFmpegLoadLibraryAsyncTask(Context context,
                               String cpuArchNameFromAssets,
                               FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler,
                               String loadingTitle,
                               String loadingMsg)
    {
        this.context = context;
        this.cpuArchName = cpuArchNameFromAssets;
        this.ffmpegLoadBinaryResponseHandler = ffmpegLoadBinaryResponseHandler;
        this.loadingTitle = loadingTitle;
        this.loadingMsg = loadingMsg;
    }

    @Override
    protected Integer doInBackground(Void... params)
    {
        File ffmpegFile = new File(FileUtils.getFFmpeg(context));

        // just for sure, we can download better ffmpeg
        if (ffmpegFile.exists() && Util.isDeviceFFmpegVersionOld(context) && !ffmpegFile.delete()) {
            startDownloadLibraryFile(context);
        }

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

            boolean isFileCopied = FileUtils.copyBinaryFromAssetsToData(context,
                    cpuArchName + File.separator + FileUtils.FFMPEG_FILE_NAME,
                    FileUtils.FFMPEG_FILE_NAME);

            if (isFileCopied && loadLibraryAndFinalize(ffmpegFile, true)) {
                return SUCCESS_INITIALIZATION_DONE;
            }

            if (!Util.checkInternetConnection(context)) {
                return ERROR_LOAD_LIB_NO_INTERNET_CONNECTION;
            }

            startDownloadLibraryFile(context);
            return SUCCESS_DOWNLOADING_STARTED;
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

    boolean isRunning() {
        return this.getStatus() == Status.PENDING || this.getStatus() == Status.RUNNING;
    }

    boolean stop() {
        return this.cancel(true);
    }

    private void startDownloadLibraryFile(Context context)
    {
        IntentFilter filter = new IntentFilter();               // set filter and register broadcast receiver
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        context.registerReceiver(downloadReceiver, filter);

        String fileUrl = "";
        if (cpuArchName.equals(FFmpeg.DEVICE_ARCHITECTURE_X86)) {
            fileUrl = DOWNLOAD_LIB_X86;
        } else if (cpuArchName.equals(FFmpeg.DEVICE_ARCHITECTURE_ARMEABI_V7A)) {
            fileUrl = DOWNLOAD_LIB_ARM;
        }
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setAllowedOverRoaming(false);
        request.setTitle(loadingTitle);
        request.setDescription(loadingMsg);
        request.setDestinationInExternalFilesDir(context, null, FileUtils.FFMPEG_FILE_NAME);

        downloadReference = downloadManager.enqueue(request);
    }

    //
    //  INNER CLASSES
    //

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {

                boolean isFileCopied = false;
                long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

                if (referenceId == downloadReference) {
                    File downloaded = new File(context.getExternalFilesDir(null) + "/" + FileUtils.FFMPEG_FILE_NAME);

                    if (FileUtils.checkFileExists(downloaded) && downloaded.length() > 0) {

                        isFileCopied = FileUtils.copyBinaryFromExternalToData(
                                context,
                                FileUtils.FFMPEG_FILE_NAME,
                                FileUtils.FFMPEG_FILE_NAME);
                    }

                    FileUtils.removeFromExternal(context);
                    ffmpegLoadBinaryResponseHandler.onLoadResult(SUCCESS_DOWNLOADING_DONE);

                    File ffmpegFile = new File(FileUtils.getFFmpeg(context));
                    boolean result = loadLibraryAndFinalize(ffmpegFile, isFileCopied);

                    ffmpegLoadBinaryResponseHandler.onLoadResult(result ? SUCCESS_INITIALIZATION_DONE : ERROR_LIB_CAN_NOT_BE_LOADED);
                }
            }
        }
    };
}
