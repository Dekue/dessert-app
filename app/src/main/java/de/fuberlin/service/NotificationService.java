package de.fuberlin.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import de.fuberlin.dessert.DessertApplication;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.activity.MainActivity;
import de.fuberlin.dessert.model.daemon.RunningDaemonInfo;

public class NotificationService extends Service {
	private class LocalBinder extends Binder {

	}

	private final IBinder mBinder = new LocalBinder();

	@Override
	public void onCreate() {
		NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification(mNM, this);
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
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public void showNotification(NotificationManager notificationManager, Context context) {
		NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.dessert_notification)
				.setContentTitle(context.getString(R.string.notification_title))
				.setContentText(context.getString(R.string.notification_text_daemon_running, getDaemonName(context)))
				.setShowWhen(true)
				.setAutoCancel(false)
				.setTicker(context.getString(R.string.notification_ticker))
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
		inboxStyle.setBigContentTitle(context.getString(R.string.notification_title));
		inboxStyle.addLine(context.getString(R.string.notification_text));
		inboxStyle.addLine(context.getString(R.string.notification_text_daemon_running, getDaemonName(context)));
		notification.setStyle(inboxStyle);
		Intent notificationIntent = new Intent(context, MainActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setContentIntent(contentIntent);
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
