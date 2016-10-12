package com.example.mukti.tracking;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TambahKurir extends Activity {

    // Progress Dialog
    private ProgressDialog pDialog;

    JSONParser jsonParser = new JSONParser();
    EditText inputNama;
    public String _Lat, _Lng;
    public double lat, lng;

    //Location Manager
    LocationManager locManager;

    // inisialisasi url tambahanggota.php
    private static String url_tambah_kurir = "http://trackingpizza.pe.hu/trackingPizza/tambahkurir.php";

    // inisialisasi nama node dari json yang dihasilkan oleh php (utk class ini
    // hanya node "sukses")
    private static final String TAG_SUKSES = "sukses";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tambah_kurir);
        CekGPS();
//mengambil latitude saat ini
        locManager =(LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000,1, locationListener);
        Location location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
        }


        // inisialisasi Edit Text
        inputNama = (EditText) findViewById(R.id.inputNama);

        // inisialisasi button
        Button btnTambahAnggota = (Button) findViewById(R.id.btnConfirm);

        // klik even tombol tambah anggota
        btnTambahAnggota.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // buat method pada background thread
                new BuatAnggotaBaru().execute();
            }
        });
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

    private void updateWithNewLocation(Location location) {
        TextView myLocationText = (TextView) findViewById(R.id.tvLokasi);
        String latLongString = "Tunggu Hingga Lokasi Muncul. . .";
        if (location != null) {
            lat = location.getLatitude();
            lng = location.getLongitude();
            double iLat = lat * 1000000000;
            double iLng = lng * 1000000000;
            _Lat = String.valueOf(lat);
            _Lng = String.valueOf(lng);
            latLongString = "Posisi Anda : \n Lat:" + lat + "\nLong:" + lng;
        } else {
            latLongString = "Lokasi Tidak Ditemukan :(";
        }
        myLocationText.setText(latLongString);
    }

    private final LocationListener locationListener = new LocationListener() {

        public void onLocationChanged(Location location) {
            updateWithNewLocation(location);
        }

        public void onProviderDisabled(String provider) {
            updateWithNewLocation(null);
        }

        public void onProviderEnabled(String provider) {}

        public void onStatusChanged(String provider,int status,Bundle extras){}
    };

    /**
     * Background Async Task untuk menambah data anggota baru
     * */
    class BuatAnggotaBaru extends AsyncTask<String, String, String> {

        // sebelum memulai background thread tampilkan Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(TambahKurir.this);
            pDialog.setMessage("Menambah data..silahkan tunggu");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        // menambah data
        protected String doInBackground(String... args) {
            String nama = inputNama.getText().toString();
            String latitude = _Lat;
            String longitude = _Lng;

            // Parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("nama", nama));
            params.add(new BasicNameValuePair("latitude", latitude));
            params.add(new BasicNameValuePair("longitude", longitude));

            // mengambil JSON Object dengan method POST
            JSONObject json = jsonParser.makeHttpRequest(url_tambah_kurir,
                    "POST", params);

            // periksa respon log cat
            Log.d("Respon tambah kurir", json.toString());

            try {
                int sukses = json.getInt(TAG_SUKSES);
                if (sukses == 1) {

                    // jika sukses menambah data baru
                    Intent i = new Intent(getApplicationContext(),
                            MainActivity.class);
                    startActivity(i);

                    // tutup activity ini
                    finish();
                } else {

                    // jika gagal dalam menambah data
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String file_url) {
            // hilangkan dialog ketika selesai menambah data baru
            pDialog.dismiss();
        }
    }
}