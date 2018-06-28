package com.eletac.tronwallet.wallet.confirm_transaction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.TronWalletApplication;
import com.eletac.tronwallet.Utils;
import com.eletac.tronwallet.block_explorer.contract.ContractLoaderFragment;
import com.eletac.tronwallet.database.Transaction;
import com.eletac.tronwallet.wallet.AccountUpdater;
import com.eletac.tronwallet.wallet.SendReceiveActivity;
import com.eletac.tronwallet.wallet.SignTransactionActivity;
import com.eletac.tronwallet.wallet.cold.SignedTransactionActivity;
import com.google.protobuf.InvalidProtocolBufferException;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;

import org.spongycastle.util.encoders.DecoderException;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Protocol;
import org.tron.walletserver.Wallet;
import org.tron.walletserver.WalletManager;

import java.text.NumberFormat;
import java.util.Locale;

public class ConfirmTransactionActivity extends AppCompatActivity {

    public static final String TRANSACTION_SENT = "com.eletac.tronwallet.block_explorer_updater.transaction_sent";

    public static final String TRANSACTION_DATA_EXTRA = "transaction_data_extra";
    public static final String TRANSACTION_DATA2_EXTRA = "transaction_data2_extra";

    public static final int TRANSACTION_FINISHED = 4325;

    private ContractLoaderFragment mContract_Fragment;
    private TextView mCurrentBandwidth_TextView;
    private TextView mEstBandwidthCost_TextView;
    private TextView mNewBandwidth_TextView;
    private ConstraintLayout mNotEnoughBandwidth_ConstraintLayout;
    private TextView mTRX_Cost_TextView;
    private Button mGetBandwidth_Button;
    private TextInputLayout mPassword_Layout;
    private TextInputEditText mPassword_EditText;
    private Button mConfirm_Button;
    private CardView mBandwidth_CardView;

    private Protocol.Transaction mTransactionUnsigned;
    private Protocol.Transaction mTransactionSigned;

    private byte[] mTransactionBytes;
    private byte[] mExtraBytes;
    private double mTRX_Cost;

