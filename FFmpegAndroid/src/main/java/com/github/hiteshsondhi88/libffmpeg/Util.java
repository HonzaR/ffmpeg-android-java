package com.github.hiteshsondhi88.libffmpeg;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StatFs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;

class Util {

    static boolean isDebug(Context context)
    {
        return (0 != (context.getApplicationContext().getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
    }

    static void close(InputStream inputStream)
    {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Do nothing
            }
        }
    }

    static void close(OutputStream outputStream)
    {
        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                // Do nothing
            }
        }
    }

    public static long kBToMB(long kilobytes)
    {
        return kilobytes / 1024;
    }

    public static long BToMB(long freeSpace)
    {
        return kBToMB(freeSpace) / 1024;
    }

    static String convertInputStreamToString(InputStream inputStream)
    {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            String str;
            StringBuilder sb = new StringBuilder();
            while ((str = r.readLine()) != null) {
                sb.append(str);
            }
            return sb.toString();
        } catch (IOException e) {
            Log.e("error converting input stream to string", e);
        }
        return null;
    }

    static void destroyProcess(Process process)
    {
        if (process != null)
            process.destroy();
    }

    static boolean killAsync(AsyncTask asyncTask)
    {
        return asyncTask != null && !asyncTask.isCancelled() && asyncTask.cancel(true);
    }

    static boolean isProcessCompleted(Process process)
    {
        try {
            if (process == null) return true;
            process.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            // do nothing
        }
        return false;
    }

    static long getStorageFreeSpace(File storage)
    {
        long availableSpace = -1L;
        try {
            StatFs e = new StatFs(storage.getAbsolutePath());
            e.restat(storage.getAbsolutePath());
            if(Build.VERSION.SDK_INT >= 18) {
                availableSpace = e.getAvailableBlocksLong() * e.getBlockSizeLong();
            } else {
                availableSpace = (long)(e.getAvailableBlocks() * e.getBlockSize());
            }
        } catch (Exception var5) {
            Log.e(var5);
        }

        return availableSpace;
    }

    static boolean checkInternetConnection(Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    static boolean isDeviceFFmpegVersionOld(Context context)
    {
        return CpuArch.fromString(FileUtils.SHA1(FileUtils.getFFmpeg(context))).equals(CpuArch.NONE);
    }

    static boolean stopFFmpegProcess(Process process)
    {
        int pid = getPid(process);

        android.os.Process.sendSignal(pid, 15); // 15 is the value for SIG_TERM
        return true;
    }

    static int getPid(java.lang.Process p)
    {
        int pid = -1;

        try {
            Field f = p.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            pid = f.getInt(p);
            f.setAccessible(false);
        } catch (Throwable e) {
            pid = -1;
        }
        return pid;
    }
}
