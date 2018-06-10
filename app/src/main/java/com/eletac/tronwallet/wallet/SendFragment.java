package com.eletac.tronwallet.wallet;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.CaptureActivityPortrait;
import com.eletac.tronwallet.InputFilterMinMax;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Utils;
import com.eletac.tronwallet.settings.SettingsFragment;
import com.eletac.tronwallet.wallet.confirm_transaction.ConfirmTransactionActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class SendFragment extends Fragment {

    private Spinner mAssets_Spinner;
    private Button mSend_Button;
    private ImageButton mQR_Button;
    private EditText mTo_EditText;
    private EditText mAmount_EditText;
    private TextView mAvailable_TextView;
    private TextView mAsset_TextView;

    private boolean mIsPublicAddressOnly;
    private String mAddress;

    private AccountUpdatedBroadcastReceiver mAccountUpdatedBroadcastReceiver;

    public SendFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() != null) {
                mTo_EditText.setText(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static SendFragment newInstance() {
        SendFragment fragment = new SendFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        mIsPublicAddressOnly = sharedPreferences.getBoolean(getString(R.string.is_public_address_only), false);

        mAccountUpdatedBroadcastReceiver = new AccountUpdatedBroadcastReceiver();

        mAddress = Utils.getPublicAddress(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_send, container, false);
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAssets_Spinner = view.findViewById(R.id.Send_assets_spinner);
        mTo_EditText = view.findViewById(R.id.Send_to_editText);
        mAmount_EditText = view.findViewById(R.id.Send_amount_editText);
        mAvailable_TextView = view.findViewById(R.id.Send_available_textView);
        mAsset_TextView = view.findViewById(R.id.Send_asset_textView);
        mSend_Button = view.findViewById(R.id.Send_send_button);
        mQR_Button = view.findViewById(R.id.Send_qr_button);


        updateAssetsSpinner();

        mAssets_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mAmount_EditText.setEnabled(true);

                updateAvailableAmount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mAmount_EditText.setEnabled(false);
            }
        });

        mAvailable_TextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAmount_EditText.setText(mAvailable_TextView.getText());
            }
        });

        mSend_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean isTrxCoin = mAssets_Spinner.getSelectedItemPosition() == 0;
                String asset = mAssets_Spinner.getSelectedItem().toString();
                String to = mTo_EditText.getText().toString();

                if(mAddress.equals(to)) {
                    new LovelyInfoDialog(getContext())
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_error_white_24px)
                            .setTitle(getString(R.string.invalid_address))
                            .setMessage(R.string.cant_send_to_own_address)
                            .show();
                    return;
                }

                byte[] toRaw = WalletClient.decodeFromBase58Check(to);
                if(toRaw == null) {
                    new LovelyInfoDialog(getContext())
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_error_white_24px)
                            .setTitle(getString(R.string.invalid_address))
                            .setMessage(getString(R.string.enter_valid_address))
                            .show();
                    return;
                }
                double amount;
                if(mAmount_EditText.getText().length() <= 0) {
                    new LovelyInfoDialog(getContext())
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_error_white_24px)
                            .setTitle(R.string.invalid_amount)
                            .setMessage(String.format(getString(R.string.enter_amount_between), 0, mAvailable_TextView.getText()))
                            .show();
                    return;
                }
                amount = Double.parseDouble(mAmount_EditText.getText().toString());

                String textBackup = mSend_Button.getText().toString();
                mSend_Button.setEnabled(false);
                mSend_Button.setText(R.string.loading);

                AsyncJob.doInBackground(() -> {
                    Protocol.Transaction transaction = null;
                    try {
                        if(isTrxCoin) {
                            Contract.TransferContract contract = WalletClient.createTransferContract(toRaw, WalletClient.decodeFromBase58Check(mAddress), (long) (amount * 1000000.0d));
                            transaction = WalletClient.createTransaction4Transfer(contract);
                        } else {
                            transaction = WalletClient.createTransferAssetTransaction(toRaw, asset.getBytes(), WalletClient.decodeFromBase58Check(mAddress), (long) amount);
                        }
                    } catch (Exception ignored) { }

                    Protocol.Transaction finalTransaction = transaction;
                    AsyncJob.doOnMainThread(() -> {
                        mSend_Button.setEnabled(true);
                        mSend_Button.setText(textBackup);
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

        mQR_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = IntentIntegrator.forSupportFragment(SendFragment.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                integrator.setPrompt(getString(R.string.scan_send_to_qr_code));
                integrator.setCameraId(0);
                integrator.setBeepEnabled(false);
                integrator.setBarcodeImageEnabled(false);
                integrator.setCaptureActivity(CaptureActivityPortrait.class);
                integrator.initiateScan();
            }
        });
    }

    private void updateAvailableAmount() {
        Protocol.Account account = Utils.getAccount(getContext());

        double assetAmount;

        int selectedPosition = mAssets_Spinner.getSelectedItemPosition();
        if(selectedPosition == 0) {
            assetAmount = account.getBalance() / 1000000.0d;
        } else {
            assetAmount = Utils.getAccountAssetAmount(account, mAssets_Spinner.getAdapter().getItem(selectedPosition).toString());
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setMaximumFractionDigits(6);

        mAmount_EditText.setFilters(new InputFilter[]{ new InputFilterMinMax(0, assetAmount)});
        mAvailable_TextView.setText(numberFormat.format(assetAmount));

        try {
            if (Long.valueOf(mAmount_EditText.getText().toString()) > assetAmount)
                mAmount_EditText.setText(String.valueOf(assetAmount));
        } catch (NumberFormatException ignored) {}
    }


    private void updateAssetsSpinner() {
        int position = mAssets_Spinner.getSelectedItemPosition();
        ArrayAdapter<String> adapter = getAssetNamesArrayAdapter();
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAssets_Spinner.setAdapter(adapter);
        mAssets_Spinner.setSelection(position);
    }

    private ArrayAdapter<String> getAssetNamesArrayAdapter() {
        ArrayAdapter<String> adapter = null;

        Context context = getContext();

        if(context != null) {
            Protocol.Account account = Utils.getAccount(context);

            Map<String, Long> assets = account.getAssetMap();

            adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
            adapter.add(context.getString(R.string.trx_symbol));
            adapter.addAll(new ArrayList<String>(assets.keySet()));
        }
        return adapter;
    }

    private class AccountUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateAssetsSpinner();
            updateAvailableAmount();
        }
    }
}
