package com.eletac.tronwallet.block_explorer;


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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Utils;
import com.eletac.tronwallet.WrapContentLinearLayoutManager;

import org.tron.protos.Protocol;
import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecentBlocksFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView mBlocks_RecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private LinearLayoutManager mLayoutManager;
    private BlockItemListAdapter mBlockItemListAdapter;

    private BlocksUpdatedBroadcastReceiver mBlocksUpdatedBroadcastReceiver;

    private List<Protocol.Block> mBlocks;

    public RecentBlocksFragment() {
        // Required empty public constructor
    }

    public static RecentBlocksFragment newInstance() {
        RecentBlocksFragment fragment = new RecentBlocksFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBlocksUpdatedBroadcastReceiver = new BlocksUpdatedBroadcastReceiver();
        mBlocks = BlockExplorerUpdater.getBlocks();
        mBlockItemListAdapter = new BlockItemListAdapter(getContext(), mBlocks);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBlocksUpdatedBroadcastReceiver, new IntentFilter(BlockExplorerUpdater.BLOCKCHAIN_UPDATED));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recent_blocks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBlocks_RecyclerView = view.findViewById(R.id.RecentBlocks_blocks_recyclerView);

        mSwipeRefreshLayout = view.findViewById(R.id.RecentBlocks_swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mLayoutManager = new WrapContentLinearLayoutManager(getContext());
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);

        mBlocks_RecyclerView.setHasFixedSize(true);
        mBlocks_RecyclerView.setLayoutManager(mLayoutManager);
        mBlocks_RecyclerView.setAdapter(mBlockItemListAdapter);

        onRefresh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBlocksUpdatedBroadcastReceiver);
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        BlockExplorerUpdater.singleShot(BlockExplorerUpdater.UpdateTask.Blockchain, true);
    }

    private class BlocksUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            mBlockItemListAdapter.notifyDataSetChanged();

            mBlocks_RecyclerView.scrollToPosition(mBlocks.size()-1);

            mSwipeRefreshLayout.setRefreshing(false);
        }
    }
}
