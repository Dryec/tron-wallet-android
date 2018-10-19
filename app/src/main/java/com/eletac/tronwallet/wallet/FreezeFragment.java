package com.eletac.tronwallet.wallet;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.InputFilterMinMax;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Utils;
import com.eletac.tronwallet.wallet.confirm_transaction.ConfirmTransactionActivity;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;

import org.tron.api.GrpcAPI;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.Wallet;
import org.tron.walletserver.WalletManager;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

public class FreezeFragment extends Fragment {

    private TextView mFrozenNow_TextView;
    private TextView mFrozenNew_TextView;
    private TextView mVotesNow_TextView;
    private TextView mVotesNew_TextView;
    private TextView mBandwidthNow_TextView;
    private TextView mBandwidthNew_TextView;
    private TextView mEnergyNow_TextView;
    private TextView mExpires_TextView;
    private TextView mEnergyWarning_TextView;
    private EditText mFreezeAmount_EditText;
    private SeekBar mFreezeAmount_SeekBar;
    private Button mFreeze_Button;
    private Button mUnfreeze_Button;
    private RadioButton mGainBandwidth_RadioButton;
    private RadioButton mGainEnergy_RadioButton;
    private RadioButton mUnfreezeBandwidth_RadioButton;
    private RadioButton mUnfreezeEnergy_RadioButton;

    private Wallet mWallet;
    private Protocol.Account mAccount;

    private AccountUpdatedBroadcastReceiver mAccountUpdatedBroadcastReceiver;

    private long mFreezeAmount = 0;
    private boolean mUpdatingUI = false;

    public FreezeFragment() {
        // Required empty public constructor
    }

    public static FreezeFragment newInstance() {
        FreezeFragment fragment = new FreezeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccountUpdatedBroadcastReceiver = new AccountUpdatedBroadcastReceiver();

        mWallet = WalletManager.getSelectedWallet();
        if(mWallet != null) {
            mAccount = Utils.getAccount(getContext(), mWallet.getWalletName());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_freeze, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFrozenNow_TextView = view.findViewById(R.id.Freeze_frozen_now_textView);
        mFrozenNew_TextView = view.findViewById(R.id.Freeze_frozen_new_textView);
        mVotesNow_TextView = view.findViewById(R.id.Freeze_votes_now_textView);
        mVotesNew_TextView = view.findViewById(R.id.Freeze_votes_new_textView);
        mBandwidthNow_TextView = view.findViewById(R.id.Freeze_bandwidth_now_textView);
        mBandwidthNew_TextView = view.findViewById(R.id.Freeze_bandwidth_new_textView);
        mEnergyNow_TextView = view.findViewById(R.id.Freeze_energy_now_textView);
        mExpires_TextView = view.findViewById(R.id.Freeze_expire_textView);
        mEnergyWarning_TextView = view.findViewById(R.id.Freeze_energy_warning_textView);
        mFreezeAmount_EditText = view.findViewById(R.id.Freeze_amount_editText);
        mFreezeAmount_SeekBar = view.findViewById(R.id.Freeze_amount_seekBar);
        mFreeze_Button = view.findViewById(R.id.Freeze_button);
        mUnfreeze_Button= view.findViewById(R.id.Freeze_un_button);
        mGainBandwidth_RadioButton= view.findViewById(R.id.Freeze_bandwidth_radioButton);
        mGainEnergy_RadioButton= view.findViewById(R.id.Freeze_energy_radioButton);
        mUnfreezeBandwidth_RadioButton= view.findViewById(R.id.Freeze_un_bandwidth_radioButton);
        mUnfreezeEnergy_RadioButton= view.findViewById(R.id.Freeze_un_energy_radioButton);

        CompoundButton.OnCheckedChangeListener radio = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateUI();
            }
        };

        mGainBandwidth_RadioButton.setOnCheckedChangeListener(radio);
        mGainEnergy_RadioButton.setOnCheckedChangeListener(radio);
        mUnfreezeBandwidth_RadioButton.setOnCheckedChangeListener(radio);
        mUnfreezeEnergy_RadioButton.setOnCheckedChangeListener(radio);

