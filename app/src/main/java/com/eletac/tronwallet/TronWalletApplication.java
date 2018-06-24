package com.eletac.tronwallet;

import android.app.Application;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.arch.lifecycle.ProcessLifecycleOwner;

import com.eletac.tronwallet.block_explorer.BlockExplorerUpdater;
import com.eletac.tronwallet.database.TronWalletDatabase;
import com.eletac.tronwallet.wallet.AccountUpdater;
import com.eletac.tronwallet.wallet.PriceUpdater;
import com.facebook.stetho.Stetho;

import java.util.HashMap;
import java.util.Map;

public class TronWalletApplication extends Application implements  LifecycleObserver{

    public static final String DATABASE_FILE_NAME = "tron_wallet.db";

    public static final String FOREGROUND_CHANGED = "com.eletac.tronwallet.app.foreground_changed";

    public static final long BLOCKCHAIN_UPDATE_INTERVAL = Long.MAX_VALUE; // not used - using singleShots only
    public static final long NETWORK_UPDATE_INTERVAL = Long.MAX_VALUE; // not used - using singleShots only
    public static final long TOKENS_UPDATE_INTERVAL = Long.MAX_VALUE; // not used - using singleShots only
    public static final long ACCOUNTS_UPDATE_INTERVAL = Long.MAX_VALUE; // not used - using singleShots only

    public static final long ACCOUNT_UPDATE_FOREGROUND_INTERVAL = 2000;
    public static final long ACCOUNT_UPDATE_BACKGROUND_INTERVAL = 15000;
    public static final long PRICE_UPDATE_INTERVAL = 15000;

    private static Context mContext;
    private static TronWalletDatabase mDatabase;
    private static boolean mIsInForeground;

    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);

        mContext = getApplicationContext();
        mDatabase = Room.databaseBuilder(mContext, TronWalletDatabase.class, DATABASE_FILE_NAME)
                .addMigrations(TronWalletDatabase.MIGRATION_1_2)
                .build();
        mIsInForeground = false;
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        Map<BlockExplorerUpdater.UpdateTask, Long> updaterIntervals = new HashMap<>();
        updaterIntervals.put(BlockExplorerUpdater.UpdateTask.Blockchain, BLOCKCHAIN_UPDATE_INTERVAL);
        updaterIntervals.put(BlockExplorerUpdater.UpdateTask.Nodes, NETWORK_UPDATE_INTERVAL);
        updaterIntervals.put(BlockExplorerUpdater.UpdateTask.Witnesses, NETWORK_UPDATE_INTERVAL);
        updaterIntervals.put(BlockExplorerUpdater.UpdateTask.Tokens, TOKENS_UPDATE_INTERVAL);
        updaterIntervals.put(BlockExplorerUpdater.UpdateTask.Accounts, ACCOUNTS_UPDATE_INTERVAL);
        BlockExplorerUpdater.init(this, updaterIntervals);


        AccountUpdater.init(this, ACCOUNT_UPDATE_FOREGROUND_INTERVAL);
        PriceUpdater.init(this, PRICE_UPDATE_INTERVAL);
    }

    public static Context getAppContext() {
        return TronWalletApplication.mContext;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void onAppBackgrounded() {
        mIsInForeground = false;
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(FOREGROUND_CHANGED));
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void onAppForegrounded() {
        mIsInForeground = true;
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(FOREGROUND_CHANGED));
    }

    public static boolean isIsInForeground() {
        return mIsInForeground;
    }

    public static TronWalletDatabase getDatabase() {
        return mDatabase;
    }
}
