package com.eletac.tronwallet;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class SimpleTextDisplayFragment extends Fragment {

    private TextView mTextView;

    private String mText;

    private static final String ARG_TEXT = "text";

    public SimpleTextDisplayFragment() {
        // Required empty public constructor
    }

    public static SimpleTextDisplayFragment newInstance(String text) {
        SimpleTextDisplayFragment fragment = new SimpleTextDisplayFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEXT, text);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mText = getArguments().getString(ARG_TEXT);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_simple_text_display, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTextView = view.findViewById(R.id.SimpleTextDisplay_textView);
        mTextView.setText(mText);
    }


}