        mFreezeAmount_EditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!mUpdatingUI) {
                    mUpdatingUI = true;
                    mFreezeAmount = mFreezeAmount_EditText.getText().length() > 0 ? Long.valueOf(mFreezeAmount_EditText.getText().toString()) : 0L;
                    if(Build.VERSION.SDK_INT >= 24) {
                        mFreezeAmount_SeekBar.setProgress((int) mFreezeAmount, true);
                    } else {
                        mFreezeAmount_SeekBar.setProgress((int) mFreezeAmount);
                    }
                    updateUI();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mFreezeAmount_SeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(!mUpdatingUI) {
                    mUpdatingUI = true;
                    mFreezeAmount = progress;
                    mFreezeAmount_EditText.setText(String.valueOf(mFreezeAmount));
                    updateUI();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mFreeze_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mWallet == null) {
                    new LovelyInfoDialog(getContext())
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_error_white_24px)
                            .setTitle(R.string.error)
                            .setMessage(R.string.no_wallet_selected)
                            .show();
                    return;
                }

                String textBackup = mFreeze_Button.getText().toString();

                mFreeze_Button.setEnabled(false);
                mFreeze_Button.setText(R.string.loading);

                AsyncJob.doInBackground(() -> {
                    long amount = mFreezeAmount*1000000;
                    boolean gainBandwidth = mGainBandwidth_RadioButton.isChecked();

                    Protocol.Transaction transaction = null;
                    try {
                        transaction = WalletManager.createFreezeBalanceTransaction(WalletManager.decodeFromBase58Check(mWallet.getAddress()), amount, 3, gainBandwidth ? Contract.ResourceCode.BANDWIDTH : Contract.ResourceCode.ENERGY);
                    } catch (Exception ignored) { }

                    Protocol.Transaction finalTransaction = transaction;
                    AsyncJob.doOnMainThread(() -> {
                        mFreeze_Button.setEnabled(true);
                        mFreeze_Button.setText(textBackup);
                        if(finalTransaction != null) {
                            if(getContext() != null)
                                ConfirmTransactionActivity.start(getContext(), finalTransaction);
                        }
                        else {
                            try {
                                new LovelyInfoDialog(getContext())
                                        .setTopColorRes(R.color.colorPrimary)
                                        .setIcon(R.drawable.ic_error_white_24px)
                                        .setTitle(R.string.failed)
                                        .setMessage(R.string.could_not_create_transaction)
                                        .show();
                            } catch (Exception ignored) {
                                // Cant show dialog, activity may gone while doing background work
                            }
                        }
                    });
                });
            }
        });

        mUnfreeze_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mWallet == null) {
                    new LovelyInfoDialog(getContext())
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_error_white_24px)
                            .setTitle(R.string.error)
                            .setMessage(R.string.no_wallet_selected)
                            .show();
                    return;
                }

                String textBackup = mUnfreeze_Button.getText().toString();
                boolean unfreezeForBandwidth = mUnfreezeBandwidth_RadioButton.isChecked();

                mUnfreeze_Button.setEnabled(false);
                mUnfreeze_Button.setText(R.string.loading);

                AsyncJob.doInBackground(() -> {
                    Protocol.Transaction transaction = null;
                    try {
                        transaction = WalletManager.createUnfreezeBalanceTransaction(WalletManager.decodeFromBase58Check(mWallet.getAddress()), unfreezeForBandwidth ? Contract.ResourceCode.BANDWIDTH : Contract.ResourceCode.ENERGY);
                    } catch (Exception ignored) { }

                    Protocol.Transaction finalTransaction = transaction;
                    AsyncJob.doOnMainThread(() -> {
                        mUnfreeze_Button.setEnabled(true);
                        mUnfreeze_Button.setText(textBackup);
                        if(finalTransaction != null)
                            ConfirmTransactionActivity.start(getContext(), finalTransaction);
                        else {
                            try {
                            new LovelyInfoDialog(getContext())
                                    .setTopColorRes(R.color.colorPrimary)
                                    .setIcon(R.drawable.ic_error_white_24px)
                                    .setTitle(R.string.failed)
                                    .setMessage(R.string.could_not_create_transaction)
                                    .show();
                            } catch (Exception ignored) {
                                // Cant show dialog, activity may gone while doing background work
                            }
                        }
                    });
                });
            }
        });

        updateUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mAccountUpdatedBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        mWallet = WalletManager.getSelectedWallet();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mAccountUpdatedBroadcastReceiver, new IntentFilter(AccountUpdater.ACCOUNT_UPDATED));
    }

    private void updateUI() {
        mUpdatingUI = true;
        if(mWallet != null && mAccount != null) {
            mFreezeAmount_EditText.setFilters(new InputFilter[]{ new InputFilterMinMax(0, mAccount.getBalance()/1000000)});
            mFreezeAmount_SeekBar.setMax((int)(mAccount.getBalance()/1000000L));

            GrpcAPI.AccountNetMessage accountNetMessage = Utils.getAccountNet(getContext(), mWallet.getWalletName());
            GrpcAPI.AccountResourceMessage accountResMessage = Utils.getAccountRes(getContext(), mWallet.getWalletName());

            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

            long freezed = 0;
            long unfreezable = 0;
            long expire = 0;
            for (Protocol.Account.Frozen frozen : mAccount.getFrozenList()) {
                freezed += frozen.getFrozenBalance();
                if (frozen.getExpireTime() > expire)
                    expire = frozen.getExpireTime();
                if (frozen.getExpireTime() <= System.currentTimeMillis()) {
                    unfreezable += frozen.getFrozenBalance();
                }
            }

            long bandwidth = accountNetMessage.getNetLimit() + accountNetMessage.getFreeNetLimit();
            long bandwidthUsed = accountNetMessage.getNetUsed() + accountNetMessage.getFreeNetUsed();

            long energy = accountResMessage.getEnergyLimit();
            long energyUsed= accountResMessage.getEnergyUsed();

            mFrozenNow_TextView.setText(numberFormat.format(freezed / 1000000));
            mVotesNow_TextView.setText(numberFormat.format(freezed / 1000000));
            mBandwidthNow_TextView.setText(
                    numberFormat.format(bandwidthUsed)
                            + " / " +
                            numberFormat.format(bandwidth)
                            + " ➡ " +
                            numberFormat.format(bandwidth - bandwidthUsed)
            );
            mEnergyNow_TextView.setText(
                    numberFormat.format(energyUsed)
                            + " / " +
                            numberFormat.format(energy)
                            + " ➡ " +
                            numberFormat.format(energy - energyUsed)
            );
            mExpires_TextView.setText(expire == 0 ? "-" : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US).format(new Date(expire)));
            if(mUnfreezeBandwidth_RadioButton.isChecked()) {
                mUnfreeze_Button.setText(String.format(Locale.US, "%s (%d)", getString(R.string.unfreeze), unfreezable / 1000000));
                mUnfreeze_Button.setEnabled(unfreezable > 0);
            }
            else {
                mUnfreeze_Button.setText((R.string.unfreeze));
                mUnfreeze_Button.setEnabled(true);
            }

            long newFreeze = (freezed / 1000000L) + mFreezeAmount;
            boolean gainBandwidth = mGainBandwidth_RadioButton.isChecked();
            mEnergyWarning_TextView.setVisibility(gainBandwidth ? View.GONE : View.VISIBLE);
            mFrozenNew_TextView.setText(numberFormat.format(gainBandwidth ? newFreeze : freezed / 1000000L));
            mVotesNew_TextView.setText(numberFormat.format(gainBandwidth ? newFreeze : freezed / 1000000L));
            mBandwidthNew_TextView.setText(numberFormat.format(mAccount.getNetUsage() + mFreezeAmount)); // not visible anymore
        }
        mUpdatingUI = false;
    }

    private class AccountUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(mWallet != null) {
                mAccount = Utils.getAccount(context, mWallet.getWalletName());
                updateUI();
            }
        }
    }
}
