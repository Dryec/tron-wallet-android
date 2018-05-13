package com.eletac.tronwallet.wallet;

import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;

import com.eletac.tronwallet.R;

public class SendReceiveActivity extends AppCompatActivity {


    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_receive);

        Toolbar toolbar = findViewById(R.id.SendReceive_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = findViewById(R.id.SendReceive_container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.SendReceive_tabs_tabLayout);
        tabLayout.setupWithViewPager(mViewPager);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            mViewPager.setCurrentItem(extras.getInt("page", 0));
        }
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
                    fragment = SendFragment.newInstance();
                    break;
                case 1:
                    fragment = ReceiveFragment.newInstance();
                    break;
                case 2:
                    fragment = FreezeFragment.newInstance();
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.tab_title_send);
                case 1:
                    return getString(R.string.tab_title_receive);
                case 2:
                    return getString(R.string.tab_title_freeze);
            }
            return null;
        }
    }
}
