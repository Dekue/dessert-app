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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import de.fuberlin.dessert.DessertApplication;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.adapter.DaemonListAdapter;
import de.fuberlin.dessert.dialog.AboutDialog;
import de.fuberlin.dessert.model.daemon.DaemonInfo;
import de.fuberlin.dessert.model.daemon.RepositoryDaemonInfo;

public class TabDaemonRepositoryActivity extends ListActivity {

    private final class DaemonInstallRunner implements Runnable {

        private final URL url;

        public DaemonInstallRunner(URL url) {
            this.url = url;
        }

        @Override
        public void run() {
            try {
                viewUpdateHandler.sendEmptyMessage(MESSAGE_INSTALL_STARTED);
                if (DessertApplication.instance.installDaemonFromURL(url)) {
                    viewUpdateHandler.sendEmptyMessage(MESSAGE_INSTALL_SUCCESS);
                } else {
                    viewUpdateHandler.sendEmptyMessage(MESSAGE_INSTALL_FAILURE);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Could not install the daemon", e);
                viewUpdateHandler.sendMessage(viewUpdateHandler.obtainMessage(MESSAGE_EXCEPTION_INSTALL_DAEMON, e));
            }
        }
    }

    private final class ListRetrieverRunner implements Runnable {
        @Override
        public void run() {
            viewUpdateHandler.sendEmptyMessage(MESSAGE_CLEAR_LIST);

            try {
                List<RepositoryDaemonInfo> daemons = DessertApplication.instance.getRepositoryDaemons();
                viewUpdateHandler.sendMessage(viewUpdateHandler.obtainMessage(MESSAGE_APPEND_LIST, daemons));
            } catch (Exception e) {
                Log.e(LOG_TAG, "Could not get the repository daemon list", e);
                viewUpdateHandler.sendMessage(viewUpdateHandler.obtainMessage(MESSAGE_EXCEPTION_REPO_QUERY, e));
            }
        }
    }

    private static final String LOG_TAG = "DESSERT -> TabDaemonRepositoryActivity";

    private static final int REQUEST_CODE_PICK_ZIP_FILE = 0;

    private static final int MESSAGE_CLEAR_LIST = 1;
    private static final int MESSAGE_APPEND_LIST = 2;
    private static final int MESSAGE_INSTALL_STARTED = 3;
    private static final int MESSAGE_INSTALL_SUCCESS = 4;
    private static final int MESSAGE_INSTALL_FAILURE = 5;

    private static final int MESSAGE_EXCEPTION_REPO_QUERY = 101;
    private static final int MESSAGE_EXCEPTION_INSTALL_DAEMON = 102;

    private ProgressDialog installProgressDialog = null;

    /**
     * Update handler of this activity. Should be used as the receiver for
     * messages to indicate a need for UI changes, updates or any other activity
     * in general.
     */
    public final Handler viewUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_CLEAR_LIST: {
                getAdapter().clear();
                break;
            }
            case MESSAGE_APPEND_LIST: {
                @SuppressWarnings("unchecked")
                List<RepositoryDaemonInfo> daemons = (List<RepositoryDaemonInfo>) msg.obj;
                Collections.sort(daemons, DaemonInfo.DAEMON_LIST_COMPARATOR);
                DaemonListAdapter<RepositoryDaemonInfo> adapter = getAdapter();
                for (RepositoryDaemonInfo daemon : daemons) {
                    adapter.add(daemon);
                }
                break;
            }
            case MESSAGE_INSTALL_STARTED: {
                installProgressDialog = ProgressDialog.show(
                        TabDaemonRepositoryActivity.this,
                        getString(R.string.install_progress_title),
                        getString(R.string.install_progress_msg_download),
                        true,
                        false);
                break;
            }
            case MESSAGE_INSTALL_SUCCESS: {
                if (installProgressDialog != null) {
                    installProgressDialog.dismiss();
                }
                Toast.makeText(TabDaemonRepositoryActivity.this, R.string.install_daemon_success, Toast.LENGTH_SHORT).show();
                getAdapter().notifyDataSetChanged();
                break;
            }
            case MESSAGE_INSTALL_FAILURE: {
                if (installProgressDialog != null) {
                    installProgressDialog.dismiss();
                }
                Toast.makeText(TabDaemonRepositoryActivity.this, R.string.install_daemon_failure, Toast.LENGTH_SHORT).show();
                break;
            }
            case MESSAGE_EXCEPTION_REPO_QUERY: {
                String exceptionText = getString(R.string.value_unknown);
                if (msg.obj != null) {
                    Exception exception = (Exception) msg.obj;
                    exceptionText = exception.getMessage() != null ? exception.getMessage() : exception.toString();
                }
                String msgText = getString(R.string.getting_repo_error, exceptionText);
                Toast.makeText(TabDaemonRepositoryActivity.this, msgText, Toast.LENGTH_LONG).show();
                break;
            }
            case MESSAGE_EXCEPTION_INSTALL_DAEMON: {
                String exceptionText = getString(R.string.value_unknown);
                if (msg.obj != null) {
                    Exception exception = (Exception) msg.obj;
                    exceptionText = exception.getMessage() != null ? exception.getMessage() : exception.toString();
                }
                String msgText = getString(R.string.install_daemon_error, exceptionText);
                Toast.makeText(TabDaemonRepositoryActivity.this, msgText, Toast.LENGTH_LONG).show();
                break;
            }
            default:
                Log.w(LOG_TAG, "Got unknown message: " + msg + ". ignoring it");
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.daemon_list);

