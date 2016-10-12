package com.example.mukti.tracking;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Maps2 extends FragmentActivity implements View.OnClickListener {
    private GoogleMap mMap;
    private ProgressDialog pDialog;
    // URL to get contacts JSON
    private static String url = "http://trackingpizza.pe.hu/trackingPizza/semua_pemesan.php";

    //inisialisasi variabel-variable
    String nama_asal, nama_tujuan;
    double latitude_asal, longitude_asal, latitude_tujuan, longitude_tujuan, lat, lng;
    GPSTracker gps;
    Button rute_intent;
    LatLng Asal, Tujuan;
    PolylineOptions lineOptions;
    TextView jarakLokasi, waktu;
    ArrayList<HashMap<String, String>> pemesanList=new ArrayList<>();

    // JSON Node names
    private static final String TAG_PEMESAN = "pemesan";
    private static final String TAG_ID = "id";
    private static final String TAG_NAMA = "nama";
    private static final String TAG_ALAMAT = "alamat";
    private static final String TAG_NOTELP = "no_telp";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_LONGITUDE = "longitude";

    JSONArray pemesan = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapss);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        rute_intent = (Button) findViewById(R.id.rute_intent);
        rute_intent.setOnClickListener(this);
        jarakLokasi = (TextView) findViewById(R.id.jarak);
        waktu = (TextView) findViewById(R.id.waktu);

        SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMap = mapFrag.getMap();

        mMap.setMyLocationEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

