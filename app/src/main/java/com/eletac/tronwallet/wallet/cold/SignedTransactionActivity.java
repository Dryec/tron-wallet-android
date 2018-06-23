package com.eletac.tronwallet.wallet.cold;

import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Utils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;

import org.spongycastle.util.encoders.DecoderException;
import org.spongycastle.util.encoders.Hex;
import org.tron.protos.Protocol;

public class SignedTransactionActivity extends AppCompatActivity {

    public static final String TRANSACTION_DATA_EXTRA = "transaction_data_extra";

    private ImageView mSignedTransactionQR_ImageView;
    private ConstraintLayout mConstraintLayout;

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
                mTransactionSigned = Protocol.Transaction.parseFrom(extras.getByteArray(TRANSACTION_DATA_EXTRA));
            } catch (InvalidProtocolBufferException | DecoderException ignored) {
            }
        }

        if (mTransactionSigned != null) {
            mSignedTransactionQR_ImageView.setImageBitmap(Utils.strToQR(Hex.toHexString(mTransactionSigned.toByteArray()), 650, 650));
            showQR();
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