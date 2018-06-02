package com.eletac.tronwallet.wallet.confirm_transaction.contract_fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.wallet.confirm_transaction.ConfirmTransactionActivity;

import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Contract;
import org.tron.walletserver.WalletClient;

import java.text.NumberFormat;
import java.util.Locale;

public class TransferAssetContractFragment extends Fragment {

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

        Contract.TransferAssetContract contract = null;
        try {
            contract = TransactionUtils.unpackContract(
                    ((ConfirmTransactionActivity)getActivity()).getUnsignedTransaction().getRawData().getContract(0), Contract.TransferAssetContract.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(contract != null) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            numberFormat.setMaximumFractionDigits(6);
            mAmountTextView.setText(numberFormat.format(contract.getAmount()));
            mSymbolTextView.setText(contract.getAssetName().toStringUtf8());
            mToTextView.setText(WalletClient.encode58Check(contract.getToAddress().toByteArray()));
        }
    }

}
