package com.eletac.tronwallet.block_explorer;


import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.WrapContentLinearLayoutManager;
import com.eletac.tronwallet.wallet.IssueTokenActivity;

import org.tron.protos.Contract;
import org.tron.walletserver.WalletManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TokensFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView mTokens_RecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mTitle_TextView;
    private Switch mSearch_Switch;
    private CardView mSearch_CardView;
    private EditText mSearch_EditText;
    private Button mCreate_Button;

    private LinearLayoutManager mLayoutManager;
    private TokenItemListAdapter mTokenItemListAdapter;

    private TokensUpdatedBroadcastReceiver mTokensUpdatedBroadcastReceiver;

    private List<Contract.AssetIssueContract> mTokens;
    private List<Contract.AssetIssueContract> mTokensFiltered;

    private int mSearchCardViewInitialHeight;

    public TokensFragment() {
        // Required empty public constructor
    }

    public static TokensFragment newInstance() {
        TokensFragment fragment = new TokensFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTokensUpdatedBroadcastReceiver = new TokensUpdatedBroadcastReceiver();
        mTokens = BlockExplorerUpdater.getTokens();
        mTokensFiltered = new ArrayList<>();

        mTokenItemListAdapter = new TokenItemListAdapter(getContext(), mTokens, mTokensFiltered);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tokens, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTokens_RecyclerView = view.findViewById(R.id.Tokens_recyclerView);
        mTitle_TextView= view.findViewById(R.id.Tokens_title_textView);
        mSwipeRefreshLayout = view.findViewById(R.id.Tokens_swipe_container);
        mSearch_Switch = view.findViewById(R.id.Tokens_search_switch);
        mSearch_CardView = view.findViewById(R.id.Tokens_search_cardView);
        mSearch_EditText = view.findViewById(R.id.Tokens_search_editText);
        mCreate_Button = view.findViewById(R.id.Tokens_create_button);

        mSwipeRefreshLayout.setOnRefreshListener(this);

        mLayoutManager = new WrapContentLinearLayoutManager(getContext());

        mTokens_RecyclerView.setHasFixedSize(true);
        mTokens_RecyclerView.setLayoutManager(mLayoutManager);
        mTokens_RecyclerView.setAdapter(mTokenItemListAdapter);

        mSearchCardViewInitialHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45f, getResources().getDisplayMetrics());//mSearch_CardView.getMeasuredHeight();
        mSearch_CardView.getLayoutParams().height = 0;
        mSearch_CardView.requestLayout();

        mSearch_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if(isChecked)
                    imm.showSoftInput(mSearch_EditText, InputMethodManager.SHOW_IMPLICIT);
                else
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                ValueAnimator animator = ValueAnimator.ofInt(mSearch_CardView.getMeasuredHeight(), isChecked ? mSearchCardViewInitialHeight : 0);

                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mSearch_CardView.getLayoutParams().height = (int) animation.getAnimatedValue();
                        mSearch_CardView.requestLayout();
                        float completion = (float)animation.getCurrentPlayTime()/(float)animation.getDuration();
                        mSearch_CardView.setAlpha(isChecked ? (completion) : ((completion-1)*(-1)));
                    }
                });
                animator.setDuration(200);
                animator.start();

                mTokenItemListAdapter.setShowFiltered(isChecked);
                mTitle_TextView.setText(String.format(Locale.US, "%s (%d)", getContext().getString(R.string.tab_title_tokens), mTokenItemListAdapter.isShowFiltered() ? mTokensFiltered.size() : mTokens.size()));
                mTokenItemListAdapter.notifyDataSetChanged();
            }
        });

        mSearch_EditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFilteredTokens();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mCreate_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), IssueTokenActivity.class);
                startActivity(intent);
            }
        });

        updateFilteredTokens();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mTokensUpdatedBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mTokensUpdatedBroadcastReceiver, new IntentFilter(BlockExplorerUpdater.TOKENS_UPDATED));
        if(BlockExplorerUpdater.getTokens().isEmpty())
            onRefresh();
        else if(!BlockExplorerUpdater.isRunning(BlockExplorerUpdater.UpdateTask.Tokens) && !BlockExplorerUpdater.isSingleShotting(BlockExplorerUpdater.UpdateTask.Tokens) ) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        BlockExplorerUpdater.singleShot(BlockExplorerUpdater.UpdateTask.Tokens, true);
    }

    private void updateFilteredTokens() {
        mTokensFiltered.clear();
        for(Contract.AssetIssueContract asset : mTokens) {
            try {
                if (checkFilterConditions(asset)) {
                    mTokensFiltered.add(asset);
                }
            }  catch (NullPointerException ignore) {}
        }
        mTitle_TextView.setText(String.format(Locale.US, "%s (%d)", getContext().getString(R.string.tab_title_tokens), mTokenItemListAdapter.isShowFiltered() ? mTokensFiltered.size() : mTokens.size()));
        mTokenItemListAdapter.notifyDataSetChanged();
    }

    private boolean checkFilterConditions(Contract.AssetIssueContract asset) {
        String filter = mSearch_EditText.getText().toString().toLowerCase();
        return WalletManager.encode58Check(asset.getOwnerAddress().toByteArray()).toLowerCase().contains(filter)
                || asset.getName().toStringUtf8().toLowerCase().contains(filter)
                || asset.getDescription().toStringUtf8().toLowerCase().contains(filter);
    }

    private class TokensUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateFilteredTokens();
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }
}
