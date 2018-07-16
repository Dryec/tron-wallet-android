package com.eletac.tronwallet.block_explorer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.block_explorer.contract.ContractLoaderFragment;
import com.google.protobuf.InvalidProtocolBufferException;

import org.spongycastle.util.encoders.DecoderException;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletManager;

import java.text.DateFormat;
import java.util.Date;

import io.grpc.StatusRuntimeException;

public class TransactionViewerActivity extends AppCompatActivity {

    public static final String TRANSACTION_DATA = "transaction_data";

    private ContractLoaderFragment mContractLoaderFragment;
    private TextView mHash_TextView;
    private TextView mTimestamp_TextView;
    private TextView mConfirmed_TextView;
    private CardView mConfirmed_CardView;
    private ProgressBar mLoadingConfirmation_ProgressBar;
    private Button mOpenTronscan_Button;
    private ImageButton mCopyTronscan_Button;

    private Protocol.Transaction mTransaction;
    private Handler mUpdateConfirmationHandler;
    private UpdateConfirmationRunnable mUpdateConfirmationRunnable;
    private boolean mFirstConfirmationStateLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_viewer);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mContractLoaderFragment = (ContractLoaderFragment) getSupportFragmentManager().findFragmentById(R.id.TransactionViewer_contract_loader_fragment);
        mHash_TextView = findViewById(R.id.TransactionViewer_hash_textView);
        mTimestamp_TextView = findViewById(R.id.TransactionViewer_timestamp_textView);
        mConfirmed_TextView = findViewById(R.id.TransactionViewer_confirmed_textView);
        mConfirmed_CardView = findViewById(R.id.TransactionViewer_confirmation_CardView);
        mLoadingConfirmation_ProgressBar = findViewById(R.id.TransactionViewer_loading_confirmation_progressBar);
        mOpenTronscan_Button = findViewById(R.id.TransactionViewer_open_tronscan_button);
        mCopyTronscan_Button = findViewById(R.id.TransactionViewer_copy_tronscan_button);

        Bundle extras = getIntent().getExtras();
        try {
            mTransaction = Protocol.Transaction.parseFrom(extras.getByteArray(TRANSACTION_DATA));
        } catch (InvalidProtocolBufferException | DecoderException | NullPointerException ignored) {
            Toast.makeText(this, "Couldn't parse transaction", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mUpdateConfirmationHandler = new Handler();
        mUpdateConfirmationRunnable = new UpdateConfirmationRunnable();

        if(mTransaction.getRawData().getContractCount() > 0) {
            mContractLoaderFragment.setContract(mTransaction.getRawData().getContract(0));
        }
        mHash_TextView.setText(getTxID());
        mTimestamp_TextView.setText(
                new StringBuilder()
                        .append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(mTransaction.getRawData().getTimestamp())))
                        .append(" (")
                        .append(DateUtils.getRelativeTimeSpanString(mTransaction.getRawData().getTimestamp(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS))
                        .append(")").toString());
        mConfirmed_TextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadConfirmationStatus();
            }
        });
        mHash_TextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("TXID", getTxID());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(TransactionViewerActivity.this, getString(R.string.copy_success), Toast.LENGTH_SHORT).show();
            }
        });

        mOpenTronscan_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //https://tronscan.org/#/transaction/
                Uri tronscanTxURL = Uri.parse("https://tronscan.org/#/transaction/"+getTxID());
                Intent intent = new Intent(Intent.ACTION_VIEW, tronscanTxURL);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        mCopyTronscan_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Tronscan_TX", "https://tronscan.org/#/transaction/"+getTxID());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(TransactionViewerActivity.this, getString(R.string.copy_success), Toast.LENGTH_SHORT).show();
            }
        });

        loadConfirmationStatus();
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

    private void loadConfirmationStatus() {
        if(!mFirstConfirmationStateLoaded) {
            mConfirmed_CardView.setVisibility(View.GONE);
            mLoadingConfirmation_ProgressBar.setVisibility(View.VISIBLE);
        }

        AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
                boolean isConfirmed = false;

                try {
                    isConfirmed = WalletManager.isTransactionConfirmed(mTransaction);

                    boolean finalIsConfirmed = isConfirmed;
                    AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                        @Override
                        public void doInUIThread() {
                            if(!mFirstConfirmationStateLoaded) {
                                mConfirmed_CardView.setVisibility(View.VISIBLE);
                                mLoadingConfirmation_ProgressBar.setVisibility(View.GONE);
                            }
                            mFirstConfirmationStateLoaded = true;

                            if(finalIsConfirmed) {
                                mConfirmed_TextView.setText(R.string.confirmed);
                                mConfirmed_CardView.setCardBackgroundColor(ContextCompat.getColor(TransactionViewerActivity.this, R.color.positive));
                            } else {
                                mConfirmed_TextView.setText(R.string.unconfirmed);
                                mConfirmed_CardView.setCardBackgroundColor(ContextCompat.getColor(TransactionViewerActivity.this, R.color.colorAccent));
                                mUpdateConfirmationHandler.postDelayed(mUpdateConfirmationRunnable, 500);
                            }
                        }
                    });

                } catch (StatusRuntimeException e) {
                    e.printStackTrace();
                    mConfirmed_CardView.setVisibility(View.VISIBLE);
                    mLoadingConfirmation_ProgressBar.setVisibility(View.GONE);
                    mConfirmed_TextView.setText(R.string.unknown);
                    mConfirmed_CardView.setCardBackgroundColor(Color.GRAY);
                }
            }
        });
    }

    private String getTxID() {
        return Hex.toHexString(Hash.sha256(mTransaction.getRawData().toByteArray()));
    }

    private class UpdateConfirmationRunnable implements Runnable {

        @Override
        public void run() {
            loadConfirmationStatus();
        }
    }
}
