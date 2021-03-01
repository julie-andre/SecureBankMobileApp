package com.example.bankaccounts;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class NewAccountActivity extends AppCompatActivity {

    private TextView textActId;
    private TextView textActName;
    private TextView textAmount;
    private TextView textIban;
    private TextView textCurrency;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_account);

        // We fetch the reference of the textView, the listView and the buttons
        textActId = findViewById(R.id.text_actId);
        textActName = findViewById(R.id.text_actName);
        textAmount = findViewById(R.id.text_actAmount);
        textIban = findViewById(R.id.text_actIban);
        textCurrency = findViewById(R.id.text_actCurrency);


    }
}
