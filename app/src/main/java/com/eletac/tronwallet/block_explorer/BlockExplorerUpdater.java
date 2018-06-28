package com.eletac.tronwallet.block_explorer;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;

import com.arasthel.asyncjob.AsyncJob;

import org.tron.api.GrpcAPI;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockExplorerUpdater {

    public enum UpdateTask {
        Blockchain,
        Nodes,
        Witnesses,
        Tokens,
        Accounts
    }

    public static final String BLOCKCHAIN_UPDATED = "com.eletac.tronwallet.block_explorer_updater.blockchain_updated";
    public static final String WITNESSES_UPDATED = "com.eletac.tronwallet.block_explorer_updater.witnesses_updated";
    public static final String NODES_UPDATED = "com.eletac.tronwallet.block_explorer_updater.nodes_updated";
    public static final String ACCOUNTS_UPDATED = "com.eletac.tronwallet.block_explorer_updater.accounts_updated";
    public static final String TOKENS_UPDATED = "com.eletac.tronwallet.block_explorer_updater.tokens_updated";

    private static Context mContext;

    private static Handler mTaskHandler;
    private static BlockchainUpdaterRunnable mBlockchainUpdaterRunnable;
    private static NodesUpdaterRunnable mNodesUpdaterRunnable;
    private static WitnessesUpdaterRunnable mWitnessesUpdaterRunnable;
    private static TokensUpdaterRunnable mTokensUpdaterRunnable;
    private static AccountsUpdaterRunnable mAccountsUpdaterRunnable;

    private static Map<UpdateTask, Long> mIntervals;

    private static Map<UpdateTask, Boolean> mRunning;
    private static Map<UpdateTask, Boolean> mSingleShot;

    private static ExecutorService mExecutorService;

    private static List<Protocol.Block> mBlocks;
    private static List<Protocol.Transaction> mTransactions;
    private static List<Protocol.Witness> mWitnesses;
    private static List<GrpcAPI.Node> mNodes;
    private static List<Contract.AssetIssueContract> mTokens;
    private static List<Protocol.Account> mAccounts;

    public static void init(Context context, Map<UpdateTask, Long> intervals) {
        if(mContext == null) {
            mContext = context;
            mIntervals = intervals;
            mRunning = new HashMap<>();
            mRunning.put(UpdateTask.Blockchain, false);
            mRunning.put(UpdateTask.Nodes, false);
            mRunning.put(UpdateTask.Witnesses, false);
            mRunning.put(UpdateTask.Tokens, false);
            mRunning.put(UpdateTask.Accounts, false);

            mSingleShot = new HashMap<>();
            mSingleShot.put(UpdateTask.Blockchain, false);
            mSingleShot.put(UpdateTask.Nodes, false);
            mSingleShot.put(UpdateTask.Witnesses, false);
            mSingleShot.put(UpdateTask.Tokens, false);
            mSingleShot.put(UpdateTask.Accounts, false);

            mBlocks = Collections.synchronizedList(new LinkedList<>());
            mTransactions = Collections.synchronizedList(new LinkedList<>());
            mWitnesses = Collections.synchronizedList(new LinkedList<>());
            mNodes = Collections.synchronizedList(new LinkedList<>());
            mTokens = Collections.synchronizedList(new LinkedList<>());
            mAccounts = Collections.synchronizedList(new LinkedList<>());

            mTaskHandler = new Handler(Looper.getMainLooper());
            mBlockchainUpdaterRunnable = new BlockchainUpdaterRunnable();
            mNodesUpdaterRunnable = new NodesUpdaterRunnable();
            mWitnessesUpdaterRunnable = new WitnessesUpdaterRunnable();
            mTokensUpdaterRunnable = new TokensUpdaterRunnable();
            mAccountsUpdaterRunnable = new AccountsUpdaterRunnable();

            mExecutorService = Executors.newFixedThreadPool(2);
        }
    }

    public static void start(UpdateTask task) {
        stop(task);
        mRunning.put(task, true);
        mTaskHandler.post(getRunnableOfTask(task));
    }

    public static void stop(UpdateTask task) {
        mRunning.put(task, false);
        mTaskHandler.removeCallbacks(getRunnableOfTask(task));
    }

    public static void stopAll() {
        mRunning.put(UpdateTask.Blockchain, false);
        mRunning.put(UpdateTask.Nodes, false);
        mRunning.put(UpdateTask.Witnesses, false);
        mRunning.put(UpdateTask.Tokens, false);
        mRunning.put(UpdateTask.Accounts, false);
        mTaskHandler.removeCallbacks(null);
    }

    public static void singleShot(UpdateTask task, boolean now) {
        mSingleShot.put(task, true);
        if(now)
            mTaskHandler.post(getRunnableOfTask(task));
        else
            mTaskHandler.postDelayed(getRunnableOfTask(task), mIntervals.containsKey(task) ? mIntervals.get(task) : 0);
    }

    private static Runnable getRunnableOfTask(UpdateTask task) {
        switch (task) {
            case Blockchain:
                return mBlockchainUpdaterRunnable;
            case Nodes:
                return mNodesUpdaterRunnable;
            case Witnesses:
                return mWitnessesUpdaterRunnable;
            case Tokens:
                return mTokensUpdaterRunnable;
            case Accounts:
                return mAccountsUpdaterRunnable;
            default:
                return null;
        }
    }

    private static class BlockchainUpdaterRunnable implements Runnable {

        @Override
        public void run() {
            AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
                    @Override
                    public void doOnBackground() {
                        if (mContext != null) {

                            // Load Blocks and transactions
                            try {
                                GrpcAPI.BlockList result = WalletManager.getBlockByLatestNum(50);
                                if (result != null) {
                                    mBlocks.clear();
                                    mBlocks.addAll(result.getBlockList());
                                    Collections.sort(mBlocks, new Comparator<Protocol.Block>() {
                                        @Override
                                        public int compare(Protocol.Block o1, Protocol.Block o2) {
                                            return Long.compare(o1.getBlockHeader().getRawData().getNumber(), o2.getBlockHeader().getRawData().getNumber());
                                        }
                                    });
                                }

                                mTransactions.clear();
                                for (Protocol.Block block : mBlocks) {
                                    mTransactions.addAll(block.getTransactionsList());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                            @Override
                            public void doInUIThread() {

                                Intent updatedIntent = new Intent(BLOCKCHAIN_UPDATED);
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(updatedIntent);

                                if(mRunning.get(UpdateTask.Blockchain)) {
                                    mTaskHandler.removeCallbacks(getRunnableOfTask(UpdateTask.Blockchain)); // remove multiple callbacks
                                    mTaskHandler.postDelayed(mBlockchainUpdaterRunnable, mIntervals.get(UpdateTask.Blockchain));
                                }
                                mSingleShot.put(UpdateTask.Blockchain, false);
                            }
                        });
                }
            }, mExecutorService);
        }
    }

    private static class NodesUpdaterRunnable implements Runnable {

        @Override
        public void run() {
            AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
                @Override
                public void doOnBackground() {
                    if(mContext != null) {
                        // Load nodes
                        try {
                            GrpcAPI.NodeList result = WalletManager.listNodes();
                            if(result != null) {
                                mNodes.clear();
                                mNodes.addAll(result.getNodesList());
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                        @Override
                        public void doInUIThread() {

                            Intent updatedIntent = new Intent(NODES_UPDATED);
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(updatedIntent);

                            if(mRunning.get(UpdateTask.Nodes)) {
                                mTaskHandler.removeCallbacks(getRunnableOfTask(UpdateTask.Nodes)); // remove multiple callbacks
                                mTaskHandler.postDelayed(mNodesUpdaterRunnable, mIntervals.get(UpdateTask.Nodes));
                            }
                            mSingleShot.put(UpdateTask.Nodes, false);
                        }
                    });
                }
            }, mExecutorService);
        }
    }

    private static class WitnessesUpdaterRunnable implements Runnable {

        @Override
        public void run() {
            AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
                @Override
                public void doOnBackground() {
                    if(mContext != null) {
                        // Load witnesses
                        try {
                            GrpcAPI.WitnessList result = WalletManager.listWitnesses(true);
                            if(result != null) {
                                mWitnesses.clear();
                                mWitnesses.addAll(result.getWitnessesList());
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                        @Override
                        public void doInUIThread() {

                            Intent updatedIntent = new Intent(WITNESSES_UPDATED);
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(updatedIntent);

                            if(mRunning.get(UpdateTask.Witnesses)) {
                                mTaskHandler.removeCallbacks(getRunnableOfTask(UpdateTask.Witnesses)); // remove multiple callbacks
                                mTaskHandler.postDelayed(mWitnessesUpdaterRunnable, mIntervals.get(UpdateTask.Witnesses));
                            }
                            mSingleShot.put(UpdateTask.Witnesses, false);
                        }
                    });
                }
            }, mExecutorService);
        }
    }

    private static class TokensUpdaterRunnable implements Runnable {

        @Override
        public void run() {
            AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
                @Override
                public void doOnBackground() {
                    if(mContext != null) {
                        // Load tokens
                        try {
                            GrpcAPI.AssetIssueList result = WalletManager.getAssetIssueList(true);
                            if(result != null) {
                                mTokens.clear();
                                mTokens.addAll(result.getAssetIssueList());
                                Collections.sort(mTokens, new Comparator<Contract.AssetIssueContract>() {
                                    @Override
                                    public int compare(Contract.AssetIssueContract o1, Contract.AssetIssueContract o2) {
                                        return o1.getName().toStringUtf8().compareTo(o2.getName().toStringUtf8());
                                    }
                                });
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                        @Override
                        public void doInUIThread() {

                            Intent updatedIntent = new Intent(TOKENS_UPDATED);
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(updatedIntent);

                            if(mRunning.get(UpdateTask.Tokens)) {
                                mTaskHandler.removeCallbacks(getRunnableOfTask(UpdateTask.Tokens)); // remove multiple callbacks
                                mTaskHandler.postDelayed(mTokensUpdaterRunnable, mIntervals.get(UpdateTask.Tokens));
                            }
                            mSingleShot.put(UpdateTask.Tokens, false);
                        }
                    });
                }
            }, mExecutorService);
        }
    }

    private static class AccountsUpdaterRunnable implements Runnable {

        @Override
        public void run() {
            AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
                @Override
                public void doOnBackground() {
                    if(mContext != null) {
                        // Load accounts
                        /*try {
                            GrpcAPI.AccountList result = WalletManager.listAccounts();
                            if(result != null) {
                                mAccounts.clear();
                                mAccounts.addAll(result.getAccountsList());
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }*/
                    }

                    AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                        @Override
                        public void doInUIThread() {

                            Intent updatedIntent = new Intent(ACCOUNTS_UPDATED);
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(updatedIntent);

                            if(mRunning.get(UpdateTask.Accounts)) {
                                mTaskHandler.removeCallbacks(getRunnableOfTask(UpdateTask.Accounts)); // remove multiple callbacks
                                mTaskHandler.postDelayed(mAccountsUpdaterRunnable, mIntervals.get(UpdateTask.Accounts));
                            }
                            mSingleShot.put(UpdateTask.Accounts, false);
                        }
                    });
                }
            }, mExecutorService);
        }
    }

    public static List<Protocol.Block> getBlocks() {
        return mBlocks;
    }

    public static List<Protocol.Transaction> getTransactions() {
        return mTransactions;
    }

    public static List<Protocol.Witness> getWitnesses() {
        return mWitnesses;
    }

    public static List<GrpcAPI.Node> getNodes() {
        return mNodes;
    }

    public static List<Contract.AssetIssueContract> getTokens() {
        return mTokens;
    }

    public static List<Protocol.Account> getAccounts() {
        return mAccounts;
    }

    public static boolean isRunning(UpdateTask task) {
        return mRunning.get(task);
    }
    public static boolean isSingleShotting(UpdateTask task) {
        return mSingleShot.get(task);
    }
}
