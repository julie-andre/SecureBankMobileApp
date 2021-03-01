package com.example.bankaccounts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * This class allows the activities to check for an internet connection
 */
public class ConnectionManager {

    private Context context;

    public ConnectionManager(Context context){
        this.context = context;
    }

    public boolean CheckConnection(){
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connManager.getActiveNetworkInfo();
        return activeNetInfo!=null && activeNetInfo.isConnectedOrConnecting();
    }



}
