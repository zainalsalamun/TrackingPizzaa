package com.example.mukti.tracking;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


public class GPSTrackerActivity extends Activity implements LocationListener {
	//inisialisasi url digunakan untuk mengirim korrdinat ke server
	private final static String CONNECTIVITY = "android.net.conn.CONNECTIVITY_CHANGE";
	public static String url="http://trackingpizza.pe.hu/trackingPizza/gps.php"; //enter your url here
	private LocationManager locationManager;
	private ConnectivityManager connectivityManager;

	SharedPreferences preferences;
	//deklarasi variabel yang digunakan ketika prgram dijalankan
	public static EditText name; 
	private TextView text_gps_status;
	private TextView text_network_status;
	private ToggleButton button_toggle;
	private TextView text_running_since;
	private TextView last_server_response;
	//menjalankan service ketika activity dijalankan
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(GPSTrackerService.NOTIFICATION)) {
				updateServiceStatus();
			}
			if (action.equals(CONNECTIVITY)) {
				updateNetworkStatus();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_self_hosted_gpstracker);

		
		text_gps_status = (TextView)findViewById(R.id.text_gps_status);
		text_network_status = (TextView)findViewById(R.id.text_network_status);
		button_toggle = (ToggleButton)findViewById(R.id.button_toggle);
		text_running_since = (TextView)findViewById(R.id.text_running_since);
		last_server_response = (TextView)findViewById(R.id.last_server_response);
          name=(EditText)findViewById(R.id.name);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences.contains("NAME") && ! preferences.getString("NAME", "").equals("")) {
			name.setText(preferences.getString("NAME", getString(R.string.hint_url)));
			name.clearFocus();
		} else {
			name.requestFocus();
		}
		
		registerReceiver(receiver, new IntentFilter(GPSTrackerService.NOTIFICATION));
		registerReceiver(receiver, new IntentFilter(GPSTrackerActivity.CONNECTIVITY));
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
	 	//mencari lokasi korrdinat dan mengupddate ker server dalam jangka waktu 30 detik
		int pref_gps_updates = Integer.parseInt(preferences.getString("pref_gps_updates", "30")); // seconds
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, pref_gps_updates * 1000, 1, this);
	}
//menjalankan gps provider
	@Override
	public void onResume() {
		super.onResume();

		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			onProviderEnabled(LocationManager.GPS_PROVIDER);
		} else {
			onProviderDisabled(LocationManager.GPS_PROVIDER);
		}

		updateNetworkStatus();
		
		updateServiceStatus();

//menjalankan background service ketika tracking dijalankan
		if (GPSTrackerService.isRunning) {
			name.setEnabled(false);
		} else {
			name.setEnabled(true);
		}
	}

//menghentikan tracking sementara ketika tombol pause dijalankan dan menjalankan kembali saat OnDestroy dijalankan
	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		locationManager.removeUpdates(this);
		unregisterReceiver(receiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_self_hosted_gpstracker, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.menu_settings:
			i = new Intent(this, GPSTrackerPrefs.class);
			startActivity(i);
			break;
		default:
		}
		return super.onOptionsItemSelected(item);
	}

	public void onToggleClicked(View view) {
		if(name.getText().toString().equalsIgnoreCase(""))
		{
		       Toast.makeText(GPSTrackerActivity.this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
		}
		else
		{	
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("NAME", name.getText().toString());
			editor.commit();
			
			
			Intent intent = new Intent(this, GPSTrackerService.class);
		if (((ToggleButton) view).isChecked()) {
			startService(intent);
		} else {
			stopService(intent);
		}}
	}

	/* -------------- GPS stuff -------------- */

	@Override
	public void onLocationChanged(Location location) {
	}
//pringatan ketika gps tidak aktif
	@Override
	public void onProviderDisabled(String provider) {
		text_gps_status.setText(getString(R.string.text_gps_status_disabled));
		text_gps_status.setTextColor(Color.RED);
	}
//peringatan ketika gps aktif
	@Override
	public void onProviderEnabled(String provider) {
		text_gps_status.setText(getString(R.string.text_gps_status_enabled));
		text_gps_status.setTextColor(Color.BLACK);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	/* ----------- utility methods -------------- */
	private void updateServiceStatus() {
		
		if (GPSTrackerService.isRunning) {
			Toast.makeText(this, getString(R.string.toast_service_running), Toast.LENGTH_SHORT).show();
			button_toggle.setChecked(true);
			text_running_since.setText(getString(R.string.text_running_since) + " " 
					+ DateFormat.getDateTimeInstance().format(GPSTrackerService.runningSince.getTime()));
		} else {
			Toast.makeText(this, getString(R.string.toast_service_stopped), Toast.LENGTH_SHORT).show();
			button_toggle.setChecked(false);
			if (preferences.contains("stoppedOn")) {
				long stoppedOn = preferences.getLong("stoppedOn", 0);
				if (stoppedOn > 0) {
					text_running_since.setText(getString(R.string.text_stopped_on) + " " 
							+ DateFormat.getDateTimeInstance().format(new Date(stoppedOn)));
				} else {
					text_running_since.setText(getText(R.string.text_killed));
				}
			}
		}
	}
	
	private void updateNetworkStatus() {
		NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
		if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
			text_network_status.setText(getString(R.string.text_network_status_enabled));
			text_network_status.setTextColor(Color.BLACK);
		} else {
			text_network_status.setText(getString(R.string.text_network_status_disabled));
			text_network_status.setTextColor(Color.RED);
		}
	}
}
