package com.eletac.tronwallet.block_explorer;


import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.WrapContentLinearLayoutManager;

import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AccountsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView mAccounts_RecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mTitle_TextView;
    private Switch mSearch_Switch;
    private CardView mSearch_CardView;
    private EditText mSearch_EditText;

    private LinearLayoutManager mLayoutManager;
    private AccountItemListAdapter mAccountItemListAdapter;

    private AccountsUpdatedBroadcastReceiver mAccountsUpdatedBroadcastReceiver;

    private List<Protocol.Account> mAccounts;
    private List<Protocol.Account> mAccountsFiltered;

    private int mSearchCardViewInitialHeight;

    public AccountsFragment() {
        // Required empty public constructor
    }

    public static AccountsFragment newInstance() {
        AccountsFragment fragment = new AccountsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccountsUpdatedBroadcastReceiver = new AccountsUpdatedBroadcastReceiver();
        mAccounts = BlockExplorerUpdater.getAccounts();
        mAccountsFiltered = new ArrayList<>();

        mAccountItemListAdapter = new AccountItemListAdapter(getContext(), mAccounts, mAccountsFiltered);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_accounts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAccounts_RecyclerView = view.findViewById(R.id.Accounts_recyclerView);
        mTitle_TextView= view.findViewById(R.id.Accounts_title_textView);
        mSwipeRefreshLayout = view.findViewById(R.id.Accounts_swipe_container);
        mSearch_Switch = view.findViewById(R.id.Accounts_search_switch);
        mSearch_CardView = view.findViewById(R.id.Accounts_search_cardView);
        mSearch_EditText = view.findViewById(R.id.Accounts_search_editText);

        mSwipeRefreshLayout.setOnRefreshListener(this);

        mLayoutManager = new WrapContentLinearLayoutManager(getContext());

        mAccounts_RecyclerView.setHasFixedSize(true);
        mAccounts_RecyclerView.setLayoutManager(mLayoutManager);
        mAccounts_RecyclerView.setAdapter(mAccountItemListAdapter);

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

                mAccountItemListAdapter.setShowFiltered(isChecked);
                mTitle_TextView.setText(String.format(Locale.US, "%s (%d)", getContext().getString(R.string.tab_title_accounts), mAccountItemListAdapter.isShowFiltered() ? mAccountsFiltered.size() : mAccounts.size()));
                mAccountItemListAdapter.notifyDataSetChanged();
            }
        });

        mSearch_EditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFilteredAccounts();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        updateFilteredAccounts();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mAccountsUpdatedBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mAccountsUpdatedBroadcastReceiver, new IntentFilter(BlockExplorerUpdater.ACCOUNTS_UPDATED));
        if(BlockExplorerUpdater.getAccounts().isEmpty())
            onRefresh();
        else if(!BlockExplorerUpdater.isRunning(BlockExplorerUpdater.UpdateTask.Accounts) && !BlockExplorerUpdater.isSingleShotting(BlockExplorerUpdater.UpdateTask.Accounts) ) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        BlockExplorerUpdater.singleShot(BlockExplorerUpdater.UpdateTask.Accounts, true);
    }

    private void updateFilteredAccounts() {
        mAccountsFiltered.clear();
        for(Protocol.Account account : mAccounts) {
            if(checkFilterConditions(account)) {
                mAccountsFiltered.add(account);
            }
        }
        mTitle_TextView.setText(String.format(Locale.US, "%s (%d)", getContext().getString(R.string.tab_title_accounts), mAccountItemListAdapter.isShowFiltered() ? mAccountsFiltered.size() : mAccounts.size()));
        mAccountItemListAdapter.notifyDataSetChanged();
    }

    private boolean checkFilterConditions(Protocol.Account account) {
        String filter = mSearch_EditText.getText().toString();
        return WalletClient.encode58Check(account.getAddress().toByteArray()).toLowerCase().contains(filter.toLowerCase());
    }

    private class AccountsUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            updateFilteredAccounts();

            mSwipeRefreshLayout.setRefreshing(false);
        }
    }
}
