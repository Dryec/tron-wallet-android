package com.eletac.tronwallet.wallet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.eletac.tronwallet.MainActivity;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.bip39.BIP39;
import com.eletac.tronwallet.bip39.ValidationException;

import org.tron.common.utils.ByteArray;
import org.tron.walletserver.Wallet;
import org.tron.walletserver.WalletManager;

import static com.eletac.tronwallet.Utils.strToQR;

public class AccountActivity extends AppCompatActivity {

    AccountActivity accountActivity;

    ImageView mQR_Address_ImageView;
    ImageView mQR_PrivateKey_ImageView;
    Button mContinue_Button;
    TextView mAddress_TextView;
    TextView mPrivKey_TextView;
    TextView mRecoveryPhrase_TextView;

    boolean mQR_Address_zoomed = false;
    boolean mQR_PrivateKey_zoomed = false;

    String mName;
    String mPassword;
    String mAddress;
    String mPrivKey;
    String mRecoveryPhrase;

    Wallet mWallet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        accountActivity = this;

        mQR_Address_ImageView = findViewById(R.id.Account_qr_address_imageView);
        mQR_PrivateKey_ImageView = findViewById(R.id.Account_qr_private_key_imageView);
        mContinue_Button = findViewById(R.id.Account_continue_button);
        mAddress_TextView = findViewById(R.id.Account_address_textView);
        mPrivKey_TextView = findViewById(R.id.Account_priv_key_textView);
        mRecoveryPhrase_TextView = findViewById(R.id.Account_recovery_phrase_textView);

        mName = getIntent().getStringExtra("name");
        mPassword = getIntent().getStringExtra("password");
        boolean freshlyCreated = getIntent().getBooleanExtra("freshly_created", false);

        mWallet = WalletManager.getWallet(mName, mPassword);

        if(mWallet == null || !mWallet.isOpen()) {
            finish();
            return;
        }

        mAddress = mWallet.getAddress();
        mPrivKey = ByteArray.toHexString(mWallet.getECKey().getPrivKeyBytes());
        try {
            mRecoveryPhrase = BIP39.encode(mWallet.getECKey().getPrivKeyBytes(), "pass");
        } catch (ValidationException e) {
            Toast.makeText(this, "Error: couldn't generate recovery phrase", Toast.LENGTH_LONG).show();
        }

        mAddress_TextView.setText(mAddress);
        mPrivKey_TextView.setText(mPrivKey);
        mRecoveryPhrase_TextView.setText(mRecoveryPhrase);

        mQR_Address_ImageView.setImageBitmap(strToQR(mAddress, 400,400));
        mQR_PrivateKey_ImageView.setImageBitmap(strToQR(mPrivKey, 400, 400));

        mContinue_Button.setVisibility(freshlyCreated ? View.VISIBLE : View.GONE);

        mContinue_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(accountActivity, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        mAddress_TextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Address", mAddress);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(accountActivity, getString(R.string.copy_success), Toast.LENGTH_SHORT).show();
            }
        });
        mPrivKey_TextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("PrivateKey", mPrivKey);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(accountActivity, getString(R.string.copy_success), Toast.LENGTH_SHORT).show();
            }
        });
        mRecoveryPhrase_TextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("RecoveryPhrase", mRecoveryPhrase);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(accountActivity, getString(R.string.copy_success), Toast.LENGTH_SHORT).show();
            }
        });

        mQR_Address_ImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mQR_Address_zoomed)
                    mQR_Address_ImageView.animate().scaleX(4).scaleY(4).setDuration(100).setListener(null);
                else
                    mQR_Address_ImageView.animate().scaleX(1).scaleY(1).setDuration(100).setListener(null);

                mQR_Address_zoomed = !mQR_Address_zoomed;
            }
        });
        mQR_PrivateKey_ImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mQR_PrivateKey_zoomed)
                    mQR_PrivateKey_ImageView.animate().scaleX(4).scaleY(4).setDuration(100).setListener(null);
                else
                    mQR_PrivateKey_ImageView.animate().scaleX(1).scaleY(1).setDuration(100).setListener(null);

                mQR_PrivateKey_zoomed = !mQR_PrivateKey_zoomed;
            }
        });
    }

}
