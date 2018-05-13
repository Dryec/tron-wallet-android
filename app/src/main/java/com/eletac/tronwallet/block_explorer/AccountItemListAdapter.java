package com.eletac.tronwallet.block_explorer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eletac.tronwallet.R;

import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.text.NumberFormat;
import java.util.List;

public class AccountItemListAdapter extends RecyclerView.Adapter<AccountItemListAdapter.AccountItemViewHolder> {

    private Context mContext;
    private List<Protocol.Account> mAccounts;
    private List<Protocol.Account> mAccountsFiltered;

    private boolean showFiltered;

    public AccountItemListAdapter(Context context, List<Protocol.Account> accounts, List<Protocol.Account> accountsFiltered) {
        mContext = context;
        mAccounts = accounts;
        mAccountsFiltered = accountsFiltered;

        showFiltered = false;
    }

    @NonNull
    @Override
    public AccountItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_account_item, parent, false);
        AccountItemViewHolder viewHolder = new AccountItemViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AccountItemViewHolder holder, int position) {
        if(showFiltered) {
            holder.bind(mAccountsFiltered.get(position));
        } else {
            holder.bind(mAccounts.get(position));
        }
    }

    @Override
    public int getItemCount() {
        if(showFiltered) {
            return mAccountsFiltered != null ? mAccountsFiltered.size() : 0;
        }
        else {
            return mAccounts != null ? mAccounts.size() : 0;
        }
    }

    public class AccountItemViewHolder extends RecyclerView.ViewHolder {
        private Context mContext;

        private TextView mAddress_TextView;
        private TextView mBalance_TextView;
        private TextView mAssets_TextView;
        private TextView mVotes_TextView;
        private TextView mLastOperation_TextView;

        public AccountItemViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();

            mAddress_TextView = itemView.findViewById(R.id.AccountItem_address_textView);
            mBalance_TextView = itemView.findViewById(R.id.AccountItem_balance_textView);
            mAssets_TextView = itemView.findViewById(R.id.AccountItem_assets_textView);
            mVotes_TextView = itemView.findViewById(R.id.AccountItem_votes_textView);
            mLastOperation_TextView = itemView.findViewById(R.id.AccountItem_last_operation_textView);
        }

        public void bind(Protocol.Account account) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance();

            mAddress_TextView.setText(WalletClient.encode58Check(account.getAddress().toByteArray()));
            mBalance_TextView.setText(numberFormat.format((double)account.getBalance()/1000000d));
            mAssets_TextView.setText(numberFormat.format(account.getAssetCount()));

            long totalVotes = 0;
            for(Protocol.Account.Vote vote : account.getVotesList()) {
                totalVotes += vote.getVoteCount();
            }
            mVotes_TextView.setText(totalVotes > 0 ? String.format(mContext.getString(R.string.account_item_votes_text), numberFormat.format(totalVotes), numberFormat.format(account.getVotesCount())) : mContext.getString(R.string.none));

            mLastOperation_TextView.setText(account.getLatestOprationTime() != 0 ? DateUtils.getRelativeTimeSpanString(account.getLatestOprationTime(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS) : mContext.getString(R.string.never));
        }
    }

    public boolean isShowFiltered() {
        return showFiltered;
    }

    public void setShowFiltered(boolean showFiltered) {
        this.showFiltered = showFiltered;
    }
}
