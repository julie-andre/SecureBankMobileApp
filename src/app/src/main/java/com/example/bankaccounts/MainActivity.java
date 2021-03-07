package com.example.bankaccounts;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Application objects
    private EditText textId;
    private TextView textResult;
    private Button buttonContinue;
    private Button buttonAuthenticate;

    // Code variables
    private static final int LOCK_REQUEST_CODE = 221;
    private static final int SECURITY_SETTING_REQUEST_CODE = 233;
    int id;
    private String name;
    private String lastname;
    private ConnectionManager connectionManager;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textId = findViewById(R.id.editTextNumberPassword);
        buttonContinue = findViewById(R.id.btnContinue);
        buttonAuthenticate = findViewById(R.id.btnAuthenticate);

        // We disable the button when launching the activity
        buttonContinue.setEnabled(false);

        buttonContinue.setOnClickListener(this);
        buttonAuthenticate.setOnClickListener(this);

        // We set the default value to the variables
        id=-1;
        name="";
        lastname="";

        // We get an instance of connectionManager
        connectionManager = new ConnectionManager(this);

        // Calling the authentication
        authenticateUser();


    }

    @Override
    protected void onStart() {
        super.onStart();
        // We set the default value to the variables
        id=-1;
        name="";
        lastname="";
    }

    
    private void authenticateUser(){
        // We need an instance of keyguardManager
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

        // This authentication only works for API level >= 21
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            // Opening an intent to display the device's screen lock screen authentication (depends on the device)
            // Either password, pin code,  ... no fingerprints for the moment
            Intent i = keyguardManager.createConfirmDeviceCredentialIntent(getResources().getString(R.string.unlock),
                    getResources().getString(R.string.confirm_authentication));

            try {
                //Start activity for result, try the authentication
                startActivityForResult(i, LOCK_REQUEST_CODE);
            } catch (Exception e) {

                // No security lock has been configured, we display the configuration pannel so the user can configure it
                Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                try {

                    //Start activity for result
                    startActivityForResult(intent, SECURITY_SETTING_REQUEST_CODE);
                } catch (Exception ex) {

                    //The app is not able to find the security settings, the user should set it manually
                    Toast.makeText(getApplicationContext(), "Please define a security setting (PIN, password) for your device", Toast.LENGTH_LONG).show();

                }
            }

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case LOCK_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    //Screen lock authentication is successful
                    Toast.makeText(getApplicationContext(), "Authentication succeeded", Toast.LENGTH_LONG).show();
                    buttonContinue.setEnabled(true);
                } else {
                    //Screen lock failed
                    Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_LONG).show();
                }
                break;
            case SECURITY_SETTING_REQUEST_CODE:
                //This is the case where no security settings were find so we have ask for the user to configure one
                //So we need to check whether device has enabled screen lock or not
                if (isDeviceSecure()) {
                    //The settings have been updated, we call the authenticateUSer method again so the user can authenticate
                    Toast.makeText(getApplicationContext(), "Security settings updated", Toast.LENGTH_LONG).show();
                    authenticateUser();
                } else {
                    //No security settings have been found, we don't enable the user to continue
                    Toast.makeText(getApplicationContext(), "Device not secured or authentication was cancelled", Toast.LENGTH_LONG).show();
                }

                break;
        }
    }


    private boolean isDeviceSecure() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

        //We check if the api level is grater than JELLY_BEAN (16) and if the a keyguard setting is present (<=> screen lock enabled)
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && keyguardManager.isKeyguardSecure();


    }


    private void sendHttpRequest(){
        OkHttpClient client = new OkHttpClient();

        String url = "https://60102f166c21e10017050128.mockapi.io/labbbank/config/" + String.valueOf(this.id);

        Request request = new Request.Builder().url(url).build();

        // Make our http request in a new thread since we can't make http request on our main thread
        // Enqueue method set limits to 5 threads runned in parallel
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
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // We update the text view
                            //textResult.setText(myResponse);
                            try{
                                // We modify the user.json file containing the info of only one user
                                JSONObject object = new JSONObject(myResponse);
                                writeUserFileBis(object);

                                String [] splitted = myResponse.split(",");
                                String id_s = splitted[0].split(":")[1];
                                int id = Integer.parseInt(id_s.substring(1,id_s.length()-1));
                                name = splitted[1].split(":")[1];
                                name = name.substring(1,name.length()-1);
                                lastname = splitted[2].split(":")[1];
                                lastname = lastname.substring(1,lastname.length()-2);

                                // We can transmit the data (id, name, last_name) so the accounts activity
                                // And there fetch the accounts info
                                Intent aboutIntent= new Intent(MainActivity.this, AccountsActivity.class);
                                aboutIntent.putExtra("id", id);
                                aboutIntent.putExtra("name", name);
                                aboutIntent.putExtra("lastName", lastname);
                                // We start the ParameterActivity and close the MainActivity
                                startActivity(aboutIntent);
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
                }
                else{
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // We reset the id value since we did not retrieve any information
                            id=-1;
                            Toast.makeText(getApplicationContext(), "Wrong input, inter a valid id please", Toast.LENGTH_LONG).show();
                        }
                    });
                }

            }
        });

        client.dispatcher().executorService().shutdown();

    }


    /**
     * We write the JSON object into an encrypted txt file using AES256 encryption
     */
    private void writeUserFileBis(JSONObject object){
        Context context = getApplicationContext();
        MasterKey mainKey = null;
        try {
            // Retrieving the masterkey
            mainKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();


            String fileToWrite = "user.txt";
            // We verify if the file already exists, if so we delete it
            File f = new File(MainActivity.this.getFilesDir(),fileToWrite);
            if(f.exists()){
                f.delete();
            }

            // We encrypt the content to be stored in the file
            EncryptedFile encryptedFile = new EncryptedFile.Builder(context,
                    f,
                    mainKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();

            byte[] fileContent = object.toString().getBytes(StandardCharsets.UTF_8);

            OutputStream outputStream = encryptedFile.openFileOutput();
            outputStream.write(fileContent);
            outputStream.flush();
            outputStream.close();


        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * We decrypt the file and then read it to retrieve the wanted data.
     * If the reading was successful, we return true, else we return false
     */
    private boolean readUserFileBis(){
        boolean success = false;
        Context context = getApplicationContext();
        MasterKey mainKey = null;
        try {

            context = getApplicationContext();
            mainKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            String fileToRead = "user.txt";
            File f = new File(MainActivity.this.getFilesDir(), fileToRead);
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

                JSONObject object = new JSONObject(new String(plaintext, "UTF-8"));
                int idTemp = object.getInt("id");
                if(this.id == idTemp){
                    id=idTemp;
                    name= object.getString("name");
                    lastname = object.getString("lastname");
                    success= true;
                }

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


    // Creating a listener for the 2 buttons of the activity
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnContinue:

                // On récupère la valeur de la zone de texte et on la converti en string
                String value = textId.getText().toString();

                if (value.equals("") || value.equals(" ")){
                    Toast.makeText(getApplicationContext(),"No id entered",Toast.LENGTH_LONG).show();
                    return;
                }

                if(value.length()>=1 && value.startsWith("0")){
                    Toast.makeText(getApplicationContext(),"Unknown Id",Toast.LENGTH_LONG).show();
                    return;
                }

                try{
                    id = Integer.parseInt(value);

                    if(connectionManager.CheckConnection()){
                        // We make a call to the api to fetch the user information if they exists
                        sendHttpRequest();
                    }
                    else{
                        // There is no internet connection
                        if(readUserFileBis()){
                            // We can transmit the data (id, name, last_name) so the accounts activity
                            // And there fetch the accounts info
                            Intent aboutIntent= new Intent(MainActivity.this, AccountsActivity.class);
                            aboutIntent.putExtra("id", id);
                            aboutIntent.putExtra("name", name);
                            aboutIntent.putExtra("lastName", lastname);
                            // We start the ParameterActivity and close the MainActivity
                            startActivity(aboutIntent);
                        }
                        else{
                            Toast.makeText(getApplicationContext(), "Unknown Id", Toast.LENGTH_LONG).show();
                        }
                    }

                }
                catch(NumberFormatException nfe){
                    // resetting the textView
                    textId.setText("");
                    Toast.makeText(getApplicationContext(), "OhOh, Something went wrong", Toast.LENGTH_LONG).show();

                }
                break;
            case R.id.btnAuthenticate:
                authenticateUser();

                break;
            default:
                break;
        }

    }
}