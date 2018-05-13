package com.eletac.tronwallet.block_explorer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eletac.tronwallet.R;

import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.util.ArrayList;
import java.util.List;

public class BlockItemListAdapter extends RecyclerView.Adapter<BlockItemListAdapter.BlockItemViewHolder> {

    private Context mContext;
    private List<Protocol.Block> mBlocks;

    public BlockItemListAdapter(Context context, List<Protocol.Block> blocks) {
        mContext = context;
        mBlocks = blocks;
    }

    @NonNull
    @Override
    public BlockItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_block_item, parent, false);
        BlockItemViewHolder viewHolder = new BlockItemViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull BlockItemViewHolder holder, int position) {
        holder.bind(mBlocks.get(position));
    }

    @Override
    public int getItemCount() {
        return mBlocks != null ? mBlocks.size() : 0;
    }

    public class BlockItemViewHolder extends RecyclerView.ViewHolder {
        private Context mContext;

        private TextView mBlockNumber_TextView;
        private TextView mBlockElapsedTime_TextView;
        private TextView mBlockTransactionsAmount_TextView;
        private TextView mBlockProducerAddress_TextView;

        public BlockItemViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();

            mBlockNumber_TextView = itemView.findViewById(R.id.Block_num_textView);
            mBlockElapsedTime_TextView = itemView.findViewById(R.id.Block_elapsed_time_textView);
            mBlockTransactionsAmount_TextView = itemView.findViewById(R.id.Block_transactions_amount_textView);
            mBlockProducerAddress_TextView = itemView.findViewById(R.id.Block_producer_address_textView);
        }

        public void bind(Protocol.Block block) {
            String blockNumberStr = "#"+String.valueOf(block.getBlockHeader().getRawData().getNumber());
            CharSequence timeString = DateUtils.getRelativeTimeSpanString(block.getBlockHeader().getRawData().getTimestamp(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);

            mBlockNumber_TextView.setText(blockNumberStr);
            mBlockElapsedTime_TextView.setText(timeString);
            mBlockTransactionsAmount_TextView.setText(String.valueOf(block.getTransactionsCount()));
            mBlockProducerAddress_TextView.setText(WalletClient.encode58Check(block.getBlockHeader().getRawData().getWitnessAddress().toByteArray()));
        }

    }
}
