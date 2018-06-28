package com.eletac.tronwallet.block_explorer.contract.contract_type_fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.block_explorer.contract.ContractFragment;
import com.eletac.tronwallet.wallet.confirm_transaction.ConfirmTransactionActivity;
import com.google.protobuf.InvalidProtocolBufferException;

import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.text.NumberFormat;
import java.util.Locale;

public class FreezeContractFragment extends ContractFragment {

    private Contract.FreezeBalanceContract mContract;

    private TextView mAmountTextView;
    private TextView mDaysTextView;

    public FreezeContractFragment() {
        // Required empty public constructor
    }

    public static FreezeContractFragment newInstance() {
        FreezeContractFragment fragment = new FreezeContractFragment();
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
        return inflater.inflate(R.layout.fragment_freeze_contract, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAmountTextView = view.findViewById(R.id.FreezeContract_amount_textView);
        mDaysTextView = view.findViewById(R.id.FreezeContract_days_textView);

        updateUI();
    }

    @Override
    public void setContract(Protocol.Transaction.Contract contract) {
        try {
            mContract = TransactionUtils.unpackContract(contract, Contract.FreezeBalanceContract.class);
            updateUI();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    public void updateUI() {
        if(mContract != null && getView() != null) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            mAmountTextView.setText(numberFormat.format(mContract.getFrozenBalance()/1000000D));
            mDaysTextView.setText(numberFormat.format(mContract.getFrozenDuration()));
        }
    }
}
