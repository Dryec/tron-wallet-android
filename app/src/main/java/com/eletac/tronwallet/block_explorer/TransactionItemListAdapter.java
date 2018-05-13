package com.eletac.tronwallet.block_explorer;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eletac.tronwallet.R;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionItemListAdapter extends RecyclerView.Adapter<TransactionItemListAdapter.TransactionItemViewHolder> {

    private Context mContext;
    private List<Protocol.Transaction> mTransactions;

    public TransactionItemListAdapter(Context context, List<Protocol.Transaction> transactions) {
        mContext = context;
        mTransactions = transactions;
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

        private TextView mTransactionFrom_TextView;
        private TextView mTransactionTo_TextView;
        private TextView mTransactionAmount_TextView;
        private TextView mTransactionAsset_TextView;

        public TransactionItemViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();

            mTransactionFrom_TextView = itemView.findViewById(R.id.Transaction_from_textView);
            mTransactionTo_TextView = itemView.findViewById(R.id.Transaction_to_textView);
            mTransactionAmount_TextView = itemView.findViewById(R.id.Transaction_amount_textView);
            mTransactionAsset_TextView = itemView.findViewById(R.id.Transaction_asset_textView);
        }

        public void bind(Protocol.Transaction transaction) {
            if(transaction.getRawData().getContractCount() > 0) {
                Protocol.Transaction.Contract contract = transaction.getRawData().getContract(0);

                String from = "", to = "", contract_desc = "";
                double amount = 0;

                // @TODO Setup other contracts
                try {
                    switch (contract.getType()) {

                        case AccountCreateContract:
                            Log.i("TRANSACTIONS", "AccountCreateContract");
                            Contract.AccountCreateContract accountCreateContract = TransactionUtils.unpackContract(contract, Contract.AccountCreateContract.class);
                            from = WalletClient.encode58Check(accountCreateContract.getOwnerAddress().toByteArray());
                            to = accountCreateContract.getAccountName().toStringUtf8();
                            amount = -1;
                            contract_desc = "account created";
                            break;
                        case TransferContract:
                            Log.i("TRANSACTIONS", "TransferContract");
                            Contract.TransferContract transferContract = TransactionUtils.unpackContract(contract, Contract.TransferContract.class);
                            from = WalletClient.encode58Check(transferContract.getOwnerAddress().toByteArray());
                            to = WalletClient.encode58Check(transferContract.getToAddress().toByteArray());
                            amount = transferContract.getAmount() / 1000000.0d;
                            contract_desc = mContext.getString(R.string.trx_symbol);
                            break;
                        case TransferAssetContract:
                            Log.i("TRANSACTIONS", "TransferAssetContract");
                            Contract.TransferAssetContract transferAssetContract = TransactionUtils.unpackContract(contract, Contract.TransferAssetContract.class);
                            from = WalletClient.encode58Check(transferAssetContract.getOwnerAddress().toByteArray());
                            to = WalletClient.encode58Check(transferAssetContract.getToAddress().toByteArray());
                            amount = transferAssetContract.getAmount();
                            contract_desc = transferAssetContract.getAssetName().toStringUtf8();
                            break;
                        case VoteAssetContract:
                            Log.i("TRANSACTIONS", "VoteAssetContract");
                            Contract.VoteAssetContract voteAssetContract = TransactionUtils.unpackContract(contract, Contract.VoteAssetContract.class);
                            from = WalletClient.encode58Check(voteAssetContract.getOwnerAddress().toByteArray());
                            to = "";
                            amount = -1;
                            contract_desc = "asset vote";
                            break;
                        case VoteWitnessContract:
                            Log.i("TRANSACTIONS", "VoteWitnessContract");
                            Contract.VoteWitnessContract voteWitnessContract = TransactionUtils.unpackContract(contract, Contract.VoteWitnessContract.class);
                            from = WalletClient.encode58Check(voteWitnessContract.getOwnerAddress().toByteArray());
                            to = voteWitnessContract.getVotesCount() > 1
                                    ? mContext.getString(R.string.multiple_witnesses)
                                    : voteWitnessContract.getVotesCount() == 0 ? "votes reset" : WalletClient.encode58Check(voteWitnessContract.getVotes(0).getVoteAddress().toByteArray());

                            for(Contract.VoteWitnessContract.Vote vote : voteWitnessContract.getVotesList()) {
                                amount += vote.getVoteCount();
                            }
                            contract_desc = mContext.getString(R.string.witness_votes);
                            break;
                        case WitnessCreateContract:
                            Log.i("TRANSACTIONS", "WitnessCreateContract");
                            Contract.WitnessCreateContract witnessCreateContract = TransactionUtils.unpackContract(contract, Contract.WitnessCreateContract.class);
                            from = WalletClient.encode58Check(witnessCreateContract.getOwnerAddress().toByteArray());
                            to = witnessCreateContract.getUrl().toStringUtf8();
                            amount = -1;
                            contract_desc = "witness created";
                            break;
                        case AssetIssueContract:
                            Log.i("TRANSACTIONS", "AssetIssueContract");
                            Contract.AssetIssueContract assetIssueContract = TransactionUtils.unpackContract(contract, Contract.AssetIssueContract.class);
                            from = WalletClient.encode58Check(assetIssueContract.getOwnerAddress().toByteArray());
                            to = assetIssueContract.getName().toStringUtf8();
                            amount = assetIssueContract.getTotalSupply();
                            contract_desc = "asset issued";
                            break;
                        case DeployContract:
                            Log.i("TRANSACTIONS", "DeployContract");
                            Contract.DeployContract deployContract = TransactionUtils.unpackContract(contract, Contract.DeployContract.class);
                            from = WalletClient.encode58Check(deployContract.getOwnerAddress().toByteArray());
                            to = deployContract.getScript().toStringUtf8();
                            amount = -1;
                            contract_desc = "DeployContract";
                            break;
                        case WitnessUpdateContract:
                            Log.i("TRANSACTIONS", "WitnessUpdateContract");
                            Contract.WitnessUpdateContract witnessUpdateContract = TransactionUtils.unpackContract(contract, Contract.WitnessUpdateContract.class);
                            from = WalletClient.encode58Check(witnessUpdateContract.getOwnerAddress().toByteArray());
                            to = witnessUpdateContract.getUpdateUrl().toStringUtf8();
                            amount = -1;
                            contract_desc = "witness updated";
                            break;
                        case ParticipateAssetIssueContract:
                            Log.i("TRANSACTIONS", "ParticipateAssetIssueContract");
                            Contract.ParticipateAssetIssueContract participateAssetIssueContract = TransactionUtils.unpackContract(contract, Contract.ParticipateAssetIssueContract.class);
                            from = WalletClient.encode58Check(participateAssetIssueContract.getOwnerAddress().toByteArray());
                            to = participateAssetIssueContract.getAssetName().toStringUtf8();
                            amount = participateAssetIssueContract.getAmount()/1000000;
                            contract_desc = "participate asset";
                            break;
                        case AccountUpdateContract:
                            Log.i("TRANSACTIONS", "AccountUpdateContract");
                            Contract.AccountUpdateContract accountUpdateContract = TransactionUtils.unpackContract(contract, Contract.AccountUpdateContract.class);
                            from = WalletClient.encode58Check(accountUpdateContract.getOwnerAddress().toByteArray());
                            to = accountUpdateContract.getAccountName().toStringUtf8();
                            amount = -1;
                            contract_desc = "account updated";
                            break;
                        case FreezeBalanceContract:
                            Log.i("TRANSACTIONS", "FreezeBalanceContract");
                            Contract.FreezeBalanceContract freezeBalanceContract = TransactionUtils.unpackContract(contract, Contract.FreezeBalanceContract.class);
                            from = WalletClient.encode58Check(freezeBalanceContract.getOwnerAddress().toByteArray());
                            to = "Duration: " + freezeBalanceContract.getFrozenDuration();
                            amount = freezeBalanceContract.getFrozenBalance()/1000000;
                            contract_desc = "freezed balance";
                            break;
                        case UnfreezeBalanceContract:
                            Log.i("TRANSACTIONS", "UnfreezeBalanceContract");
                            Contract.UnfreezeBalanceContract unfreezeBalanceContract = TransactionUtils.unpackContract(contract, Contract.UnfreezeBalanceContract.class);
                            from = WalletClient.encode58Check(unfreezeBalanceContract.getOwnerAddress().toByteArray());
                            to = "";
                            amount = -1;
                            contract_desc = "unfreezed balance";
                            break;
                        case WithdrawBalanceContract:
                            Log.i("TRANSACTIONS", "WithdrawBalanceContract");
                            Contract.WithdrawBalanceContract withdrawBalanceContract = TransactionUtils.unpackContract(contract, Contract.WithdrawBalanceContract.class);
                            from = WalletClient.encode58Check(withdrawBalanceContract.getOwnerAddress().toByteArray());
                            to = "";
                            amount = -1;
                            contract_desc = "withdraw balance";
                            break;
                        case CustomContract:
                            Log.i("TRANSACTIONS", "CustomContract");
                            //Contract.CustomContract customContract = TransactionUtils.unpackContract(contract, Contract.CustomContract.class);
                            from = "";//WalletClient.encode58Check(withdrawBalanceContract.getOwnerAddress().toByteArray());
                            to = "";
                            amount = -1;
                            contract_desc = "CustomContract";
                            break;
                        case UNRECOGNIZED:
                            contract_desc = "UNRECOGNIZED";
                            break;
                            default:
                                contract_desc = "Unknown";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
                numberFormat.setMaximumFractionDigits(6);

                mTransactionFrom_TextView.setText(from);
                mTransactionTo_TextView.setText(to);
                mTransactionAmount_TextView.setText(amount != -1 ? numberFormat.format(amount) : "");
                mTransactionAsset_TextView.setText(contract_desc);
            }
        }
    }
}
