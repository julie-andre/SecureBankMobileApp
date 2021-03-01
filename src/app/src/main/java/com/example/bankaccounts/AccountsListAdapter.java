package com.example.bankaccounts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 Adapter class to correctly display the accounts items in a listView
 */
public class AccountsListAdapter extends ArrayAdapter<Account> {

    private Context context;
    private int resource;

    public AccountsListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Account> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource=resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // We get the account object
        Account account = getItem(position);

        // We check if an existing view is being reused, otherwise we inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(resource, parent, false);
        }

        // We fetch the reference to the textviews of the adapter
        TextView id = (TextView) convertView.findViewById(R.id.idAccount);
        TextView accountName = (TextView) convertView.findViewById(R.id.accountName);
        TextView iban = (TextView) convertView.findViewById(R.id.iban);
        TextView amount = (TextView) convertView.findViewById(R.id.amount);
        TextView currency = (TextView) convertView.findViewById(R.id.currency);

        // We set the the data to the template view using the data object
        id.setText(String.valueOf(account.getId()));
        accountName.setText(account.getAccountName());
        iban.setText(account.getIban());
        amount.setText(String.valueOf(account.getAmount()));
        currency.setText(account.getCurrency());

        // Once the view is completed, we can return it so it can be rendered on screen
        return convertView;
    }

}
