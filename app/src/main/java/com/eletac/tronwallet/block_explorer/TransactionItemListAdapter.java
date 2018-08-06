package com.eletac.tronwallet.block_explorer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.R;

import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.grpc.StatusRuntimeException;

public class TransactionItemListAdapter extends RecyclerView.Adapter<TransactionItemListAdapter.TransactionItemViewHolder> {

    private Context mContext;
    private List<Protocol.Transaction> mTransactions;
    private ExecutorService mExecutorService;
    private List<Protocol.Transaction> mConfirmedTransactions;

    public TransactionItemListAdapter(Context context, List<Protocol.Transaction> transactions) {
        mContext = context;
        mTransactions = transactions;
        mConfirmedTransactions = new ArrayList<>();
        mExecutorService = Executors.newFixedThreadPool(3);
    }

    @NonNull
    @Override
    public TransactionItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_transaction_item, parent, false);
        TransactionItemViewHolder viewHolder = new TransactionItemViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionItemViewHolder holder, int position) {
        holder.bind(mTransactions.get(position));
    }

    @Override
    public int getItemCount() {
        return mTransactions != null ? mTransactions.size() : 0;
    }

    public class TransactionItemViewHolder extends RecyclerView.ViewHolder {
        private Context mContext;
        private Protocol.Transaction mTransaction;
        private Handler mUpdateConfirmationHandler;
        private UpdateConfirmationRunnable mUpdateConfirmationRunnable;
        private boolean mFirstConfirmationStateLoaded;

        private TextView mTransactionFrom_TextView;
        private TextView mTransactionTo_TextView;
        private TextView mTransactionTimestamp_TextView;
        private TextView mTransactionAmount_TextView;
        private TextView mTransactionAsset_TextView;
        private TextView mTransactionConfirmed_TextView;
        private CardView mTransactionConfirmed_CardView;
        private ProgressBar mTransactionLoadingConfirmation_ProgressBar;

        public TransactionItemViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();
            mTransaction = null;
            mUpdateConfirmationHandler = new Handler();
            mUpdateConfirmationRunnable = new UpdateConfirmationRunnable();

            mTransactionFrom_TextView = itemView.findViewById(R.id.Transaction_from_textView);
            mTransactionTo_TextView = itemView.findViewById(R.id.Transaction_to_textView);
            mTransactionTimestamp_TextView = itemView.findViewById(R.id.Transaction_timestamp_textView);
            mTransactionAmount_TextView = itemView.findViewById(R.id.Transaction_amount_textView);
            mTransactionAsset_TextView = itemView.findViewById(R.id.Transaction_asset_textView);
            mTransactionConfirmed_TextView = itemView.findViewById(R.id.Transaction_confirmed_textView);
            mTransactionConfirmed_CardView = itemView.findViewById(R.id.Transaction_confirmation_CardView);
            mTransactionLoadingConfirmation_ProgressBar = itemView.findViewById(R.id.Transaction_loading_confirmation_progressBar);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mTransaction != null) {
                        Intent intent = new Intent(mContext, TransactionViewerActivity.class);
                        intent.putExtra(TransactionViewerActivity.TRANSACTION_DATA, mTransaction.toByteArray());
                        mContext.startActivity(intent);
                    }
                }
            });
        }

        public void bind(Protocol.Transaction transaction) {
            mTransaction = transaction;
            mFirstConfirmationStateLoaded = false;

            mUpdateConfirmationHandler.removeCallbacks(mUpdateConfirmationRunnable);
            mUpdateConfirmationHandler.post(mUpdateConfirmationRunnable);

            if(transaction.getRawData().getContractCount() > 0) {
                Protocol.Transaction.Contract contract = transaction.getRawData().getContract(0);

                String from = "", to = "", contract_desc = "";
                String amount_prefix = "";
                double amount = 0;

                // @TODO Setup other contracts
                try {
                    switch (contract.getType()) {

                        case AccountCreateContract:
                            Log.i("TRANSACTIONS", "AccountCreateContract");
                            Contract.AccountCreateContract accountCreateContract = TransactionUtils.unpackContract(contract, Contract.AccountCreateContract.class);
                            from = WalletManager.encode58Check(accountCreateContract.getOwnerAddress().toByteArray());
                            to = WalletManager.encode58Check(accountCreateContract.getAccountAddress().toByteArray());
                            amount = -1;
                            contract_desc = mContext.getString(R.string.account_creation);
                            break;
                        case TransferContract:
                            Log.i("TRANSACTIONS", "TransferContract");
                            Contract.TransferContract transferContract = TransactionUtils.unpackContract(contract, Contract.TransferContract.class);
                            from = WalletManager.encode58Check(transferContract.getOwnerAddress().toByteArray());
                            to = WalletManager.encode58Check(transferContract.getToAddress().toByteArray());
                            amount = transferContract.getAmount() / 1000000.0d;
                            amount_prefix = mContext.getString(R.string.trx_symbol);
                            contract_desc = mContext.getString(R.string.transfer);
                            break;
                        case TransferAssetContract:
                            Log.i("TRANSACTIONS", "TransferAssetContract");
                            Contract.TransferAssetContract transferAssetContract = TransactionUtils.unpackContract(contract, Contract.TransferAssetContract.class);
                            from = WalletManager.encode58Check(transferAssetContract.getOwnerAddress().toByteArray());
                            to = WalletManager.encode58Check(transferAssetContract.getToAddress().toByteArray());
                            amount = transferAssetContract.getAmount();
                            amount_prefix = ""; // TODO abbreviation
                            contract_desc = mContext.getString(R.string.token_transfer) + "\n" + transferAssetContract.getAssetName().toStringUtf8();
                            break;
                        case VoteAssetContract:
                            Log.i("TRANSACTIONS", "VoteAssetContract");
                            Contract.VoteAssetContract voteAssetContract = TransactionUtils.unpackContract(contract, Contract.VoteAssetContract.class);
                            from = WalletManager.encode58Check(voteAssetContract.getOwnerAddress().toByteArray());
                            to = "";
                            amount = -1;
                            contract_desc = mContext.getString(R.string.token_vote);
                            break;
                        case VoteWitnessContract:
                            Log.i("TRANSACTIONS", "VoteWitnessContract");
                            Contract.VoteWitnessContract voteWitnessContract = TransactionUtils.unpackContract(contract, Contract.VoteWitnessContract.class);
                            from = WalletManager.encode58Check(voteWitnessContract.getOwnerAddress().toByteArray());
                            to = voteWitnessContract.getVotesCount() > 1
                                    ? mContext.getString(R.string.multiple_candidates)
                                    : voteWitnessContract.getVotesCount() == 0 ? mContext.getString(R.string.votes_reset) : WalletManager.encode58Check(voteWitnessContract.getVotes(0).getVoteAddress().toByteArray());

                            for(Contract.VoteWitnessContract.Vote vote : voteWitnessContract.getVotesList()) {
                                amount += vote.getVoteCount();
                            }
                            amount_prefix = mContext.getString(R.string.votes);
                            contract_desc = mContext.getString(R.string.candidate_votes);
                            break;
                        case WitnessCreateContract:
                            Log.i("TRANSACTIONS", "WitnessCreateContract");
                            Contract.WitnessCreateContract witnessCreateContract = TransactionUtils.unpackContract(contract, Contract.WitnessCreateContract.class);
                            from = WalletManager.encode58Check(witnessCreateContract.getOwnerAddress().toByteArray());
                            to = witnessCreateContract.getUrl().toStringUtf8();
                            amount = -1;
                            contract_desc = mContext.getString(R.string.witness_creation);
                            break;
                        case AssetIssueContract:
                            Log.i("TRANSACTIONS", "AssetIssueContract");
                            Contract.AssetIssueContract assetIssueContract = TransactionUtils.unpackContract(contract, Contract.AssetIssueContract.class);
                            from = WalletManager.encode58Check(assetIssueContract.getOwnerAddress().toByteArray());
                            to = assetIssueContract.getName().toStringUtf8();
                            amount = assetIssueContract.getTotalSupply();
                            contract_desc = mContext.getString(R.string.token_creation);
                            break;
                        case DeployContract:
                            Log.i("TRANSACTIONS", "DeployContract");
                            Contract.DeployContract deployContract = TransactionUtils.unpackContract(contract, Contract.DeployContract.class);
                            from = WalletManager.encode58Check(deployContract.getOwnerAddress().toByteArray());
                            to = deployContract.getScript().toStringUtf8();
                            amount = -1;
                            contract_desc = mContext.getString(R.string.deploy_contract);
                            break;
                        case WitnessUpdateContract:
                            Log.i("TRANSACTIONS", "WitnessUpdateContract");
                            Contract.WitnessUpdateContract witnessUpdateContract = TransactionUtils.unpackContract(contract, Contract.WitnessUpdateContract.class);
                            from = WalletManager.encode58Check(witnessUpdateContract.getOwnerAddress().toByteArray());
                            to = witnessUpdateContract.getUpdateUrl().toStringUtf8();
                            amount = -1;
                            contract_desc = mContext.getString(R.string.witness_update);
                            break;
                        case ParticipateAssetIssueContract:
                            Log.i("TRANSACTIONS", "ParticipateAssetIssueContract");
                            Contract.ParticipateAssetIssueContract participateAssetIssueContract = TransactionUtils.unpackContract(contract, Contract.ParticipateAssetIssueContract.class);
                            from = WalletManager.encode58Check(participateAssetIssueContract.getOwnerAddress().toByteArray());
                            to = participateAssetIssueContract.getAssetName().toStringUtf8();
                            amount = participateAssetIssueContract.getAmount()/1000000;
                            amount_prefix = mContext.getString(R.string.trx_symbol);
                            contract_desc = mContext.getString(R.string.token_participation);
                            break;
                        case AccountUpdateContract:
                            Log.i("TRANSACTIONS", "AccountUpdateContract");
                            Contract.AccountUpdateContract accountUpdateContract = TransactionUtils.unpackContract(contract, Contract.AccountUpdateContract.class);
                            from = WalletManager.encode58Check(accountUpdateContract.getOwnerAddress().toByteArray());
                            to = accountUpdateContract.getAccountName().toStringUtf8();
                            amount = -1;
                            contract_desc = mContext.getString(R.string.account_update);
                            break;
                        case FreezeBalanceContract:
                            Log.i("TRANSACTIONS", "FreezeBalanceContract");
                            Contract.FreezeBalanceContract freezeBalanceContract = TransactionUtils.unpackContract(contract, Contract.FreezeBalanceContract.class);
                            from = WalletManager.encode58Check(freezeBalanceContract.getOwnerAddress().toByteArray());
                            to = mContext.getString(R.string.duration) + ": " + freezeBalanceContract.getFrozenDuration();
                            amount = freezeBalanceContract.getFrozenBalance()/1000000;
                            amount_prefix = mContext.getString(R.string.trx_symbol);
                            contract_desc = mContext.getString(R.string.freeze_balance);
                            break;
                        case UnfreezeBalanceContract:
                            Log.i("TRANSACTIONS", "UnfreezeBalanceContract");
                            Contract.UnfreezeBalanceContract unfreezeBalanceContract = TransactionUtils.unpackContract(contract, Contract.UnfreezeBalanceContract.class);
                            from = WalletManager.encode58Check(unfreezeBalanceContract.getOwnerAddress().toByteArray());
                            to = "";
                            amount = -1;
                            contract_desc = mContext.getString(R.string.unfreeze_balance);
                            break;
                        case WithdrawBalanceContract:
                            Log.i("TRANSACTIONS", "WithdrawBalanceContract");
                            Contract.WithdrawBalanceContract withdrawBalanceContract = TransactionUtils.unpackContract(contract, Contract.WithdrawBalanceContract.class);
                            from = WalletManager.encode58Check(withdrawBalanceContract.getOwnerAddress().toByteArray());
                            to = "";
                            amount = -1;
                            contract_desc = mContext.getString(R.string.withdraw_balance);
                            break;
                        case UnfreezeAssetContract:
                            break;
                        case UpdateAssetContract:
                            break;
                        case CustomContract:
                            Log.i("TRANSACTIONS", "CustomContract");
                            //Contract.CustomContract customContract = TransactionUtils.unpackContract(contract, Contract.CustomContract.class);
                            from = "";//WalletManager.encode58Check(withdrawBalanceContract.getOwnerAddress().toByteArray());
                            to = "";
                            amount = -1;
                            contract_desc = mContext.getString(R.string.custom_contract);
                            break;
                        case UNRECOGNIZED:
                            contract_desc = mContext.getString(R.string.unrecognized);
                            break;
                            default:
                                contract_desc = mContext.getString(R.string.unknown);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
                numberFormat.setMaximumFractionDigits(6);

                mTransactionFrom_TextView.setText(from);
                mTransactionTo_TextView.setText(to);
                mTransactionAmount_TextView.setText((amount != -1 ? numberFormat.format(amount) : "") + " " + amount_prefix);
                mTransactionAsset_TextView.setText(contract_desc);

                long timestamp = transaction.getRawData().getTimestamp();
                if(timestamp == 0) {
                    try {
                        for (Protocol.Block block : BlockExplorerUpdater.getBlocks()) {
                            for (Protocol.Transaction blockTransaction : block.getTransactionsList()) {
                                if (blockTransaction.equals(transaction)) {
                                    timestamp = block.getBlockHeader().getRawData().getTimestamp();
                                    break;
                                }
                            }
                            if (timestamp != 0) {
                                break;
                            }
                        }
                    }
                    catch (ConcurrentModificationException e) {
                        e.printStackTrace();
                    }

                }
                mTransactionTimestamp_TextView.setText(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new Date(timestamp)));
            }
        }

        private void loadConfirmation() {
            if(mConfirmedTransactions.contains(mTransaction)) {
                mTransactionConfirmed_TextView.setText(R.string.confirmed);
                mTransactionConfirmed_CardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.positive));
                mTransactionLoadingConfirmation_ProgressBar.setVisibility(View.GONE);
            } else {
                if(!mFirstConfirmationStateLoaded) {
                    mTransactionConfirmed_CardView.setVisibility(View.GONE);
                    mTransactionLoadingConfirmation_ProgressBar.setVisibility(View.VISIBLE);
                }

                AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
                    @Override
                    public void doOnBackground() {

                        boolean isConfirmed = false;
                        try {
                            isConfirmed = WalletManager.isTransactionConfirmed(mTransaction);

                            boolean finalIsConfirmed = isConfirmed;
                            AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                                @Override
                                public void doInUIThread() {
                                    if(!mFirstConfirmationStateLoaded) {
                                        mTransactionConfirmed_CardView.setVisibility(View.VISIBLE);
                                        mTransactionLoadingConfirmation_ProgressBar.setVisibility(View.GONE);
                                    }
                                    mFirstConfirmationStateLoaded = true;

                                    if (finalIsConfirmed) {
                                        if (!mConfirmedTransactions.contains(mTransaction)) {
                                            mConfirmedTransactions.add(mTransaction);
                                        }
                                        mTransactionConfirmed_TextView.setText(R.string.confirmed);
                                        mTransactionConfirmed_CardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.positive));
                                    } else {
                                        mTransactionConfirmed_TextView.setText(R.string.unconfirmed);
                                        mTransactionConfirmed_CardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.colorAccent));

                                        mUpdateConfirmationHandler.postDelayed(mUpdateConfirmationRunnable, 500);
                                    }
                                }
                            });
                        } catch (StatusRuntimeException e) {
                            e.printStackTrace();
                            mTransactionConfirmed_CardView.setVisibility(View.VISIBLE);
                            mTransactionLoadingConfirmation_ProgressBar.setVisibility(View.GONE);
                            mTransactionConfirmed_TextView.setText(R.string.unknown);
                            mTransactionConfirmed_CardView.setCardBackgroundColor(Color.GRAY);
                        }
                    }
                }, mExecutorService);
            }
        }

        private class UpdateConfirmationRunnable implements Runnable {

            @Override
            public void run() {
                loadConfirmation();
            }
        }
    }
}
