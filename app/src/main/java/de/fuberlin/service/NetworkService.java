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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Outputs information on network changes in log.
 */
public class NetworkService extends Service {
	private class LocalBinder extends Binder {}
	private final IBinder mBinder = new LocalBinder();

	private static final String LOG_TAG = "DESSERT -> NtwrkSrvc";

	private AtomicBoolean wasConnected = new AtomicBoolean(false);
	private AtomicBoolean wasAvailable = new AtomicBoolean(false);
	private AtomicReference<String> SSID = new AtomicReference<>("");
	private AtomicReference<String> failReason = new AtomicReference<>("");
	private AtomicReference<String> detailedState = new AtomicReference<>("");

	/**
	 * "Call-by-reference" method for suppressing multiple redundant log messages.
	 * Only output a log if the (network) state changed.
	 * @param isState current (network) state
	 * @param wasState atomic (network) state: the state before "isState"
	 * @param positive a positive log message (like connected or available)
	 * @param negative a negative log message (like disconnected or unavailable)
	 */
	private void suppressMultipleLogs(boolean isState, AtomicBoolean wasState, String positive, String negative) {
		if (isState) {
			if (!wasState.get())
				Log.d(LOG_TAG, positive);
			wasState.set(true);
		} else {
			if (wasState.get())
				Log.d(LOG_TAG, negative);
			wasState.set(false);
		}
	}

	/**
	 * "Call-by-reference" method for suppressing multiple redundant log messages.
	 * Only output a log if the (network) state changed.
	 * This checks again if a sub value  is null and has no "negative" message.
	 * @param extraCond extra conditions to output a log message
	 * @param atomicString variable to check if a log was already output
	 * @param networkInfo the relevant network information
	 * @param logMessage first part of the log message
	 */
	private void suppressMultipleLogs(boolean extraCond, AtomicReference<String> atomicString, String networkInfo, String logMessage) {
		if (extraCond && !atomicString.get().equals(networkInfo)) {
			Log.d(LOG_TAG, logMessage + networkInfo);
			atomicString.set(networkInfo);
		}
	}

	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo wifi = connMgr.getActiveNetworkInfo();
			if (wifi != null) {
				// log network information
				try {
					suppressMultipleLogs(wifi.isAvailable(), wasAvailable, "network connection (wifi): available", "network connection (wifi): not available");
					suppressMultipleLogs(wifi.isConnectedOrConnecting(), wasConnected, "network connection (wifi): enabled/connected", "network connection (wifi): disabled/disconnected");

					suppressMultipleLogs(wifi.isConnected() && wifi.getExtraInfo() != null, SSID, wifi.getExtraInfo(), "connected to SSID: ");
					suppressMultipleLogs(wifi.getDetailedState() != null, detailedState, wifi.getDetailedState().toString(), "detailed network state: ");
					suppressMultipleLogs(wifi.getReason() != null, failReason, wifi.getReason(), "reason an attempt to establish connectivity failed: ");
				}
				catch(Exception e){
					Log.d(LOG_TAG, "null information: " + e);
				}
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