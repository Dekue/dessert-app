/*******************************************************************************
 * Copyright 2010, Freie Universitaet Berlin (FUB). All rights reserved.
 *
 * These sources were developed at the Freie Universitaet Berlin, 
 * Computer Systems and Telematics / Distributed, embedded Systems (DES) group 
 * (http://cst.mi.fu-berlin.de, http://www.des-testbed.net)
 * -------------------------------------------------------------------------------
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see http://www.gnu.org/licenses/ .
 * --------------------------------------------------------------------------------
 * For further information and questions please use the web site
 *        http://www.des-testbed.net
 ******************************************************************************/
package de.fuberlin.dessert.activity;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.TabHost;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import de.fuberlin.dessert.DessertApplication;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.tasks.FileTasks;
import de.fuberlin.dessert.tasks.NativeTasks;
import de.fuberlin.service.NotificationService;

/**
 * Main activity of the application. Only holds the tab host containing the
 * running daemon, installed daemons and repository activities.
 */
public class MainActivity extends TabActivity {

	private final class InstallFilesRunner implements Runnable {

		@Override
		public void run() {

			// we clear the temporary directory every time we start the app
			FileTasks.clearTemporaryDirectory();

			// try to install the daemon startup script
			try {
				FileTasks.installStartScript();
			} catch (Exception e) {
				Log.e(LOG_TAG, "Could not install the start script", e);
				viewUpdateHandler.sendMessage(viewUpdateHandler.obtainMessage(MESSAGE_EXCEPTION_INSTALL_SCRIPT, e));
			}

			// check if libraries installation is necessary
			try {
				if (FileTasks.isLibrariesUpdateNeeded()) {
					if (FileTasks.installLibraryFiles()) {
						viewUpdateHandler.sendEmptyMessage(MESSAGE_INSTALL_LIBRARIES_SUCCESS);
					} else {
						viewUpdateHandler.sendEmptyMessage(MESSAGE_INSTALL_LIBRARIES_FAILURE);
					}
				}
			} catch (Exception e) {
				Log.e(LOG_TAG, "Could not install the start script", e);
				viewUpdateHandler.sendMessage(viewUpdateHandler.obtainMessage(MESSAGE_EXCEPTION_INSTALL_LIBRARIES, e));
			}
		}
	}

	private static final String LOG_TAG = "DESSERT -> MainActivity";

	private static final String TAB_TAG_RUNNING = "tab_running";
	private static final String TAB_TAG_INSTALLED = "tab_installed";
	private static final String TAB_TAG_TOOLS = "tab_tools";

	private static final int MESSAGE_INSTALL_LIBRARIES_SUCCESS = 1;
	private static final int MESSAGE_INSTALL_LIBRARIES_FAILURE = 2;

	private static final int MESSAGE_EXCEPTION_INSTALL_SCRIPT = 101;
	private static final int MESSAGE_EXCEPTION_INSTALL_LIBRARIES = 102;

	private NotificationService mBoundService = null;

	/**
	 * Update handler of this activity. Should be used as the receiver for
	 * messages to indicate a need for UI changes, updates or any other activity
	 * in general.
	 */
	public static class StaticHandler extends Handler {
		private final WeakReference<MainActivity> mActivity;

		public StaticHandler(MainActivity activity) {
			mActivity = new WeakReference<>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			MainActivity activity = mActivity.get();
			if (activity != null) {
				switch (msg.what) {
					case MESSAGE_INSTALL_LIBRARIES_SUCCESS: {
						Toast.makeText(activity, R.string.install_libraries_success, Toast.LENGTH_SHORT).show();
						break;
					}
					case MESSAGE_INSTALL_LIBRARIES_FAILURE: {
						Toast.makeText(activity, R.string.install_libraries_failure, Toast.LENGTH_LONG).show();
						break;
					}
					case MESSAGE_EXCEPTION_INSTALL_SCRIPT: {
						String exceptionText = activity.getString(R.string.value_unknown);
						if (msg.obj != null) {
							Exception exception = (Exception) msg.obj;
							exceptionText = exception.getMessage() != null ? exception.getMessage() : exception.toString();
						}
						String msgText = activity.getString(R.string.install_start_script_error, exceptionText);
						Toast.makeText(activity, msgText, Toast.LENGTH_LONG).show();
						break;
					}
					case MESSAGE_EXCEPTION_INSTALL_LIBRARIES: {
						String exceptionText = activity.getString(R.string.value_unknown);
						if (msg.obj != null) {
							Exception exception = (Exception) msg.obj;
							exceptionText = exception.getMessage() != null ? exception.getMessage() : exception.toString();
						}
						String msgText = activity.getString(R.string.install_libraries_error, exceptionText);
						Toast.makeText(activity, msgText, Toast.LENGTH_LONG).show();
						break;
					}
					default:
						Log.w(LOG_TAG, "Got unknown message: " + msg + ". ignoring it");
				}
				super.handleMessage(msg);
			}
		}
	}
	private final StaticHandler viewUpdateHandler = new StaticHandler(this);

