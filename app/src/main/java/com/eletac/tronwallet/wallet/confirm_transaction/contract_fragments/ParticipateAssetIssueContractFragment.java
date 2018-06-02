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

public class ParticipateAssetIssueContractFragment extends Fragment {

    private TextView mTokenTextView;
    private TextView mAmountTextView;
    private TextView mCostTextView;

    public ParticipateAssetIssueContractFragment() {
        // Required empty public constructor
    }

    public static ParticipateAssetIssueContractFragment newInstance() {
        ParticipateAssetIssueContractFragment fragment = new ParticipateAssetIssueContractFragment();
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
        return inflater.inflate(R.layout.fragment_participate_asset_issue_contract, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTokenTextView = view.findViewById(R.id.ParticipateAssetIssue_asset_textView);
        mAmountTextView = view.findViewById(R.id.ParticipateAssetIssue_amount_textView);
        mCostTextView = view.findViewById(R.id.ParticipateAssetIssue_cost_textView);

        Contract.ParticipateAssetIssueContract contract = null;
        Contract.AssetIssueContract assetIssueContract = null;
        try {
            contract = TransactionUtils.unpackContract(
                    ((ConfirmTransactionActivity)getActivity()).getUnsignedTransaction().getRawData().getContract(0), Contract.ParticipateAssetIssueContract.class);
            assetIssueContract = Contract.AssetIssueContract.parseFrom(((ConfirmTransactionActivity)getActivity()).getExtraBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(contract != null && assetIssueContract != null) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            numberFormat.setMaximumFractionDigits(6);
            mTokenTextView.setText(contract.getAssetName().toStringUtf8());
            mAmountTextView.setText(numberFormat.format((contract.getAmount()) * ((double)assetIssueContract.getNum()/(double)(assetIssueContract.getTrxNum()))));
            mCostTextView.setText(numberFormat.format(contract.getAmount()/1000000D));
        }
    }

}
