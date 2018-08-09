package com.eletac.tronwallet.wallet;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.InputFilter;
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
import android.widget.Spinner;
import android.widget.TextView;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.CaptureActivityPortrait;
import com.eletac.tronwallet.InputFilterMinMax;
import com.eletac.tronwallet.Price;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Utils;
import com.eletac.tronwallet.wallet.confirm_transaction.ConfirmTransactionActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;

import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.Wallet;
import org.tron.walletserver.WalletManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SendFragment extends Fragment {

    private Spinner mAssets_Spinner;
    private Button mSend_Button;
    private ImageButton mQR_Button;
    private EditText mTo_EditText;
    private EditText mAmount_EditText;
    private TextView mAvailableLabel_TextView;
    private TextView mAvailable_TextView;
    private TextView mAsset_TextView;
    private EditText mAmountFiat_EditText;
    private TextView mAvailableFiatLabel_TextView;
    private TextView mAvailableFiat_TextView;
    private TextView mAmountEqualFiat_TextView;

    private Wallet mWallet;
    private Price mPrice;

    private boolean mIsUpdatingAmount = false;

    private AccountUpdatedBroadcastReceiver mAccountUpdatedBroadcastReceiver;

    public SendFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            String content = result.getContents();
            if(content != null) {
                List<String> contentParts = Arrays.asList(content.split(":"));

                switch (contentParts.size()) {
                    case 1: {
                        mTo_EditText.setText(content);
                        break;
                    }
                    case 2: {
                        mTo_EditText.setText(contentParts.get(0));
                        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
                        try {
                            double amount = numberFormat.parse(contentParts.get(1)).doubleValue();
                            mAmount_EditText.setText(String.valueOf(amount));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case 3: {
                        String asset = contentParts.get(0);

                        mTo_EditText.setText(contentParts.get(1));
                        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
                        try {
                            double amount = numberFormat.parse(contentParts.get(2)).doubleValue();
                            mAmount_EditText.setText(String.valueOf(amount));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    default:
                        mTo_EditText.setText(content);
                }
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

        mWallet = WalletManager.getSelectedWallet();
        mAccountUpdatedBroadcastReceiver = new AccountUpdatedBroadcastReceiver();
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
        mWallet = WalletManager.getSelectedWallet();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mAccountUpdatedBroadcastReceiver, new IntentFilter(AccountUpdater.ACCOUNT_UPDATED));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAssets_Spinner = view.findViewById(R.id.Send_assets_spinner);
        mTo_EditText = view.findViewById(R.id.Send_to_editText);
        mAmount_EditText = view.findViewById(R.id.Send_amount_editText);
        mAvailable_TextView = view.findViewById(R.id.Send_available_textView);
        mAvailableLabel_TextView = view.findViewById(R.id.Send_available_label_textView);
        mAsset_TextView = view.findViewById(R.id.Send_asset_textView);
        mSend_Button = view.findViewById(R.id.Send_send_button);
        mQR_Button = view.findViewById(R.id.Send_qr_button);
        mAmountFiat_EditText = view.findViewById(R.id.Send_fiat_value_editText);
        mAvailableFiat_TextView = view.findViewById(R.id.Send_max_fiat_textView);
        mAvailableFiatLabel_TextView = view.findViewById(R.id.Send_max_fiat_label_textView);
        mAmountEqualFiat_TextView = view.findViewById(R.id.Send_amount_fiat_equal_textView);

        updateAssetsSpinner();

        mAssets_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mAmount_EditText.setEnabled(true);

                updateAvailableAmount();
                updateFiatPrice();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mAmount_EditText.setEnabled(false);
            }
        });

        View.OnClickListener setAmountToAvailableClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
                try {
                    mAmount_EditText.setText(String.valueOf(numberFormat.parse(mAvailable_TextView.getText().toString()).doubleValue()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        };
        mAvailable_TextView.setOnClickListener(setAmountToAvailableClickListener);
        mAvailableLabel_TextView.setOnClickListener(setAmountToAvailableClickListener);

        View.OnClickListener setFiatAmountToAvailableClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
                try {
                    mAmountFiat_EditText.setText(String.valueOf(numberFormat.parse(mAvailableFiat_TextView.getText().toString()).doubleValue()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        };
        mAvailableFiat_TextView.setOnClickListener(setFiatAmountToAvailableClickListener);
        mAvailableFiatLabel_TextView.setOnClickListener(setFiatAmountToAvailableClickListener);

        mSend_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean isTrxCoin = mAssets_Spinner.getSelectedItemPosition() == 0;
                String asset = mAssets_Spinner.getSelectedItem().toString();
                String to = mTo_EditText.getText().toString();

                if(mWallet == null) {
                    new LovelyInfoDialog(getContext())
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_error_white_24px)
                            .setTitle(R.string.error)
                            .setMessage(R.string.no_wallet_selected)
                            .show();
                    return;
                }

                if(mWallet.getAddress().equals(to)) {
                    new LovelyInfoDialog(getContext())
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_error_white_24px)
                            .setTitle(getString(R.string.invalid_address))
                            .setMessage(R.string.cant_send_to_own_address)
                            .show();
                    return;
                }

                byte[] toRaw;
                try {
                    toRaw = WalletManager.decodeFromBase58Check(to);
                } catch (IllegalArgumentException ignored) {
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

                byte[] finalToRaw = toRaw;
                AsyncJob.doInBackground(() -> {
                    Protocol.Transaction transaction = null;
                    try {
                        if(isTrxCoin) {
                            Contract.TransferContract contract = WalletManager.createTransferContract(finalToRaw, WalletManager.decodeFromBase58Check(mWallet.getAddress()), (long) (amount * 1000000.0d));
                            transaction = WalletManager.createTransaction4Transfer(contract);
                        } else {
                            transaction = WalletManager.createTransferAssetTransaction(finalToRaw, asset.getBytes(), WalletManager.decodeFromBase58Check(mWallet.getAddress()), (long) amount);
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


        mAmount_EditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFiatPrice();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mAmountFiat_EditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAssetAmount();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void updateAvailableAmount() {
        if(mWallet != null) {
            Protocol.Account account = Utils.getAccount(getContext(), mWallet.getWalletName());

            double assetAmount;

            int selectedPosition = mAssets_Spinner.getSelectedItemPosition();
            if (selectedPosition == 0) {
                assetAmount = account.getBalance() / 1000000.0d;
            } else {
                assetAmount = Utils.getAccountAssetAmount(account, mAssets_Spinner.getAdapter().getItem(selectedPosition).toString());
            }

            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            numberFormat.setMaximumFractionDigits(6);

            mAmount_EditText.setFilters(new InputFilter[]{new InputFilterMinMax(0, assetAmount)});
            mAvailable_TextView.setText(numberFormat.format(assetAmount));

            try {
                if (Double.valueOf(mAmount_EditText.getText().toString()) > assetAmount) {
                    mAmount_EditText.setText(String.valueOf(assetAmount));
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            mPrice = selectedPosition == 0 ? PriceUpdater.getTRX_price() : new Price(); // TODO load other asset prices (need to wait for first asset on exchanges)
            numberFormat.setMaximumFractionDigits(3);
            numberFormat.setRoundingMode(RoundingMode.DOWN);
            mAvailableFiat_TextView.setText(numberFormat.format(mPrice.getPrice()*assetAmount));
            mAmountFiat_EditText.setFilters(new InputFilter[]{new InputFilterMinMax(0, Utils.round(mPrice.getPrice()*assetAmount, 3, RoundingMode.DOWN))});
        }
    }

    private void updateAssetAmount() {
        if(mIsUpdatingAmount) {
            return;
        }
        mIsUpdatingAmount = true;
        String fiatAmountText = mAmountFiat_EditText.getText().toString();

        if(!fiatAmountText.equals("")) {
            double fiat_amount = Double.parseDouble(fiatAmountText);
            try {
                mAmount_EditText.setText(String.valueOf(Utils.round(fiat_amount / mPrice.getPrice(), 3, RoundingMode.DOWN)));
            } catch (NumberFormatException ignored) {
            }
        } else {
            mAmount_EditText.setText("");
        }
        mIsUpdatingAmount = false;
    }

    private void updateFiatPrice() {
        if(mIsUpdatingAmount) {
            return;
        }
        mIsUpdatingAmount = true;
        String assetAmountText = mAmount_EditText.getText().toString();

        if(!assetAmountText.equals("")) {
            double asset_amount = Double.parseDouble(assetAmountText);
            try {
                mAmountFiat_EditText.setText(String.valueOf(Utils.round(asset_amount * mPrice.getPrice(), 3, RoundingMode.DOWN)));
            } catch (NumberFormatException ignored) {
            }
        } else {
            mAmountFiat_EditText.setText("");
        }
        mIsUpdatingAmount = false;
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

        if(context != null && mWallet != null) {
            Protocol.Account account = Utils.getAccount(context, mWallet.getWalletName());

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
            //updateAssetsSpinner();
            updateAvailableAmount();
        }
    }
}
