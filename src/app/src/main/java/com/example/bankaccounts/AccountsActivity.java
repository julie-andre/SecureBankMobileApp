package com.example.bankaccounts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;

import com.google.android.material.navigation.NavigationView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AccountsActivity extends AppCompatActivity implements View.OnClickListener {

    // Application objects
    private TextView textOwner;
    private ListView listAccounts;
    private Button buttonRefresh;
    //private Button buttonCreate;

    // Code variables
    int id;
    private String name;
    private String lastName;
    private ArrayList<Account> accounts = new ArrayList<>();
    private ConnectionManager connectionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);

        // We fetch the reference of the textView, the listView and the buttons
        textOwner = findViewById(R.id.textViewOwner);
        listAccounts = findViewById(R.id.listViewAccounts);
        buttonRefresh= findViewById(R.id.btnRefresh);
        //buttonCreate = findViewById(R.id.btnCreate);

        // We link the listener to the buttons
        buttonRefresh.setOnClickListener(this);
        //buttonCreate.setOnClickListener(this);

        // We call the method which role is to fetch the parameters
        fetchParameters();

        // We display the names of the client in the corresponding textView
        textOwner.setText(name+ " "+lastName);

        // We get an instance of connectionManager
        connectionManager = new ConnectionManager(this);

        // We fetch the accounts information either by connecting to the API or thanks to stored file
        if(connectionManager.CheckConnection()){
            sendHttpRequestForAccounts();
        }
        else{
            if(!readFile()){
                // In the case where there is no internet connection available and no stored file has been found
                // It is probably the first connection of the user to the app and since there is no internet, the accounts info
                // can't be found
                Toast.makeText(getApplicationContext(),"Accounts info unavailable",Toast.LENGTH_SHORT).show();
            }
        }


    }

    private void fetchParameters(){
        name="";
        lastName="";
        // We fetch the parameters sent by the main activity
        Intent intent = getIntent();
        if(intent!=null){
            // We check that the intent has indeed a parameter named name
            if(intent.hasExtra("name")){
                name = intent.getStringExtra("name");
            }

            if(intent.hasExtra("lastName")){
                lastName= intent.getStringExtra("lastName");
            }
            // We gat the value of the integer extra if it exists, otherwise we set the id to -1
            id = intent.getIntExtra("id",-1);
        }
    }


    private void sendHttpRequestForAccounts(){
        OkHttpClient client = new OkHttpClient();

        String url = "https://60102f166c21e10017050128.mockapi.io/labbbank/accounts";

        Request request = new Request.Builder().url(url).build();

        // Make our http request in a new thread since we can't make http request on our main thread
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                //e.printStackTrace();

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.isSuccessful()){
                    String myResponse = response.body().string();
                    // We can access the text view in the main thread but here we are in another thread
                    // So we have to make a callback to the main thread
                    AccountsActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                // We assign the list of accounts to an empty list
                                accounts = new ArrayList<>();
                                // We read and process the json data coming from the API
                                JSONArray array = new JSONArray(myResponse);
                                JSONObject account;

                                for (int i = 0; i < array.length(); i++) {
                                    account = array.getJSONObject(i);

                                    accounts.add(new Account(account.getInt("id"), account.getString("accountName"),
                                            account.getDouble("amount"), account.getString("iban"), account.getString("currency")));
                                    
                                }
                                udpateListView();

                                // We write into the JSON file stored internally
                                writeFile();
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
                }

            }
        });
    }

    private void udpateListView()
    {
        AccountsListAdapter adapter = new AccountsListAdapter(this,R.layout.adapter_view_accounts, accounts);
        listAccounts.setAdapter(adapter);
        // Call writefile to store the changes?
    }

    private void writeFile() {
        JSONArray array = new JSONArray();

        try{
            for(Account account : accounts){
                JSONObject acc = new JSONObject();
                acc.put("id", String.valueOf(account.getId()));
                acc.put("accountName", account.getAccountName());
                acc.put("amount", String.valueOf(account.getAmount()));
                acc.put("iban", account.getIban());
                acc.put("currency", account.getCurrency());

                // We had ths json object to the array
                array.put(acc);
            }
        }catch (JSONException e) {
            e.printStackTrace();
        }


        Context context = getApplicationContext();
        MasterKey mainKey = null;
        try {
            mainKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();


            String fileToWrite = "accountsJson.txt";
            // We verify if the file already exists, if so we delete it
            File f = new File(AccountsActivity.this.getFilesDir(),fileToWrite);
            if(f.exists()){
                f.delete();
            }

            EncryptedFile encryptedFile = new EncryptedFile.Builder(context,
                    f,
                    mainKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();

            byte[] fileContent = array.toString().getBytes(StandardCharsets.UTF_8);

            OutputStream outputStream = encryptedFile.openFileOutput();
            outputStream.write(fileContent);
            outputStream.flush();
            outputStream.close();


        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        readFile();
    }

    private boolean readFile(){

        boolean success = false;
        Context context = getApplicationContext();
        MasterKey mainKey = null;
        try {

            context = getApplicationContext();
            mainKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            String fileToRead = "accountsJson.txt";
            File f = new File(AccountsActivity.this.getFilesDir(), fileToRead);
            if(f.exists()){
                EncryptedFile encryptedFile = new EncryptedFile.Builder(context,
                        f,
                        mainKey,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build();

                InputStream inputStream = encryptedFile.openFileInput();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                int nextByte = inputStream.read();
                while (nextByte != -1) {
                    byteArrayOutputStream.write(nextByte);
                    nextByte = inputStream.read();
                }

                byte[] plaintext = byteArrayOutputStream.toByteArray();


                inputStream.close();

                JSONArray array = new JSONArray(new String(plaintext, "UTF-8"));
                JSONObject account;
                // We assign the list of accounts to an empty list
                accounts = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    account = array.getJSONObject(i);
                    accounts.add(new Account(account.getInt("id"), account.getString("accountName"),
                            account.getDouble("amount"), account.getString("iban"), account.getString("currency")));
                }

                success = true;
                udpateListView();

            }


        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return success;
    }

    private boolean createNewAccount(){
        boolean created = false;

        return created;
    }


    // Creating a listener for the 2 buttons of the activity
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnRefresh:
                // If the internet connection is up
                if(connectionManager.CheckConnection()){
                    sendHttpRequestForAccounts();
                    Toast.makeText(getApplicationContext(),"Accounts have been actualized",Toast.LENGTH_SHORT).show();
                }

                // Else we fetch the data from the json, or not
                // considering the file has been updated the last time the connection was up
                break;

            /*case R.id.btnCreate:
                // We redirect to the account creation activity
                Toast.makeText(getApplicationContext(),"Creating a new account",Toast.LENGTH_SHORT).show();

                break;*/
            default:
                break;
        }

    }
}
