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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.lang.ref.WeakReference;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import de.fuberlin.dessert.DessertApplication;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.event.DaemonStartStopEventListener;
import de.fuberlin.dessert.model.config.ConfigEntry;
import de.fuberlin.dessert.model.config.DaemonConfiguration;
import de.fuberlin.dessert.model.daemon.RunningDaemonInfo;
import de.fuberlin.dessert.model.daemon.InstalledDaemonInfo;
import de.fuberlin.dessert.tasks.FileTasks;

public class LaunchDaemonActivity extends Activity implements DaemonStartStopEventListener {

    private final class LaunchRunner implements Runnable {
        @Override
        public void run() {
            try {
                DessertApplication.instance.startDaemon(getIntent().getStringExtra(BUNDLE_DAEMON_ID_KEY));
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error while starting the daemon", e);
                viewUpdateHandler.sendMessage(viewUpdateHandler.obtainMessage(MESSAGE_EXCEPTION_START_DAEMON, e));
            }
            viewUpdateHandler.sendEmptyMessage(MESSAGE_STARTED_DAEMON);
        }
    }

    private static final String LOG_TAG = "LaunchDaemonActivity";

    /**
     * Key to a string containing the daemon id to be shown in the activity. To
     * be used in an Intent for this activity.
     */
    public static final String BUNDLE_DAEMON_ID_KEY = "de.fuberlin.dessert.daemonID";

    private static final int MESSAGE_STARTED_DAEMON = 1;
    private static final int MESSAGE_EXCEPTION_START_DAEMON = 101;

    /**
     * Update handler of this activity. Should be used as the receiver for
     * messages to indicate a need for UI changes, updates or any other activity
     * in general.
     */
    public static class StaticHandler extends Handler {
        private final WeakReference<LaunchDaemonActivity> mActivity;

