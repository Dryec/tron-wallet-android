package com.eletac.tronwallet.block_explorer.contract;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.block_explorer.contract.contract_type_fragments.AssetIssueContractFragment;
import com.eletac.tronwallet.block_explorer.contract.contract_type_fragments.FreezeContractFragment;
import com.eletac.tronwallet.block_explorer.contract.contract_type_fragments.ParticipateAssetIssueContractFragment;
import com.eletac.tronwallet.block_explorer.contract.contract_type_fragments.TransferAssetContractFragment;
import com.eletac.tronwallet.block_explorer.contract.contract_type_fragments.TransferContractFragment;
import com.eletac.tronwallet.block_explorer.contract.contract_type_fragments.VoteWitnessContractFragment;
import com.google.protobuf.InvalidProtocolBufferException;

import org.tron.protos.Contract;
import org.tron.protos.Protocol;

public class ContractLoaderFragment extends ContractFragment {

    private TextView mContractNameTextView;
    private FrameLayout mContract_FrameLayout;

    public ContractLoaderFragment() {
    }

    public static ContractLoaderFragment newInstance() {
        ContractLoaderFragment fragment = new ContractLoaderFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contract_loader, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mContractNameTextView = view.findViewById(R.id.Contract_name_textView);
        mContract_FrameLayout = view.findViewById(R.id.Contract_frameLayout);
    }

    @Override
    public void setContract(@NonNull Protocol.Transaction.Contract contract) {
        mContractNameTextView.setText(getContractName(contract));
        loadContractFragment(contract);
        // TODO animate
    }

    private String getContractName(@NonNull Protocol.Transaction.Contract contract) {
        switch (contract.getType()) {
            case AccountCreateContract:
                return "Account Create Contract";
            case TransferContract:
                return "Transfer Contract";
            case TransferAssetContract:
                return "Transfer Asset Contract";
            case VoteAssetContract:
                return "Vote Asset Contract";
            case VoteWitnessContract:
                return "Vote Witness Contract";
            case WitnessCreateContract:
                return "Witness Create Contract";
            case AssetIssueContract:
                return "Asset Issue Contract";
            case DeployContract:
                return "Deploy Contract";
            case WitnessUpdateContract:
                return "Witness Update Contract";
            case ParticipateAssetIssueContract:
                return "Participate Asset Issue Contract";
            case AccountUpdateContract:
                return "Account Update Contract";
            case FreezeBalanceContract:
                return "Freeze Balance Contract";
            case UnfreezeBalanceContract:
                return "Unfreeze Balance Contract";
            case WithdrawBalanceContract:
                return "Withdraw Balance Contract";
            case UnfreezeAssetContract:
                return "Unfreeze Asset Contract";
            case UpdateAssetContract:
                return "Update Asset Contract";
            case CustomContract:
                return "Custom Contract";
            case UNRECOGNIZED:
                return "UNRECOGNIZED";
        }
        return "";
    }

    private void loadContractFragment(@NonNull Protocol.Transaction.Contract contract) {
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        ContractFragment fragment = null;

        switch (contract.getType()) {
            case AccountCreateContract:
                break;
            case TransferContract:
                fragment = TransferContractFragment.newInstance();
                break;
            case TransferAssetContract:
                fragment = TransferAssetContractFragment.newInstance();
                break;
            case VoteAssetContract:
                break;
            case VoteWitnessContract:
                fragment = VoteWitnessContractFragment.newInstance();
                break;
            case WitnessCreateContract:
                break;
            case AssetIssueContract:
                fragment = AssetIssueContractFragment.newInstance();
                break;
            case DeployContract:
                break;
            case WitnessUpdateContract:
                break;
            case ParticipateAssetIssueContract:
                fragment = ParticipateAssetIssueContractFragment.newInstance();
                break;
            case AccountUpdateContract:
                break;
            case FreezeBalanceContract:
                fragment = FreezeContractFragment.newInstance();
                break;
            case UnfreezeBalanceContract:
                break;
            case WithdrawBalanceContract:
                break;
            case UnfreezeAssetContract:
                break;
            case UpdateAssetContract:
                break;
            case CustomContract:
                break;
            case UNRECOGNIZED:
                break;
        }
        if(fragment != null) {
            fragment.setContract(contract);
            transaction.replace(R.id.Contract_frameLayout, fragment);
            transaction.disallowAddToBackStack();
            transaction.commit();
        }
    }
}