	private final ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundService = ((NotificationService.LocalBinder) service).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundService = null;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// init tabs
		TabHost theTabHost = getTabHost();

		// first tab: running daemons
		theTabHost.addTab(
				theTabHost.newTabSpec(TAB_TAG_RUNNING)
						.setIndicator("Running")
						.setContent(new Intent(this, TabRunningDaemonActivity.class)));
		// second tab: installed daemons
		theTabHost.addTab(
				theTabHost.newTabSpec(TAB_TAG_INSTALLED)
						.setIndicator("Installed")
						.setContent(new Intent(this, TabInstalledDaemonsActivity.class)));
		// third tab: manage daemons
		theTabHost.addTab(
				theTabHost.newTabSpec(TAB_TAG_TOOLS)
						.setIndicator("Manage")
						.setContent(new Intent(this, TabDaemonRepositoryActivity.class)));

		// set start tab
		int startTab = 1;
		if (DessertApplication.instance.getRunningDaemon() != null) {
			startTab = 0;
		}
		if (!DessertApplication.instance.hasDaemonsInstalled()) {
			startTab = 2;
		}
		theTabHost.setCurrentTab(startTab);

		// install files and libraries
		DessertApplication.taskExecutor.execute(new InstallFilesRunner());

		// meanwhile check guess the native command files
		if (!NativeTasks.isNativeCommandsGuessed()) {
			NativeTasks.guessAndSetNativeCommandPaths();
			NativeTasks.setNativeCommandsGuessed(true);
		}

		try {
			bindService(new Intent(getApplicationContext(),  NotificationService.class), mConnection, Context.BIND_AUTO_CREATE);
		}
		catch(SecurityException e){
			Log.i("MainActivity", "Caught SecurityException: " + e.getMessage());
		}
	}

	/**
	 * Sets the currently active tab to the running tab
	 */
	public void setCurrentTabToRunning() {
		getTabHost().setCurrentTab(0);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!NativeTasks.isCheckedForNativeCommands()) {
			String[] faultyPaths = NativeTasks.checkCommandPaths();

			if (faultyPaths == null || faultyPaths.length == 0) {
				NativeTasks.setCheckedForNativeCommands(true);
			} else {
				StringBuilder sb = new StringBuilder();
				for (String faultyPath : faultyPaths) {
					sb.append('\n').append(faultyPath);
				}

				new AlertDialog.Builder(this)
						.setTitle(R.string.native_commands_not_found_title)
						.setMessage(getString(R.string.native_commands_not_found_msg, sb.toString()))
						.setPositiveButton(R.string.preferences, new AlertDialog.OnClickListener() {
							@Override
							public void onClick(DialogInterface dlg, int which) {
								startActivity(new Intent(MainActivity.this, SetupActivity.class));
								dlg.dismiss();
							}
						})
						.setNegativeButton(R.string.ignore, new AlertDialog.OnClickListener() {
							@Override
							public void onClick(DialogInterface dlg, int which) {
								NativeTasks.setCheckedForNativeCommands(true);
								dlg.dismiss();
							}
						})
						.show();
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mConnection);
	}

	public void terminate() {
		Log.i("MainActivity","terminated!!");
		super.onDestroy();
		this.finish();
	}
}
