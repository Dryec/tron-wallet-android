package com.eletac.tronwallet.wallet;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.Price;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class PriceUpdater {
    public static final String PRICES_UPDATED = "com.eletac.tronwallet.price_updater.updated";

    private static Context mContext;
    private static long mInterval;

    private static ExecutorService mExecutorService;

    private static Handler mTaskHandler;
    private static PriceUpdaterRunnable mPriceUpdaterRunnable;

    private static Price mTRX_price;

    private static boolean mRunning;

    PriceUpdater() {}

    public static void init(Context context, long interval) {
        if(mContext == null) {
            mContext = context;
            mInterval = interval;
            mRunning = false;
            mTaskHandler = new Handler(Looper.getMainLooper());
            mPriceUpdaterRunnable = new PriceUpdaterRunnable();
            mExecutorService = Executors.newSingleThreadExecutor();
        }
    }

    public static void start() {
        stop();
        mRunning = true;
        mTaskHandler.post(mPriceUpdaterRunnable);
    }

    public static void startDelayed(long delayMillis) {
        stop();
        mRunning = true;
        mTaskHandler.postDelayed(mPriceUpdaterRunnable, delayMillis);
    }

    public static void stop() {
        mRunning = false;
        mTaskHandler.removeCallbacks(mPriceUpdaterRunnable);
    }

    public static void setInterval(long intervalMillis, boolean restart) {
        mInterval = intervalMillis;
        if(restart) {
            start();
        }
    }

    public static Price getTRX_price() {
        return mTRX_price;
    }

    private static class PriceUpdaterRunnable implements Runnable {

        @Override
        public void run() {

            AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
                @Override
                public void doOnBackground() {
                    Price price = null;
                    if(mContext != null) {
                        try {
                            // @TODO Load token prices (need to wait for Mainnet and first official token with a price)
                            price = getPrice("tron");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    Price finalPrice = price;
                    AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                        @Override
                        public void doInUIThread() {
                            Log.i("PRICE_UPDATER", "Prices updated");

                            mTRX_price = finalPrice;

                            Intent pricesUpdatedIntent = new Intent(PRICES_UPDATED);
                            // Load tron just for now
                            pricesUpdatedIntent.putExtra("tron_price", finalPrice != null ? finalPrice.getPrice() : 0);
                            pricesUpdatedIntent.putExtra("tron_24h_change", finalPrice != null ? finalPrice.getChange_24h() : 0);

                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(pricesUpdatedIntent);

                            if(mRunning) {
                                mTaskHandler.removeCallbacks(mPriceUpdaterRunnable); // remove multiple callbacks
                                mTaskHandler.postDelayed(mPriceUpdaterRunnable, mInterval);
                            }
                        }
                    });
                }
            }, mExecutorService);
        }

        private Price getPrice(String asset) {
            Price price = new Price();

            try {
                StringBuilder jsonData = new StringBuilder();
                URL url = new URL("https://api.coinmarketcap.com/v1/ticker/"+asset);

                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonData.append(line);
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(jsonData.toString());
                JSONObject jsonObject = jsonArray.getJSONObject(0);

                float current_price = Float.parseFloat(jsonObject.getString("price_usd"));
                float price_1hChange= Float.parseFloat(jsonObject.getString("percent_change_1h"));
                float price_24hChange= Float.parseFloat(jsonObject.getString("percent_change_24h"));
                float price_7dChange= Float.parseFloat(jsonObject.getString("percent_change_7d"));

                price.setPrice(current_price);
                price.setChange_1h(price_1hChange);
                price.setChange_24h(price_24hChange);
                price.setChange_7d(price_7dChange);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return price;
        }
    }

    public static boolean isRunning() {
        return mRunning;
    }

    public static boolean isInitialized() {
        return mContext != null;
    }
}
