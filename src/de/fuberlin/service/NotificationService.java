package de.fuberlin.service;

import de.fuberlin.dessert.R;
import de.fuberlin.dessert.activity.MainActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class NotificationService extends Service {
   public class LocalBinder extends Binder {
	   public NotificationService getService() {
           return NotificationService.this;
       }
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
        int icon = R.drawable.dessert_notification;
        CharSequence tickerText = "Hello";
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);
        Context context = getApplicationContext();
        CharSequence contentTitle = "DES-SERT started";
        CharSequence contentText = "The DES-SERT app has started";
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        mNM.notify(R.string.local_service_started, notification);
	}
}
