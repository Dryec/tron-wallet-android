package com.eletac.tronwallet.settings;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.wallet.AboutActivity;
import com.eletac.tronwallet.wallet.AccountActivity;
import com.eletac.tronwallet.wallet.CreateWalletActivity;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import org.tron.walletserver.WalletClient;

public class SettingsFragment extends Fragment {

    private Button mReset_Button;
    private Button mAccount_Button;
    private Button mConnection_Button;
    private Button mAbout_Button;

    private boolean mIsPublicAddressOnly;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        mIsPublicAddressOnly = sharedPreferences.getBoolean(getString(R.string.is_public_address_only), false);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAccount_Button = view.findViewById(R.id.Settings_account_button);
        mConnection_Button = view.findViewById(R.id.Settings_connection_button);
        mReset_Button = view.findViewById(R.id.Settings_reset_button);
        mAbout_Button = view.findViewById(R.id.Settings_about_button);

        mReset_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsPublicAddressOnly) {
                    new LovelyStandardDialog(getActivity())
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_info_white_24px)
                            .setTitle(R.string.confirm_reset)
                            .setMessage(R.string.reset_address_only_info)
                            .setPositiveButton(R.string.reset, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    reset();
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } else {
                    new LovelyTextInputDialog(getActivity(), R.style.EditTextTintTheme)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_info_white_24px)
                            .setTitle(R.string.confirm_reset)
                            .setMessage(R.string.reset_info)
                            .setHint(R.string.password)
                            .setInputType(InputType.TYPE_CLASS_TEXT |
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD)
                            .setConfirmButtonColor(Color.WHITE)
                            .setConfirmButton(R.string.reset, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                                @Override
                                public void onTextInputConfirmed(String text) {
                                    if (WalletClient.checkPassWord(text)) {
                                        reset();
                                    } else {
                                        new LovelyInfoDialog(getContext())
                                                .setTopColorRes(R.color.colorPrimary)
                                                .setIcon(R.drawable.ic_error_white_24px)
                                                .setTitle(R.string.resetting_failed)
                                                .setMessage(R.string.wrong_password)
                                                .show();
                                    }
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                }
            }
        });
        mAccount_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsPublicAddressOnly) {
                    new LovelyInfoDialog(getContext())
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_error_white_24px)
                            .setTitle(R.string.no_access)
                            .setMessage(R.string.account_no_access_info)
                            .show();
                } else {
                    new LovelyTextInputDialog(getActivity(), R.style.EditTextTintTheme)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_info_white_24px)
                            .setTitle(R.string.password_needed)
                            .setHint(R.string.password)
                            .setInputType(InputType.TYPE_CLASS_TEXT |
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD)
                            .setConfirmButtonColor(Color.WHITE)
                            .setConfirmButton(R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                                @Override
                                public void onTextInputConfirmed(String text) {
                                    if (WalletClient.checkPassWord(text)) {
                                        Intent intent = new Intent(getContext(), AccountActivity.class);
                                        intent.putExtra("password", text);
                                        startActivity(intent);
                                    } else {
                                        new LovelyInfoDialog(getContext())
                                                .setTopColorRes(R.color.colorPrimary)
                                                .setIcon(R.drawable.ic_error_white_24px)
                                                .setTitle(R.string.access_denied)
                                                .setMessage(R.string.wrong_password)
                                                .show();
                                    }
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                }
            }
        });
        mConnection_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), SettingConnectionActivity.class);
                startActivity(intent);
            }
        });
        mAbout_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), AboutActivity.class);
                startActivity(intent);
            }
        });
    }

    private void reset() {
        SharedPreferences.Editor editor = getContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
        getActivity().recreate();
    }
}
