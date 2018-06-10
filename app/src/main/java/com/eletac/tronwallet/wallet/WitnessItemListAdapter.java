package com.eletac.tronwallet.wallet;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.eletac.tronwallet.R;

import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WitnessItemListAdapter extends RecyclerView.Adapter<WitnessItemListAdapter.WitnessItemViewHolder> {

    public static final String VOTES_UPDATED = "com.eletac.tronwallet.witness_adapter.votes_updated";

    private Context mContext;
    private List<Protocol.Witness> mWitnesses;
    private List<Protocol.Witness> mWitnessesFiltered;
    private boolean mShowVoteEditText;

    private HashMap<String, String> mVotes;

    private boolean showFiltered = false;

    public WitnessItemListAdapter(Context context, List<Protocol.Witness> witnesses, List<Protocol.Witness> witnessesFiltered) {
        mContext = context;
        mWitnesses = witnesses;
        mWitnessesFiltered = witnessesFiltered;
        mShowVoteEditText = false;
        mVotes = null;
    }

    public WitnessItemListAdapter(Context context, List<Protocol.Witness> witnesses, List<Protocol.Witness> witnessesFiltered, boolean showVoteEditText, HashMap<String, String> votes) {
        mContext = context;
        mWitnesses = witnesses;
        mWitnessesFiltered = witnessesFiltered;
        mShowVoteEditText = showVoteEditText;
        mVotes = votes;
    }

    @NonNull
    @Override
    public WitnessItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_vote_witness_item, parent, false);
        WitnessItemViewHolder viewHolder = new WitnessItemViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull WitnessItemViewHolder holder, int position) {
        if(showFiltered) {
            holder.bind(mWitnessesFiltered.get(position));
        } else {
            holder.bind(mWitnesses.get(position));
        }
    }

    @Override
    public int getItemCount() {
        if(showFiltered) {
            return mWitnessesFiltered != null ? mWitnessesFiltered.size() : 0;
        }
        else {
            return mWitnesses != null ? mWitnesses.size() : 0;
        }
    }


    public class WitnessItemViewHolder extends RecyclerView.ViewHolder {
        private Context mContext;

        private TextView mRank_TextView;
        private TextView mUrl_TextView;
        private TextView mAddress_TextView;
        private TextView mTotalVotes_TextView;
        private TextView mLastBlock_TextView;
        private TextView mProduced_TextView;
        private TextView mMissed_TextView;
        private EditText mVoteNumber_EditText;

        private Protocol.Witness mWitness;

        private TextWatcher mTextWatcher;

        public WitnessItemViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();

            mRank_TextView = itemView.findViewById(R.id.Witness_rank_textView);
            mUrl_TextView = itemView.findViewById(R.id.Witness_url_textView);
            mAddress_TextView = itemView.findViewById(R.id.Witness_address_textView);
            mTotalVotes_TextView = itemView.findViewById(R.id.Witness_total_votes_textView);
            mLastBlock_TextView = itemView.findViewById(R.id.Witness_last_block_textView);
            mProduced_TextView = itemView.findViewById(R.id.Witness_produced_textView);
            mMissed_TextView = itemView.findViewById(R.id.Witness_missed_textView);
            mVoteNumber_EditText = itemView.findViewById(R.id.Witness_votes_editText);

            mVoteNumber_EditText.setVisibility(mShowVoteEditText ? View.VISIBLE : View.GONE);

            if(mShowVoteEditText) {
                mTextWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        int votes;

                        try {
                            votes = mVoteNumber_EditText.getText().length() > 0 ? Integer.parseInt(mVoteNumber_EditText.getText().toString()) : 0;
                        } catch (NumberFormatException ignored) {
                            votes = 0;
                            mVoteNumber_EditText.setText(String.valueOf(votes));
                        }

                        String witnessAddress = null;

                        if (mWitness != null) {
                            witnessAddress = WalletClient.encode58Check(mWitness.getAddress().toByteArray());
                        }

                        if (witnessAddress != null) {
                            if (votes > 0) {
                                mVotes.put(witnessAddress, String.valueOf(votes));
                            } else {
                                mVotes.remove(witnessAddress);
                            }
                            Intent updatedIntent = new Intent(VOTES_UPDATED);
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(updatedIntent);
                        }
                    }
                };

                mVoteNumber_EditText.addTextChangedListener(mTextWatcher);
            }
        }

        public void bind(Protocol.Witness witness) {

            mWitness = witness;

            NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

            mRank_TextView.setText(String.format("#%s", numberFormat.format(mWitnesses.indexOf(witness)+1)));
            mRank_TextView.setTextColor(witness.getIsJobs() ? Color.GREEN : Color.WHITE);
            mUrl_TextView.setText(witness.getUrl());
            mAddress_TextView.setText(WalletClient.encode58Check(witness.getAddress().toByteArray()));
            mTotalVotes_TextView.setText(numberFormat.format(witness.getVoteCount()));
            mLastBlock_TextView.setText(numberFormat.format(witness.getLatestBlockNum()));
            mProduced_TextView.setText(numberFormat.format(witness.getTotalProduced()));
            mMissed_TextView.setText(numberFormat.format(witness.getTotalMissed()));

            if(mShowVoteEditText) {
                mVoteNumber_EditText.removeTextChangedListener(mTextWatcher);
                mVoteNumber_EditText.setText("");
                for(Map.Entry<String, String> entry : mVotes.entrySet()) {
                    if(entry.getKey().equals(mAddress_TextView.getText().toString())) {
                        mVoteNumber_EditText.setText(entry.getValue());
                    }
                }
                mVoteNumber_EditText.addTextChangedListener(mTextWatcher);
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
