package de.fuberlin.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Outputs information on network changes in log.
 */
public class NetworkService extends Service {
	private class LocalBinder extends Binder {}
	private final IBinder mBinder = new LocalBinder();

	private static final String LOG_TAG = "DESSERT -> NtwrkSrvc";

	private boolean wasConnected = false; // used for suppression of multiple log outputs
	private boolean wasAvailable = false;

	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo wifi = connMgr.getActiveNetworkInfo();

			// output: network available / connected
			boolean isAvailable = wifi.isAvailable();
			if (isAvailable) {
				if (!wasAvailable)
					Log.d(LOG_TAG, "network connection (wifi): available");
				wasAvailable = true;
			} else {
				if (wasAvailable)
					Log.d(LOG_TAG, "network connection (wifi): not available");
				wasAvailable = false;
			}

			boolean isConnected = wifi.isConnectedOrConnecting();
			if (isConnected) {
				if (!wasConnected)
					Log.d(LOG_TAG, "network connection (wifi): enabled/connected");
				wasConnected = true;
			} else {
				if (wasConnected)
					Log.d(LOG_TAG, "network connection (wifi): disabled/disconnected");
				wasConnected = false;
			}
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy(){
		unregisterReceiver(receiver);
	}

	@Override
	public void onCreate() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.ACTION_PICK_WIFI_NETWORK);

		registerReceiver(receiver, filter);
	}
}
