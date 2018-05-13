package com.eletac.tronwallet.wallet;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.eletac.tronwallet.R;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;

import org.tron.walletserver.WalletClient;

public class CreateWalletActivity extends AppCompatActivity {

    private TextInputEditText mPassword_EditText;
    private Switch mColdWallet_Switch;
    private CheckBox mRisks_CheckBox;
    private Button mGenerate_Button;
    private Button mImport_Button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_wallet);

        mPassword_EditText = findViewById(R.id.CreateWallet_password_editText);
        mColdWallet_Switch = findViewById(R.id.CreateWallet_cold_wallet_switch);
        mRisks_CheckBox = findViewById(R.id.CreateWallet_risks_checkbox);
        mGenerate_Button = findViewById(R.id.CreateWallet_generate_button);
        mImport_Button= findViewById(R.id.CreateWallet_import_button);

        mImport_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startImportWalletActivity();
            }
        });

        mGenerate_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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

                String password = mPassword_EditText.getText().toString();

                boolean coldWallet = mColdWallet_Switch.isChecked();
                boolean validPassword = isValidPassword(password);

                LovelyStandardDialog dialog = new LovelyStandardDialog(CreateWalletActivity.this, LovelyStandardDialog.ButtonLayout.HORIZONTAL)
                        .setTopColorRes(R.color.colorPrimary)
                        .setButtonsColor(Color.WHITE)
                        .setIcon(R.drawable.ic_info_white_24px)
                        .setTitle(validPassword ? R.string.create_wallet_dialog_title : R.string.create_wallet_inv_password_dialog_title)
                        .setMessage(
                                validPassword ?
                                        (coldWallet ? R.string.create_wallet_cold_dialog_message : R.string.create_wallet_dialog_message)
                                        : R.string.create_wallet_inv_password_dialog_message)
                        .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(validPassword) {
                                    WalletClient wallet = new WalletClient(true);
                                    wallet.store(password);

                                    SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();

                                    editor.putString(getString(R.string.public_address_raw), WalletClient.encode58Check(wallet.getAddress()));
                                    editor.putBoolean(getString(R.string.is_cold_wallet_key), coldWallet);
                                    editor.commit();

                                    Intent intent = new Intent(CreateWalletActivity.this, AccountActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    intent.putExtra("password", password);
                                    intent.putExtra("freshly_created", true);
                                    intent.putExtra("is_offline_wallet", coldWallet);
                                    startActivity(intent);
                                }
                            }
                        });

                if(validPassword) {
                    dialog.setNegativeButton(android.R.string.cancel, null);
                }

                dialog.show();
            }
        });

        mColdWallet_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    new LovelyInfoDialog(CreateWalletActivity.this)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_info_white_24px)
                            .setTitle(R.string.cold_wallet)
                            .setMessage(
                                    R.string.cold_wallet_info_dialog)
                            .show();
                }
            }
        });
    }

    private void startImportWalletActivity() {
        Intent intent = new Intent(this, ImportWalletActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean isValidPassword(String password) {
        return (WalletClient.passwordValid(password) && !password.equals("") && password.length() >= 6 && !password.contains("\\s"));
    }
}
