package com.eletac.tronwallet.block_explorer;


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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.WrapContentLinearLayoutManager;

import org.tron.protos.Protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecentTransactionsFragment extends Fragment {

    private RecyclerView mTransactions_RecyclerView;

    private LinearLayoutManager mLayoutManager;
    private TransactionItemListAdapter mTransactionsItemListAdapter;

    private TransactionsUpdatedBroadcastReceiver mTransactionsUpdatedBroadcastReceiver;
    private List<Protocol.Transaction> mTransactions;

    public RecentTransactionsFragment() {
        // Required empty public constructor
    }

    public static RecentTransactionsFragment newInstance() {
        RecentTransactionsFragment fragment = new RecentTransactionsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mTransactions = BlockExplorerUpdater.getTransactions();
        mTransactionsItemListAdapter = new TransactionItemListAdapter(getContext(), mTransactions);

        mTransactionsUpdatedBroadcastReceiver = new TransactionsUpdatedBroadcastReceiver();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mTransactionsUpdatedBroadcastReceiver, new IntentFilter(BlockExplorerUpdater.BLOCKCHAIN_UPDATED));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recent_transactions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTransactions_RecyclerView = view.findViewById(R.id.RecentTransactions_transactions_recyclerView);

        mLayoutManager = new WrapContentLinearLayoutManager(getContext());
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);

        mTransactions_RecyclerView.setHasFixedSize(true);
        mTransactions_RecyclerView.setLayoutManager(mLayoutManager);
        mTransactions_RecyclerView.setAdapter(mTransactionsItemListAdapter);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mTransactionsUpdatedBroadcastReceiver);
    }

    private class TransactionsUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mTransactionsItemListAdapter.notifyDataSetChanged();

            mTransactions_RecyclerView.scrollToPosition(mTransactions.size()-1);
        }
    }
}
