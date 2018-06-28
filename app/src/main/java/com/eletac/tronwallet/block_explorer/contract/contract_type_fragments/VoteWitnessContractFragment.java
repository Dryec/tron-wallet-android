package com.eletac.tronwallet.block_explorer.contract.contract_type_fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.WrapContentLinearLayoutManager;
import com.eletac.tronwallet.block_explorer.contract.ContractFragment;
import com.eletac.tronwallet.wallet.confirm_transaction.ConfirmTransactionActivity;
import com.google.protobuf.InvalidProtocolBufferException;

import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.util.ArrayList;
import java.util.List;

public class VoteWitnessContractFragment extends ContractFragment {

    private Contract.VoteWitnessContract mContract;

    private RecyclerView mWitnesses_RecyclerView;
    private VoteItemListAdapter mVoteItemListAdapter;
    private List<Contract.VoteWitnessContract.Vote> mVotes;
    private LinearLayoutManager mLayoutManager;

    public VoteWitnessContractFragment() {
        // Required empty public constructor
    }

    public static VoteWitnessContractFragment newInstance() {
        VoteWitnessContractFragment fragment = new VoteWitnessContractFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVotes = new ArrayList<>();
        mVoteItemListAdapter = new VoteItemListAdapter(getContext(), mVotes);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vote_witness_contract, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mWitnesses_RecyclerView = view.findViewById(R.id.VoteWitnessContract_votes_recyclerView);

        mLayoutManager = new WrapContentLinearLayoutManager(getContext());

        mWitnesses_RecyclerView.setHasFixedSize(true);
        mWitnesses_RecyclerView.setLayoutManager(mLayoutManager);
        mWitnesses_RecyclerView.setAdapter(mVoteItemListAdapter);

        updateUI();
    }

    @Override
    public void setContract(Protocol.Transaction.Contract contract) {
        try {
            mContract = TransactionUtils.unpackContract(contract, Contract.VoteWitnessContract.class);
            updateUI();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    public void updateUI() {
        if(mContract != null && getView() != null) {
            mVotes.clear();
            mVotes.addAll(mContract.getVotesList());
            mVoteItemListAdapter.notifyDataSetChanged();
        }
    }
}