    private Wallet mWallet;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == SignTransactionActivity.TRANSACTION_SIGN_REQUEST_CODE) {
            byte[] transactionData = data.getByteArrayExtra(SignTransactionActivity.TRANSACTION_SIGNED_EXTRA);

            try {
                mTransactionSigned = Protocol.Transaction.parseFrom(transactionData);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }

            updateConfirmButton();
            setupBandwidth();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_transaction);

        Toolbar toolbar = findViewById(R.id.ConfirmTrans_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mContract_Fragment = (ContractLoaderFragment) getSupportFragmentManager().findFragmentById(R.id.ConfirmTrans_contract_fragment);
        mCurrentBandwidth_TextView = findViewById(R.id.ConfirmTrans_current_bandwidth_textView);
        mEstBandwidthCost_TextView = findViewById(R.id.ConfirmTrans_est_bandwidth_cost_textView);
        mNewBandwidth_TextView = findViewById(R.id.ConfirmTrans_new_bandwidth_textView);
        mNotEnoughBandwidth_ConstraintLayout = findViewById(R.id.ConfirmTrans_not_enough_bandwidth_constrain);
        mTRX_Cost_TextView = findViewById(R.id.ConfirmTrans_trx_cost_textView);
        mPassword_EditText = findViewById(R.id.ConfirmTrans_password_editText);
        mPassword_Layout = findViewById(R.id.ConfirmTrans_password_textInputLayout);
        mGetBandwidth_Button = findViewById(R.id.ConfirmTrans_get_bandwidth_button);
        mConfirm_Button = findViewById(R.id.ConfirmTrans_confirm_button);
        mBandwidth_CardView = findViewById(R.id.ConfirmTrans_bandwidth_cardView);

        Bundle extras = getIntent().getExtras();
        try {
            mTransactionBytes = extras.getByteArray(TRANSACTION_DATA_EXTRA);
            mExtraBytes = extras.getByteArray(TRANSACTION_DATA2_EXTRA);
            mTransactionUnsigned = Protocol.Transaction.parseFrom(mTransactionBytes);
        } catch (InvalidProtocolBufferException | DecoderException | NullPointerException ignored) {
            Toast.makeText(this, "Couldn't parse transaction", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if(mTransactionUnsigned.getRawData().getContractCount() == 0) {
            Toast.makeText(this, "No valid contract, check your input.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mWallet = WalletManager.getSelectedWallet();
        if(mWallet == null) {
            Toast.makeText(this, "No wallet selected", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupBandwidth();
        updateConfirmButton();

        mContract_Fragment.setContract(mTransactionUnsigned.getRawData().getContract(0));

        mConfirm_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = mPassword_EditText.getText().toString();

                if(isTransactionSigned()) {
                    if(mTRX_Cost > 0) {

                        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
                        numberFormat.setMaximumFractionDigits(6);

                        new LovelyStandardDialog(ConfirmTransactionActivity.this, LovelyStandardDialog.ButtonLayout.HORIZONTAL)
                                .setTopColorRes(R.color.colorPrimary)
                                .setButtonsColor(Color.WHITE)
                                .setIcon(R.drawable.ic_info_white_24px)
                                .setTitle(R.string.attention)
                                .setMessage(String.format("%s %s %s", getString(R.string.transaction_will_cost), numberFormat.format(mTRX_Cost), getString(R.string.trx_symbol)))
                                .setPositiveButton(R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        broadcastTransaction();
                                    }
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    }
                    else {
                        broadcastTransaction();
                    }
                }
                else if(mWallet.isWatchOnly()) {
                    Intent intent = new Intent(ConfirmTransactionActivity.this, SignTransactionActivity.class);
                    intent.putExtra(SignTransactionActivity.TRANSACTION_DATA_EXTRA, mTransactionBytes);
                    startActivityForResult(intent, SignTransactionActivity.TRANSACTION_SIGN_REQUEST_CODE);
                }
                else if(WalletManager.checkPassword(mWallet, password)) {
                    if(mWallet.open(password)) {
                        mTransactionSigned = TransactionUtils.setTimestamp(mTransactionUnsigned);
                        mTransactionSigned = TransactionUtils.sign(mTransactionSigned, mWallet.getECKey());

                        // Hide Keyboard
                        View view = ConfirmTransactionActivity.this.getCurrentFocus();
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }

                        if (mWallet.isColdWallet()) {
                            Intent intent = new Intent(ConfirmTransactionActivity.this, SignedTransactionActivity.class);
                            intent.putExtra(SignedTransactionActivity.TRANSACTION_DATA_EXTRA, mTransactionSigned.toByteArray());
                            startActivity(intent);

                            resetSign();
                        } else {
                            updateConfirmButton();
                            setupBandwidth();
                        }
                    } else {
                        new LovelyInfoDialog(ConfirmTransactionActivity.this)
                                .setTopColorRes(R.color.colorPrimary)
                                .setIcon(R.drawable.ic_error_white_24px)
                                .setTitle(R.string.failed)
                                .setMessage("Couldn't open wallet")
                                .show();
                    }
                }
                else {
                    new LovelyInfoDialog(ConfirmTransactionActivity.this)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_error_white_24px)
                            .setTitle(R.string.failed)
                            .setMessage(R.string.wrong_password)
                            .show();
                }
            }
        });

        mGetBandwidth_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConfirmTransactionActivity.this, SendReceiveActivity.class);
                intent.putExtra("page", 2);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWallet = WalletManager.getSelectedWallet();
        //resetSign();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if ( id == android.R.id.home ) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void broadcastTransaction() {
        LovelyProgressDialog progressDialog = new LovelyProgressDialog(ConfirmTransactionActivity.this)
                .setIcon(R.drawable.ic_send_white_24px)
                .setTitle(R.string.sending)
                .setTopColorRes(R.color.colorPrimary);
        progressDialog.show();

        mConfirm_Button.setEnabled(false);
        AsyncJob.doInBackground(() -> {
            final boolean sent = WalletManager.broadcastTransaction(mTransactionSigned);

            if(sent) {
                Transaction dbTransaction = new Transaction();
                dbTransaction.senderAddress = mWallet.getAddress();
                dbTransaction.transaction = mTransactionSigned;
                TronWalletApplication.getDatabase().transactionDao().insert(dbTransaction);
            }
            AsyncJob.doOnMainThread(() -> {
                progressDialog.dismiss();

                AccountUpdater.singleShot(0);
                LovelyStandardDialog dialog = new LovelyStandardDialog(ConfirmTransactionActivity.this, LovelyStandardDialog.ButtonLayout.HORIZONTAL)
                        .setTopColorRes(R.color.colorPrimary)
                        .setButtonsColor(Color.WHITE)
                        .setIcon(R.drawable.ic_info_white_24px)
                        .setTitle(sent ? R.string.sent_successfully : R.string.sending_failed)
                        .setPositiveButton(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                            }
                        });
                if(!sent) {
                    dialog.setMessage(getString(R.string.try_later));
                } else {
                    Intent transactionSentIntent = new Intent(TRANSACTION_SENT);
                    LocalBroadcastManager.getInstance(ConfirmTransactionActivity.this).sendBroadcast(transactionSentIntent);
                }
                dialog.show();

                mConfirm_Button.setEnabled(true);
                mTransactionSigned = null;
                updateConfirmButton();
                setupBandwidth();
            });
        });
    }

    private void resetSign() {
        mPassword_EditText.setText("");
        mTransactionSigned = null;
        updateConfirmButton();
        setupBandwidth();
    }

    private void setupBandwidth() {
        if(isTransactionSigned()) {
            mBandwidth_CardView.setVisibility(mWallet.isColdWallet() ? View.GONE : View.VISIBLE);

            GrpcAPI.AccountNetMessage accountNetMessage = Utils.getAccountNet(this, mWallet.getWalletName());
            long bandwidthNormal = accountNetMessage.getNetLimit() - accountNetMessage.getNetUsed();
            long bandwidthFree = accountNetMessage.getFreeNetLimit() - accountNetMessage.getFreeNetUsed();

            long bandwidth = accountNetMessage.getNetLimit() + accountNetMessage.getFreeNetLimit();
            long bandwidthUsed = accountNetMessage.getNetUsed() + accountNetMessage.getFreeNetUsed();

            long currentBandwidth = bandwidth - bandwidthUsed;
            long bandwidthCost = mTransactionSigned.getSerializedSize();
            long newBandwidth = currentBandwidth - bandwidthCost;

            boolean enoughBandwidth = bandwidthNormal >= bandwidthCost || bandwidthFree >= bandwidthCost;

            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            numberFormat.setMaximumFractionDigits(6);

            mCurrentBandwidth_TextView.setText(numberFormat.format(currentBandwidth));
            mEstBandwidthCost_TextView.setText(numberFormat.format(bandwidthCost));
            mNewBandwidth_TextView.setText(enoughBandwidth ? numberFormat.format(newBandwidth) : "-");

            if (!enoughBandwidth) {
                mTRX_Cost = (double) bandwidthCost / 100000D;
                mNotEnoughBandwidth_ConstraintLayout.setVisibility(View.VISIBLE);
                mTRX_Cost_TextView.setText(String.format("%s %s", numberFormat.format(mTRX_Cost), getString(R.string.trx_symbol)));
            } else {
                mTRX_Cost = 0D;
                mNotEnoughBandwidth_ConstraintLayout.setVisibility(View.GONE);
            }
        } else {
            mBandwidth_CardView.setVisibility(View.GONE);
            mCurrentBandwidth_TextView.setText("-");
            mEstBandwidthCost_TextView.setText("-");
            mNewBandwidth_TextView.setText("-");
            mNotEnoughBandwidth_ConstraintLayout.setVisibility(View.GONE);
        }
    }

    private void updateConfirmButton() {
        boolean needSign = !isTransactionSigned();

        mPassword_Layout.setVisibility(needSign && !mWallet.isWatchOnly() ? View.VISIBLE : View.GONE);

        mConfirm_Button.setBackgroundTintList(ColorStateList.valueOf(
                (needSign ? getResources().getColor(R.color.colorAccent) : getResources().getColor(R.color.positive))
        ));
        mConfirm_Button.setText(needSign ? R.string.sign : R.string.send);
    }

    private boolean isTransactionSigned() {
        return mTransactionSigned != null && TransactionUtils.validTransaction(mTransactionSigned);
    }

    public Protocol.Transaction getUnsignedTransaction() {
        return mTransactionUnsigned;
    }

    public static boolean start(@NonNull Context context, @NonNull Protocol.Transaction transaction) {
        return start(context, transaction, null);
    }

    public static boolean start(@NonNull Context context, @NonNull Protocol.Transaction transaction, @Nullable byte[] data) {
        Intent intent = new Intent(context, ConfirmTransactionActivity.class);

        intent.putExtra(ConfirmTransactionActivity.TRANSACTION_DATA_EXTRA, transaction.toByteArray());// Utils.transactionToByteArray(transaction));
        intent.putExtra(TRANSACTION_DATA2_EXTRA, data);

        context.startActivity(intent);
        return true;
    }

    public static boolean startForResult(@NonNull Activity context, @NonNull Protocol.Transaction transaction) {
        return start(context, transaction, null);
    }

    public static boolean startForResult(@NonNull Activity activity, @NonNull Protocol.Transaction transaction, @Nullable byte[] data) {
        Intent intent = new Intent(activity, ConfirmTransactionActivity.class);

        intent.putExtra(ConfirmTransactionActivity.TRANSACTION_DATA_EXTRA, transaction.toByteArray());// Utils.transactionToByteArray(transaction));
        intent.putExtra(TRANSACTION_DATA2_EXTRA, data);

        activity.startActivityForResult(intent, TRANSACTION_FINISHED);
        return true;
    }

    public byte[] getExtraBytes() {
        return mExtraBytes;
    }
}
