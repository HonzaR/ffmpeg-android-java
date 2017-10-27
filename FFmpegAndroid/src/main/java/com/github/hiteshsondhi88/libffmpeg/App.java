package com.github.hiteshsondhi88.libffmpeg;

import android.app.Application;

/**
 * Created by Honza Rychnovský on 27.10.17.
 * honzar@appsdevteam.com
 * Apps Dev Team
 */

public class App extends Application {

    private static App instance;
    public static App get() { return instance; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
