package com.eletac.tronwallet.wallet;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.Utils;

import org.tron.walletserver.WalletClient;

import static com.eletac.tronwallet.Utils.strToQR;


public class ReceiveFragment extends Fragment {

    private ImageView mQR_Address_ImageView;
    private TextView mAddress_TextView;

    private String mAddress;
    private Bitmap mAddressQRBitmap;

    public ReceiveFragment() {
        // Required empty public constructor
    }

    public static ReceiveFragment newInstance() {
        ReceiveFragment fragment = new ReceiveFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAddress = WalletClient.getSelectedWallet().computeAddress();
        mAddressQRBitmap = strToQR(mAddress, 800,800);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_receive, container, false);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mAddress_TextView = view.findViewById(R.id.Receive_address_textView);
        mQR_Address_ImageView = view.findViewById(R.id.Receive_address_imageView);

        mAddress_TextView.setText(mAddress);
        mQR_Address_ImageView.setImageBitmap(mAddressQRBitmap);

        CopyAddressOnClickListener clickListener = new CopyAddressOnClickListener();
        mAddress_TextView.setOnClickListener(clickListener);
        mQR_Address_ImageView.setOnClickListener(clickListener);
    }

    private class CopyAddressOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Address", mAddress);
            clipboard.setPrimaryClip(clip);
            
            Toast.makeText(getActivity(), getString(R.string.copy_success), Toast.LENGTH_SHORT).show();
        }
    }
}
