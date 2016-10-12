package com.example.mukti.tracking;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mukti.tracking.GPSTracker;
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

public class Maps extends FragmentActivity implements View.OnClickListener {
    private GoogleMap mMap;

    //inisialisasi variabel-variable
    String nama_asal, nama_tujuan;
    double latitude_asal, longitude_asal, latitude_tujuan,longitude_tujuan;
    GPSTracker gps;
    Button rute_intent;
    LatLng Asal, Tujuan;
    PolylineOptions lineOptions;
    TextView jarakLokasi, waktu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapss);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        rute_intent =(Button)findViewById(R.id.rute_intent);
        rute_intent.setOnClickListener(this);
        jarakLokasi=(TextView)findViewById(R.id.jarak);
        waktu=(TextView)findViewById(R.id.waktu);

        Intent i = getIntent();
        latitude_tujuan = Double.parseDouble(i.getStringExtra("latitude"));
        longitude_tujuan = Double.parseDouble(i.getStringExtra("longitude"));

        getLokasi();
        tampilPetaMarker();
        buatPoli();
        hitungJarak();
    }

    public void getLokasi(){
        gps = new GPSTracker(Maps.this);

        // check if GPS enabled
        if(gps.canGetLocation()){

            latitude_asal = gps.getLatitude();
            longitude_asal = gps.getLongitude();

        }else {
            gps.showSettingsAlert();
        }

    }

    public void tampilPetaMarker() {
        nama_asal = "posisi anda";
        nama_tujuan = "lokasi pemesan";

//        Asal = new LatLng(latitude_asal, longitude_asal);
        Tujuan = new LatLng(latitude_tujuan, longitude_tujuan);

        SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMap = mapFrag.getMap();
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
    }

    public void buatPoli(){
        try {

            String url = getDirectionsUrl(Asal, Tujuan);
            DownloadTask downloadTask = new DownloadTask();
            downloadTask.execute(url);

        } catch (Exception e) {
            Intent inten = new Intent(
                    getApplicationContext(),
                    Maps.class);
            startActivity(inten);
            finish();
        }
    }

    public void hitungJarak(){
        Location titik_asal =new Location("titik_asal");
        titik_asal.setLatitude(Asal.latitude);
        titik_asal.setLongitude(Asal.longitude);

        Location titik_akhir =new Location("titik_akhir");
        titik_akhir.setLatitude(Tujuan.latitude);
        titik_akhir.setLongitude(Tujuan.longitude);


        double jarak  = getDistanceInfo(Asal.latitude, Asal.longitude,
                Tujuan.latitude, Tujuan.longitude);
        jarakLokasi.setText("Jarak ke lokasi pemesan : " + jarak / 1000 + " Km");

        double durasi = getDurasiInfo(Asal.latitude, Asal.longitude,
                Tujuan.latitude, Tujuan.longitude);
        waktu.setText("Waktu ke lokasi pemesan : " + String.format("%.2f",durasi / 60) + " Menit");

    }

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
                        Maps.class);
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
                        Maps.class);
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
                lineOptions.width(5);
                lineOptions.color(Color.BLUE);

            }

            mMap.addPolyline(lineOptions);

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

}
