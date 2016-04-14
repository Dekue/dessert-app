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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;
import de.fuberlin.dessert.DessertApplication;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.adapter.DaemonListAdapter;
import de.fuberlin.dessert.dialog.AboutDialog;
import de.fuberlin.dessert.model.daemon.DaemonInfo;
import de.fuberlin.dessert.model.daemon.InstalledDaemonInfo;
import de.fuberlin.dessert.model.daemon.RunningDaemonInfo;

public class TabInstalledDaemonsActivity extends ListActivity {

    private final class ListRetrieverRunner implements Runnable {
        @Override
        public void run() {
            viewUpdateHandler.sendEmptyMessage(MESSAGE_CLEAR_LIST);

            DessertApplication.instance.clearInstalledDaemonsCache();
            List<InstalledDaemonInfo> installedDaemons = DessertApplication.instance.getInstalledDaemons();
            viewUpdateHandler.sendMessage(viewUpdateHandler.obtainMessage(MESSAGE_APPEND_LIST, installedDaemons));
        }
    }

    private static final String LOG_TAG = "DESSERT -> TabInstalledDaemonsActivity";

    private static final int LAUNCH_ACTIVITY_REQUESTCODE = 1;

    private static final int MESSAGE_CLEAR_LIST = 1;
    private static final int MESSAGE_APPEND_LIST = 2;

    private static final int CONTEXT_MENU_GROUP_DEFAULT = 0;
    private static final int CONTEXT_MENU_GROUP_RUNNINGDAEMON = 1;

    private static final int CONTEXT_MENU_UNINSTALL = 0;

    /**
     * Update handler of this activity. Should be used as the receiver for
     * messages to indicate a need for UI changes, updates or any other activity
     * in general.
     */
    public final Handler viewUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_CLEAR_LIST:
                getAdapter().clear();
                break;
            case MESSAGE_APPEND_LIST:
                @SuppressWarnings("unchecked")
                List<InstalledDaemonInfo> installedDaemons = (List<InstalledDaemonInfo>) msg.obj;
                Collections.sort(installedDaemons, DaemonInfo.DAEMON_LIST_COMPARATOR);
                DaemonListAdapter<InstalledDaemonInfo> adapter = getAdapter();
                for (InstalledDaemonInfo daemon : installedDaemons) {
                    adapter.add(daemon);
                }
                break;
            default:
                Log.w(LOG_TAG, "Got unknown message: " + msg + ". ignoring it");
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case CONTEXT_MENU_UNINSTALL: {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

            InstalledDaemonInfo daemonInfo = getAdapter().getItem(info.position);
            DessertApplication.instance.uninstallDaemon(daemonInfo);
            // start runner
            DessertApplication.taskExecutor.execute(new ListRetrieverRunner());
            // show some info
            Toast.makeText(TabInstalledDaemonsActivity.this, R.string.uninstalled_daemon, Toast.LENGTH_SHORT).show();
            return true;
        }
        default:
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.daemon_list);

        List<InstalledDaemonInfo> listContent = new ArrayList<InstalledDaemonInfo>();
        DaemonListAdapter<InstalledDaemonInfo> adapter = new DaemonListAdapter<InstalledDaemonInfo>(this, listContent, false, true);
        setListAdapter(adapter);

        ListView listView = getListView();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (getAdapter() == parent.getAdapter()) {
                    InstalledDaemonInfo daemonInfo = getAdapter().getItem(position);

                    Intent intent = new Intent(TabInstalledDaemonsActivity.this, LaunchDaemonActivity.class);
                    intent.putExtra(LaunchDaemonActivity.BUNDLE_DAEMON_ID_KEY, daemonInfo.getDaemonID());
                    startActivityForResult(intent, LAUNCH_ACTIVITY_REQUESTCODE);
                }
            }
        });

        //
        registerForContextMenu(listView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        InstalledDaemonInfo selectedDaemon = getAdapter().getItem(info.position);

        // set title
        menu.setHeaderTitle(selectedDaemon.getName() + " " + selectedDaemon.getVersion());

        // add menu items
        menu.add(CONTEXT_MENU_GROUP_RUNNINGDAEMON, CONTEXT_MENU_UNINSTALL, 0, R.string.uninstall_daemon);

        // enable/disable menu groups
        RunningDaemonInfo runningDaemon = DessertApplication.instance.getRunningDaemon();
        boolean enableRunningDaemonGroup = runningDaemon == null || !runningDaemon.getDaemonID().equals(selectedDaemon.getDaemonID());
        menu.setGroupEnabled(CONTEXT_MENU_GROUP_RUNNINGDAEMON, enableRunningDaemonGroup);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tab_installed, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        boolean supRetVal = super.onOptionsItemSelected(menuItem);

        Log.d(LOG_TAG, "menuItem id: " + menuItem.getItemId());

        switch (menuItem.getItemId()) {
        case R.id.Refresh:
            // start runner
            DessertApplication.taskExecutor.execute(new ListRetrieverRunner());

            supRetVal = true;
            break;
        case R.id.Preferences:
            startActivity(new Intent(TabInstalledDaemonsActivity.this, SetupActivity.class));

            supRetVal = true;
            break;
        case R.id.About:
            new AboutDialog(this).show();

            supRetVal = true;
            break;
        case R.id.Exit:
        	this.finish();
        	supRetVal = true;
        	break;
        default:
            break;
        }

        return supRetVal;
    }

    @SuppressWarnings("unchecked")
    protected DaemonListAdapter<InstalledDaemonInfo> getAdapter() {
        return (DaemonListAdapter<InstalledDaemonInfo>) getListAdapter();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LAUNCH_ACTIVITY_REQUESTCODE && resultCode == RESULT_OK) {
            Activity parent = getParent();
            if (parent instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) parent;
                mainActivity.setCurrentTabToRunning();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // start runner
        DessertApplication.taskExecutor.execute(new ListRetrieverRunner());
    }
}
