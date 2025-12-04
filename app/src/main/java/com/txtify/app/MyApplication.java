package com.txtify.app;

import android.content.Context;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

// This is the definitive, corrected version for MultiDex support in AIDE.
public class MyApplication extends MultiDexApplication {

    // NEW: Reference to the AppOpenManager
    private AppOpenManager appOpenManager;

    // This method is called BEFORE onCreate(). It's the safest place to install MultiDex.
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            MultiDex.install(this);
        } catch (Exception e) {
            // This is a critical failure, but we'll let the app continue
            // and potentially crash later with a more detailed message.
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // This line sets up our crash handler. It will now be activated
        // after the critical MultiDex setup is complete.
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));

        // NEW: Initialize the Google Mobile Ads SDK
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                // Ads SDK initialized successfully
            }
        });

        // NEW: Initialize the AppOpenManager to handle "Reopen" ads
        appOpenManager = new AppOpenManager(this);
    }
}