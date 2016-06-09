package de.fuberlin.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import de.fuberlin.dessert.DessertApplication;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.activity.MainActivity;
import de.fuberlin.dessert.model.daemon.RunningDaemonInfo;

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
		if(Log.isLoggable("NotificationService", Log.INFO)) {
			Log.i("NotificationService", "Received start id " + startId + ": " + intent);
		}
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
						.setShowWhen(true)
						.setTicker(getString(R.string.notification_ticker));
		updateNotification(mNM, this);

		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
		inboxStyle.setBigContentTitle(getString(R.string.notification_title));
		inboxStyle.addLine(getString(R.string.notification_text));
		inboxStyle.addLine(getString(R.string.notification_text_2, getDaemonName(this)));
		mBuilder.setStyle(inboxStyle);
		mNM.notify(R.string.local_service_started, mBuilder.build());

		Intent notificationIntent = new Intent(this, MainActivity.class);

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(MainActivity.class);
		stackBuilder.addNextIntent(notificationIntent);
		PendingIntent contentIntent =
				stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(contentIntent);
	}

	public void updateNotification(NotificationManager notificationManager, Context context) {
		NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
		notification.setSmallIcon(R.drawable.dessert_notification);

		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
		inboxStyle.setBigContentTitle(context.getString(R.string.notification_title));
		inboxStyle.addLine(context.getString(R.string.notification_text));
		inboxStyle.addLine(context.getString(R.string.notification_text_2, getDaemonName(context)));
		notification.setStyle(inboxStyle);

		notificationManager.notify(R.string.local_service_started, notification.build());
	}

	private String getDaemonName(Context context) {
		RunningDaemonInfo daemonInfo = DessertApplication.instance.getRunningDaemon();
		String daemonName;
		if(daemonInfo != null){
			daemonName = daemonInfo.getName();
		}
		else{
			daemonName = context.getString(R.string.daemon_not_running);
		}
		return daemonName;
	}
}
