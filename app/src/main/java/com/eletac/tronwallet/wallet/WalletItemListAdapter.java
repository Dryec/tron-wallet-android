package com.eletac.tronwallet.wallet;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eletac.tronwallet.R;

import org.tron.walletserver.Wallet;
import org.tron.walletserver.WalletManager;

import java.util.List;

public class WalletItemListAdapter extends RecyclerView.Adapter<WalletItemListAdapter.WalletItemViewHolder> {

    private Context mContext;
    private List<Wallet> mWallets;
    private List<Wallet> mWalletsFiltered;

    private boolean showFiltered;

    public WalletItemListAdapter(Context context, List<Wallet> wallets, List<Wallet> walletsFiltered) {
        mContext = context;
        mWallets = wallets;
        mWalletsFiltered = walletsFiltered;

        showFiltered = false;
    }

    @NonNull
    @Override
    public WalletItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_wallet_item, parent, false);
        WalletItemViewHolder viewHolder = new WalletItemViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull WalletItemViewHolder holder, int position) {
        if(showFiltered) {
            holder.bind(mWalletsFiltered.get(position));
        } else {
            holder.bind(mWallets.get(position));
        }
    }

    @Override
    public int getItemCount() {
        if(showFiltered) {
            return mWalletsFiltered != null ? mWalletsFiltered.size() : 0;
        }
        else {
            return mWallets != null ? mWallets.size() : 0;
        }
    }

    public class WalletItemViewHolder extends RecyclerView.ViewHolder {
        private Context mContext;
        private Wallet mWallet;

        private TextView mName_TextView;
        private TextView mAddress_TextView;

        public WalletItemViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();

            mName_TextView = itemView.findViewById(R.id.WalletItem_name_textView);
            mAddress_TextView = itemView.findViewById(R.id.WalletItem_address_textView);
        }

        public void bind(Wallet wallet) {
            mWallet = wallet;

            mName_TextView.setText(wallet.getWalletName());
            mAddress_TextView.setText(wallet.getAddress());

            Wallet selectedWallet = WalletManager.getSelectedWallet();
            if(selectedWallet != null) {
                mName_TextView.setTextColor(wallet.getWalletName().equals(selectedWallet.getWalletName()) ? Color.GREEN : Color.WHITE);
            } else {
                mName_TextView.setTextColor(Color.WHITE);
            }
        }
    }

    public boolean isShowFiltered() {
        return showFiltered;
    }

    public void setShowFiltered(boolean showFiltered) {
        this.showFiltered = showFiltered;
    }
}
