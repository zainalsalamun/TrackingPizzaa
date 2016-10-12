package com.example.mukti.tracking;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class LoginActivity extends ActionBarActivity {
    private Button login;
    private EditText etNama,etPass;
    ProgressDialog pDialog;
    JSONParser jsonParser = new JSONParser();
    JSONObject json;
    JSONArray kurir;
    int success=0;
    private static final String LOGIN_URL = "http://trackingpizza.pe.hu/trackingPizza/loginakurir.php";
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_MESSAGE = "message";
    private static final String TAG_NAMA = "username_kurir";
    private static final String TAG_PASSWORD = "password_kurir";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        etNama = (EditText) findViewById(R.id.username);
        etPass = (EditText) findViewById(R.id.password);
        login = (Button) findViewById(R.id.btnLogin);

        etNama.setText("zainal");
        etPass.setText("kurir");


        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Login().execute(etNama.getText().toString(),etPass.getText().toString());
            }
        });

//        daftar.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent i = new Intent(getApplicationContext(), DaftarActivity.class);
//                startActivity(i);
//            }
//        });
    }

    /**
     * Background Async Task untuk menyimpan/mengupdate data anggota
     * */
    class Login extends AsyncTask<String , String , Integer>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(LoginActivity.this);
            pDialog.setMessage("Sedang proses .....");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected Integer doInBackground(String... params) {
            List<NameValuePair> param = new ArrayList<NameValuePair>();
            String username_kurir = params[0];
            String password_kurir = params[1];
            param.add(new BasicNameValuePair(TAG_NAMA,username_kurir));
            param.add(new BasicNameValuePair(TAG_PASSWORD,password_kurir));

            json = jsonParser.makeHttpRequest(LOGIN_URL,"GET",param);
            Log.d(" ", "json: " +json);
            try{
                success = json.getInt(TAG_SUCCESS);
                kurir = json.getJSONArray("kurir");
                if(success == 1){
                    Log.d("Attempt","Success");
                }else
                    Log.d("JSON Parser",String.valueOf(success));
                Log.i("JSON Parser",json.toString());
            }catch (JSONException e){
                e.printStackTrace();
            }
            return success;
        }

        @Override
        protected void onPostExecute(Integer s) {
            pDialog.dismiss();
            if(s == 1){
                Intent home = new Intent(getApplicationContext(),Maps2.class);
                home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(home);
            }else{
                try{
                    Toast.makeText(getApplicationContext(),"Error : "+json.getString(TAG_MESSAGE),Toast.LENGTH_LONG).show();

                }catch (JSONException e){
                    Toast.makeText(getApplicationContext(),"Error : "+e,Toast.LENGTH_LONG).show();
                }

            }
        }
    }

}
