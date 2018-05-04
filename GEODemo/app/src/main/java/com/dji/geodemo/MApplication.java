package com.dji.geodemo;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.support.multidex.MultiDex;

import com.secneo.sdk.Helper;

//import com.secneo.sdk.Helper;

public class MApplication extends Application {

    public static GEODemoApplication mApplication;

    public void loadApplication(Context paramContext) {
        if (mApplication != null)
            return;
        mApplication = new GEODemoApplication(this);
    }

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);

        MultiDex.install(this);
        if (mApplication != null) {
            return;
        }
        Helper.install(this);
        loadApplication(paramContext);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApplication.onCreate();

    }

    @Override
    public void onConfigurationChanged(Configuration paramConfiguration) {
        super.onConfigurationChanged(paramConfiguration);
        mApplication.onConfigurationChanged(paramConfiguration);

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mApplication.onLowMemory();

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

}
