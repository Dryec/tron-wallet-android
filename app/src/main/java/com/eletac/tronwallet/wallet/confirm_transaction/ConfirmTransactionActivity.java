package com.eletac.tronwallet.wallet.confirm_transaction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Utils;
import com.eletac.tronwallet.wallet.AccountUpdater;
import com.eletac.tronwallet.wallet.SendReceiveActivity;
import com.eletac.tronwallet.wallet.SignTransactionActivity;
import com.eletac.tronwallet.wallet.confirm_transaction.contract_fragments.FreezeContractFragment;
import com.eletac.tronwallet.wallet.confirm_transaction.contract_fragments.ParticipateAssetIssueContractFragment;
import com.eletac.tronwallet.wallet.confirm_transaction.contract_fragments.TransferAssetContractFragment;
import com.eletac.tronwallet.wallet.confirm_transaction.contract_fragments.TransferContractFragment;
import com.eletac.tronwallet.wallet.confirm_transaction.contract_fragments.VoteWitnessContractFragment;
import com.google.protobuf.InvalidProtocolBufferException;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;

import org.spongycastle.util.encoders.DecoderException;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.text.NumberFormat;
import java.util.Locale;

public class ConfirmTransactionActivity extends AppCompatActivity {

    public static final String TRANSACTION_DATA_EXTRA = "transaction_data_extra";
    public static final String TRANSACTION_DATA2_EXTRA = "transaction_data2_extra";

    public static final int TRANSACTION_FINISHED = 4325;

    private TextView mContractNameTextView;
    private FrameLayout mContract_FrameLayout;
    private TextView mCurrentBandwidth_TextView;
    private TextView mEstBandwidthCost_TextView;
    private TextView mNewBandwidth_TextView;
    private ConstraintLayout mNotEnoughBandwidth_ConstraintLayout;
    private TextView mTRX_Cost_TextView;
    private Button mGetBandwidth_Button;
    private TextInputLayout mPassword_Layout;
    private TextInputEditText mPassword_EditText;
    private Button mConfirm_Button;

    private Protocol.Transaction mTransactionUnsigned;
    private Protocol.Transaction mTransactionSigned;
    private boolean mIsPublicAddressOnly;
    private byte[] mTransactionBytes;
    private byte[] mExtraBytes;

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

        mContractNameTextView = findViewById(R.id.ConfirmTrans_contract_name_textView);
        mContract_FrameLayout = findViewById(R.id.ConfirmTrans_contract_frameLayout);
        mCurrentBandwidth_TextView = findViewById(R.id.ConfirmTrans_current_bandwidth_textView);
        mEstBandwidthCost_TextView = findViewById(R.id.ConfirmTrans_est_bandwidth_cost_textView);
        mNewBandwidth_TextView = findViewById(R.id.ConfirmTrans_new_bandwidth_textView);
        mNotEnoughBandwidth_ConstraintLayout = findViewById(R.id.ConfirmTrans_not_enough_bandwidth_constrain);
        mTRX_Cost_TextView = findViewById(R.id.ConfirmTrans_trx_cost_textView);
        mPassword_EditText = findViewById(R.id.ConfirmTrans_password_editText);
        mPassword_Layout = findViewById(R.id.ConfirmTrans_password_textInputLayout);
        mGetBandwidth_Button = findViewById(R.id.ConfirmTrans_get_bandwidth_button);
        mConfirm_Button = findViewById(R.id.ConfirmTrans_confirm_button);

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

        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        mIsPublicAddressOnly = sharedPreferences.getBoolean(getString(R.string.is_public_address_only), false);

        setupBandwidth();
        updateConfirmButton();

        mContractNameTextView.setText(getContractName());
        loadContractFragment();

        mConfirm_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = mPassword_EditText.getText().toString();

