package com.eletac.tronwallet.wallet;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.InputFilterMinMax;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Utils;
import com.eletac.tronwallet.wallet.confirm_transaction.ConfirmTransactionActivity;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;

import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IssueTokenActivity extends AppCompatActivity {

    private EditText mName_EditText;
    private EditText mAbbr_EditText;
    private EditText mSupply_EditText;
    private EditText mURL_EditText;
    private EditText mDesc_EditText;

    private EditText mExchangeTrxAmount_EditText;
    private EditText mExchangeTokenAmount_EditText;
    private TextView mTokenPrice_TextView;

    private EditText mFrozenAmount_EditText;
    private EditText mFrozenDays_EditText;

    private EditText mTotalBandwidth_EditText;
    private EditText mBandwidthPerAccount_EditText;

    private Button mSetStart_Button;
    private Button mSetEnd_Button;
    private TextView mStart_TextView;
    private TextView mEnd_TextView;

    private Button mCreate_Button;

    private int mStartYear, mStartMonth, mStartDay;
    private int mStartHour, mStartMinute, mStartSecond;

    private int mEndYear, mEndMonth, mEndDay;
    private int mEndHour, mEndMinute, mEndSecond;

    private boolean mIsStartSet = false, mIsEndSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue_token);

        mName_EditText = findViewById(R.id.IssueToken_name_editText);
        mAbbr_EditText = findViewById(R.id.IssueToken_abbr_editText);
        mSupply_EditText = findViewById(R.id.IssueToken_supply_editText);
        mURL_EditText = findViewById(R.id.IssueToken_url_editText);
        mDesc_EditText = findViewById(R.id.IssueToken_desc_editText);
        mExchangeTrxAmount_EditText = findViewById(R.id.IssueToken_trx_amount_editText);
        mExchangeTokenAmount_EditText = findViewById(R.id.IssueToken_token_amount_editText);
        mTokenPrice_TextView = findViewById(R.id.IssueToken_price_textView);
        mFrozenAmount_EditText = findViewById(R.id.IssueToken_frozen_amount_editText);
        mFrozenDays_EditText = findViewById(R.id.IssueToken_frozen_days_editText);
        mTotalBandwidth_EditText = findViewById(R.id.IssueToken_total_bandwidth_editText);
        mBandwidthPerAccount_EditText = findViewById(R.id.IssueToken_bandwidth_per_account_editText);
        mSetStart_Button = findViewById(R.id.IssueToken_set_start_button);
        mSetEnd_Button = findViewById(R.id.IssueToken_set_end_button);
        mStart_TextView = findViewById(R.id.IssueToken_start_time_textView);
        mEnd_TextView = findViewById(R.id.IssueToken_end_time_textView);
        mCreate_Button = findViewById(R.id.IssueToken_create_button);


        mSupply_EditText.setFilters(new InputFilter[]{ new InputFilterMinMax(0, Long.MAX_VALUE)});
        mExchangeTrxAmount_EditText.setFilters(new InputFilter[]{ new InputFilterMinMax(0, Integer.MAX_VALUE)});
        mExchangeTokenAmount_EditText.setFilters(new InputFilter[]{ new InputFilterMinMax(0, Integer.MAX_VALUE)});
        mFrozenAmount_EditText.setFilters(new InputFilter[]{ new InputFilterMinMax(0, Long.MAX_VALUE)});
        mFrozenDays_EditText.setFilters(new InputFilter[]{ new InputFilterMinMax(0, Long.MAX_VALUE)});
        mTotalBandwidth_EditText.setFilters(new InputFilter[]{ new InputFilterMinMax(0, Long.MAX_VALUE)});
        mBandwidthPerAccount_EditText.setFilters(new InputFilter[]{ new InputFilterMinMax(0, Long.MAX_VALUE)});

        mTokenPrice_TextView.setText("-");

        mSetStart_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar now = Calendar.getInstance();
                DatePickerDialog dpd = DatePickerDialog.newInstance(
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                                TimePickerDialog tpd = TimePickerDialog.newInstance(
                                        new TimePickerDialog.OnTimeSetListener() {
                                            @Override
                                            public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
                                                mStartYear = year;
                                                mStartMonth = monthOfYear;
                                                mStartDay = dayOfMonth;
                                                mStartHour = hourOfDay;
                                                mStartMinute = minute;
                                                mStartSecond = second;


                                                DateFormat dateTimeInstance = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US);
                                                mStart_TextView.setText(dateTimeInstance.format(new Date(startDateTimeToMillis())));

                                                mIsStartSet = true;
                                            }
                                        },
                                        true
                                );
                                tpd.show(getFragmentManager(), "timepickerdialog");
                            }
                        },
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)
                );
                dpd.show(getFragmentManager(), "datepickerdialog");
            }
        });

        mSetEnd_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar now = Calendar.getInstance();
                DatePickerDialog dpd = DatePickerDialog.newInstance(
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                                TimePickerDialog tpd = TimePickerDialog.newInstance(
                                        new TimePickerDialog.OnTimeSetListener() {
                                            @Override
                                            public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
                                                mEndYear = year;
                                                mEndMonth = monthOfYear;
                                                mEndDay = dayOfMonth;
                                                mEndHour = hourOfDay;
                                                mEndMinute = minute;
                                                mEndSecond = second;


                                                DateFormat dateTimeInstance = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US);
                                                mEnd_TextView.setText(dateTimeInstance.format(new Date(endDateTimeToMillis())));

                                                mIsEndSet = true;
                                            }
                                        },
                                        true
                                );
                                tpd.show(getFragmentManager(), "timepickerdialog");
                            }
                        },
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)
                );
                dpd.show(getFragmentManager(), "datepickerdialog");
            }
        });

        mCreate_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mName_EditText.getText().toString();
                String abbr = mAbbr_EditText.getText().toString();
                String url = mURL_EditText.getText().toString();
                String desc = mDesc_EditText.getText().toString();

                String s_supply = mSupply_EditText.getText().toString();
                String s_trx_amount = mExchangeTrxAmount_EditText.getText().toString();
                String s_token_amount = mExchangeTokenAmount_EditText.getText().toString();
                String s_frozen_amount = mFrozenAmount_EditText.getText().toString();
                String s_frozen_days = mFrozenDays_EditText.getText().toString();
                String s_total_bandwidth_amount = mTotalBandwidth_EditText.getText().toString();
                String s_bandwidth_per_account = mBandwidthPerAccount_EditText.getText().toString();

                long start = startDateTimeToMillis();
                long end = endDateTimeToMillis();

                long supply = 0;
                int trxAmount = 0;
                int tokenAmount = 0;
                long frozenAmount = 0;
                long frozenDays = 0;
                long totalBandwidth = 0;
                long bandwidthPerAccount = 0;
                List<Contract.AssetIssueContract.FrozenSupply> frozenSupplyList = new ArrayList<>();

                String failedMessage = "";

                if(name.isEmpty()) {
                    failedMessage = "Missing name";
                } else if(abbr.isEmpty()) {
                    failedMessage = "Missing abbreviation";
                } else if(url.isEmpty()) {
                    failedMessage = "Missing URL";
                } else if(desc.isEmpty()) {
                    failedMessage = "Missing description";
                } else if(s_supply.isEmpty()) {
                    failedMessage = "Missing supply";
                } else if(s_trx_amount.isEmpty()) {
                    failedMessage = "Missing TRX exchange amount";
                } else if(s_token_amount.isEmpty()) {
                    failedMessage = "Missing token exchange amount";
                } else if(!s_frozen_amount.isEmpty() && s_frozen_days.isEmpty()) {
                    failedMessage = "Missing frozen days";
                } else if(!s_frozen_days.isEmpty() && s_frozen_amount.isEmpty()) {
                    failedMessage = "Missing frozen amount";
                } else if(s_total_bandwidth_amount.isEmpty()) {
                    failedMessage = "Missing total bandwidth";
                } else if(s_bandwidth_per_account.isEmpty()) {
                    failedMessage = "Missing bandwidth per account";
                }

                if(failedMessage.isEmpty()) {
                    supply = Long.parseLong(s_supply);
                    trxAmount = Integer.parseInt(s_trx_amount);
                    tokenAmount = Integer.parseInt(s_token_amount);
                    totalBandwidth = Long.parseLong(s_total_bandwidth_amount);
                    bandwidthPerAccount = Long.parseLong(s_bandwidth_per_account);

                    if(supply <= 0) {
                        failedMessage = "Supply to small";
                    } else if(trxAmount <= 0 || tokenAmount <= 0) {
                        failedMessage = "Invalid exchange rate";
                    } else if(bandwidthPerAccount > totalBandwidth) {
                        failedMessage = "Bandwidth per account can't be greater than the total available bandwidth";
                    } else if(start <= System.currentTimeMillis()) {
                        failedMessage = "Start should be in the future";
                    } else if(end <= start) {
                        failedMessage = "End should be after start";
                    }

                    Protocol.Account account = Utils.getAccount(IssueTokenActivity.this, WalletClient.getSelectedWallet().getWalletName());

                    if(account.getBalance()/1000000D < 1024.D) {
                        failedMessage = "Not enough TRX\nNeeded: 1024 TRX";
                    }
                }

                if(!failedMessage.isEmpty()) {
                    new LovelyInfoDialog(IssueTokenActivity.this)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_info_white_24px)
                            .setTitle(R.string.failed)
                            .setMessage(failedMessage)
                            .show();
                    return;
                }



                if(!s_frozen_amount.isEmpty() && !s_frozen_days.isEmpty()) {
                    frozenAmount = Long.parseLong(s_frozen_amount);
                    frozenDays = Long.parseLong(s_frozen_days);

                    Contract.AssetIssueContract.FrozenSupply.Builder builder = Contract.AssetIssueContract.FrozenSupply.newBuilder();
                    builder.setFrozenAmount(frozenAmount);
                    builder.setFrozenDays(frozenDays);
                    frozenSupplyList.add(builder.build());
                }

                Contract.AssetIssueContract contract = WalletClient.createAssetIssueContract(
                        WalletClient.decodeFromBase58Check(WalletClient.getSelectedWallet().computeAddress()),
                        name,
                        abbr,
                        supply,
                        trxAmount*1000000,
                        tokenAmount,
                        start,
                        end,
                        0,
                        desc,
                        url,
                        0,//bandwidthPerAccount, // Limit per account
                        0,//totalBandwidth, // Total limit
                        frozenSupplyList
                        );



                new LovelyStandardDialog(IssueTokenActivity.this, LovelyStandardDialog.ButtonLayout.HORIZONTAL)
                        .setTopColorRes(R.color.colorPrimary)
                        .setButtonsColor(Color.WHITE)
                        .setIcon(R.drawable.ic_info_white_24px)
                        .setTitle(R.string.attention)
                        .setMessage("Creating a token costs 1024 TRX and will be consumed when the transaction is sent to the network and approved.\nContinue only if you are aware of this.")
                        .setPositiveButton(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String textBackup = mCreate_Button.getText().toString();
                                mCreate_Button.setEnabled(false);
                                mCreate_Button.setText(R.string.loading);
                                AsyncJob.doInBackground(() -> {
                                    Protocol.Transaction transaction = null;
                                    try {
                                        transaction = WalletClient.createAssetIssueTransaction(contract);
                                    } catch (Exception ignored) { }

                                    Protocol.Transaction finalTransaction = transaction;
                                    AsyncJob.doOnMainThread(() -> {
                                        mCreate_Button.setEnabled(true);
                                        mCreate_Button.setText(textBackup);
                                        if(finalTransaction != null) {
                                            ConfirmTransactionActivity.start(IssueTokenActivity.this, finalTransaction);
                                        }
                                        else {
                                            try {
                                                new LovelyInfoDialog(IssueTokenActivity.this)
                                                        .setTopColorRes(R.color.colorPrimary)
                                                        .setIcon(R.drawable.ic_error_white_24px)
                                                        .setTitle(R.string.failed)
                                                        .setMessage(R.string.could_not_create_transaction)
                                                        .show();
                                            } catch (Exception ignored) {
                                                // Cant show dialog, activity may gone while doing background work
                                            }
                                        }
                                    });
                                });
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });

        TextWatcher exchangeTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int trxAmount = 0;
                int tokenAmount = 0;

                try {
                    trxAmount = Integer.parseInt(mExchangeTrxAmount_EditText.getText().toString());
                    tokenAmount = Integer.parseInt(mExchangeTokenAmount_EditText.getText().toString());
                } catch (NumberFormatException ignored) { }

                if(tokenAmount != 0) {
                    double price = trxAmount / (double) tokenAmount;
                    NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
                    numberFormat.setMaximumFractionDigits(6);
                    mTokenPrice_TextView.setText(numberFormat.format(price));
                } else {
                    mTokenPrice_TextView.setText("-");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        mExchangeTrxAmount_EditText.addTextChangedListener(exchangeTextWatcher);
        mExchangeTokenAmount_EditText.addTextChangedListener(exchangeTextWatcher);
    }

    private long startDateTimeToMillis() {
        Calendar dateTime = Calendar.getInstance();
        dateTime.set(mStartYear, mStartMonth, mStartDay, mStartHour, mStartMinute, mStartSecond);
        return dateTime.getTimeInMillis();
    }

    private long endDateTimeToMillis() {
        Calendar dateTime = Calendar.getInstance();
        dateTime.set(mEndYear, mEndMonth, mEndDay, mEndHour, mEndMinute, mEndSecond);
        return dateTime.getTimeInMillis();
    }
}
