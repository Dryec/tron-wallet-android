package com.eletac.tronwallet.wallet;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Utils;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import org.spongycastle.util.encoders.Hex;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class VoteActivity extends AppCompatActivity {

    public static final String VOTES_UPDATED = "com.eletac.tronwallet.vote_activity.votes_updated";

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private TextView mRemaining_TextView;
    private Button mSubmit_Button;

    private HashMap<String, String> mVoteWitnesses;
    private VotesUpdatedBroadcastReceiver mVotesUpdatedBroadcastReceiver;

    private boolean mIsPublicAddressOnly;
    private Protocol.Account mAccount;
    private AccountUpdatedBroadcastReceiver mAccountUpdatedBroadcastReceiver;

    private boolean mLoadVotesOnNextAccountUpdate;

    private long mFrozen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote);

        Toolbar toolbar = findViewById(R.id.Vote_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mVoteWitnesses = new HashMap<>();
        mVotesUpdatedBroadcastReceiver = new VotesUpdatedBroadcastReceiver();
        mAccountUpdatedBroadcastReceiver = new AccountUpdatedBroadcastReceiver();

        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        mIsPublicAddressOnly = sharedPreferences.getBoolean(getString(R.string.is_public_address_only), false);
        mAccount = Utils.getAccount(this);

        mViewPager = findViewById(R.id.Vote_container);
        mRemaining_TextView = findViewById(R.id.Vote_remaining_textView);
        mSubmit_Button = findViewById(R.id.Vote_submit_button);

        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.Vote_tabs_tabLayout);
        tabLayout.setupWithViewPager(mViewPager);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                // Update event to load the new vote amounts on the fragments | Delayed because UI lag @TODO move iterations to background thread
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent updatedIntent = new Intent(VOTES_UPDATED);
                        LocalBroadcastManager.getInstance(VoteActivity.this).sendBroadcast(updatedIntent);
                    }
                }, 250);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mSubmit_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsPublicAddressOnly) {
                    new LovelyStandardDialog(VoteActivity.this)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_info_white_24px)
                            .setTitle(R.string.confirm_votes)
                            .setPositiveButton(R.string.sign_to_submit, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        Protocol.Transaction transaction = WalletClient.createVoteWitnessTransaction(WalletClient.decodeFromBase58Check(Utils.getPublicAddress(VoteActivity.this)), mVoteWitnesses);

                                        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
                                            new LovelyInfoDialog(VoteActivity.this)
                                                    .setTopColorRes(R.color.colorPrimary)
                                                    .setIcon(R.drawable.ic_error_white_24px)
                                                    .setTitle(R.string.submitting_failed)
                                                    .setMessage(R.string.could_not_create_transaction)
                                                    .show();
                                        } else {
                                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                                            transaction.writeTo(outputStream);
                                            outputStream.flush();

                                            Intent intent = new Intent(VoteActivity.this, SignTransactionActivity.class);
                                            intent.putExtra(SignTransactionActivity.TRANSACTION_DATA_EXTRA, outputStream.toByteArray());
                                            startActivity(intent);
                                        }
                                    } catch(Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } else {
                    new LovelyTextInputDialog(VoteActivity.this, R.style.EditTextTintTheme)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_info_white_24px)
                            .setTitle(R.string.confirm_votes)
                            .setHint(R.string.password)
                            .setInputType(InputType.TYPE_CLASS_TEXT |
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD)
                            .setConfirmButtonColor(Color.WHITE)
                            .setConfirmButton(R.string.submit, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                                @Override
                                public void onTextInputConfirmed(String text) {

                                    if (WalletClient.checkPassWord(text)) {

                                        LovelyProgressDialog progressDialog = new LovelyProgressDialog(VoteActivity.this)
                                                .setIcon(R.drawable.ic_send_white_24px)
                                                .setTitle(R.string.sending)
                                                .setTopColorRes(R.color.colorPrimary);
                                        progressDialog.show();

                                        AsyncJob.doInBackground(() -> {
                                            WalletClient walletClient = WalletClient.GetWalletByStorage(text);
                                            if (walletClient != null) {
                                                boolean sent = false;
                                                try {
                                                    sent = walletClient.voteWitness(mVoteWitnesses);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                boolean finalSent = sent;
                                                AsyncJob.doOnMainThread(() -> {
                                                    progressDialog.dismiss();

                                                    LovelyInfoDialog infoDialog = new LovelyInfoDialog(VoteActivity.this)
                                                            .setTopColorRes(R.color.colorPrimary)
                                                            .setIcon(R.drawable.ic_send_white_24px);
                                                    if (finalSent) {
                                                        infoDialog.setTitle(R.string.votes_submitted_successfully);
                                                        AccountUpdater.singleShot(3000);
                                                        mLoadVotesOnNextAccountUpdate = true;
                                                    } else {
                                                        infoDialog.setTitle(R.string.submitting_failed);
                                                        infoDialog.setMessage(R.string.try_later);
                                                    }
                                                    infoDialog.show();
                                                });
                                            }
                                        });
                                    } else {
                                        new LovelyInfoDialog(VoteActivity.this)
                                                .setTopColorRes(R.color.colorPrimary)
                                                .setIcon(R.drawable.ic_error_white_24px)
                                                .setTitle(R.string.submitting_failed)
                                                .setMessage(R.string.wrong_password)
                                                .show();
                                    }
                                }
                            })
                            .setNegativeButtonColor(Color.WHITE)
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                }
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(mVotesUpdatedBroadcastReceiver, new IntentFilter(WitnessItemListAdapter.VOTES_UPDATED));
        LocalBroadcastManager.getInstance(this).registerReceiver(mAccountUpdatedBroadcastReceiver, new IntentFilter(AccountUpdater.ACCOUNT_UPDATED));

        loadVotes();
        updateRemain();

        // No votes
        if(mFrozen == 0) {
            new LovelyStandardDialog(VoteActivity.this)
                    .setTopColorRes(R.color.colorPrimary)
                    .setIcon(R.drawable.ic_info_white_24px)
                    .setTitle("Missing Votes")
                    .setMessage("To get votes you have to freeze some TRX\n\n1 TRX = 1 Vote")
                    .setPositiveButton(R.string.freeze, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(VoteActivity.this, SendReceiveActivity.class);
                            intent.putExtra("page", 2);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mVotesUpdatedBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mAccountUpdatedBroadcastReceiver);
    }

    private void loadVotes() {
        mLoadVotesOnNextAccountUpdate = false;
        for(Protocol.Account.Vote vote : mAccount.getVotesList()) {
            mVoteWitnesses.put(WalletClient.encode58Check(vote.getVoteAddress().toByteArray()), String.valueOf(vote.getVoteCount()));
        }
        Intent updatedIntent = new Intent(VOTES_UPDATED);
        LocalBroadcastManager.getInstance(VoteActivity.this).sendBroadcast(updatedIntent);
    }

    private void updateRemain() {
        int totalVotes = 0;
        for(Map.Entry<String, String> entry : mVoteWitnesses.entrySet()) {
            totalVotes += Integer.parseInt(entry.getValue());
        }
        NumberFormat numberFormat = NumberFormat.getIntegerInstance();

        mFrozen = 0;
        for(Protocol.Account.Frozen frozen : mAccount.getFrozenList()) {
            mFrozen += frozen.getFrozenBalance();
        }

        long balance = mFrozen / 1000000;
        mRemaining_TextView.setText(String.format("%s / %s", numberFormat.format(balance - totalVotes), numberFormat.format(balance)));
    }

    public HashMap<String, String> getVoteWitnesses() {
        return mVoteWitnesses;
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = VoteWitnessesFragment.newInstance();
                    break;
                case 1:
                    fragment = OwnVotesFragment.newInstance();
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.tab_title_candidates);
                case 1:
                    return getString(R.string.tab_title_votes);
            }
            return null;
        }
    }

    private class VotesUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateRemain();
        }
    }

    private class AccountUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mAccount = Utils.getAccount(context);
            if(mLoadVotesOnNextAccountUpdate)
                loadVotes();
        }
    }
}
