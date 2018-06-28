package com.eletac.tronwallet.block_explorer.contract.contract_type_fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import org.tron.walletserver.WalletManager;

import java.text.NumberFormat;
import java.util.Locale;

public class TransferAssetContractFragment extends ContractFragment {

    private Contract.TransferAssetContract mContract;

    private TextView mAmountTextView;
    private TextView mSymbolTextView;
    private TextView mToTextView;

    public TransferAssetContractFragment() {
        // Required empty public constructor
    }

    public static TransferAssetContractFragment newInstance() {
        TransferAssetContractFragment fragment = new TransferAssetContractFragment();
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
        return inflater.inflate(R.layout.fragment_transfer_asset_contract, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAmountTextView = view.findViewById(R.id.TransferAssetContract_amount_textView);
        mSymbolTextView = view.findViewById(R.id.TransferAssetContract_symbol_textView);
        mToTextView = view.findViewById(R.id.TransferAssetContract_to_textView);

        updateUI();
    }

    @Override
    public void setContract(Protocol.Transaction.Contract contract) {
        try {
            mContract = TransactionUtils.unpackContract(contract, Contract.TransferAssetContract.class);
            updateUI();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    public void updateUI() {
        if(mContract != null && getView() != null) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            numberFormat.setMaximumFractionDigits(6);
            mAmountTextView.setText(numberFormat.format(mContract.getAmount()));
            mSymbolTextView.setText(mContract.getAssetName().toStringUtf8());
            mToTextView.setText(WalletManager.encode58Check(mContract.getToAddress().toByteArray()));
        }
    }
}
