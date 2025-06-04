package com.example.umbrellaalert.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.umbrellaalert.R;

public class BusFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(android.R.layout.simple_list_item_1, container, false);
        
        TextView textView = view.findViewById(android.R.id.text1);
        textView.setText("버스 정보\n\n추후 구현 예정입니다.");
        textView.setTextSize(18);
        textView.setPadding(32, 32, 32, 32);
        textView.setGravity(android.view.Gravity.CENTER);
        
        return view;
    }
}
