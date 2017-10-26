package com.github.hiteshsondhi88.libffmpeg;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;

class FFmpegLoadLibraryAsyncTask extends AsyncTask<Void, Void, Integer> {

    private static final int SUCCESS_LOAD_LIB = 600;
    private static final int SUCCESS_LOADING_STARTED = 601;
    private static final int ERROR_LIB_CAN_NOT_BE_LOADED = 610;
    private static final int ERROR_LOAD_LIB_NOT_ENOUGH_FREE_SPACE = 611;
    private static final int ERROR_LOAD_LIB_NO_INTERNET_CONNECTION = 612;

    private static final long NEEDED_FREE_MEMORY_SPACE = 50L;
    private static final String DOWNLOAD_LIB_X86 = "https://drive.google.com/uc?authuser=0&id=0B6oNTFuzvl9ncm4yd0x1Y1pKZEU&export=download";
    private static final String DOWNLOAD_LIB_ARM = "https://drive.google.com/uc?authuser=0&id=0B6oNTFuzvl9nZ1d2NnRydHpwc1U&export=download";

    private final String cpuArchName;
    private final FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler;
    private final Context context;
    private Long downloadReference;

    FFmpegLoadLibraryAsyncTask(Context context, String cpuArchNameFromAssets, FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler) {
        this.context = context;
        this.cpuArchName = cpuArchNameFromAssets;
        this.ffmpegLoadBinaryResponseHandler = ffmpegLoadBinaryResponseHandler;
    }

    @Override
    protected Integer doInBackground(Void... params)
    {
        File ffmpegFile = new File(FileUtils.getFFmpeg(context));

        if (ffmpegFile.exists() && Util.isDeviceFFmpegVersionOld(context) && !ffmpegFile.delete()) {
            startDownloadLibraryFile();
        }

        if (!ffmpegFile.exists()) {

            if (!Util.checkInternetConnection(context)) {
                return ERROR_LOAD_LIB_NO_INTERNET_CONNECTION;
            }

            long freeSpace = Util.BToMB(Util.getStorageFreeSpace(context.getFilesDir()));
            long neededMemorySize = NEEDED_FREE_MEMORY_SPACE;

            if (neededMemorySize >= freeSpace) {   // not enough free space
                return ERROR_LOAD_LIB_NOT_ENOUGH_FREE_SPACE;
            }

            startDownloadLibraryFile();
            return SUCCESS_LOADING_STARTED;
        }

        return loadLibraryAndFinalize(ffmpegFile, true);
    }

    private int loadLibraryAndFinalize(File ffmpegFile, boolean isCopied)
    {
        if(!ffmpegFile.canExecute()) {
            ffmpegFile.setExecutable(true);
        }

        if (isCopied && ffmpegFile.exists() && ffmpegFile.canExecute()) {
            return SUCCESS_LOAD_LIB;
        } else {
            return ERROR_LIB_CAN_NOT_BE_LOADED;
        }
    }

    @Override
    protected void onPostExecute(Integer state) {
        super.onPostExecute(state);

        if (ffmpegLoadBinaryResponseHandler != null) {
            ffmpegLoadBinaryResponseHandler.onResult(state);

            if (state != SUCCESS_LOADING_STARTED)
                ffmpegLoadBinaryResponseHandler.onFinish();
        }
    }

    private void startDownloadLibraryFile()
    {
        IntentFilter filter = new IntentFilter();               // set filter and register broadcast receiver
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        context.registerReceiver(downloadReceiver, filter);

        String fileUrl;
        if (cpuArchName.equals("x86")) {
            fileUrl = DOWNLOAD_LIB_X86;
        } else {
            fileUrl = DOWNLOAD_LIB_ARM;
        }
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setAllowedOverRoaming(false);
        request.setTitle("Everyday");
        request.setDescription("Loading app components...");
        request.setDestinationInExternalFilesDir(context, null, FileUtils.ffmpegFileName);

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
                    File downloaded = new File(context.getExternalFilesDir(null) + "/" + FileUtils.ffmpegFileName);

                    if (FileUtils.checkFileExists(downloaded) && downloaded.length() > 0) {

                        isFileCopied = FileUtils.copyBinaryFromExternalToData(
                                context,
                                FileUtils.ffmpegFileName,
                                FileUtils.ffmpegFileName);
                    }

                    File ffmpegFile = new File(FileUtils.getFFmpeg(context));
                    int result = loadLibraryAndFinalize(ffmpegFile, isFileCopied);

                    ffmpegLoadBinaryResponseHandler.onResult(result);
                    ffmpegLoadBinaryResponseHandler.onFinish();
                }


            } else if (intent.getAction().equals(DownloadManager.ACTION_NOTIFICATION_CLICKED)) {
                Toast.makeText(context, "Wait please...", Toast.LENGTH_LONG).show();
            }
        }
    };
}
