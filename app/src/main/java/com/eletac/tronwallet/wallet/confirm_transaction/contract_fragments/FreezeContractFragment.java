package com.eletac.tronwallet.wallet.confirm_transaction.contract_fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.WrapContentLinearLayoutManager;
import com.eletac.tronwallet.wallet.confirm_transaction.ConfirmTransactionActivity;

import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Contract;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class FreezeContractFragment extends Fragment {

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

        Contract.FreezeBalanceContract contract = null;
        try {
            contract = TransactionUtils.unpackContract(
                    ((ConfirmTransactionActivity)getActivity()).getUnsignedTransaction().getRawData().getContract(0), Contract.FreezeBalanceContract.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(contract != null) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            mAmountTextView.setText(numberFormat.format(contract.getFrozenBalance()/1000000D));
            mDaysTextView.setText(numberFormat.format(contract.getFrozenDuration()));
        }
    }

}
