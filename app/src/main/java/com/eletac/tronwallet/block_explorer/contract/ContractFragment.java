package com.eletac.tronwallet.block_explorer.contract;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.wallet.confirm_transaction.ConfirmTransactionActivity;

import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.text.NumberFormat;
import java.util.Locale;

public abstract class ContractFragment extends Fragment {
    public abstract void setContract(Protocol.Transaction.Contract contract);
}
