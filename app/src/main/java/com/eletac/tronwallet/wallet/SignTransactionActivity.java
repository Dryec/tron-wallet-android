package com.eletac.tronwallet.wallet;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.CaptureActivityPortrait;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Utils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import org.spongycastle.util.encoders.DecoderException;
import org.spongycastle.util.encoders.Hex;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SignTransactionActivity extends AppCompatActivity {

    public static final String TRANSACTION_DATA_EXTRA = "transaction_data_extra";

    private ImageView mUnsignedTransactionQR_ImageView;
    private Switch mSignedTransaction_Switch;
    private ImageButton mScanFromColdWallet_Button;

    private ConstraintLayout mColdWalletQR_ConstraintLayout;
    private ConstraintLayout mScanColdWalletQR_ConstraintLayout;

    private Protocol.Transaction mTransactionUnsigned;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() != null) {

                LovelyProgressDialog progressDialog = new LovelyProgressDialog(this)
                        .setIcon(R.drawable.ic_send_white_24px)
                        .setTitle(R.string.sending)
                        .setTopColorRes(R.color.colorPrimary);
                progressDialog.show();

                AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
                    @Override
                    public void doOnBackground() {
                        boolean sent = false;
                        try {
                            sent = WalletClient.broadcastTransaction(Hex.decode(result.getContents()));
                        } catch (Exception ignored) { }

                        final boolean sentTransaction = sent;

                        AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                            @Override
                            public void doInUIThread() {
                                progressDialog.dismiss();
                                Toast.makeText(SignTransactionActivity.this, sentTransaction ? R.string.transaction_successfully_sent : R.string.transaction_sending_failed, Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                });
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_transaction);

        mUnsignedTransactionQR_ImageView = findViewById(R.id.SignTransaction_qr_imageView);
        mSignedTransaction_Switch = findViewById(R.id.SignTransaction_signed_switch);
        mScanFromColdWallet_Button = findViewById(R.id.SignTransaction_qr_imageButton);

        mColdWalletQR_ConstraintLayout = findViewById(R.id.SignTransaction_cold_wallet_qr_constraintLayout);
        mScanColdWalletQR_ConstraintLayout = findViewById(R.id.SignTransaction_scan_cold_wallet_qr_constraintLayout);


        mScanColdWalletQR_ConstraintLayout.setAlpha(0);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            try {
                byte[] transactionData = extras.getByteArray(TRANSACTION_DATA_EXTRA);
                mTransactionUnsigned = Protocol.Transaction.parseFrom(transactionData);

                if(transactionData != null) {
                    Bitmap bitmap = Utils.strToQR(Hex.toHexString(transactionData), 800, 800);
                    mUnsignedTransactionQR_ImageView.setImageBitmap(bitmap);
                }
            } catch (DecoderException | IOException e) {
                e.printStackTrace();
            }
        }

        mSignedTransaction_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mScanColdWalletQR_ConstraintLayout
                        .animate()
                        .setDuration(150)
                        .alpha(isChecked ? 1 : 0)
                        .scaleX(isChecked ? 1 : 0)
                        .scaleY(isChecked ? 1 : 0)
                        .start();
            }
        });

        mScanFromColdWallet_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(SignTransactionActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                integrator.setPrompt(getString(R.string.scan_signed_transaction));
                integrator.setCameraId(0);
                integrator.setBeepEnabled(false);
                integrator.setOrientationLocked(true);
                integrator.setCaptureActivity(CaptureActivityPortrait.class);
                integrator.initiateScan();
            }
        });
    }
}