        List<RepositoryDaemonInfo> listContent = new ArrayList<>();
        DaemonListAdapter<RepositoryDaemonInfo> adapter = new DaemonListAdapter<>(this, listContent, true, false);
        setListAdapter(adapter);

        ListView listView = getListView();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (getAdapter() == parent.getAdapter()) {
                    final RepositoryDaemonInfo daemonInfo = getAdapter().getItem(position);

                    // ask the user if he really want's to install it
                    AlertDialog.Builder alertbox = new AlertDialog.Builder(TabDaemonRepositoryActivity.this);
                    alertbox.setMessage(getString(R.string.ask_install_daemon, daemonInfo.getName(),
                            daemonInfo.getVersion(), daemonInfo.getPackageURL()));

                    // set yes to install
                    alertbox.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            // start runner
                            DessertApplication.taskExecutor.execute(new DaemonInstallRunner(daemonInfo.getPackageURL()));
                        }
                    });

                    // set no to do nothing
                    alertbox.setNegativeButton(R.string.no, null);

                    // display dialog
                    alertbox.show();
                }
            }
        });

        //
        registerForContextMenu(listView);

        // start runner
        Toast.makeText(TabDaemonRepositoryActivity.this, R.string.getting_repo_index, Toast.LENGTH_SHORT).show();
        DessertApplication.taskExecutor.execute(new ListRetrieverRunner());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tab_repository, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        boolean supRetVal = super.onOptionsItemSelected(menuItem);

        Log.d(LOG_TAG, "menuItem id: " + menuItem.getItemId());

        switch (menuItem.getItemId()) {
        case R.id.Refresh:
            // start runner
            Toast.makeText(TabDaemonRepositoryActivity.this, R.string.getting_repo_index, Toast.LENGTH_LONG).show();
            DessertApplication.taskExecutor.execute(new ListRetrieverRunner());

            supRetVal = true;
            break;
        case R.id.InstallFile:
            pickFlatFileInstall();

            supRetVal = true;
            break;
        case R.id.Preferences:
            startActivity(new Intent(TabDaemonRepositoryActivity.this, SetupActivity.class));

            supRetVal = true;
            break;
        case R.id.About:
            new AboutDialog(this).show();

            supRetVal = true;
            break;
        default:
            break;
        }

        return supRetVal;
    }

    public void tryInstallFromFile(File daemonZip) {
        if (DessertApplication.instance.installDaemonFromZip(daemonZip)) {
            Toast.makeText(this, getString(R.string.install_daemon_success), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getString(R.string.install_daemon_failure), Toast.LENGTH_LONG).show();
        }
    }

    @SuppressWarnings("unchecked")
    protected DaemonListAdapter<RepositoryDaemonInfo> getAdapter() {
        return (DaemonListAdapter<RepositoryDaemonInfo>) getListAdapter();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
        case REQUEST_CODE_PICK_ZIP_FILE: {
            if (resultCode == RESULT_OK && intent != null) {
                Uri uri = intent.getData();
                try {
                    if (uri != null) {
                        tryInstallFromFile(new File(URI.create(uri.toString())));
                    } else {
                        String filename = intent.getDataString();
                        if (filename != null) {
                            tryInstallFromFile(new File(URI.create(filename)));
                        }
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(LOG_TAG, "Exception while reading from file " + (uri == null ? intent.getDataString() : uri), e);
                }
            }
            break;
        }
        default:
            break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // make sure to reflect the current state of installed daemons when the activity becomes active
        getAdapter().notifyDataSetChanged();
    }

    /**
     * Let's the user pick a file from the sdcard root directory.
     */
    protected void pickFlatFileInstall() {
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
                if (file.isDirectory() || !file.getName().endsWith(".zip")) {
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
                        .setTitle(R.string.select_daemon_zip_title)
                        .setItems(namesList, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface view, int position) {
                                String name = namesList[position];
                                tryInstallFromFile(new File(Environment.getExternalStorageDirectory(), name));
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show();
    }

}
