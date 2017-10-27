package com.github.hiteshsondhi88.libffmpeg;

public interface FFmpegExecuteResponseHandler {

    /**
     * on Success
     * @param message complete output of the FFmpeg command
     */
    void onSuccess(String message);

    /**
     * on Progress
     * @param message current output of FFmpeg command
     */
    void onProgress(String message);

    /**
     * on Failure
     * @param message complete output of the FFmpeg command
     */
    void onFailure(String message);

    /**
     * on Start
     */
    void onStart();

    /**
     * on Finish
     */
    void onFinish();
}
