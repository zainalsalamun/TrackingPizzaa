package com.example.mukti.tracking;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends ListActivity {

    private ProgressDialog pDialog;

    // URL to get contacts JSON
    private static String url = "http://trackingpizza.pe.hu/trackingPizza/semua_pemesan.php";

    // JSON Node names
    private static final String TAG_PEMESAN = "pemesan";
    private static final String TAG_ID = "id";
    private static final String TAG_NAMA = "nama";
    private static final String TAG_ALAMAT= "alamat";
    private static final String TAG_NOTELP = "no_telp";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_LONGITUDE = "longitude";

    // contacts JSONArray
    JSONArray pemesan = null;

    // Hashmap for ListView
    ArrayList<HashMap<String, String>> pemesanList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CekGPS();

        pemesanList = new ArrayList <HashMap<String, String>>();

        new GetPemesan().execute();
        ListView lv = getListView();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String latitude = ((TextView) view.findViewById(R.id.latitude)).getText().toString();
                String longitude = ((TextView) view.findViewById(R.id.longitude)).getText().toString();

                Intent in = new Intent(getApplicationContext(), Maps.class);
                in.putExtra("latitude", latitude);
                in.putExtra("longitude", longitude);
                startActivityForResult(in, 100);
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // jika result code 100
        if (resultCode == 100) {
            // jika result code 100 diterima artinya user mengedit/menghapus member
            // reload layar ini lagi
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }
    public void CekGPS() {
        try {
            LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("info");
                builder.setMessage("Apakah anda akan mengaktifkan GPS?");
                builder.setPositiveButton("Ya",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // TODO Auto-generated method stub
                                Intent i = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(i);

                            }
                        });
                builder.setNegativeButton("Tidak",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int arg1) {
                                // TODO Auto-generated method stub
                                dialog.dismiss();
                            }
                        });
                builder.create().show();
            }
        } catch (Exception e) {
            // TODO: handle exception

        }

    }

    /**
     * Async task class to get json by making HTTP call
     * */
    private class GetPemesan extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.GET);

            Log.d("Response: ", "> " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    pemesan = jsonObj.getJSONArray(TAG_PEMESAN);

                    // looping through All Contacts
                    for (int i = 0; i < pemesan.length(); i++) {
                        JSONObject c = pemesan.getJSONObject(i);

                        String id = c.getString(TAG_ID);
                        String nama = c.getString(TAG_NAMA);
                        String alamat = c.getString(TAG_ALAMAT);
                        String no_telp = c.getString(TAG_NOTELP);
                        String latitude = c.getString(TAG_LATITUDE);
                        String longitude = c.getString(TAG_LONGITUDE);

                        // tmp hashmap for single contact
                        HashMap<String, String> pemesan = new HashMap<String, String>();

                        // adding each child node to HashMap key => value
                        pemesan.put(TAG_ID, id);
                        pemesan.put(TAG_NAMA, nama);
                        pemesan.put(TAG_ALAMAT, alamat);
                        pemesan.put(TAG_NOTELP, no_telp);
                        pemesan.put(TAG_LATITUDE, latitude);
                        pemesan.put(TAG_LONGITUDE, longitude);

                        // adding contactpemesan to contact list
                        pemesanList.add(pemesan);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Tidak bisa mendapatkan data dari url");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
            /**
             * Updating parsed JSON data into ListView
             * */
            ListAdapter adapter = new SimpleAdapter(
                    MainActivity.this, pemesanList,
                    R.layout.list_item, new String[] { TAG_ID, TAG_NAMA,
                    TAG_ALAMAT, TAG_NOTELP, TAG_LATITUDE, TAG_LONGITUDE }, new int[] { R.id.idmem,
                    R.id.nama, R.id.alamat,  R.id.no_telp,  R.id.latitude,  R.id.longitude });

            setListAdapter(adapter);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
