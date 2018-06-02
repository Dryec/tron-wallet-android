package com.eletac.tronwallet.wallet.confirm_transaction.contract_fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.eletac.tronwallet.R;

import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VoteItemListAdapter extends RecyclerView.Adapter<VoteItemListAdapter.VoteItemViewHolder> {

    private Context mContext;
    private List<Contract.VoteWitnessContract.Vote> mVotes;

    public VoteItemListAdapter(Context context, List<Contract.VoteWitnessContract.Vote> votes) {
        mContext = context;
        mVotes = votes;
    }

    @NonNull
    @Override
    public VoteItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_vote_item, parent, false);
        VoteItemViewHolder viewHolder = new VoteItemViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull VoteItemViewHolder holder, int position) {
        holder.bind(mVotes.get(position));
    }

    @Override
    public int getItemCount() {
        return mVotes != null ? mVotes.size() : 0;
    }


    public class VoteItemViewHolder extends RecyclerView.ViewHolder {
        private Context mContext;

        private TextView mAddress_TextView;
        private TextView mAmount_TextView;

        public VoteItemViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();

            mAddress_TextView = itemView.findViewById(R.id.Vote_address_textView);
            mAmount_TextView = itemView.findViewById(R.id.Vote_amount_textView);
        }

        public void bind(Contract.VoteWitnessContract.Vote vote) {
            NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

            mAddress_TextView.setText(WalletClient.encode58Check(vote.getVoteAddress().toByteArray()));
            mAmount_TextView.setText(numberFormat.format(vote.getVoteCount()));
        }
    }
}
