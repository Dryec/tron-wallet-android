package com.eletac.tronwallet.wallet;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.CaptureActivityPortrait;
import com.eletac.tronwallet.InputFilterMinMax;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Utils;
import com.eletac.tronwallet.wallet.confirm_transaction.ConfirmTransactionActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import org.tron.api.GrpcAPI;
import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class FreezeFragment extends Fragment {

    private TextView mFrozenNow_TextView;
    private TextView mFrozenNew_TextView;
    private TextView mVotesNow_TextView;
    private TextView mVotesNew_TextView;
    private TextView mBandwidthNow_TextView;
    private TextView mBandwidthNew_TextView;
    private TextView mExpires_TextView;
    private EditText mFreezeAmount_EditText;
    private SeekBar mFreezeAmount_SeekBar;
    private Button mFreeze_Button;
    private Button mUnfreeze_Button;

    private Protocol.Account mAccount;
    private boolean mIsPublicAddressOnly;
    private String mAddress;

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

        SharedPreferences sharedPreferences = getContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        mIsPublicAddressOnly = sharedPreferences.getBoolean(getString(R.string.is_public_address_only), false);

        mAccountUpdatedBroadcastReceiver = new AccountUpdatedBroadcastReceiver();

        mAddress = Utils.getPublicAddress(getContext());
        mAccount = Utils.getAccount(getContext());
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
        mExpires_TextView = view.findViewById(R.id.Freeze_expire_textView);
        mFreezeAmount_EditText = view.findViewById(R.id.Freeze_amount_editText);
        mFreezeAmount_SeekBar = view.findViewById(R.id.Freeze_amount_seekBar);
        mFreeze_Button = view.findViewById(R.id.Freeze_button);
        mUnfreeze_Button= view.findViewById(R.id.Freeze_un_button);

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

                mFreeze_Button.setEnabled(false);
                AsyncJob.doInBackground(() -> {
                    long amount = mFreezeAmount*1000000;
                    Protocol.Transaction transaction = null;
                    try {
                        transaction = WalletClient.createFreezeBalanceTransaction(WalletClient.decodeFromBase58Check(mAddress), amount, 3);
                    } catch (Exception ignored) { }

                    Protocol.Transaction finalTransaction = transaction;
                    AsyncJob.doOnMainThread(() -> {
                        mFreeze_Button.setEnabled(true);
                        if(finalTransaction != null)
                            ConfirmTransactionActivity.start(getContext(), finalTransaction);
                        else
                            new LovelyInfoDialog(getContext())
                                    .setTopColorRes(R.color.colorPrimary)
                                    .setIcon(R.drawable.ic_error_white_24px)
                                    .setTitle(R.string.failed)
                                    .setMessage(R.string.could_not_create_transaction)
                                    .show();
                    });
                });
            }
        });

        mUnfreeze_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mUnfreeze_Button.setEnabled(false);
                AsyncJob.doInBackground(() -> {
                    Protocol.Transaction transaction = null;
                    try {
                        transaction = WalletClient.createUnfreezeBalanceTransaction(WalletClient.decodeFromBase58Check(mAddress));
                    } catch (Exception ignored) { }

                    Protocol.Transaction finalTransaction = transaction;
                    AsyncJob.doOnMainThread(() -> {
                        mUnfreeze_Button.setEnabled(true);
                        if(finalTransaction != null)
                            ConfirmTransactionActivity.start(getContext(), finalTransaction);
                        else
                            new LovelyInfoDialog(getContext())
                                    .setTopColorRes(R.color.colorPrimary)
                                    .setIcon(R.drawable.ic_error_white_24px)
                                    .setTitle(R.string.failed)
                                    .setMessage(R.string.could_not_create_transaction)
                                    .show();
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
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mAccountUpdatedBroadcastReceiver, new IntentFilter(AccountUpdater.ACCOUNT_UPDATED));
    }

    private void updateUI() {
        mUpdatingUI = true;
        mFreezeAmount_EditText.setFilters(new InputFilter[]{ new InputFilterMinMax(0, mAccount.getBalance()/1000000)});
        mFreezeAmount_SeekBar.setMax((int)(mAccount.getBalance()/1000000L));

        GrpcAPI.AccountNetMessage accountNetMessage = Utils.getAccountNet(getContext());
        Log.i("TEEEEST", String.valueOf(accountNetMessage.getFreeNetLimit()-accountNetMessage.getFreeNetUsed()));

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

        long freezed = 0;
        long unfreezable = 0;
        long expire = 0;
        for (Protocol.Account.Frozen frozen : mAccount.getFrozenList()) {
            freezed += frozen.getFrozenBalance();
            if(frozen.getExpireTime() > expire)
                expire = frozen.getExpireTime();
            if(frozen.getExpireTime() <= System.currentTimeMillis()) {
                unfreezable += frozen.getFrozenBalance();
            }
        }

        long bandwidth = accountNetMessage.getNetLimit() + accountNetMessage.getFreeNetLimit();
        long bandwidthUsed = accountNetMessage.getNetUsed()+accountNetMessage.getFreeNetUsed();

        mFrozenNow_TextView.setText(numberFormat.format(freezed/1000000));
        mVotesNow_TextView.setText(numberFormat.format(freezed/1000000));
        mBandwidthNow_TextView.setText(
                        numberFormat.format(bandwidthUsed)
                        + " / " +
                        numberFormat.format(bandwidth)
                        + " ➡ " +
                        numberFormat.format(bandwidth-bandwidthUsed)
                        );
        mExpires_TextView.setText(expire == 0 ? "-" : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US).format(new Date(expire)));
        mUnfreeze_Button.setText(String.format(Locale.US,"%s (%d)", getString(R.string.unfreeze), unfreezable / 1000000));

        long newFreeze = (freezed/1000000L) + mFreezeAmount;
        mFrozenNew_TextView.setText(numberFormat.format(newFreeze));
        mVotesNew_TextView.setText(numberFormat.format(newFreeze));
        mBandwidthNew_TextView.setText(numberFormat.format(mAccount.getNetUsage() + mFreezeAmount)); // not visible anymore

        mUpdatingUI = false;
    }

    private class AccountUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mAccount = Utils.getAccount(context);
            updateUI();
        }
    }
}
