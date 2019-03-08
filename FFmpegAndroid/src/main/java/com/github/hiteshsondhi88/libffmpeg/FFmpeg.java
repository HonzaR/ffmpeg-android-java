package com.github.hiteshsondhi88.libffmpeg;

import android.content.Context;
import android.text.TextUtils;

import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

import java.lang.reflect.Array;
import java.util.Map;

@SuppressWarnings("unused")
public class FFmpeg implements FFmpegInterface {

    static final String DEVICE_ARCHITECTURE_X86 = "x86";
    static final String DEVICE_ARCHITECTURE_ARMEABI_V7A = "armeabi-v7a";

    public static final int SUCCESS_INITIALIZATION_DONE = 600;
    public static final int SUCCESS_DOWNLOADING_STARTED = 601;
    public static final int SUCCESS_DOWNLOADING_DONE = 602;
    public static final int ERROR_LIB_CAN_NOT_BE_LOADED = 610;
    public static final int ERROR_LOAD_LIB_NOT_ENOUGH_FREE_SPACE = 611;
    public static final int ERROR_LOAD_LIB_NO_INTERNET_CONNECTION = 612;
    public static final int ERROR_DEVICE_NOT_SUPPORTED = 613;

    private static final long MINIMUM_TIMEOUT = 10 * 1000;

    private Context context;
    private FFmpegExecuteAsyncTask ffmpegExecuteAsyncTask;
    private FFmpegLoadLibraryAsyncTask ffmpegLoadLibraryAsyncTask;

    private long timeout = Long.MAX_VALUE;

    private static FFmpeg firstInstance = null;
    private static FFmpeg secondInstance = null;

    private FFmpeg() {
    }

    public static FFmpeg getFirstInstance() {
        if (firstInstance == null) {
            firstInstance = new FFmpeg();
        }
        return firstInstance;
    }
    public static FFmpeg getSecondInstance() {
        if (secondInstance == null) {
            secondInstance = new FFmpeg();
        }
        return secondInstance;
    }

    @Override
    public void loadBinary(Context context, FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler, String loadingTitle, String loadingMsg, String[] remoteLibsLinks)
    {
        this.context = context;
        Log.setDEBUG(Util.isDebug(this.context));

        String cpuArchNameFromAssets = null;
        switch (CpuArchHelper.getCpuArch()) {
            case x86:
                Log.i("Loading FFmpeg for x86 CPU");
                cpuArchNameFromAssets = DEVICE_ARCHITECTURE_X86;
                break;
            case ARMv7:
                Log.i("Loading FFmpeg for armv7 CPU");
                cpuArchNameFromAssets = DEVICE_ARCHITECTURE_ARMEABI_V7A;
                break;
            case NONE:
                ffmpegLoadBinaryResponseHandler.onLoadResult(ERROR_DEVICE_NOT_SUPPORTED);
        }

        if (!TextUtils.isEmpty(cpuArchNameFromAssets)) {
            ffmpegLoadLibraryAsyncTask = new FFmpegLoadLibraryAsyncTask(context, cpuArchNameFromAssets, ffmpegLoadBinaryResponseHandler, loadingTitle, loadingMsg, remoteLibsLinks);
            ffmpegLoadLibraryAsyncTask.execute();
        } else {
            ffmpegLoadBinaryResponseHandler.onLoadResult(ERROR_DEVICE_NOT_SUPPORTED);
        }
    }

    @Override
    public void execute(Map<String, String> environvenmentVars, String[] cmd, FFmpegExecuteResponseHandler ffmpegExecuteResponseHandler) throws FFmpegCommandAlreadyRunningException {
        if (ffmpegExecuteAsyncTask != null && !ffmpegExecuteAsyncTask.isProcessCompleted()) {
            throw new FFmpegCommandAlreadyRunningException("FFmpeg command is already running, you are only allowed to run single command at a time");
        }
        if (cmd.length != 0) {
            String[] ffmpegBinary = new String[] { FileUtils.getFFmpeg(context, environvenmentVars) };
            String[] command = concatenate(ffmpegBinary, cmd);
            ffmpegExecuteAsyncTask = new FFmpegExecuteAsyncTask(command , timeout, ffmpegExecuteResponseHandler);
            ffmpegExecuteAsyncTask.execute();
        } else {
            throw new IllegalArgumentException("shell command cannot be empty");
        }
    }

    public <T> T[] concatenate (T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    @Override
    public void execute(String[] cmd, FFmpegExecuteResponseHandler ffmpegExecuteResponseHandler) throws FFmpegCommandAlreadyRunningException {
        execute(null, cmd, ffmpegExecuteResponseHandler);
    }

    @Override
    public String getDeviceFFmpegVersion() throws FFmpegCommandAlreadyRunningException {
        ShellCommand shellCommand = new ShellCommand();
        CommandResult commandResult = shellCommand.runWaitFor(new String[] { FileUtils.getFFmpeg(context), "-version" });
        if (commandResult.success) {
            return commandResult.output.split(" ")[2];
        }
        // if unable to find version then return "" to avoid NPE
        return "";
    }

    @Override
    public String getLibraryFFmpegVersion() {
        return context.getString(R.string.shipped_ffmpeg_version);
    }

    @Override
    public boolean isFFmpegCommandRunning() {
        return ffmpegExecuteAsyncTask != null
                && !ffmpegExecuteAsyncTask.isProcessCompleted()
                && ffmpegExecuteAsyncTask.isRunning();
    }

    @Override
    public boolean killRunningProcesses() {

        if (ffmpegLoadLibraryAsyncTask != null && ffmpegLoadLibraryAsyncTask.isRunning())
            return ffmpegLoadLibraryAsyncTask.stop();

        if (ffmpegExecuteAsyncTask != null && ffmpegExecuteAsyncTask.isRunning())
            return ffmpegExecuteAsyncTask.stop();

        return true;
    }

    @Override
    public void setTimeout(long timeout) {
        if (timeout >= MINIMUM_TIMEOUT) {
            this.timeout = timeout;
        }
    }
}
