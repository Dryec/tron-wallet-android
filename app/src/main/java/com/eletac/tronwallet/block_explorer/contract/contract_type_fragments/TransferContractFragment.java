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

public class TransferContractFragment extends ContractFragment {

    private Contract.TransferContract mContract;

    private TextView mAmount_TextView;
    private TextView mFrom_TextView;
    private TextView mFromName_TextView;
    private TextView mTo_TextView;
    private TextView mToName_TextView;

    public TransferContractFragment() {
        // Required empty public constructor
    }

    public static TransferContractFragment newInstance() {
        TransferContractFragment fragment = new TransferContractFragment();
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
        return inflater.inflate(R.layout.fragment_transfer_contract, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAmount_TextView = view.findViewById(R.id.TransferContract_amount_textView);
        mFrom_TextView = view.findViewById(R.id.TransferContract_from_textView);
        mFromName_TextView = view.findViewById(R.id.TransferContract_from_name_textView);
        mTo_TextView = view.findViewById(R.id.TransferContract_to_textView);
        mToName_TextView = view.findViewById(R.id.TransferContract_to_name_textView);

        updateUI();
    }

    @Override
    public void setContract(Protocol.Transaction.Contract contract) {
        try {
            mContract = TransactionUtils.unpackContract(contract, Contract.TransferContract.class);
            updateUI();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    public void updateUI() {
        if(mContract != null && getView() != null) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            numberFormat.setMaximumFractionDigits(6);
            mAmount_TextView.setText(numberFormat.format(mContract.getAmount() / 1000000D));
            mFrom_TextView.setText(WalletManager.encode58Check(mContract.getOwnerAddress().toByteArray()));
            mTo_TextView.setText(WalletManager.encode58Check(mContract.getToAddress().toByteArray()));

            mFromName_TextView.setVisibility(View.GONE);
            mToName_TextView.setVisibility(View.GONE);
            if (!WalletManager.getSelectedWallet().isColdWallet()) {
                loadAccountNames();
            }
        }
    }

    private void loadAccountNames() {
        AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
                Protocol.Account fromAccount = WalletManager.queryAccount(mContract.getOwnerAddress().toByteArray(), false);
                Protocol.Account toAccount = WalletManager.queryAccount(mContract.getToAddress().toByteArray(), false);

                AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                    @Override
                    public void doInUIThread() {
                        String fromName = fromAccount.getAccountName().toStringUtf8();
                        String toName = toAccount.getAccountName().toStringUtf8();
                        mFromName_TextView.setText(fromName);
                        mToName_TextView.setText(toName);

                        if (!fromName.isEmpty()) {
                            mFromName_TextView.setVisibility(View.VISIBLE);
                            mFromName_TextView.setScaleX(0);
                            mFromName_TextView.setAlpha(0);
                            mFromName_TextView.animate().alpha(1).setDuration(250).start();
                        }
                        if (!toName.isEmpty()) {
                            mToName_TextView.setVisibility(View.VISIBLE);
                            mToName_TextView.setAlpha(0);
                            mToName_TextView.animate().alpha(1).setDuration(250).start();
                        }
                    }
                });
            }
        });
    }
}
