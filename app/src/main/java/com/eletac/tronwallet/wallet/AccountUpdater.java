package com.eletac.tronwallet.wallet;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.Utils;

import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;
import org.tron.walletserver.Wallet;
import org.tron.walletserver.WalletManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AccountUpdater {

    public static final String ACCOUNT_UPDATED = "com.eletac.tronwallet.account_updater.updated";

    private static Context mContext;
    private static long mInterval;

    private static ExecutorService mExecutorService;

    private static Handler mTaskHandler;
    private static AccountUpdaterRunnable mAccountUpdaterRunnable;

    private static boolean running;

    AccountUpdater() {

    }

    public static void init(Context context, long interval) {
        if(mContext == null) {
            mContext = context;
            mInterval = interval;
            running = false;
            mTaskHandler = new Handler(Looper.getMainLooper());
            mAccountUpdaterRunnable = new AccountUpdaterRunnable();
            mExecutorService = Executors.newSingleThreadExecutor();
        }
    }

    public static void start() {
        stop();
        running = true;
        mTaskHandler.post(mAccountUpdaterRunnable);
    }

    public static void startDelayed(long delayMillis) {
        stop();
        running = true;
        mTaskHandler.postDelayed(mAccountUpdaterRunnable, delayMillis);
    }

    public static void stop() {
        running = false;
        mTaskHandler.removeCallbacks(mAccountUpdaterRunnable);
    }

    public static void setInterval(long intervalMillis, boolean restart) {
        mInterval = intervalMillis;
        if(restart) {
            start();
        }
    }

    public static void singleShot(long delayMillis) {
        if(delayMillis <= 0)
            mTaskHandler.post(mAccountUpdaterRunnable);
        else
            mTaskHandler.postDelayed(mAccountUpdaterRunnable, delayMillis);
    }

    private static class AccountUpdaterRunnable implements Runnable {

        @Override
        public void run() {

            AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
                @Override
                public void doOnBackground() {
                    if(mContext != null) {
                        try {
                            Wallet selectedWallet = WalletManager.getSelectedWallet();
                            byte[] address = WalletManager.decodeFromBase58Check(selectedWallet.getAddress());

                            Protocol.Account account = WalletManager.queryAccount(address, false);
                            GrpcAPI.AccountNetMessage accountNetMessage = WalletManager.getAccountNet(address);

                            Utils.saveAccount(mContext, selectedWallet.getWalletName(), account);
                            Utils.saveAccountNet(mContext, selectedWallet.getWalletName(), accountNetMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                        @Override
                        public void doInUIThread() {
                            Log.i("ACCOUNT_UPDATER", "Account updated");

                            Intent accountUpdatedIntent = new Intent(ACCOUNT_UPDATED);
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(accountUpdatedIntent);

                            if(running) {
                                mTaskHandler.removeCallbacks(mAccountUpdaterRunnable); // remove multiple callbacks
                                mTaskHandler.postDelayed(mAccountUpdaterRunnable, mInterval);
                            }
                        }
                    });
                }
            }, mExecutorService);
        }
    }

    public static boolean isRunning() {
        return running;
    }

    public static boolean isInitialized() {
        return mContext != null;
    }
}
