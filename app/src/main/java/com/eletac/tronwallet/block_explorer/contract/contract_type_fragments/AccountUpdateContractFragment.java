package com.eletac.tronwallet.block_explorer.contract.contract_type_fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.block_explorer.contract.ContractFragment;
import com.google.protobuf.InvalidProtocolBufferException;

import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletManager;

import java.text.NumberFormat;
import java.util.Locale;

public class AccountUpdateContractFragment extends ContractFragment {

    private Contract.AccountUpdateContract mContract;

    private TextView mFrom_TextView;
    private TextView mName_TextView;

    public AccountUpdateContractFragment() {
        // Required empty public constructor
    }

    public static AccountUpdateContractFragment newInstance() {
        AccountUpdateContractFragment fragment = new AccountUpdateContractFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account_update_contract, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFrom_TextView = view.findViewById(R.id.AccountUpdateContract_from_textView);
        mName_TextView = view.findViewById(R.id.AccountUpdateContract_name_textView);

        updateUI();
    }

    @Override
    public void setContract(Protocol.Transaction.Contract contract) {
        try {
            mContract = TransactionUtils.unpackContract(contract, Contract.AccountUpdateContract.class);
            updateUI();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    public void updateUI() {
        if(mContract != null && getView() != null) {
            mFrom_TextView.setText(WalletManager.encode58Check(mContract.getOwnerAddress().toByteArray()));
            mName_TextView.setText(mContract.getAccountName().toStringUtf8());
        }
    }
}
