package com.eletac.tronwallet.settings;

import android.animation.Animator;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.RecyclerItemClickListener;
import com.eletac.tronwallet.WrapContentLinearLayoutManager;
import com.eletac.tronwallet.wallet.CreateWalletActivity;
import com.eletac.tronwallet.wallet.WalletItemListAdapter;

import org.tron.walletserver.Wallet;
import org.tron.walletserver.WalletManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SelectWalletActivity extends AppCompatActivity {

    private FloatingActionButton mAdd_FloatingActionButton;
    private RecyclerView mWallets_RecyclerView;
    private LinearLayoutManager mLayoutManager;

    private List<Wallet> mWallets;
    private List<Wallet> mWalletsFiltered;

    private WalletItemListAdapter mWalletItemListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_wallet);

        mAdd_FloatingActionButton = findViewById(R.id.SelectWallet_add_floatingActionButton);
        mWallets_RecyclerView = findViewById(R.id.SelectWallet_wallets_recyclerView);

        mWallets = new ArrayList<>();
        mWalletsFiltered = new ArrayList<>();

        Set<String> walletNames = WalletManager.getWalletNames();
        for(String name : walletNames) {
            mWallets.add(WalletManager.getWallet(name));
        }

        mWalletItemListAdapter = new WalletItemListAdapter(this, mWallets, mWalletsFiltered);

        mLayoutManager = new WrapContentLinearLayoutManager(this);
        mWallets_RecyclerView.setHasFixedSize(true);
        mWallets_RecyclerView.setAdapter(mWalletItemListAdapter);
        mWallets_RecyclerView.setLayoutManager(mLayoutManager);
        mWallets_RecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, mWallets_RecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                WalletManager.selectWallet(mWallets.get(position).getWalletName());

                view.animate().scaleY(1.1f).scaleX(1.1f).setDuration(150).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.animate().scaleY(1.0f).scaleX(1.0f).setDuration(250).setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                finish();
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        }).start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).start();
            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        }));

        mAdd_FloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SelectWalletActivity.this, CreateWalletActivity.class);
                startActivity(intent);
            }
        });
    }
}
