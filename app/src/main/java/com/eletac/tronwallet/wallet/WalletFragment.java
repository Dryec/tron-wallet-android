package com.eletac.tronwallet.wallet;

import android.animation.Animator;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Token;
import com.eletac.tronwallet.TronWalletApplication;
import com.eletac.tronwallet.Utils;
import com.eletac.tronwallet.wallet.confirm_transaction.ConfirmTransactionActivity;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import org.tron.api.GrpcAPI;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.Wallet;
import org.tron.walletserver.WalletManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WalletFragment extends Fragment {

    private static final long SWITCH_BALANCE_ANIM_INTERVAL = 200;
    private static final long COPY_ADDRESS_ANIM_INTERVAL = 100;

    private RecyclerView mTokens_RecyclerView;
    private TextView mPriceUSD_TextView;
    private TextView mPriceUSDChange_TextView;
    private TextView mTRX_balance_TextView;
    private TextView mTRX_address_TextView;
    private TextView mTRX_TextView;
    private TextView mTRX_frozen_TextView;
    private TextView mBandwidth_TextView;
    private FloatingActionButton mSendReceive_Button;
    private FloatingActionButton mVote_Button;
    private TextView mName_TextView;
    private TextView mAccountName_TextView;
    private ImageView mEditName_ImageView;
    private TextView mPubAccountNameInfo_TextView;
    private CardView mTokenHeader_CardView;

    private Protocol.Account mLatestAccountData;
    private List<Token> mTokens;

    private TokenListAdapter mTokensAdapter;

    private Wallet mWallet;

    private float mTRX_price;
    private float mTRX_24hChange;
    private boolean showBalanceAsFiat = false;

    private AccountUpdatedBroadcastReceiver mAccountUpdatedBroadcastReceiver;
    private PricesUpdatedBroadcastReceiver mPricesUpdatedBroadcastReceiver;
    private ForeOrBackgroundedBroadcastReceiver mForeOrBackgroundedBroadcastReceiver;

    public WalletFragment() {
        // Required empty public constructor
    }

    public static WalletFragment newInstance() {
        WalletFragment fragment = new WalletFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccountUpdatedBroadcastReceiver = new AccountUpdatedBroadcastReceiver();
        mPricesUpdatedBroadcastReceiver = new PricesUpdatedBroadcastReceiver();
        mForeOrBackgroundedBroadcastReceiver = new ForeOrBackgroundedBroadcastReceiver();

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mForeOrBackgroundedBroadcastReceiver, new IntentFilter(TronWalletApplication.FOREGROUND_CHANGED));

        // Account Updater starts in OnResume();
        PriceUpdater.start();

        mTokens = new ArrayList<>();
        mTokensAdapter = new TokenListAdapter(getContext(), mTokens);

        mWallet = WalletManager.getSelectedWallet();

        if(mWallet != null) {
            // Make sure latest data is loaded if no internet connection
            mLatestAccountData = Utils.getAccount(getContext(), mWallet.getWalletName());
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        AccountUpdater.setInterval(TronWalletApplication.ACCOUNT_UPDATE_BACKGROUND_INTERVAL, false);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mAccountUpdatedBroadcastReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mPricesUpdatedBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        mWallet = WalletManager.getSelectedWallet();
        onAccountUpdated();

        AccountUpdater.setInterval(TronWalletApplication.ACCOUNT_UPDATE_FOREGROUND_INTERVAL, true);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mAccountUpdatedBroadcastReceiver, new IntentFilter(AccountUpdater.ACCOUNT_UPDATED));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mPricesUpdatedBroadcastReceiver, new IntentFilter(PriceUpdater.PRICES_UPDATED));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTokens_RecyclerView = view.findViewById(R.id.Wallet_tokens_recyclerView);
        mPriceUSD_TextView = view.findViewById(R.id.Wallet_trx_price_usd_textView);
        mPriceUSDChange_TextView = view.findViewById(R.id.Wallet_trx_price_usd_change_textView);
        mTRX_balance_TextView = view.findViewById(R.id.Wallet_trx_balance_textView);
        mTRX_address_TextView = view.findViewById(R.id.Wallet_trx_address_textView);
        mTRX_TextView = view.findViewById(R.id.Wallet_TRX_textView);
        mTRX_frozen_TextView = view.findViewById(R.id.Wallet_tp_textView);
        mBandwidth_TextView = view.findViewById(R.id.Wallet_bandwidth_textView);
        mSendReceive_Button = view.findViewById(R.id.Wallet_send_receive_floatingActionButton);
        mVote_Button = view.findViewById(R.id.Wallet_vote_floatingActionButton);
        mName_TextView = view.findViewById(R.id.Wallet_name_textView);
        mAccountName_TextView = view.findViewById(R.id.Wallet_account_name_textView);
        mEditName_ImageView = view.findViewById(R.id.Wallet_edit_name_imageView);
        mPubAccountNameInfo_TextView = view.findViewById(R.id.Wallet_pub_account_name_info_textView);
        mTokenHeader_CardView = view.findViewById(R.id.Wallet_token_header_cardView);

        mTRX_balance_TextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mTRX_balance_TextView.animate()
                        .scaleX(0)
                        .scaleY(0)
                        .alpha(0)
                        .setDuration(SWITCH_BALANCE_ANIM_INTERVAL)
                        .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        showBalanceAsFiat = !showBalanceAsFiat;
                        updateBalanceTextViews();
                        mTRX_balance_TextView.animate().scaleX(1).scaleY(1).alpha(1).setDuration(SWITCH_BALANCE_ANIM_INTERVAL).setListener(null);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                mTRX_TextView.animate()
                        .scaleX(0)
                        .scaleY(0)
                        .alpha(0)
                        .setDuration(SWITCH_BALANCE_ANIM_INTERVAL)
                        .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mTRX_TextView.animate().scaleX(1).scaleY(1).alpha(1).setDuration(SWITCH_BALANCE_ANIM_INTERVAL).setListener(null);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
            }
        });

        mTRX_address_TextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mTRX_address_TextView.animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .setDuration(COPY_ADDRESS_ANIM_INTERVAL)
                        .setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mTRX_address_TextView.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(COPY_ADDRESS_ANIM_INTERVAL)
                                        .setListener(null);

                                if(mWallet != null) {
                                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText("Address", mWallet.getAddress());
                                    clipboard.setPrimaryClip(clip);
                                    Toast.makeText(getActivity(), getString(R.string.copy_success), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getActivity(), R.string.could_not_copy, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
            }
        });

        mSendReceive_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), SendReceiveActivity.class);
                startActivity(intent);
            }
        });

        mVote_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), VoteActivity.class);
                startActivity(intent);
            }
        });

        View.OnClickListener editNameClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new LovelyStandardDialog(getContext(), LovelyStandardDialog.ButtonLayout.HORIZONTAL)
                        .setTopColorRes(R.color.colorPrimary)
                        .setButtonsColor(Color.WHITE)
                        .setIcon(R.drawable.ic_info_white_24px)
                        .setTitle(R.string.attention)
                        .setMessage(R.string.account_name_info)
                        .setPositiveButton(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                new LovelyTextInputDialog(getContext(), R.style.EditTextTintTheme)
                                        .setTopColorRes(R.color.colorAccent)
                                        .setTitle(R.string.setup_account_name)
                                        .setMessage(R.string.account_name_requirements)
                                        .setHint(R.string.public_name)
                                        .setIcon(R.drawable.baseline_edit_white_24)
                                        .setConfirmButton(R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                                            @Override
                                            public void onTextInputConfirmed(String text) {
                                                LovelyProgressDialog progressDialog = new LovelyProgressDialog(getContext())
                                                        .setIcon(R.drawable.ic_send_white_24px)
                                                        .setTitle(R.string.loading)
                                                        .setTopColorRes(R.color.colorPrimary);
                                                progressDialog.show();
                                                AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
                                                    @Override
                                                    public void doOnBackground() {
                                                        Protocol.Transaction transaction =  WalletManager.createUpdateAccountTransaction(WalletManager.decodeFromBase58Check(mWallet.getAddress()), text);
                                                        if(transaction.hasRawData()) {
                                                            Context context = getContext();
                                                            if(context != null) {
                                                                AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                                                                    @Override
                                                                    public void doInUIThread() {
                                                                        progressDialog.dismiss();
                                                                        ConfirmTransactionActivity.start(context, transaction);
                                                                    }
                                                                });
                                                            }
                                                        } else {
                                                            AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                                                                @Override
                                                                public void doInUIThread() {
                                                                    progressDialog.dismiss();
                                                                    new LovelyStandardDialog(getContext(), LovelyStandardDialog.ButtonLayout.HORIZONTAL)
                                                                            .setTopColorRes(R.color.colorPrimary)
                                                                            .setButtonsColor(Color.WHITE)
                                                                            .setIcon(R.drawable.ic_info_white_24px)
                                                                            .setTitle(R.string.invalid_name)
                                                                            .setMessage(R.string.name_not_valid_or_forgiven)
                                                                            .setPositiveButton(R.string.ok, null)
                                                                            .show();
                                                                }
                                                            });
                                                        }
                                                    }
                                                });
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, null)
                                        .show();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        };
        mPubAccountNameInfo_TextView.setOnClickListener(editNameClickListener);
        mEditName_ImageView.setOnClickListener(editNameClickListener);

        mTokens_RecyclerView.setHasFixedSize(true);
        mTokens_RecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mTokens_RecyclerView.setAdapter(mTokensAdapter);

        mTokenHeader_CardView.setAlpha(0);

        mTokens_RecyclerView.setAlpha(0);

        onAccountUpdated();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mForeOrBackgroundedBroadcastReceiver);
    }

    private void updateBalanceTextViews() {
        if(mLatestAccountData != null && mWallet != null) {

            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            GrpcAPI.AccountNetMessage accountNetMessage = Utils.getAccountNet(getContext(), mWallet.getWalletName());

            double balance = (mLatestAccountData.getBalance() / 1000000.0d);
            mTRX_balance_TextView
                    .setText(
                            showBalanceAsFiat ?
                                    NumberFormat.getCurrencyInstance(Locale.US).format(balance*mTRX_price)
                                    :
                                    numberFormat.format(balance));

            mTRX_TextView
                    .setText(
                            showBalanceAsFiat ?
                                    numberFormat.format(balance) + " " + getContext().getString(R.string.trx_symbol)
                                    :
                                    getContext().getString(R.string.trx_symbol));


            long frozenTotal = 0;
            for(Protocol.Account.Frozen frozen : mLatestAccountData.getFrozenList()) {
                frozenTotal += frozen.getFrozenBalance();
            }
            //mTRX_frozen_TextView.setText(String.format("%s %s", numberFormat.format(frozenTotal/1000000), getString(R.string.tron_power_short)));
            mTRX_frozen_TextView.setText(numberFormat.format(frozenTotal/1000000));

            //mTRX_frozen_TextView.setVisibility(frozenTotal == 0 ? View.GONE : View.VISIBLE);

            long bandwidth = accountNetMessage.getNetLimit() + accountNetMessage.getFreeNetLimit();
            long bandwidthUsed = accountNetMessage.getNetUsed()+accountNetMessage.getFreeNetUsed();
            long bandwidthLeft = bandwidth - bandwidthUsed;

            //mBandwidth_TextView.setVisibility(bandwidthLeft == 0 ? View.GONE : View.VISIBLE);
            //mBandwidth_TextView.setText(String.format("645,435,43%s %s", numberFormat.format(bandwidthLeft), getString(R.string.bandwidth)));
            mBandwidth_TextView.setText(numberFormat.format(bandwidthLeft));
        }

    }

    private void onPricesUpdated() {
        NumberFormat currencyNumberFormat = NumberFormat.getCurrencyInstance(Locale.US);
        currencyNumberFormat.setMinimumFractionDigits(3);
        NumberFormat percentNumberFormat = NumberFormat.getPercentInstance(Locale.US);
        percentNumberFormat.setMinimumFractionDigits(2);


        String price_str = currencyNumberFormat.format(mTRX_price);
        String percentChange_24h_str = percentNumberFormat.format(mTRX_24hChange/100.0f);
        boolean isPositivePercentChange = mTRX_24hChange > 0.0f;

        mPriceUSD_TextView.setText(price_str);
        mPriceUSDChange_TextView.setText(percentChange_24h_str);

        mPriceUSDChange_TextView.setTextColor(isPositivePercentChange ? Color.GREEN : Color.RED);
    }

    private void onAccountUpdated() {
        if(getContext() != null && mLatestAccountData != null && mWallet != null) {

            String accountName = mLatestAccountData.getAccountName().toStringUtf8();
            boolean needPublicName = accountName.isEmpty() && mLatestAccountData.getCreateTime() > 0;

            mPubAccountNameInfo_TextView.setVisibility(needPublicName ? View.VISIBLE : View.GONE);
            mEditName_ImageView.setVisibility(needPublicName ? View.VISIBLE : View.GONE);

            mName_TextView.setText(mWallet.getWalletName());
            mAccountName_TextView.setVisibility(!accountName.isEmpty() ? View.VISIBLE : View.GONE);
            mAccountName_TextView.setText(accountName);

            mTRX_address_TextView.setText(mWallet.getAddress());

            updateBalanceTextViews();

            AsyncJob.doInBackground(() -> {
                Map<String, Long> assets = mLatestAccountData.getAssetMap();

                boolean anyTokenBalance = false;
                List<Token> tokens = new ArrayList<>();
                for (Map.Entry<String, Long> asset : assets.entrySet()) {

                    if(asset.getValue() > 0) {
                        tokens.add(new Token(asset.getKey(), asset.getValue()));
                        anyTokenBalance = true;
                    }
                }

                boolean finalAnyTokenBalance = anyTokenBalance;
                AsyncJob.doOnMainThread(() -> {
                    mTokens.clear();
                    mTokens.addAll(tokens);
                    mTokensAdapter.notifyDataSetChanged();


                    int animDuration = 500;
                    int startDelay = 250;
                    if(!mTokens.isEmpty() && finalAnyTokenBalance) {
                        mTokenHeader_CardView.animate().alpha(1).setDuration(animDuration).setStartDelay(startDelay).start();
                        mTokens_RecyclerView.animate().alpha(1).setDuration(animDuration).setStartDelay(startDelay).start();
                    } else {
                        mTokenHeader_CardView.animate().alpha(0).setDuration(animDuration).setStartDelay(startDelay).start();
                        mTokens_RecyclerView.animate().alpha(0).setDuration(animDuration).setStartDelay(startDelay).start();
                    }
                });
            });
        }
    }

    private class AccountUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(mWallet != null) {
                mLatestAccountData = Utils.getAccount(context, mWallet.getWalletName());
                onAccountUpdated();
            }
        }
    }

    private class PricesUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mTRX_price = intent.getFloatExtra("tron_price", 0.0f);
            mTRX_24hChange = intent.getFloatExtra("tron_24h_change", 0.0f);
            onPricesUpdated();
        }
    }

    private class ForeOrBackgroundedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(AccountUpdater.isInitialized() && PriceUpdater.isInitialized()) {
                if (TronWalletApplication.isIsInForeground()) {
                    if (!AccountUpdater.isRunning()) {
                        AccountUpdater.start();
                    }
                    if (!PriceUpdater.isRunning()) {
                        PriceUpdater.start();
                    }
                } else {
                    AccountUpdater.stop();
                    PriceUpdater.stop();
                }
            }
        }
    }

}
