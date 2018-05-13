package com.eletac.tronwallet.wallet.cold;

import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ImageView;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Utils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import org.spongycastle.util.encoders.DecoderException;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SignedTransactionActivity extends AppCompatActivity {

    public static final String TRANSACTION_DATA_EXTRA = "transaction_data_extra";

    private ImageView mSignedTransactionQR_ImageView;
    private ConstraintLayout mConstraintLayout;

    Protocol.Transaction mTransactionUnsigned;
    Protocol.Transaction mTransactionSigned;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signed_transaction);

        mSignedTransactionQR_ImageView = findViewById(R.id.SignedTransaction_qr_imageView);
        mConstraintLayout = findViewById(R.id.SignedTransaction_constraintLayout);

        mConstraintLayout.setVisibility(View.INVISIBLE);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            try {
                mTransactionUnsigned = Protocol.Transaction.parseFrom(Hex.decode(extras.getString(TRANSACTION_DATA_EXTRA, "")));
            } catch (InvalidProtocolBufferException | DecoderException ignored) {
            }
        }

        if (mTransactionUnsigned != null) {
            // Sign Transaction

            new LovelyTextInputDialog(this, R.style.EditTextTintTheme)
                    .setTopColorRes(R.color.colorPrimary)
                    .setIcon(R.drawable.ic_info_white_24px)
                    .setTitle(R.string.confirm_signing)
                    .setHint(R.string.password)
                    .setInputType(InputType.TYPE_CLASS_TEXT |
                            InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    .setConfirmButtonColor(Color.WHITE)
                    .setConfirmButton(R.string.sign, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                        @Override
                        public void onTextInputConfirmed(String text) {
                            if (WalletClient.checkPassWord(text)) {
                                WalletClient walletClient = WalletClient.GetWalletByStorage(text);
                                if (walletClient != null && walletClient.getEcKey() != null) {
                                    mTransactionSigned = TransactionUtils.setTimestamp(mTransactionUnsigned);
                                    mTransactionSigned = TransactionUtils.sign(mTransactionSigned, walletClient.getEcKey());

                                    try {
                                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                        mTransactionSigned.writeTo(outputStream);
                                        mSignedTransactionQR_ImageView.setImageBitmap(Utils.strToQR(Hex.toHexString(outputStream.toByteArray()), 650, 650));
                                        showQR();

                                    } catch (IOException e) {
                                        new LovelyStandardDialog(SignedTransactionActivity.this)
                                                .setTopColorRes(R.color.colorPrimary)
                                                .setIcon(R.drawable.ic_info_white_24px)
                                                .setTitle(R.string.error)
                                                .setMessage(R.string.failed_to_generate_qr_code)
                                                .setPositiveButton(R.string.ok, new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        finish();
                                                    }
                                                })
                                                .show();
                                    }
                                }
                            } else {
                                new LovelyStandardDialog(SignedTransactionActivity.this)
                                        .setTopColorRes(R.color.colorPrimary)
                                        .setIcon(R.drawable.ic_info_white_24px)
                                        .setTitle(R.string.wrong_password)
                                        .setMessage(R.string.signing_canceled)
                                        .setPositiveButton(R.string.ok, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                finish();
                                            }
                                        })
                                        .show();
                            }
                        }
                    })
                    .setNegativeButtonColor(Color.WHITE)
                    .setNegativeButton(R.string.cancel, null)
                    .show();

        } else {
            new LovelyStandardDialog(SignedTransactionActivity.this)
                    .setTopColorRes(R.color.colorPrimary)
                    .setIcon(R.drawable.ic_info_white_24px)
                    .setTitle(R.string.invalid_transaction)
                    .setMessage(R.string.no_valid_transaction_detected)
                    .setPositiveButton(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    })
                    .show();
        }
    }


    private void showQR() {
        mConstraintLayout.setVisibility(View.VISIBLE);
        mConstraintLayout.setAlpha(0);
        mConstraintLayout.setScaleX(0);
        mConstraintLayout.setScaleY(0);
        mConstraintLayout.animate().setDuration(350).alpha(1).scaleX(1).scaleY(1).setStartDelay(200).start();
    }
}