                if(isTransactionSigned()) {
                    // TODO Loading...
                    mConfirm_Button.setEnabled(false);
                    AsyncJob.doInBackground(() -> {
                        final boolean sent = WalletClient.broadcastTransaction(mTransactionSigned);
                        AsyncJob.doOnMainThread(() -> {
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
                            if(!sent)
                             dialog.setMessage(getString(R.string.try_later));
                            dialog.show();

                            mConfirm_Button.setEnabled(true);
                            mTransactionSigned = null;
                            updateConfirmButton();
                            setupBandwidth();
                        });
                    });
                }
                else if(mIsPublicAddressOnly) {
                    Intent intent = new Intent(ConfirmTransactionActivity.this, SignTransactionActivity.class);
                    intent.putExtra(SignTransactionActivity.TRANSACTION_DATA_EXTRA, mTransactionBytes);
                    startActivityForResult(intent, SignTransactionActivity.TRANSACTION_SIGN_REQUEST_CODE); // TODO TEST AND DO NOT SEND FROM SIGN ACTIVITY But HERE
                }
                else if(WalletClient.checkPassWord(password)) {
                        WalletClient walletClient = WalletClient.GetWalletByStorage(password);
                        mTransactionSigned = TransactionUtils.setTimestamp(mTransactionUnsigned);
                        mTransactionSigned = TransactionUtils.sign(mTransactionSigned, walletClient.getEcKey());
                        updateConfirmButton();
                        setupBandwidth();

                        View view = ConfirmTransactionActivity.this.getCurrentFocus();
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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

    private void resetSign() {
        mPassword_EditText.setText("");
        mTransactionSigned = null;
        updateConfirmButton();
        setupBandwidth();
    }

    private void setupBandwidth() {
        if(isTransactionSigned()) {
            GrpcAPI.AccountNetMessage accountNetMessage = Utils.getAccountNet(this);
            long bandwidthNormal = accountNetMessage.getNetLimit()-accountNetMessage.getNetUsed();
            long bandwidthFree = accountNetMessage.getFreeNetLimit()-accountNetMessage.getFreeNetUsed();

            long bandwidth = accountNetMessage.getNetLimit() + accountNetMessage.getFreeNetLimit();
            long bandwidthUsed = accountNetMessage.getNetUsed() + accountNetMessage.getFreeNetUsed();

            long currentBandwidth = bandwidth - bandwidthUsed;
            long bandwidthCost = mTransactionSigned.getSerializedSize();
            long newBandwidth = currentBandwidth - bandwidthCost;

            boolean enoughBandwidth = bandwidthNormal >= bandwidthCost || bandwidthFree >= bandwidthCost;

            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

            mCurrentBandwidth_TextView.setText(numberFormat.format(currentBandwidth));
            mEstBandwidthCost_TextView.setText(numberFormat.format(bandwidthCost));
            mNewBandwidth_TextView.setText(enoughBandwidth ? numberFormat.format(newBandwidth) : "-");

            if(!enoughBandwidth) {
                mNotEnoughBandwidth_ConstraintLayout.setVisibility(View.VISIBLE);
                mTRX_Cost_TextView.setText(String.format("%s %s", numberFormat.format((double) bandwidthCost / 1000D), getString(R.string.trx_symbol)));
            } else {
                mNotEnoughBandwidth_ConstraintLayout.setVisibility(View.GONE);
            }
        } else {
            mCurrentBandwidth_TextView.setText("-");
            mEstBandwidthCost_TextView.setText("-");
            mNewBandwidth_TextView.setText("-");
            mNotEnoughBandwidth_ConstraintLayout.setVisibility(View.GONE);
        }
    }

    private void updateConfirmButton() {
        boolean needSign = !isTransactionSigned();

        mPassword_Layout.setVisibility(needSign && !mIsPublicAddressOnly ? View.VISIBLE : View.GONE);

        mConfirm_Button.setBackgroundTintList(ColorStateList.valueOf(
                (needSign ? getResources().getColor(R.color.colorAccent) : getResources().getColor(R.color.positive))
        ));
        mConfirm_Button.setText(needSign ? R.string.sign : R.string.send);
    }

    private boolean isTransactionSigned() {
        return mTransactionSigned != null && TransactionUtils.validTransaction(mTransactionSigned);
    }

    private String getContractName() {
        Protocol.Transaction.Contract contract = mTransactionUnsigned.getRawData().getContract(0);

        switch (contract.getType()) {
            case AccountCreateContract:
                return "Account Create Contract";
            case TransferContract:
                return "Transfer Contract";
            case TransferAssetContract:
                return "Transfer Asset Contract";
            case VoteAssetContract:
                return "Vote Asset Contract";
            case VoteWitnessContract:
                return "Vote Witness Contract";
            case WitnessCreateContract:
                return "Witness Create Contract";
            case AssetIssueContract:
                return "Asset Issue Contract";
            case DeployContract:
                return "Deploy Contract";
            case WitnessUpdateContract:
                return "Witness Update Contract";
            case ParticipateAssetIssueContract:
                return "Participate Asset Issue Contract";
            case AccountUpdateContract:
                return "Account Update Contract";
            case FreezeBalanceContract:
                return "Freeze Balance Contract";
            case UnfreezeBalanceContract:
                return "Unfreeze Balance Contract";
            case WithdrawBalanceContract:
                return "Withdraw Balance Contract";
            case UnfreezeAssetContract:
                return "Unfreeze Asset Contract";
            case UpdateAssetContract:
                return "Update Asset Contract";
            case CustomContract:
                return "Custom Contract";
            case UNRECOGNIZED:
                return "UNRECOGNIZED";
        }
        return "";
    }

    private void loadContractFragment() {
        Protocol.Transaction.Contract contract = mTransactionUnsigned.getRawData().getContract(0);

        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = null;

        switch (contract.getType()) {
            case AccountCreateContract:
                break;
            case TransferContract:
                fragment = TransferContractFragment.newInstance();
                break;
            case TransferAssetContract:
                fragment = TransferAssetContractFragment.newInstance();
                break;
            case VoteAssetContract:
                break;
            case VoteWitnessContract:
                fragment = VoteWitnessContractFragment.newInstance();
                break;
            case WitnessCreateContract:
                break;
            case AssetIssueContract:
                break;
            case DeployContract:
                break;
            case WitnessUpdateContract:
                break;
            case ParticipateAssetIssueContract:
                fragment = ParticipateAssetIssueContractFragment.newInstance();
                break;
            case AccountUpdateContract:
                break;
            case FreezeBalanceContract:
                fragment = FreezeContractFragment.newInstance();
                break;
            case UnfreezeBalanceContract:
                break;
            case WithdrawBalanceContract:
                break;
            case UnfreezeAssetContract:
                break;
            case UpdateAssetContract:
                break;
            case CustomContract:
                break;
            case UNRECOGNIZED:
                break;
        }
        if(fragment != null) {
            transaction.replace(R.id.ConfirmTrans_contract_frameLayout, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
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

    public static boolean start(@NonNull Activity context, @NonNull Protocol.Transaction transaction) {
        return start(context, transaction, null);
    }

    public static boolean start(@NonNull Activity activity, @NonNull Protocol.Transaction transaction, @Nullable byte[] data) {
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
