package com.eletac.tronwallet.block_explorer.contract.contract_type_fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.block_explorer.contract.ContractFragment;
import com.eletac.tronwallet.wallet.confirm_transaction.ConfirmTransactionActivity;
import com.google.protobuf.InvalidProtocolBufferException;

import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

public class AssetIssueContractFragment extends ContractFragment {

    private Contract.AssetIssueContract mContract;

    private EditText mName_EditText;
    private EditText mAbbr_EditText;
    private EditText mSupply_EditText;
    private EditText mURL_EditText;
    private EditText mDesc_EditText;

    private EditText mExchangeTrxAmount_EditText;
    private EditText mExchangeTokenAmount_EditText;
    private TextView mTokenPrice_TextView;

    private EditText mFrozenAmount_EditText;
    private EditText mFrozenDays_EditText;

    private EditText mTotalBandwidth_EditText;
    private EditText mBandwidthPerAccount_EditText;

    private TextView mStart_TextView;
    private TextView mEnd_TextView;

    public AssetIssueContractFragment() {
        // Required empty public constructor
    }

    public static AssetIssueContractFragment newInstance() {
        AssetIssueContractFragment fragment = new AssetIssueContractFragment();
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
        return inflater.inflate(R.layout.fragment_asset_issue_contract, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mName_EditText = view.findViewById(R.id.IssueTokenContract_name_editText);
        mAbbr_EditText = view.findViewById(R.id.IssueTokenContract_abbr_editText);
        mSupply_EditText = view.findViewById(R.id.IssueTokenContract_supply_editText);
        mURL_EditText = view.findViewById(R.id.IssueTokenContract_url_editText);
        mDesc_EditText = view.findViewById(R.id.IssueTokenContract_desc_editText);
        mExchangeTrxAmount_EditText = view.findViewById(R.id.IssueTokenContract_trx_amount_editText);
        mExchangeTokenAmount_EditText = view.findViewById(R.id.IssueTokenContract_token_amount_editText);
        mTokenPrice_TextView = view.findViewById(R.id.IssueTokenContract_price_textView);
        mFrozenAmount_EditText = view.findViewById(R.id.IssueTokenContract_frozen_amount_editText);
        mFrozenDays_EditText = view.findViewById(R.id.IssueTokenContract_frozen_days_editText);
        mTotalBandwidth_EditText = view.findViewById(R.id.IssueTokenContract_total_bandwidth_editText);
        mBandwidthPerAccount_EditText = view.findViewById(R.id.IssueTokenContract_bandwidth_per_account_editText);
        mStart_TextView = view.findViewById(R.id.IssueTokenContract_start_time_textView);
        mEnd_TextView = view.findViewById(R.id.IssueTokenContract_end_time_textView);

        updateUI();
    }

    public void updateUI() {
        if(mContract != null && getView() != null) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            numberFormat.setMaximumFractionDigits(6);

            mName_EditText.setText(mContract.getName().toStringUtf8());
            mAbbr_EditText.setText(mContract.getAbbr().toStringUtf8());
            mSupply_EditText.setText(numberFormat.format(mContract.getTotalSupply()));
            mURL_EditText.setText(mContract.getUrl().toStringUtf8());
            mDesc_EditText.setText(mContract.getDescription().toStringUtf8());
            mExchangeTrxAmount_EditText.setText(numberFormat.format(mContract.getTrxNum()/(double)1000000));
            mExchangeTokenAmount_EditText.setText(numberFormat.format(mContract.getNum()));
            mTokenPrice_TextView.setText(numberFormat.format((mContract.getTrxNum()/(double)mContract.getNum())/(double)1000000));

            if(mContract.getFrozenSupplyCount() > 0) {
                Contract.AssetIssueContract.FrozenSupply frozenSupply = mContract.getFrozenSupply(0);
                mFrozenAmount_EditText.setText(numberFormat.format(frozenSupply.getFrozenAmount()));
                mFrozenDays_EditText.setText(numberFormat.format(frozenSupply.getFrozenDays()));
            }
            mTotalBandwidth_EditText.setText(numberFormat.format(mContract.getFreeAssetNetLimit()));
            mBandwidthPerAccount_EditText.setText(numberFormat.format(mContract.getPublicFreeAssetNetLimit()));


            DateFormat dateTimeInstance = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US);
            Date start = new Date(mContract.getStartTime());
            Date end = new Date(mContract.getEndTime());

            mStart_TextView.setText(dateTimeInstance.format(start));
            mEnd_TextView.setText(dateTimeInstance.format(end));
        }
    }

    @Override
    public void setContract(Protocol.Transaction.Contract contract) {
        try {
            mContract = TransactionUtils.unpackContract(contract, Contract.AssetIssueContract.class);
            updateUI();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}
