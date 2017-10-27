package com.github.hiteshsondhi88.libffmpeg;

interface FFmpegLoadBinaryResponseHandler extends ResponseHandler {

    void onLoadResult(int state);
}