        public StaticHandler(LaunchDaemonActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            LaunchDaemonActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case MESSAGE_STARTED_DAEMON:
                        activity.setResult(RESULT_OK);
                        Toast.makeText(activity, R.string.daemon_started, Toast.LENGTH_SHORT).show();
                        activity.finish();
                        break;
                    case MESSAGE_EXCEPTION_START_DAEMON:
                        String exceptionText = activity.getString(R.string.value_unknown);
                        if (msg.obj != null) {
                            Exception exception = (Exception) msg.obj;
                            exceptionText = exception.getMessage() != null ? exception.getMessage() : exception.toString();
                        }
                        String msgText = activity.getString(R.string.start_daemon_error, exceptionText);
                        Toast.makeText(activity, msgText, Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Log.w(LOG_TAG, "Got unknown message: " + msg + ". ignoring it");
                }
                super.handleMessage(msg);
            }
        }
    }
    private final StaticHandler viewUpdateHandler = new StaticHandler(this);

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.launch_daemon_activity, menu);

        return true;
    }

    @Override
    public void onDaemonStarted(final RunningDaemonInfo daemonInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button launchButton = (Button) findViewById(R.id.LaunchButton);
                launchButton.setEnabled(false);
            }
        });
    }

    @Override
    public void onDaemonStopped() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button launchButton = (Button) findViewById(R.id.LaunchButton);
                launchButton.setEnabled(true);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        boolean supRetVal = super.onOptionsItemSelected(menuItem);

        Log.d(LOG_TAG, "menuItem id: " + menuItem.getItemId());

        switch (menuItem.getItemId()) {
        case R.id.Load: {
            pickFlatFileLoad();

            supRetVal = true;
            break;
        }
        case R.id.Save: {
            final Dialog dialog = new Dialog(LaunchDaemonActivity.this);
            dialog.setTitle(R.string.save_options_title);
            dialog.setCancelable(true);
            dialog.setOwnerActivity(LaunchDaemonActivity.this);
            dialog.setContentView(R.layout.save_options_file);

            LinearLayout contentLayout = (LinearLayout) dialog.findViewById(R.id.ContentLayout);
            contentLayout.setMinimumWidth(1000);

            final String daemonID = getIntent().getStringExtra(BUNDLE_DAEMON_ID_KEY);
            final EditText editText = (EditText) dialog.findViewById(R.id.TargetFileText);
            editText.setText(daemonID);

            Button saveButton = (Button) dialog.findViewById(R.id.SaveButton);
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String filename = editText.getText().toString();
                    if (!filename.endsWith(".properties")) {
                        filename += ".properties";
                    }

                    final File sdDir = Environment.getExternalStorageDirectory();
                    final File targetFile = new File(sdDir, filename);

                    final SharedPreferences daemonPreferences = DessertApplication.instance.getDaemonPreferences(daemonID);
                    final DaemonConfiguration configuration = DessertApplication.instance.readConfigForDaemon(daemonID);

                    final Properties properties = new Properties();
                    for (ConfigEntry entry : configuration.getEntries()) {
                        entry.writeValueToProperties(daemonPreferences, properties);
                    }

                    if (targetFile.exists()) {
                        // ask the user if he want's to overwrite the file
                        AlertDialog.Builder alertbox = new AlertDialog.Builder(LaunchDaemonActivity.this);
                        alertbox.setMessage(R.string.ask_overwrite_file);

                        // set yes to restore the options
                        alertbox.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dlg, int which) {
                                targetFile.delete();
                                FileTasks.writePropertiesFile(targetFile, properties, daemonID);
                            }
                        });

                        // set no to do nothing
                        alertbox.setNegativeButton(R.string.no, null);

                        // display dialog
                        alertbox.show();
                    } else {
                        FileTasks.writePropertiesFile(targetFile, properties, daemonID);
                    }

                    dialog.dismiss();
                }
            });

            dialog.show();

            supRetVal = true;
            break;
        }
        case R.id.Defaults: {
            // ask the user if he really want's to to restore the default values
            AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
            alertbox.setMessage(R.string.ask_restore_defaults);

            // set yes to restore the options
            alertbox.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // get needed preferences and configuration
                    String daemonID = getIntent().getStringExtra(BUNDLE_DAEMON_ID_KEY);
                    SharedPreferences daemonPreferences = DessertApplication.instance.getDaemonPreferences(daemonID);
                    DaemonConfiguration configuration = DessertApplication.instance.readConfigForDaemon(daemonID);

                    for (ConfigEntry entry : configuration.getEntries()) {
                        entry.ensureDefaultValue(daemonPreferences, true); // force defaults
                    }

                    Intent intent = new Intent(LaunchDaemonActivity.this, LaunchDaemonActivity.class);
                    intent.putExtra(LaunchDaemonActivity.BUNDLE_DAEMON_ID_KEY, daemonID);
                    startActivity(intent);
                }
            });

            // set no to do nothing
            alertbox.setNegativeButton(R.string.no, null);

            // display dialog
            alertbox.show();

            supRetVal = true;
            break;
        }
        default:
            break;
        }

        return supRetVal;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.launch_daemon);

        // set launch button event
        Button launchButton = (Button) findViewById(R.id.LaunchButton);
        launchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                DessertApplication.taskExecutor.execute(new LaunchRunner());
            }
        });
        
        
        // get needed preferences and configuration
        String daemonID = getIntent().getStringExtra(BUNDLE_DAEMON_ID_KEY);
        SharedPreferences daemonPreferences = DessertApplication.instance.getDaemonPreferences(daemonID);
        DaemonConfiguration configuration = DessertApplication.instance.readConfigForDaemon(daemonID);
        InstalledDaemonInfo info = DessertApplication.instance.getInstalledDaemon(daemonID);
        
        // set the icon
        ImageView iconView = (ImageView) findViewById(R.id.Icon);
        Drawable icon = info.getIconDrawable();
        if (icon == null) {
            iconView.setImageDrawable(DessertApplication.defaultDaemonIcon);
        } else {
            iconView.setImageDrawable(icon);
        }
        
        // set the daemon name
        launchButton.setText("Launch " + info.getName());
        
        // generate configuration elements
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.ConfigLayout);
        linearLayout.removeAllViews();
        for (ConfigEntry entry : configuration.getEntries()) {
            entry.ensureDefaultValue(daemonPreferences, false);
            View view = entry.getView(getLayoutInflater(), daemonPreferences);
            linearLayout.addView(view);
        }
        linearLayout.requestLayout();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // unregister change listener
        DessertApplication.instance.unregisterRunningDaemonsChangedListener(this);

        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register change listener
        DessertApplication.instance.registerRunningDaemonsChangedListener(this);

        // manually fire an initial event
        if (DessertApplication.instance.getRunningDaemon() != null) {
            onDaemonStarted(DessertApplication.instance.getRunningDaemon());
        } else {
            onDaemonStopped();
        }

        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // no-op
        // we disable automatic saving of the instance state because it
        // interferes with out dynamic view creation
        // the views share a few common IDs and therefore will be set to the
        // same value on reuse of the saved state
    }

    /**
     * Let's the user pick a file from the sdcard root directory.
     */
    protected void pickFlatFileLoad() {
        // Don't show a dialog if the SD card is completely absent.
        final String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) && !Environment.MEDIA_MOUNTED.equals(state)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.no_sdcard_installed)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show();
            return;
        }

        // build list of all files in sdcard root
        List<String> names = new ArrayList<>();
        File[] files = Environment.getExternalStorageDirectory().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() || !file.getName().endsWith(".properties")) {
                    continue;
                }
                names.add(file.getName());
            }
        }
        Collections.sort(names);

        final String[] namesList = names.toArray(new String[names.size()]);
        Log.d(LOG_TAG, "Files found " + names.toString());

        // prompt user to select any file from the sdcard root
        new AlertDialog.Builder(this)
                        .setTitle(R.string.select_daemon_options_title)
                        .setItems(namesList, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface view, int position) {
                                String name = namesList[position];
                                File sourceFile = new File(Environment.getExternalStorageDirectory(), name);

                                Properties properties = FileTasks.readPropertiesFile(sourceFile);

                                String daemonID = getIntent().getStringExtra(BUNDLE_DAEMON_ID_KEY);
                                SharedPreferences daemonPreferences = DessertApplication.instance.getDaemonPreferences(daemonID);
                                DaemonConfiguration configuration = DessertApplication.instance.readConfigForDaemon(daemonID);

                                for (ConfigEntry entry : configuration.getEntries()) {
                                    entry.readValueFromProperties(daemonPreferences, properties);
                                }

                                Intent intent = new Intent(LaunchDaemonActivity.this, LaunchDaemonActivity.class);
                                intent.putExtra(LaunchDaemonActivity.BUNDLE_DAEMON_ID_KEY, daemonID);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show();
    }
}
