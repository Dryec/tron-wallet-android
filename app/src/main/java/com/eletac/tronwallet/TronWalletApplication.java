package com.eletac.tronwallet;

import android.app.Application;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.arch.lifecycle.ProcessLifecycleOwner;

public class TronWalletApplication extends Application implements  LifecycleObserver{

    public static final String FOREGROUND_CHANGED = "com.eletac.tronwallet.app.foreground_changed";

    private static Context context;
    private static boolean mIsInForeground;

    public void onCreate() {
        super.onCreate();
        TronWalletApplication.context = getApplicationContext();
        mIsInForeground = false;
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    public static Context getAppContext() {
        return TronWalletApplication.context;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void onAppBackgrounded() {
        mIsInForeground = false;
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(FOREGROUND_CHANGED));
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void onAppForegrounded() {
        mIsInForeground = true;
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(FOREGROUND_CHANGED));
    }

    public static boolean isIsInForeground() {
        return mIsInForeground;
    }
}
