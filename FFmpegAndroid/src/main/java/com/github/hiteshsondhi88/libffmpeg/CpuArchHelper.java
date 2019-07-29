package com.github.hiteshsondhi88.libffmpeg;

import android.os.Build;

class CpuArchHelper {

    public static final String ARM_64_CPU = "arm64-v8a";
    public static final String ARM_V7_CPU = "armeabi-v7a";

    static CpuArch getCpuArch() {
        Log.d("Build.CPU_ABI : " + Build.CPU_ABI);

        switch (Build.CPU_ABI) {
            case ARM_64_CPU:
                return CpuArch.ARMv8;
            case ARM_V7_CPU:
                return CpuArch.ARMv7;
            default:
                return CpuArch.NONE;
        }
    }
}
