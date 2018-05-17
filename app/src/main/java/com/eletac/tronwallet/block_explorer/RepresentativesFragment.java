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
import android.util.Log;
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

import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RepresentativesFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView mCandidates_RecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mTitle_TextView;
    private Switch mSearch_Switch;
    private CardView mSearch_CardView;
    private EditText mSearch_EditText;

    private LinearLayoutManager mLayoutManager;
    private WitnessItemListAdapter mWitnessItemListAdapter;

    private WitnessesUpdatedBroadcastReceiver mWitnessesUpdatedBroadcastReceiver;

    private List<Protocol.Witness> mWitnesses;
    private List<Protocol.Witness> mWitnessesFiltered;

    private int mSearchCardViewInitialHeight;

    public RepresentativesFragment() {
        // Required empty public constructor
    }

    public static RepresentativesFragment newInstance() {
        RepresentativesFragment fragment = new RepresentativesFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mWitnessesUpdatedBroadcastReceiver = new WitnessesUpdatedBroadcastReceiver();
        mWitnesses = BlockExplorerUpdater.getWitnesses();
        mWitnessesFiltered = new ArrayList<>();

        mWitnessItemListAdapter = new WitnessItemListAdapter(getContext(), mWitnesses, mWitnessesFiltered);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_representatives, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCandidates_RecyclerView = view.findViewById(R.id.Representatives_recyclerView);
        mTitle_TextView= view.findViewById(R.id.Representatives_title_textView);
        mSwipeRefreshLayout = view.findViewById(R.id.Representatives_swipe_container);
        mSearch_Switch = view.findViewById(R.id.Representatives_search_switch);
        mSearch_CardView = view.findViewById(R.id.Representatives_search_cardView);
        mSearch_EditText = view.findViewById(R.id.Representatives_search_editText);

        mSwipeRefreshLayout.setOnRefreshListener(this);

        mLayoutManager = new WrapContentLinearLayoutManager(getContext());

        mCandidates_RecyclerView.setHasFixedSize(true);
        mCandidates_RecyclerView.setLayoutManager(mLayoutManager);
        mCandidates_RecyclerView.setAdapter(mWitnessItemListAdapter);

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

                mWitnessItemListAdapter.setShowFiltered(isChecked);
                mTitle_TextView.setText(String.format(Locale.US, "%s (%d)", getContext().getString(R.string.tab_title_candidates), mWitnessItemListAdapter.isShowFiltered() ? mWitnessesFiltered.size() : mWitnesses.size()));
                mWitnessItemListAdapter.notifyDataSetChanged();
            }
        });

        mSearch_EditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFilteredWitnesses();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        updateFilteredWitnesses();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mWitnessesUpdatedBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mWitnessesUpdatedBroadcastReceiver, new IntentFilter(BlockExplorerUpdater.WITNESSES_UPDATED));
        if(BlockExplorerUpdater.getWitnesses().isEmpty())
            onRefresh();
        else if(!BlockExplorerUpdater.isRunning(BlockExplorerUpdater.UpdateTask.Witnesses) && !BlockExplorerUpdater.isSingleShotting(BlockExplorerUpdater.UpdateTask.Witnesses) ) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        BlockExplorerUpdater.singleShot(BlockExplorerUpdater.UpdateTask.Witnesses, true);
    }

    private void updateFilteredWitnesses() {
        mWitnessesFiltered.clear();
        for(Protocol.Witness witness : mWitnesses) {
            try {
                if (checkFilterConditions(witness)) {
                    mWitnessesFiltered.add(witness);
                }
            } catch (NullPointerException ignore) {}
        }
        mTitle_TextView.setText(String.format(Locale.US, "%s (%d)", getContext().getString(R.string.tab_title_candidates), mWitnessItemListAdapter.isShowFiltered() ? mWitnessesFiltered.size() : mWitnesses.size()));
        mWitnessItemListAdapter.notifyDataSetChanged();
    }

    private boolean checkFilterConditions(Protocol.Witness witness) {
        String filter = mSearch_EditText.getText().toString();
        return WalletClient.encode58Check(witness.getAddress().toByteArray()).toLowerCase().contains(filter.toLowerCase()) || witness.getUrl().toLowerCase().contains(filter.toLowerCase());
    }

    private class WitnessesUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateFilteredWitnesses();
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }
}
