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
import com.eletac.tronwallet.block_explorer.contract.contract_type_fragments.AccountUpdateContractFragment;
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
                return getString(R.string.account_create_contract);
            case TransferContract:
                return getString(R.string.transfer_contract);
            case TransferAssetContract:
                return getString(R.string.transfer_asset_contract);
            case VoteAssetContract:
                return getString(R.string.vote_asset_contract);
            case VoteWitnessContract:
                return getString(R.string.vote_witness_contract);
            case WitnessCreateContract:
                return getString(R.string.witness_create_contract);
            case AssetIssueContract:
                return getString(R.string.asset_issue_contract);
            case DeployContract:
                return getString(R.string.deploy_contract);
            case WitnessUpdateContract:
                return getString(R.string.witness_update_contract);
            case ParticipateAssetIssueContract:
                return getString(R.string.participate_asset_issue_contract);
            case AccountUpdateContract:
                return getString(R.string.account_update_contract);
            case FreezeBalanceContract:
                return getString(R.string.freeze_balance_contract);
            case UnfreezeBalanceContract:
                return getString(R.string.unfreeze_balance_contract);
            case WithdrawBalanceContract:
                return getString(R.string.withdraw_balance_contract);
            case UnfreezeAssetContract:
                return getString(R.string.unfreeze_asset_contract);
            case UpdateAssetContract:
                return getString(R.string.update_asset_contract);
            case CustomContract:
                return getString(R.string.custom_contract);
            case UNRECOGNIZED:
                return getString(R.string.unrecognized);
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
                fragment = AccountUpdateContractFragment.newInstance();
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
