package com.eletac.tronwallet.wallet;

import android.animation.Animator;
import android.content.Intent;
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

import org.tron.walletserver.DuplicateNameException;
import org.tron.walletserver.InvalidNameException;
import org.tron.walletserver.InvalidPasswordException;
import org.tron.walletserver.Wallet;
import org.tron.walletserver.WalletManager;

public class CreateWalletActivity extends AppCompatActivity {

    private TextInputEditText mPassword_EditText;
    private TextInputEditText mName_EditText;
    private Switch mColdWallet_Switch;
    private CheckBox mRisks_CheckBox;
    private Button mGenerate_Button;
    private Button mImport_Button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_wallet);

        mPassword_EditText = findViewById(R.id.CreateWallet_password_editText);
        mName_EditText = findViewById(R.id.CreateWallet_name_editText);
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

                String name = mName_EditText.getText().toString();
                String password = mPassword_EditText.getText().toString();

                boolean coldWallet = mColdWallet_Switch.isChecked();

                if(!WalletManager.isNameValid(name)) {
                    new LovelyInfoDialog(CreateWalletActivity.this)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_info_white_24px)
                            .setTitle("Invalid Name")
                            .setMessage("Please enter a valid name")
                            .show();
                    return;
                }
                if(WalletManager.existWallet(name)) {
                    new LovelyInfoDialog(CreateWalletActivity.this)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_info_white_24px)
                            .setTitle("Invalid Name")
                            .setMessage("You already have an wallet with this name")
                            .show();
                    return;
                }
                if(!WalletManager.isPasswordValid(password)) {
                    new LovelyInfoDialog(CreateWalletActivity.this)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_info_white_24px)
                            .setTitle(R.string.create_wallet_inv_password_dialog_title)
                            .setMessage(R.string.create_wallet_inv_password_dialog_message)
                            .show();
                    return;
                }

                new LovelyStandardDialog(CreateWalletActivity.this, LovelyStandardDialog.ButtonLayout.HORIZONTAL)
                        .setTopColorRes(R.color.colorPrimary)
                        .setButtonsColor(Color.WHITE)
                        .setIcon(R.drawable.ic_info_white_24px)
                        .setTitle(R.string.create_wallet_dialog_title)
                        .setMessage(coldWallet ? R.string.create_wallet_cold_dialog_message : R.string.create_wallet_dialog_message)
                        .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {

                                    Wallet wallet = new Wallet(true);
                                    wallet.setWalletName(name);
                                    wallet.setColdWallet(coldWallet);

                                    WalletManager.store(wallet, password);
                                    WalletManager.selectWallet(name);

                                } catch (DuplicateNameException | InvalidPasswordException | InvalidNameException e) {
                                    // Should be already checked above
                                    e.printStackTrace();
                                    return;
                                }

                                Intent intent = new Intent(CreateWalletActivity.this, AccountActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                intent.putExtra("name", name);
                                intent.putExtra("password", password);
                                intent.putExtra("freshly_created", true);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
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
}
