package com.eletac.tronwallet.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.eletac.tronwallet.R;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;

import org.tron.walletserver.WalletClient;

public class SettingConnectionActivity extends AppCompatActivity {

    private EditText mIP_EditText;
    private EditText mPort_EditText;
    private Button mReset_Button;
    private Button mSave_Button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_connection);

        Toolbar toolbar = findViewById(R.id.SettingConnection_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mIP_EditText = findViewById(R.id.SettingConnection_node_ip_editText);
        mPort_EditText= findViewById(R.id.SettingConnection_node_port_editText);
        mReset_Button= findViewById(R.id.SettingConnection_reset_button);
        mSave_Button= findViewById(R.id.SettingConnection_save_button);

        loadNode();

        mSave_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = mIP_EditText.getText().toString();
                String portStr = mPort_EditText.getText().toString();

                if(!portStr.isEmpty() && isIP(ip)) {
                    int port = Integer.parseInt(portStr);

                    SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    editor.putString(getString(R.string.ip_key), ip);
                    editor.putInt(getString(R.string.port_key), port);

                    editor.commit();

                    loadNode();

                    new LovelyInfoDialog(SettingConnectionActivity.this)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_info_white_24px)
                            .setTitle("Saved new node connection")
                            .show();

                    WalletClient.init();
                } else {
                    new LovelyInfoDialog(SettingConnectionActivity.this)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.ic_error_white_24px)
                            .setTitle("Invalid IP or Port")
                            .show();
                }
            }
        });

        mReset_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                editor.remove(getString(R.string.ip_key));
                editor.remove(getString(R.string.port_key));

                editor.commit();

                loadNode();

                new LovelyInfoDialog(SettingConnectionActivity.this)
                        .setTopColorRes(R.color.colorPrimary)
                        .setIcon(R.drawable.ic_info_white_24px)
                        .setTitle("Node connection reset to default")
                        .show();

                WalletClient.init();
            }
        });
    }

    private void loadNode() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String ip = sharedPreferences.getString(getString(R.string.ip_key), getString(R.string.fullnode_ip));
        int port = sharedPreferences.getInt(getString(R.string.port_key), Integer.parseInt(getString(R.string.fullnode_port)));

        mIP_EditText.setText(ip);
        mPort_EditText.setText(String.valueOf(port));
    }

    private boolean isIP(String input) {
        if (input!= null && !input.isEmpty()) {
            String regex = "^((25[0-5])|(2[0-4]\\d)|(1\\d\\d)|([1-9]\\d)|\\d)(\\.((25[0-5])|(2[0-4]\\d)|(1\\d\\d)|([1-9]\\d)|\\d)){3}$";
            if (input.matches(regex)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
