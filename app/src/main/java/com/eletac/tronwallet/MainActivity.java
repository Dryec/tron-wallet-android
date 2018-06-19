package com.eletac.tronwallet;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.eletac.tronwallet.block_explorer.BlockExplorerFragment;
import com.eletac.tronwallet.settings.SettingsFragment;
import com.eletac.tronwallet.wallet.CreateWalletActivity;
import com.eletac.tronwallet.wallet.cold.WalletColdFragment;
import com.eletac.tronwallet.wallet.WalletFragment;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import org.tron.walletserver.Wallet;
import org.tron.walletserver.WalletClient;

public class MainActivity extends AppCompatActivity {

    public MainActivity mainActivity;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ExtendedViewPager mViewPager;

    private WalletFragment mWalletFragment;
    private WalletColdFragment mWalletColdFragment;
    private BlockExplorerFragment mBlockExplorerFragment;
    private SimpleTextDisplayFragment mSimpleTextDisplayFragment;

    private WalletClient mWalletClient;

    private boolean mIsColdWallet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivity = this;

        BottomBar bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setDefaultTab(R.id.tab_wallet);

        WalletClient.init();

        mWalletClient = WalletClient.GetWalletByStorageIgnorePrivKey(WalletClient.getSelectedWallet().getWalletName());
        Wallet wallet = WalletClient.getSelectedWallet();

        if(mWalletClient == null && !wallet.isWatchOnly())
        {
            Intent intent = new Intent(this, CreateWalletActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        mIsColdWallet = wallet.isColdWallet();

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = findViewById(R.id.Main_container);
        mViewPager.setAllowedSwipeDirection(SwipeDirection.none); // Disable swiping
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(5);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                bottomBar.selectTabAtPosition(position, true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(int tabId) {
                int position = 0;

                switch (tabId)
                {
                    case R.id.tab_block_explorer:
                        position = 0;
                        break;
                    case R.id.tab_wallet:
                        position = 1;
                        break;
                    case R.id.tab_settings:
                        position = 3;
                        break;
                }

                mViewPager.setCurrentItem(position);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
                    if(mIsColdWallet) {
                        mSimpleTextDisplayFragment = SimpleTextDisplayFragment.newInstance(getString(R.string.not_available_in_cold_wallet));
                        fragment = mSimpleTextDisplayFragment;
                    } else {
                        mBlockExplorerFragment = BlockExplorerFragment.newInstance();
                        fragment = mBlockExplorerFragment;
                    }
                    break;
                case 1:
                    if(mIsColdWallet) {
                        mWalletColdFragment = WalletColdFragment.newInstance();
                        fragment = mWalletColdFragment;
                    } else {
                        mWalletFragment = WalletFragment.newInstance();
                        fragment = mWalletFragment;
                    }
                    break;
                case 2:
                    fragment = SettingsFragment.newInstance();
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }


}
