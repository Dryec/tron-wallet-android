package com.eletac.tronwallet.wallet;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Token;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TokenListAdapter extends RecyclerView.Adapter<TokenListAdapter.TokenViewHolder> {

    private Context mContext;
    private List<Token> mTokens;

    public TokenListAdapter(Context context, List<Token> tokens) {
        mContext = context;
        mTokens = tokens;
    }

    @NonNull
    @Override
    public TokenListAdapter.TokenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_token_item, parent, false);
        TokenListAdapter.TokenViewHolder viewHolder = new TokenListAdapter.TokenViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TokenListAdapter.TokenViewHolder holder, int position) {
        holder.bind(mTokens.get(position));
    }

    @Override
    public int getItemCount() {
        return mTokens.size();
    }

    public class TokenViewHolder extends RecyclerView.ViewHolder {
        private Context mContext;

        private TextView mTokenName_TextView;
        private TextView mTokenPerPrice_TextView;
        private TextView mTokenBalance_TextView;
        private TextView mTokenTotalPrice_TextView;

        public TokenViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();

            mTokenName_TextView = itemView.findViewById(R.id.token_name_textView);
            mTokenPerPrice_TextView = itemView.findViewById(R.id.token_per_price_textView);
            mTokenBalance_TextView = itemView.findViewById(R.id.token_balance_textView);
            mTokenTotalPrice_TextView = itemView.findViewById(R.id.token_total_price_textView);
        }

        public void bind(Token token) {
            mTokenName_TextView.setText(token.getName());
            mTokenPerPrice_TextView.setText("$0.00"); // @TODO load token price (CoinMarketCap(?))
            mTokenBalance_TextView.setText(NumberFormat.getInstance(Locale.US).format(token.getAmount()));
            mTokenTotalPrice_TextView.setText("$0.00");
        }
    }
}
