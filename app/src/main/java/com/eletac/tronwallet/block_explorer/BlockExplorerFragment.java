package com.eletac.tronwallet.block_explorer;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.SimpleTextDisplayFragment;
import com.eletac.tronwallet.block_explorer.BlockExplorerUpdater.UpdateTask;
import com.eletac.tronwallet.wallet.ReceiveFragment;
import com.eletac.tronwallet.wallet.SendFragment;

import java.util.HashMap;
import java.util.Map;


public class BlockExplorerFragment extends Fragment {


    private ViewPager mViewPager;
    private TabLayout mTabLayout;

    private BlockchainFragment mBlockchainFragment;
    private RepresentativesFragment mRepresentativesFragment;
    private NodesFragment mNodesFragment;
    private AccountsFragment mAccountsFragment;
    private TokensFragment mTokensFragment;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    public BlockExplorerFragment() {
    }

    public static BlockExplorerFragment newInstance() {
        BlockExplorerFragment fragment = new BlockExplorerFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        BlockExplorerUpdater.singleShot(UpdateTask.Blockchain, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_block_explorer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewPager = view.findViewById(R.id.BlockExplorer_viewPager);
        mTabLayout = view.findViewById(R.id.BlockExplorer_tabLayout);

        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(5);
        mTabLayout.setupWithViewPager(mViewPager);
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
                    mBlockchainFragment = BlockchainFragment.newInstance();
                    fragment = mBlockchainFragment;
                    break;
                case 1:
                    mRepresentativesFragment = RepresentativesFragment.newInstance();
                    fragment = mRepresentativesFragment;
                    break;
                case 2:
                    mNodesFragment = NodesFragment.newInstance();
                    fragment = mNodesFragment;
                    break;
                case 3:
                    mTokensFragment = TokensFragment.newInstance();
                    fragment = mTokensFragment;
                    break;
                case 4:
                    mAccountsFragment = AccountsFragment.newInstance();
                    fragment = mAccountsFragment;
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.tab_title_blockchain);
                case 1:
                    return getString(R.string.tab_title_representatives);
                case 2:
                    return getString(R.string.tab_title_nodes);
                case 3:
                    return getString(R.string.tab_title_tokens);
                case 4:
                    return getString(R.string.tab_title_accounts);
            }
            return null;
        }
    }
}
