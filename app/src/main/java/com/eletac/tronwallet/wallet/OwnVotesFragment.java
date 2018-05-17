package com.eletac.tronwallet.wallet;


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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Utils;
import com.eletac.tronwallet.WrapContentLinearLayoutManager;
import com.eletac.tronwallet.block_explorer.BlockExplorerUpdater;

import org.tron.protos.Protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OwnVotesFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView mWitnesses_RecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private LinearLayoutManager mLayoutManager;
    private WitnessItemListAdapter mWitnessItemListAdapter;

    private AccountUpdatedBroadcastReceiver mAccountUpdatedBroadcastReceiver;
    private WitnessesUpdatedBroadcastReceiver mWitnessesUpdatedBroadcastReceiver;
    private VotesUpdatedBroadcastReceiver mVotesUpdatedBroadcastReceiver;

    private List<Protocol.Witness> mWitnesses;
    private List<Protocol.Witness> mVotedWitnesses;

    private Protocol.Account mAccount;

    private VoteActivity mVoteActivity;

    public OwnVotesFragment() {
        // Required empty public constructor
    }

    public static OwnVotesFragment newInstance() {
        OwnVotesFragment fragment = new OwnVotesFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVoteActivity = (VoteActivity) getActivity();

        mAccountUpdatedBroadcastReceiver = new AccountUpdatedBroadcastReceiver();
        mWitnessesUpdatedBroadcastReceiver = new WitnessesUpdatedBroadcastReceiver();
        mVotesUpdatedBroadcastReceiver = new VotesUpdatedBroadcastReceiver();
        mAccount = Utils.getAccount(getContext());
        mWitnesses = BlockExplorerUpdater.getWitnesses();
        mVotedWitnesses = new ArrayList<>();

        mWitnessItemListAdapter = new WitnessItemListAdapter(getContext(), mWitnesses, mVotedWitnesses, true, mVoteActivity.getVoteWitnesses());
        mWitnessItemListAdapter.setShowFiltered(true);

        loadVotedWitnesses();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_own_votes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mWitnesses_RecyclerView = view.findViewById(R.id.Votes_votes_recyclerView);

        mSwipeRefreshLayout = view.findViewById(R.id.Votes_swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mLayoutManager = new WrapContentLinearLayoutManager(getContext());

        mWitnesses_RecyclerView.setHasFixedSize(true);
        mWitnesses_RecyclerView.setLayoutManager(mLayoutManager);
        mWitnesses_RecyclerView.setAdapter(mWitnessItemListAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mAccountUpdatedBroadcastReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mWitnessesUpdatedBroadcastReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mVotesUpdatedBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mAccountUpdatedBroadcastReceiver, new IntentFilter(AccountUpdater.ACCOUNT_UPDATED));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mWitnessesUpdatedBroadcastReceiver, new IntentFilter(BlockExplorerUpdater.WITNESSES_UPDATED));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mVotesUpdatedBroadcastReceiver, new IntentFilter(VoteActivity.VOTES_UPDATED));
        onRefresh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mWitnessesUpdatedBroadcastReceiver);
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        AccountUpdater.singleShot(0);
        BlockExplorerUpdater.singleShot(BlockExplorerUpdater.UpdateTask.Witnesses, true);

        // Reset refresher when loading takes to long or broke
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, 20000);
    }

    private void loadVotedWitnesses() {
        mVotedWitnesses.clear();
        for(Protocol.Account.Vote vote : mAccount.getVotesList()) {
            for(Protocol.Witness witness : mWitnesses) {
                try {
                    if (Arrays.equals(vote.getVoteAddress().toByteArray(), witness.getAddress().toByteArray())) {
                        mVotedWitnesses.add(witness);
                        break;
                    }
                }  catch (NullPointerException ignore) {}
            }
        }

        mWitnessItemListAdapter.notifyDataSetChanged();
    }

    private class WitnessesUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            loadVotedWitnesses();
            mWitnessItemListAdapter.notifyDataSetChanged();
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    private class AccountUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mAccount = Utils.getAccount(context);
        }
    }

    private class VotesUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            loadVotedWitnesses();
        }
    }
}