//                        Marker asal =  mMap.addMarker(new MarkerOptions()
//                                .position(Asal)
//                                .title(nama_asal)
//                                .icon(BitmapDescriptorFactory
//                                        .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
//                        asal.showInfoWindow();

      /*  Intent i = getIntent();
        latitude_tujuan = Double.parseDouble(i.getStringExtra("latitude"));
        longitude_tujuan = Double.parseDouble(i.getStringExtra("longitude"));*/

        getLokasi();
        //tampilPetaMarker();

        new GetPemesan().execute();

    }

    public void getLokasi() {
        gps = new GPSTracker(Maps2.this);

        // check if GPS enabled
        if (gps.canGetLocation()) {

            latitude_asal = gps.getLatitude();
            longitude_asal = gps.getLongitude();

        } else {
            gps.showSettingsAlert();
        }

    }


   /* public void tampilPetaMarker() {
        nama_asal = "posisi anda";
        nama_tujuan = "lokasi pemesan";

        Asal = new LatLng(latitude_asal, longitude_asal);
        Tujuan = new LatLng(latitude_tujuan, longitude_tujuan);

        SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMap = mapFrag.getMap();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        Marker asal =  mMap.addMarker(new MarkerOptions()
                .position(Asal)
                .title(nama_asal)
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        asal.showInfoWindow();

        Marker tujuan = mMap.addMarker(new MarkerOptions()
                .position(Tujuan)
                .title(nama_tujuan)
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        tujuan.showInfoWindow();

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(Tujuan)
                .zoom(15)
                .bearing(0)
                .tilt(45)
                .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
    }*/

    public void buatPoli(ArrayList<HashMap<String, String>> pemesanList){
        try {
            LatLng stateA = null;
            for (int i = 0; i < pemesanList.size(); i++) {
                String latitude = pemesanList.get(i).get(TAG_LATITUDE);
                String longitude = pemesanList.get(i).get(TAG_LONGITUDE);
                if(i==0){
                    String url = getDirectionsUrl(Asal, new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude)));
                    DownloadTask downloadTask = new DownloadTask();
                    downloadTask.execute(url);
                    stateA = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                }else{
                    String url = getDirectionsUrl(stateA, new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude)));
                    DownloadTask downloadTask = new DownloadTask();
                    downloadTask.execute(url);
                    stateA = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                }

            }

        } catch (Exception e) {
            Intent inten = new Intent(
                    getApplicationContext(),
                    Maps2.class);
            startActivity(inten);
            finish();
        }
    }

    double jarak,durasi,lat_, lng_;
    String j,w;

    /*public void hitungJarak(ArrayList<HashMap<String, String>> pemesanList){
        try {

            for (int i = 0; i < pemesanList.size(); i++) {
                String lat = pemesanList.get(i).get(TAG_LATITUDE);
                String lng = pemesanList.get(i).get(TAG_LONGITUDE);

                lat_ = Double.parseDouble(lat);
                lng_ = Double.parseDouble(lng);

                jarak  = getDistanceInfo(Asal.latitude, Asal.longitude, pemesan.latitude, lng_);
                jarakLokasi.setText("Jarak ke lokasi pemesan : " + jarak / 1000 + " Km");

                durasi = getDurasiInfo(Asal.latitude, Asal.longitude, lat_, lng_);
                waktu.setText("Waktu ke lokasi pemesan : " + String.format("%.2f",durasi / 60) + " Menit");

            }


        }
        catch (Exception e) {
            Intent inten = new Intent(
                    getApplicationContext(),
                    Maps2.class);
            startActivity(inten);
            finish();
        }
        *//*Location titik_asal =new Location("titik_asal");
        titik_asal.setLatitude(Asal.latitude);
        titik_asal.setLongitude(Asal.longitude);

        Location titik_akhir =new Location("titik_akhir");
        titik_akhir.setLatitude(Tujuan.latitude);
        titik_akhir.setLongitude(Tujuan.longitude);*//*



    }*/

    @Override
    public void onClick(View v) {
        if (v==rute_intent){
            Intent i = new Intent(getApplicationContext(),
                    GPSTrackerActivity.class);
            startActivity(i);
        }
    }

    // fungsi mendapatkan rute
    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Awal rute
        String str_origin = "origin=" + origin.latitude + ","
                + origin.longitude;

        // Tujuan rute
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Membuat parameters untuk dimasukkan web service rute map google
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // URL untuk eksekusi rute
        String url = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + parameters;

        return url;
    }

    // Metode mendapatkan json data dari url

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // menghubungkan ke url
            urlConnection.connect();

            // Membaca data dari url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("While downloading url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // class untuk download data dari Google Directions URL
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Mendowload data dalam non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
                Intent i = new Intent(getApplicationContext(),
                        Maps2.class);
                startActivity(i);
                finish();
            }
            return data;
        }

        // di eksekusi di layar tampilan,setelah selesai ekseksui di
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Thread untuk parsing the JSON data
            parserTask.execute(result);

        }
    }

    /** Class untuk mengekstrak Google Directions dalam format JSON */
    private class ParserTask extends
            AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing data di dalam thread background
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(
                String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Mulai parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                Intent i = new Intent(getApplicationContext(),
                        Maps2.class);
                startActivity(i);
                finish();
            }
            return routes;
        }

        // Mengeksekusi di tampilan setelah proses ektrak data selesai
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Menginisialisasi i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Menambahkan semua points dalam rute ke LineOptions
                lineOptions.addAll(points);
                lineOptions.width(5f);
                lineOptions.color(Color.BLUE);

            }

            try{
                mMap.addPolyline(lineOptions);
            }catch (Exception d){
                d.printStackTrace();
            }

        }
    }
    private double getDistanceInfo(double lat1, double lng1,double lat2, double lng2) {
        StringBuilder stringBuilder = new StringBuilder();
        Double dist = 0.0;
        try {
            String url = "http://maps.googleapis.com/maps/api/directions/json?origin=" + lat1 + "," + lng1 + "&destination=" + lat2+","+lng2 + "&mode=driving&sensor=false";

            HttpPost httppost = new HttpPost(url);

            HttpClient client = new DefaultHttpClient();
            HttpResponse response;
            stringBuilder = new StringBuilder();


            response = client.execute(httppost);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            int b;
            while ((b = stream.read()) != -1) {
                stringBuilder.append((char) b);
            }
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }

        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject = new JSONObject(stringBuilder.toString());

            JSONArray array = jsonObject.getJSONArray("routes");

            JSONObject routes = array.getJSONObject(0);

            JSONArray legs = routes.getJSONArray("legs");

            JSONObject steps = legs.getJSONObject(0);

            JSONObject distance = steps.getJSONObject("distance");

            dist = Double.parseDouble(distance.getString("value"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return dist;
    }
    private double getDurasiInfo(double lat1, double lng1,double lat2, double lng2) {
        StringBuilder stringBuilder = new StringBuilder();
        Double durat = 0.0;
        try {
            String url = "http://maps.googleapis.com/maps/api/directions/json?origin=" + lat1 + "," + lng1 + "&destination=" + lat2+","+lng2 + "&mode=driving&sensor=false";

            HttpPost httppost = new HttpPost(url);

            HttpClient client = new DefaultHttpClient();
            HttpResponse response;
            stringBuilder = new StringBuilder();


            response = client.execute(httppost);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            int b;
            while ((b = stream.read()) != -1) {
                stringBuilder.append((char) b);
            }
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }

        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject = new JSONObject(stringBuilder.toString());

            JSONArray array = jsonObject.getJSONArray("routes");

            JSONObject routes = array.getJSONObject(0);

            JSONArray legs = routes.getJSONArray("legs");

            JSONObject steps = legs.getJSONObject(0);

            JSONObject duration = steps.getJSONObject("duration");

            durat = Double.parseDouble(duration.getString("value"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return durat;
    }
    private class GetPemesan extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(Maps2.this);
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

            show(pemesanList);

            buatPoli(pemesanList);
            //hitungJarak(pemesanList);
            /**
             * Updating parsed JSON data into ListView
             * */
//            ListAdapter adapter = new SimpleAdapter(
//                    MainActi.this, pemesanList,
//                    R.layout.list_item, new String[] { TAG_ID, TAG_NAMA,
//                    TAG_ALAMAT, TAG_NOTELP, TAG_LATITUDE, TAG_LONGITUDE }, new int[] { R.id.idmem,
//                    R.id.nama, R.id.alamat,  R.id.no_telp,  R.id.latitude,  R.id.longitude });
//
//            setListAdapter(adapter);
        }

    }

    void show(ArrayList<HashMap<String, String>> pemesanList){
        Asal = new LatLng(latitude_asal, longitude_asal);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(Asal)
                .zoom(15)
                .bearing(0)
                .tilt(45)
                .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        Marker asal =  mMap.addMarker(new MarkerOptions()
                .position(Asal)
                        .title(nama_asal)
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        asal.showInfoWindow();

        LatLng waktuA = null;
        for (int i = 0; i < pemesanList.size(); i++) {
            String latitude = pemesanList.get(i).get(TAG_LATITUDE);
            String longitude = pemesanList.get(i).get(TAG_LONGITUDE);
            String alamat = pemesanList.get(i).get(TAG_ALAMAT);
            Log.d(":lat", latitude);
            Log.d(":lat", longitude);
            Log.d(":lat", alamat);

            Double lat = Double.parseDouble(latitude);
            Double lng = Double.parseDouble(longitude);

            if(i==0){
                jarak  = getDistanceInfo(Asal.latitude, Asal.longitude, lat, lng);
                durasi = getDurasiInfo(Asal.latitude, Asal.longitude, lat, lng);
                waktuA = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
            }
            else{
                jarak  = getDistanceInfo(waktuA.latitude, waktuA.longitude, lat, lng);
                durasi = getDurasiInfo(waktuA.latitude, waktuA.longitude, lat, lng);
                waktuA = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
            }

            LatLng pemesan2= new LatLng(Double.parseDouble(latitude ), Double.parseDouble(longitude));


            //jarakLokasi.setText("Jarak ke lokasi pemesan : " + jarak / 1000 + " Km");


            //waktu.setText("Waktu ke lokasi pemesan : " + String.format("%.2f",durasi / 60) + " Menit");

            //tujuan.showInfoWindow();
            /*String jarak_ = String.format();
            String durasi_ = ;*/



            if (mMap != null){
                Marker tujuan = mMap.addMarker(new MarkerOptions()
                        .position(pemesan2)
                        .title(alamat)
                        .snippet("jarak : " + jarak / 1000 + " KM, waktu : " + String.format("%.2f", durasi / 60) + " Menit")
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                tujuan.showInfoWindow();
            }
        }

    }
}
