package com.eletac.tronwallet.wallet;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import com.eletac.tronwallet.CaptureActivityPortrait;
import com.eletac.tronwallet.MainActivity;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.bip39.BIP39;
import com.eletac.tronwallet.bip39.ValidationException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;

import org.tron.common.utils.ByteArray;
import org.tron.walletserver.DuplicateNameException;
import org.tron.walletserver.WalletClient;

public class ImportWalletActivity extends AppCompatActivity {

    private Switch mAddressOnly_Switch;
    private Switch mRecoveryPhrase_Switch;

    private TextInputLayout mPassword_Layout;
    private TextInputLayout mPublicAddress_Layout;
    private TextInputLayout mPrivateKey_Layout;

    private EditText mName_EditText;
    private EditText mPassword_EditText;
    private EditText mPublicAddress_EditText;
    private EditText mPrivateKey_EditText;

    private Switch mColdWallet_Switch;
    private CheckBox mRisks_CheckBox;

    private Button mImport_Button;
    private Button mCreateWallet_Button;
    private ImageButton mPublicAddressQR_Button;
    private ImageButton mPrivateKeyQR_Button;

    private TextView mPasswordInfo_TextView;
    private TextView mPrivateKeyInfo_TextView;

    private static final int ADDRESS_REQUEST_CODE = 7541;
    private static final int PRIV_KEY_REQUEST_CODE = 9554;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, data);
        if(result != null) {
            if(result.getContents() != null) {
                switch (requestCode) {
                    case ADDRESS_REQUEST_CODE:
                        mPublicAddress_EditText.setText(result.getContents());
                        break;
                    case PRIV_KEY_REQUEST_CODE:
                        mPrivateKey_EditText.setText(result.getContents());
                        break;

                        default:
                            super.onActivityResult(requestCode, resultCode, data);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_wallet);

        mPublicAddress_Layout = findViewById(R.id.ImportWallet_pub_address_textInputLayout);
        mPassword_Layout = findViewById(R.id.ImportWallet_password_textInputLayout);
        mPrivateKey_Layout = findViewById(R.id.ImportWallet_priv_key_textInputLayout);

        mName_EditText = findViewById(R.id.ImportWallet_name_editText);
        mPublicAddress_EditText = findViewById(R.id.ImportWallet_pub_address_editText);
        mPassword_EditText = findViewById(R.id.ImportWallet_password_editText);
        mPrivateKey_EditText = findViewById(R.id.ImportWallet_priv_key_editText);

        mColdWallet_Switch = findViewById(R.id.ImportWallet_cold_wallet_switch);
        mRisks_CheckBox = findViewById(R.id.ImportWallet_risks_checkbox);

        mImport_Button = findViewById(R.id.ImportWallet_import_button);
        mCreateWallet_Button = findViewById(R.id.ImportWallet_create_wallet_button);
        mPublicAddressQR_Button = findViewById(R.id.ImportWallet_pub_address_qr_button);
        mPrivateKeyQR_Button = findViewById(R.id.ImportWallet_priv_key_qr_button);

        mPasswordInfo_TextView = findViewById(R.id.ImportWallet_password_info_textView);
        mPrivateKeyInfo_TextView = findViewById(R.id.ImportWallet_priv_key_info_textView);

        mAddressOnly_Switch = findViewById(R.id.ImportWallet_address_switch);
        mRecoveryPhrase_Switch = findViewById(R.id.ImportWallet_recovery_phrase_switch);


        mCreateWallet_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCreateWalletActivity();
            }
        });

        mImport_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAddressOnly_Switch.isChecked()) {
                    importPublicAddress();
                } else {
                    importPrivateKey();
                }
            }
        });

        mAddressOnly_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPublicAddress_Layout.setVisibility(isChecked ? View.VISIBLE : View.GONE);

                mPassword_Layout.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                mPasswordInfo_TextView.setVisibility(isChecked ? View.GONE : View.VISIBLE);

                mRecoveryPhrase_Switch.setVisibility(isChecked ? View.GONE : View.VISIBLE);

                mPrivateKey_Layout.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                mPrivateKeyInfo_TextView.setVisibility(isChecked ? View.GONE : View.VISIBLE);

                mColdWallet_Switch.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                mRisks_CheckBox.setVisibility(isChecked ? View.GONE : View.VISIBLE);

                mImport_Button.setText(isChecked ? R.string.import_text : R.string.import_wallet);

                mPrivateKeyQR_Button.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                mPublicAddressQR_Button.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        mRecoveryPhrase_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPrivateKey_Layout.setHint(isChecked ? getString(R.string.recovery_phrase) : getString(R.string.private_key));
                mPrivateKeyInfo_TextView.setText(isChecked ? R.string.recovery_phrase_info : R.string.private_key_info);
            }
        });

        mPublicAddressQR_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(ImportWalletActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                integrator.setPrompt(getString(R.string.scan_your_address_qr_code));
                integrator.setCameraId(0);
                integrator.setRequestCode(ADDRESS_REQUEST_CODE);
                integrator.setBeepEnabled(false);
                integrator.setBarcodeImageEnabled(false);
                integrator.setCaptureActivity(CaptureActivityPortrait.class);
                integrator.initiateScan();
            }
        });

        mPrivateKeyQR_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(ImportWalletActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                integrator.setPrompt(getString(R.string.scan_your_priv_key_qr_code));
                integrator.setCameraId(0);
                integrator.setRequestCode(PRIV_KEY_REQUEST_CODE);
                integrator.setBeepEnabled(false);
                integrator.setBarcodeImageEnabled(false);
                integrator.setCaptureActivity(CaptureActivityPortrait.class);
                integrator.initiateScan();
            }
        });

        mColdWallet_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    new LovelyInfoDialog(ImportWalletActivity.this)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_info_white_24px)
                            .setTitle(getString(R.string.cold_wallet))
                            .setMessage(
                                    R.string.cold_wallet_info_dialog)
                            .show();
                }
            }
        });
    }

    private void importPublicAddress() {
        String address = mPublicAddress_EditText.getText().toString();

        boolean addressValid = false;

        try {
            addressValid = WalletClient.addressValid(WalletClient.decodeFromBase58Check(address));
        } catch (IllegalArgumentException ignored) { }

        if(addressValid) {
            String name = mName_EditText.getText().toString();

            boolean validName = isValidName(name);

            if(!validName) {
                new LovelyInfoDialog(ImportWalletActivity.this)
                        .setTopColorRes(R.color.colorPrimary)
                        .setIcon(R.drawable.ic_info_white_24px)
                        .setTitle("Invalid Name")
                        .setMessage("Please enter a valid name")
                        .show();
                return;
            }

            new LovelyStandardDialog(this, LovelyStandardDialog.ButtonLayout.HORIZONTAL)
                    .setTopColorRes(R.color.colorPrimary)
                    .setButtonsColor(Color.WHITE)
                    .setIcon(R.drawable.ic_info_white_24px)
                    .setTitle(R.string.import_address)
                    .setMessage(R.string.import_address_message)
                    .setPositiveButton(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                WalletClient.storeWatchOnlyWallet(name, address);
                                WalletClient.selectWallet(name);
                                startMainActivity();
                            } catch (DuplicateNameException e) {
                                new LovelyInfoDialog(ImportWalletActivity.this)
                                        .setTopColorRes(R.color.colorPrimary)
                                        .setIcon(R.drawable.ic_info_white_24px)
                                        .setTitle("Invalid Name")
                                        .setMessage("You already have an wallet with this name")
                                        .show();
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();

        } else {
            new LovelyInfoDialog(this)
                    .setTopColorRes(R.color.colorPrimary)
                    .setIcon(R.drawable.ic_info_white_24px)
                    .setTitle(R.string.invalid_address)
                    .setMessage(R.string.enter_valid_address)
                    .show();
        }
    }

    private void importPrivateKey() {
        if(!mRisks_CheckBox.isChecked()) {
            mRisks_CheckBox.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(100)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mRisks_CheckBox.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(100)
                                    .setListener(null);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
            return;
        }

        LovelyStandardDialog dialog = new LovelyStandardDialog(this, LovelyStandardDialog.ButtonLayout.HORIZONTAL)
                .setTopColorRes(R.color.colorPrimary)
                .setButtonsColor(Color.WHITE)
                .setIcon(R.drawable.ic_info_white_24px);

        boolean coldWallet = mColdWallet_Switch.isChecked();
        boolean inputInvalid = false;

        String name = mName_EditText.getText().toString();
        String password = mPassword_EditText.getText().toString();
        String privKey = "";

        try {
            if(mRecoveryPhrase_Switch.isChecked()) {
                privKey = ByteArray.toHexString(BIP39.decode(mPrivateKey_EditText.getText().toString(), "pass"));
            } else {
                privKey = mPrivateKey_EditText.getText().toString();
            }
        } catch (ValidationException e) {
            e.printStackTrace();
        }

        if(!isValidName(name)) {
            dialog.setTitle("Invalid Name")
                    .setMessage("Please enter a valid name");
            inputInvalid = true;
        }
        if (!isValidPassword(password)) {
            dialog.setMessage(R.string.create_wallet_inv_password_dialog_message)
                    .setTitle(R.string.create_wallet_inv_password_dialog_title);
            inputInvalid = true;
        } else if (!WalletClient.priKeyValid(privKey)) {
            dialog.setMessage(mRecoveryPhrase_Switch.isChecked() ? R.string.inv_recovery_phrase_dialog_message : R.string.inv_private_key_dialog_message)
                    .setTitle(mRecoveryPhrase_Switch.isChecked() ? R.string.inv_recovery_phrase_dialog_title : R.string.inv_private_key_dialog_title);
            inputInvalid = true;
        }


        if(inputInvalid) {
            dialog.setPositiveButton(R.string.ok, null);
            dialog.show();
        }
        else {
            dialog.setMessage(coldWallet ? R.string.import_wallet_cold_dialog_message : R.string.import_wallet_dialog_message)
                    .setTitle(R.string.import_wallet_dialog_title);

            String finalPrivKey = privKey;
            dialog.setPositiveButton(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        WalletClient wallet = new WalletClient(finalPrivKey);
                        wallet.store(name, password, coldWallet);
                        WalletClient.selectWallet(name);
                        startMainActivity();
                    } catch (DuplicateNameException e) {
                        new LovelyInfoDialog(ImportWalletActivity.this)
                                .setTopColorRes(R.color.colorPrimary)
                                .setIcon(R.drawable.ic_info_white_24px)
                                .setTitle("Invalid Name")
                                .setMessage("You already have an wallet with this name")
                                .show();
                    }
                }
            });

            dialog.setNegativeButton(R.string.cancel, null);
            dialog.show();
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(ImportWalletActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startCreateWalletActivity() {
        Intent intent = new Intent(this, CreateWalletActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean isValidName(String name) {
        return !name.isEmpty(); // TODO
    }

    private boolean isValidPassword(String password) {
        return (WalletClient.passwordValid(password) && !password.equals("") && password.length() >= 6 && !password.contains("\\s"));
    }
}
