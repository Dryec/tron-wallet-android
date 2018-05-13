package com.eletac.tronwallet.block_explorer;

import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.settings.SettingConnectionActivity;
import com.eletac.tronwallet.wallet.ParticipateAssetActivity;

import org.tron.protos.Contract;
import org.tron.walletserver.WalletClient;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TokenItemListAdapter extends RecyclerView.Adapter<TokenItemListAdapter.TokenItemViewHolder> {

    private Context mContext;
    private List<Contract.AssetIssueContract> mAssets;
    private List<Contract.AssetIssueContract> mAssetsFiltered;

    private boolean showFiltered;

    public TokenItemListAdapter(Context context, List<Contract.AssetIssueContract> assets, List<Contract.AssetIssueContract> assetsFiltered) {
        mContext = context;
        mAssets = assets;
        mAssetsFiltered = assetsFiltered;

        showFiltered = false;
    }

    @NonNull
    @Override
    public TokenItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_be_token_item, parent, false);
        TokenItemViewHolder viewHolder = new TokenItemViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TokenItemViewHolder holder, int position) {
        if(showFiltered) {
            holder.bind(mAssetsFiltered.get(position));
        } else {
            holder.bind(mAssets.get(position));
        }
    }

    @Override
    public int getItemCount() {
        if(showFiltered) {
            return mAssetsFiltered != null ? mAssetsFiltered.size() : 0;
        }
        else {
            return mAssets != null ? mAssets.size() : 0;
        }
    }

    public class TokenItemViewHolder extends RecyclerView.ViewHolder {
        private Context mContext;

        private TextView mName_TextView;
        private TextView mDescription_TextView;
        private TextView mSupply_TextView;
        private TextView mIssuer_TextView;
        private TextView mStart_TextView;
        private TextView mEnd_TextView;
        private Button mParticipate_Button;

        private Contract.AssetIssueContract mAsset;


        public TokenItemViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();

            mName_TextView = itemView.findViewById(R.id.TokenBE_name_textView);
            mDescription_TextView = itemView.findViewById(R.id.TokenBE_description_textView);
            mSupply_TextView = itemView.findViewById(R.id.TokenBE_supply_textView);
            mIssuer_TextView = itemView.findViewById(R.id.TokenBE_issuer_textView);
            mStart_TextView = itemView.findViewById(R.id.TokenBE_start_textView);
            mEnd_TextView = itemView.findViewById(R.id.TokenBE_end_textView);
            mParticipate_Button = itemView.findViewById(R.id.TokenBE_participate_button);

            mParticipate_Button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mAsset != null && mContext != null) {
                        Intent intent = new Intent(mContext, ParticipateAssetActivity.class);
                        intent.putExtra(ParticipateAssetActivity.ASSET_NAME_EXTRA, mAsset.getName().toStringUtf8());
                        mContext.startActivity(intent);
                    }
                }
            });
        }

        public void bind(Contract.AssetIssueContract asset) {

            mAsset = asset;

            mName_TextView.setText(asset.getName().toStringUtf8());
            mDescription_TextView.setText(asset.getDescription().toStringUtf8());
            mSupply_TextView.setText(NumberFormat.getInstance(Locale.US).format(asset.getTotalSupply()));
            mIssuer_TextView.setText(WalletClient.encode58Check(asset.getOwnerAddress().toByteArray()));
            mStart_TextView.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US).format(new Date(asset.getStartTime())));
            mEnd_TextView.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US).format(new Date(asset.getEndTime())));

            long currentTimeMillis = System.currentTimeMillis();
            boolean isRunning = currentTimeMillis >= asset.getStartTime() && currentTimeMillis <= asset.getEndTime();

            mParticipate_Button.setVisibility(isRunning ? View.VISIBLE : View.GONE);
        }
    }

    public boolean isShowFiltered() {
        return showFiltered;
    }

    public void setShowFiltered(boolean showFiltered) {
        this.showFiltered = showFiltered;
    }
}
