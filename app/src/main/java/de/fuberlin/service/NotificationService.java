package de.fuberlin.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.activity.MainActivity;

public class NotificationService extends Service {
	private class LocalBinder extends Binder {

	}

	private NotificationManager mNM;
	private final IBinder mBinder = new LocalBinder();

	@Override
	public void onCreate() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification();
		Log.i("NotificationService", "onCreate finished");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("NotificationService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		mNM.cancel(R.string.local_service_started);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private void showNotification() {
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(getApplicationContext())
						.setSmallIcon(R.drawable.dessert_notification)
						.setContentTitle(getString(R.string.notification_title))
						.setContentText(getString(R.string.notification_text))
						.setShowWhen(true)
						.setTicker(getString(R.string.notification_ticker));
		Intent notificationIntent = new Intent(this, MainActivity.class);

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(MainActivity.class);
		stackBuilder.addNextIntent(notificationIntent);
		PendingIntent contentIntent =
				stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(contentIntent);
		mNM.notify(R.string.local_service_started, mBuilder.build());
	}
}
