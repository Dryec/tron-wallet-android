package com.eletac.tronwallet.block_explorer;


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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.WrapContentLinearLayoutManager;
import com.eletac.tronwallet.wallet.WitnessItemListAdapter;

import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NodesFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView mNodes_RecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mTitle_TextView;
    private Switch mSearch_Switch;
    private CardView mSearch_CardView;
    private EditText mSearch_EditText;

    private LinearLayoutManager mLayoutManager;
    private NodeItemListAdapter mNodesItemListAdapter;

    private NodesUpdatedBroadcastReceiver mNodesUpdatedBroadcastReceiver;

    private List<GrpcAPI.Node> mNodes;
    private List<GrpcAPI.Node> mNodesFiltered;

    private int mSearchCardViewInitialHeight;

    public NodesFragment() {
        // Required empty public constructor
    }

    public static NodesFragment newInstance() {
        NodesFragment fragment = new NodesFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNodesUpdatedBroadcastReceiver = new NodesUpdatedBroadcastReceiver();
        mNodes = BlockExplorerUpdater.getNodes();
        mNodesFiltered = new ArrayList<>();

        mNodesItemListAdapter = new NodeItemListAdapter(getContext(), mNodes, mNodesFiltered);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nodes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNodes_RecyclerView = view.findViewById(R.id.Nodes_recyclerView);
        mTitle_TextView= view.findViewById(R.id.Nodes_title_textView);
        mSwipeRefreshLayout = view.findViewById(R.id.Nodes_swipe_container);
        mSearch_Switch = view.findViewById(R.id.Nodes_search_switch);
        mSearch_CardView = view.findViewById(R.id.Nodes_search_cardView);
        mSearch_EditText = view.findViewById(R.id.Nodes_search_editText);

        mSwipeRefreshLayout.setOnRefreshListener(this);

        mLayoutManager = new WrapContentLinearLayoutManager(getContext());

        mNodes_RecyclerView.setHasFixedSize(true);
        mNodes_RecyclerView.setLayoutManager(mLayoutManager);
        mNodes_RecyclerView.setAdapter(mNodesItemListAdapter);

        mSearchCardViewInitialHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45f, getResources().getDisplayMetrics());
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

                mNodesItemListAdapter.setShowFiltered(isChecked);
                mTitle_TextView.setText(String.format(Locale.US, "%s (%d)", getContext().getString(R.string.tab_title_nodes), mNodesItemListAdapter.isShowFiltered() ? mNodesFiltered.size() : mNodes.size()));
                mNodesItemListAdapter.notifyDataSetChanged();
            }
        });

        mSearch_EditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFilteredNodes();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        updateFilteredNodes();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mNodesUpdatedBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mNodesUpdatedBroadcastReceiver, new IntentFilter(BlockExplorerUpdater.NODES_UPDATED));
        if(BlockExplorerUpdater.getNodes().isEmpty()) {
            onRefresh();
        }
        else if(!BlockExplorerUpdater.isRunning(BlockExplorerUpdater.UpdateTask.Nodes) && !BlockExplorerUpdater.isSingleShotting(BlockExplorerUpdater.UpdateTask.Nodes) ) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        BlockExplorerUpdater.singleShot(BlockExplorerUpdater.UpdateTask.Nodes, true);
    }

    private void updateFilteredNodes() {
        mNodesFiltered.clear();
        for(GrpcAPI.Node node : mNodes) {
            try {
                if (checkFilterConditions(node)) {
                    mNodesFiltered.add(node);
                }
            }  catch (NullPointerException ignore) {}
        }
        mTitle_TextView.setText(String.format(Locale.US, "%s (%d)", getContext().getString(R.string.tab_title_nodes), mNodesItemListAdapter.isShowFiltered() ? mNodesFiltered.size() : mNodes.size()));
        mNodesItemListAdapter.notifyDataSetChanged();
    }

    private boolean checkFilterConditions(GrpcAPI.Node node) {
        String filter = mSearch_EditText.getText().toString();
        return ByteArray.toStr(node.getAddress().getHost().toByteArray()).toLowerCase().contains(filter.toLowerCase()) || String.valueOf(node.getAddress().getPort()).toLowerCase().contains(filter.toLowerCase());
    }

    private class NodesUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            updateFilteredNodes();
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }
}
