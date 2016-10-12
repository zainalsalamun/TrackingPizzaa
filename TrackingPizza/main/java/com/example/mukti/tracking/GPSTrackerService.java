package com.example.mukti.tracking;

import java.util.Calendar;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;


public class GPSTrackerService extends IntentService implements LocationListener {

	public static final String NOTIFICATION = "com.example.mukti.tracking";

	public static boolean isRunning;
	public static Calendar runningSince;
	public Calendar stoppedOn;

	private final static String MY_TAG = "GPSTrackerService";
	
	private SharedPreferences preferences;
	private String urlText;
	private LocationManager locationManager;
	private int pref_gps_updates;
	private long latestUpdate;
	private int pref_max_run_time;

	public GPSTrackerService() {
		super("GPSTrackerService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();		
		Log.d(MY_TAG, "in onCreate, init GPS stuff");
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			onProviderEnabled(LocationManager.GPS_PROVIDER);
		} else {
			onProviderDisabled(LocationManager.GPS_PROVIDER);
		}
		
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong("stoppedOn", 0);
		editor.commit();
		pref_gps_updates = Integer.parseInt(preferences.getString("GPS update", "30")); // seconds
		pref_max_run_time = Integer.parseInt(preferences.getString("Maksimal berjalan", "24")); // hours
		urlText = GPSTrackerActivity.url;
		if (urlText.contains("?")) {
			urlText = urlText + "&"; 
		} else {
			urlText = urlText + "?";
		}
		
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, pref_gps_updates * 1000, 1, this);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(MY_TAG, "in onHandleIntent, run for maximum time set in preferences");
		NotificationManager manager;
		manager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);


		isRunning = true;
		runningSince = Calendar.getInstance();
		Intent notifIntent = new Intent(NOTIFICATION);
		sendBroadcast(notifIntent);
		
//		Notification notification = new Notification(R.drawable.ic_notif, getText(R.string.toast_service_running), System.currentTimeMillis());
//		Intent notificationIntent = new Intent(this, GPSTrackerActivity.class);
//		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//		notification.setLatestEventInfo(this, getText(R.string.app_name), getText(R.string.toast_service_running), pendingIntent);
//		startForeground(R.id.logo, notification);
		Intent notificationIntent = new Intent();
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//untuk mengaktifkan notifikasi ketika background service dijalankan
		Notification myNotication = new Notification.Builder(this)
				.setContentTitle(getText(R.string.app_name))
				.setContentText(getText(R.string.toast_service_running))
				.setContentIntent(pendingIntent)
				.setSmallIcon(R.drawable.ic_notif)
				.setWhen(System.currentTimeMillis())
				.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
				.setAutoCancel(true)
				.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
				.setLights(Color.RED, 3000, 3000)
				.build();
		manager.notify(1, myNotication);

		long endTime = System.currentTimeMillis() + pref_max_run_time*60*60*1000;
		while (System.currentTimeMillis() < endTime) {
			try {
				Thread.sleep(60*1000); // note: when device is sleeping, it may last up to 5 minutes or more
			} catch (Exception e) {
			}
		}
	}
	
	@Override
	public void onDestroy() {
		// (user clicked the stop button, or max run time has been reached)
		Log.d(MY_TAG, "in onDestroy, stop listening to the GPS");
		new GPSTrackerRequest().execute(urlText + "tracker=stop");
		
		locationManager.removeUpdates(this);
		
		isRunning = false;
		stoppedOn = Calendar.getInstance();
		
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong("stoppedOn", stoppedOn.getTimeInMillis());
		editor.commit();
		
		Intent intent = new Intent(NOTIFICATION);
		sendBroadcast(intent);
	}

	/* -------------- GPS stuff -------------- */

	@Override
	public void onLocationChanged(Location location) {
		Log.d(MY_TAG, "in onLocationChanged, latestUpdate == " + latestUpdate);
		
		if ((System.currentTimeMillis() - latestUpdate) < pref_gps_updates*1000) {
			return;
		} else {
			latestUpdate = System.currentTimeMillis();
		}
		
		new GPSTrackerRequest().execute(urlText + "lat=" + location.getLatitude() + "&lon=" + location.getLongitude()+ "&name="+GPSTrackerActivity.name.getText().toString());
	          System.out.println("excecuted fresh tracking requuest======="+urlText + "lat=" + location.getLatitude() + "&lon=" + location.getLongitude());
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
