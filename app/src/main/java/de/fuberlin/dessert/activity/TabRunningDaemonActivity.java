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
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import de.fuberlin.dessert.DessertApplication;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.Utils;
import de.fuberlin.dessert.adapter.ManageConfigurationListAdapter;
import de.fuberlin.dessert.dialog.AboutDialog;
import de.fuberlin.dessert.event.CommandResultEventListener;
import de.fuberlin.dessert.event.DaemonStartStopEventListener;
import de.fuberlin.dessert.event.DataChangedEventListener;
import de.fuberlin.dessert.model.daemon.RunningDaemonInfo;
import de.fuberlin.dessert.model.manage.CommandLine;
import de.fuberlin.dessert.model.manage.CommandOption;
import de.fuberlin.dessert.model.manage.ManageEntry;
import de.fuberlin.dessert.model.manage.ManageEntry.ManageEntryType;
import de.fuberlin.dessert.model.manage.ManageEntryCommand;
import de.fuberlin.dessert.model.manage.ManageEntryProperty;
import de.fuberlin.dessert.tasks.FileTasks;
import de.fuberlin.dessert.tasks.NativeTasks;
import de.fuberlin.dessert.telnet.TelnetCommandMode;
import de.fuberlin.dessert.telnet.TelnetScheduler.Priority;
import de.fuberlin.dessert.telnet.jobs.CommandTelnetJob;
import de.fuberlin.dessert.telnet.jobs.PropertyTelnetJob;

/**
 * Activity to show the management entries as defined in the XML file of the
 * running daemon.
 */
public class TabRunningDaemonActivity extends ListActivity implements DaemonStartStopEventListener, DataChangedEventListener,
        CommandResultEventListener {

    private final class ResultDialog extends Dialog {

        private final String content;

        public ResultDialog(Activity owner, Context context, String title, String content) {
            super(context);
            this.content = content;

            this.setTitle(title);
            this.setOwnerActivity(owner);
            this.setCancelable(true);
            this.setContentView(R.layout.command_results);
            this.setCanceledOnTouchOutside(true);

            ScrollView contentLayout = (ScrollView) this.findViewById(R.id.ContentLayout);
            contentLayout.setMinimumWidth(1000);

            // fill result
            TextView textView = (TextView) this.findViewById(R.id.Text);
            textView.setText(this.content);
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            super.onCreateOptionsMenu(menu);

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.result_dialog, menu);

            return true;
        }

        @Override
        public boolean onMenuItemSelected(int featureId, MenuItem menuItem) {
            boolean supRetVal = super.onMenuItemSelected(featureId, menuItem);

            Log.d(LOG_TAG, "menuItem id: " + menuItem.getItemId());
            switch (menuItem.getItemId()) {
            case R.id.Save: {
                final Dialog dialog = new Dialog(TabRunningDaemonActivity.this);
                dialog.setTitle(R.string.command_results_title);
                dialog.setCancelable(true);
                dialog.setOwnerActivity(TabRunningDaemonActivity.this);
                dialog.setContentView(R.layout.save_result_file);

                final ScrollView contentLayout = (ScrollView) dialog.findViewById(R.id.ContentLayout);
                contentLayout.setMinimumWidth(1000);

                final RunningDaemonInfo runningDaemon = DessertApplication.instance.getRunningDaemon();

                final File sdDir = Environment.getExternalStorageDirectory();
                final String daemonID = runningDaemon.getDaemonID(); // des-ara-0.6
                final String dateString = DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()).toString(); // 20101031-211620

                final EditText targetEditText = (EditText) dialog.findViewById(R.id.TargetDirectoryText);
				targetEditText.setText(getString(R.string.daemon_path, sdDir.getAbsolutePath(), daemonID));

                final EditText fileEditText = (EditText) dialog.findViewById(R.id.TargetFileText);
                fileEditText.setText(getString(R.string.command_log));

                final CheckBox prependTimestampBox = (CheckBox) dialog.findViewById(R.id.TimestampCheckBox);

                final Button saveButton = (Button) dialog.findViewById(R.id.SaveButton);
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        File targetDirectory = new File(targetEditText.getText().toString());
                        File targetFile = new File(targetDirectory,
                                (prependTimestampBox.isChecked() ? (dateString + "-") : "") + fileEditText.getText().toString());

                        try {
                            FileTasks.writeStringToFile(content, targetFile);
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Could not save file", e);
                        }

                        dialog.dismiss();
                    }
                });

                dialog.show();

                supRetVal = true;
                break;
            }
            default:
                break;
            }

            return supRetVal;
        }
    }

    private static final String LOG_TAG = "TabRunningDaemonActv";

    private static final boolean USE_PROGRESS_BAR = false;
    private volatile ProgressDialog progressDialog;

    private final TelnetCommandMode[] customCommandModes = {
            TelnetCommandMode.DEFAULT,
            TelnetCommandMode.PRIVILEGED,
            TelnetCommandMode.CONFIG };

    private String lastCustomCommand = "";
    private int lastCustomCommandModeIndex = 0;

    @Override
    public void onAborted() {
        // stop progress bar and inform user
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        new AlertDialog.Builder(this)
                .setMessage(R.string.command_aborted)
                .setNegativeButton(R.string.ok, null)
                .create()
                .show();
    }

    @Override
    public void onCompleted(String[] values) {
        // hide progress
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        // show result value
        String tmp = Utils.toString(values, true);
        if (tmp.trim().length() == 0) {
            tmp = TabRunningDaemonActivity.this.getString(R.string.command_results_done);
        }
        final String result = tmp;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // create dialog
                final Dialog dialog = new ResultDialog(
                        TabRunningDaemonActivity.this,
                        TabRunningDaemonActivity.this,
                        TabRunningDaemonActivity.this.getString(R.string.command_results_title),
                        result);

                // show   
                dialog.show();
            }
        });

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.daemon_list);

        ListView listView = getListView();
        View headerView = getLayoutInflater().inflate(R.layout.manage_header_element, getListView(), false);
        listView.addHeaderView(headerView, null, false);
        setListAdapter(new ManageConfigurationListAdapter(TabRunningDaemonActivity.this));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ManageEntry entry = (ManageEntry) adapterView.getItemAtPosition(position);
                switch (entry.getType()) {
                case PROPERTY_GETTER_ONLY:
                case PROPERTY_GETTER_SETTER:
                    ManageEntryProperty propertyEntry = (ManageEntryProperty) entry;
                    handlePropertyClick(propertyEntry);
                    break;
                case COMMAND:
                    ManageEntryCommand commandEntry = (ManageEntryCommand) entry;
                    handleCommandClick(commandEntry);
                    break;
                default:
                    break;
                }
            }

        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                boolean result;

                ManageEntry entry = (ManageEntry) adapterView.getItemAtPosition(position);
                switch (entry.getType()) {
                case PROPERTY_GETTER_SETTER:
                    ManageEntryProperty propertyEntry = (ManageEntryProperty) entry;
                    handlePropertyLongClick(propertyEntry);
                    result = true;
                    break;

                case COMMAND:
                    ManageEntryCommand commandEntry = (ManageEntryCommand) entry;
                    handleCommandLongClick(commandEntry);
                    result = true;
                    break;
                default:
                    result = false;
                    break;
                }

                return result;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tab_running, menu);

        return true;
    }

    @Override
    public void onDaemonStarted(final RunningDaemonInfo daemonInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View headerView = getListView().findViewById(R.id.HeaderView);
                headerView.findViewById(R.id.NotRunningLayout).setVisibility(View.GONE);
                headerView.findViewById(R.id.RunningLayout).setVisibility(View.VISIBLE);

                ((TextView) headerView.findViewById(R.id.NameText)).setText(getString(R.string.daemon_name, daemonInfo.getName()));
                ((TextView) headerView.findViewById(R.id.VersionText)).setText(getString(R.string.daemon_version, daemonInfo.getVersion()));
                ((TextView) headerView.findViewById(R.id.PidText)).setText(getString(R.string.daemon_pid, daemonInfo.getPID()));
                ((TextView) headerView.findViewById(R.id.CliportText)).setText(getString(R.string.daemon_cli_port, daemonInfo.getCLIPort()));

                ManageConfigurationListAdapter adapter = getAdapter();

                // set adapter values 
                adapter.setValues(DessertApplication.instance.getManageForRunningDaemon());

                // queue property updates
                for (int i = 0; i < adapter.getCount(); i++) {
                    ManageEntry entry = adapter.getItem(i);
                    if (entry.getType() == ManageEntryType.PROPERTY_GETTER_ONLY
                            || entry.getType() == ManageEntryType.PROPERTY_GETTER_SETTER) {
                        handlePropertyClick((ManageEntryProperty) entry);
                    }
                }
            }
        });
    }

    @Override
    public void onDaemonStopped() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View headerView = getListView().findViewById(R.id.HeaderView);
                headerView.findViewById(R.id.NotRunningLayout).setVisibility(View.VISIBLE);
                headerView.findViewById(R.id.RunningLayout).setVisibility(View.GONE);
                getAdapter().clear();
            }
        });
    }

    @Override
    public void onDataChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getAdapter().notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onError() {
        // stop progress bar and inform user
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        new AlertDialog.Builder(this)
                .setMessage(R.string.command_error)
                .setNegativeButton(R.string.ok, null)
                .create()
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        boolean supRetVal = super.onOptionsItemSelected(menuItem);

        Log.d(LOG_TAG, "menuItem id: " + menuItem.getItemId());

        switch (menuItem.getItemId()) {
        case R.id.Refresh: {
            if (DessertApplication.instance.getRunningDaemon() == null) {
                onDaemonStopped();
            } else {
                onDaemonStarted(DessertApplication.instance.getRunningDaemon());
            }

            supRetVal = true;
            break;
        }
        case R.id.CustomCommand: {
            handleCustomCommandClick();

            supRetVal = true;
            break;
        }
        case R.id.Telnet: {
            // make sure telnet connection is not active
            DessertApplication.telnetScheduler.disconnect();

            // call telnet activity
            RunningDaemonInfo daemonInfo = DessertApplication.instance.getRunningDaemon();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("telnet://localhost:" + daemonInfo.getCLIPort() + "/#" + daemonInfo.getName())));

            supRetVal = true;
            break;
        }
        case R.id.Shutdown: {
            // query shutdown command the earliest execution time
            DessertApplication.telnetScheduler.enqueueShutdownCommand(Priority.HIGHEST);

            // user message
            Toast.makeText(TabRunningDaemonActivity.this, R.string.issued_shutdown_command, Toast.LENGTH_SHORT).show();

            supRetVal = true;
            break;
        }
        case R.id.Kill: {
            RunningDaemonInfo daemonInfo = DessertApplication.instance.getRunningDaemon();
            if (NativeTasks.killProcess(daemonInfo.getPID(), true)) {
                Toast.makeText(TabRunningDaemonActivity.this, R.string.issued_kill_command, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(TabRunningDaemonActivity.this, R.string.error_kill_command, Toast.LENGTH_LONG).show();
            }
            supRetVal = true;
            break;
        }
        case R.id.Preferences: {
            startActivity(new Intent(TabRunningDaemonActivity.this, SetupActivity.class));
            supRetVal = true;
            break;
        }
        case R.id.About: {
            new AboutDialog(this).show();

            supRetVal = true;
            break;
        }
        default:
            break;
        }

        return supRetVal;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result = super.onPrepareOptionsMenu(menu);

        menu.setGroupEnabled(R.id.DaemonGroup, DessertApplication.instance.getRunningDaemon() != null);

        return result;
    }

    private ManageConfigurationListAdapter getAdapter() {
        return (ManageConfigurationListAdapter) getListAdapter();
    }

    private void handleCommandClick(final ManageEntryCommand commandEntry) {
        // create dialog
        final Dialog dialog = new Dialog(this);
        dialog.setTitle(getString(R.string.run_command, commandEntry.getDescription()));
        dialog.setCancelable(true);
        dialog.setOwnerActivity(this);
        dialog.setContentView(R.layout.execute_command);

        final LinearLayout contentLayout = (LinearLayout) dialog.findViewById(R.id.ContentLayout);
        contentLayout.setMinimumWidth(1000);

        // fill with options
        final LinearLayout linearLayout = (LinearLayout) dialog.findViewById(R.id.OptionsLayout);
        for (CommandOption option : commandEntry.getCommandOptions()) {
            View view = option.getView(getLayoutInflater());
            linearLayout.addView(view);
        }

        // hook events
        final Button executeButton = (Button) dialog.findViewById(R.id.ExecuteButton);
        executeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // build options map
                Map<String, String> optionsMap = new HashMap<>();
                for (CommandOption option : commandEntry.getCommandOptions()) {
                    optionsMap.put(option.getName().toUpperCase(), option.getValue());
                }

                // build job
                CommandTelnetJob job = new CommandTelnetJob(TabRunningDaemonActivity.this);
                for (CommandLine command : commandEntry.getCommands()) {
                    String commandString = prepareCommandString(command.getCommandLine(), optionsMap);
                    job.addCommand(commandString, command.getModes());
                }

                // queue job
                DessertApplication.telnetScheduler.enqueueJob(job);

                // dialog is done
                dialog.dismiss();

                // start a progress bar
                if (USE_PROGRESS_BAR) {
                    String title = TabRunningDaemonActivity.this.getString(R.string.command_progress_title);
                    String msg = TabRunningDaemonActivity.this.getString(R.string.command_progress_message, commandEntry.getDescription());
                    TabRunningDaemonActivity.this.progressDialog =
                            ProgressDialog.show(TabRunningDaemonActivity.this, title, msg, true, true,
                                    new ProgressDialog.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dlg) {
                                            // we cut the connection here
                                            DessertApplication.telnetScheduler.disconnect();
                                        }
                                    });
                }
            }
        });

        // show   
        dialog.show();
    }

    private void handleCommandLongClick(final ManageEntryCommand commandEntry) {
        handleCommandClick(commandEntry);
    }

    private void handleCustomCommandClick() {
        // create dialog
        final Dialog dialog = new Dialog(this);
        dialog.setTitle(R.string.run_custom_command);
        dialog.setCancelable(true);
        dialog.setOwnerActivity(this);
        dialog.setContentView(R.layout.execute_command);

        LinearLayout contentLayout = (LinearLayout) dialog.findViewById(R.id.ContentLayout);
        contentLayout.setMinimumWidth(1000);

        // fill with options
        final EditText cmdEditText = new EditText(this);
        cmdEditText.setText(lastCustomCommand);
        cmdEditText.setInputType(InputType.TYPE_CLASS_TEXT);

        final int[] selectedModeIndex = { lastCustomCommandModeIndex };
        final EditText modeSelectionView = new EditText(this);
        modeSelectionView.setText(customCommandModes[selectedModeIndex[0]].toString());
        modeSelectionView.setInputType(InputType.TYPE_NULL);
        modeSelectionView.setCursorVisible(false);
        modeSelectionView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_more, 0, 0, 0);
        modeSelectionView.setFocusable(false);
        modeSelectionView.setFocusableInTouchMode(false);
        modeSelectionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final String[] modeStrings = new String[customCommandModes.length];
                for (int i = 0; i < customCommandModes.length; i++) {
                    modeStrings[i] = customCommandModes[i].toString();
                }

                new AlertDialog.Builder(TabRunningDaemonActivity.this)
                        .setTitle(R.string.choose_value)
                        .setCancelable(true)
                        .setItems(modeStrings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dlg, int which) {
                                modeSelectionView.setText(modeStrings[which]);
                                selectedModeIndex[0] = which;
                            }
                        })
                        .show();
            }
        });

        LinearLayout linearLayout = (LinearLayout) dialog.findViewById(R.id.OptionsLayout);
        linearLayout.setPadding(5, 5, 5, 5);
        linearLayout.addView(cmdEditText);
        linearLayout.addView(modeSelectionView);

        // hook events
        Button executeButton = (Button) dialog.findViewById(R.id.ExecuteButton);
        executeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // build job
                String commandString = cmdEditText.getText().toString();
                CommandTelnetJob job = new CommandTelnetJob(TabRunningDaemonActivity.this);
                job.addCommand(commandString, EnumSet.of(customCommandModes[selectedModeIndex[0]]));
                lastCustomCommand = commandString; // remember last command
                lastCustomCommandModeIndex = selectedModeIndex[0]; // remember last command mode

                // queue job
                DessertApplication.telnetScheduler.enqueueJob(job);

                // dialog is done
                dialog.dismiss();

                // start a progress bar
                if (USE_PROGRESS_BAR) {
                    String title = TabRunningDaemonActivity.this.getString(R.string.command_progress_title);
                    String customCommandString = TabRunningDaemonActivity.this.getString(R.string.custom_command);
                    String msg = TabRunningDaemonActivity.this.getString(R.string.command_progress_message, customCommandString);
                    TabRunningDaemonActivity.this.progressDialog =
                            ProgressDialog.show(TabRunningDaemonActivity.this, title, msg, true, true,
                                    new ProgressDialog.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dlg) {
                                            // we cut the connection here
                                            DessertApplication.telnetScheduler.disconnect();
                                        }
                                    });
                }
            }
        });

        // show   
        dialog.show();
    }

    private void handlePropertyClick(ManageEntryProperty propertyEntry) {
        // set querying state
        propertyEntry.setQuerying(true);

        // update view
        onDataChanged();

        // enqueue property getter command
        PropertyTelnetJob job = new PropertyTelnetJob(propertyEntry, TabRunningDaemonActivity.this,
                propertyEntry.getGetterCommand().getCommandLine(), propertyEntry.getGetterCommand().getModes());
        DessertApplication.telnetScheduler.enqueueJob(job);
    }

    private void handlePropertyLongClick(final ManageEntryProperty propertyEntry) {
        // create dialog
        final Dialog dialog = new Dialog(this);
        dialog.setTitle(getString(R.string.set_property, propertyEntry.getDescription()));
        dialog.setCancelable(true);
        dialog.setOwnerActivity(this);
        dialog.setContentView(R.layout.execute_command);

        LinearLayout contentLayout = (LinearLayout) dialog.findViewById(R.id.ContentLayout);
        contentLayout.setMinimumWidth(1000);

        // fill with options
        LinearLayout linearLayout = (LinearLayout) dialog.findViewById(R.id.OptionsLayout);
        for (CommandOption option : propertyEntry.getSetterCommandOptions()) {
            View view = option.getView(getLayoutInflater());
            linearLayout.addView(view);
        }

        // hook events
        Button executeButton = (Button) dialog.findViewById(R.id.ExecuteButton);
        executeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // build options map
                Map<String, String> optionsMap = new HashMap<>();
                for (CommandOption option : propertyEntry.getSetterCommandOptions()) {
                    optionsMap.put(option.getName().toUpperCase(), option.getValue());
                }

                // build job
                PropertyTelnetJob job = new PropertyTelnetJob(propertyEntry, TabRunningDaemonActivity.this,
                        propertyEntry.getGetterCommand().getCommandLine(), propertyEntry.getGetterCommand().getModes());
                for (CommandLine command : propertyEntry.getSetterCommands()) {
                    String commandString = prepareCommandString(command.getCommandLine(), optionsMap);
                    job.addSetterCommand(commandString, command.getModes());
                }

                // set querying state
                propertyEntry.setQuerying(true);

                // update view
                onDataChanged();

                // queue job
                DessertApplication.telnetScheduler.enqueueJob(job);

                // dialog is done
                dialog.dismiss();
            }
        });

        // show   
        dialog.show();
    }

    private String prepareCommandString(String command, Map<String, String> optionsMap) {
        StringBuilder result = new StringBuilder(command.length());

        StringBuilder variableName = new StringBuilder(64);
        boolean readingVariable = false;
        boolean gotEscape = false;
        for (char chr : command.toCharArray()) {
            if (readingVariable) {
                // either we find the end marker which might be just an escape or we append the variableName
                if (chr == '%') {
                    // if we already found an escape this is a literal % otherwise this is the first one
                    if (gotEscape) {
                        variableName.append(chr);
                        gotEscape = false;
                    } else {
                        gotEscape = true;
                    }
                } else {
                    // if we already found an escape then it was the end otherwise this is just part of the variable name
                    if (gotEscape) {
                        readingVariable = false;
                        gotEscape = false;
                        // write var and push last character
                        String varName = variableName.toString().toUpperCase();
                        variableName.setLength(0);

                        // get from map or use fallback and don't replace
                        String value = optionsMap.get(varName);
                        if (value == null) {
                            value = "%" + varName + "%";
                        }

                        result.append(value);
                        result.append(chr); // need to write the latest character too
                    } else {
                        variableName.append(chr);
                    }
                }
            } else {
                // either we find the start of a variable or just push through
                if (chr == '%') {
                    readingVariable = true;
                } else {
                    result.append(chr);
                }
            }
        }

        // in case of the last character
        if (readingVariable && gotEscape) {
            // write var and push last character
            String varName = variableName.toString().toUpperCase();

            // get from map or use fallback and don't replace
            String value = optionsMap.get(varName);
            if (value == null) {
                value = "%" + varName + "%";
            }
            result.append(value);
        }

        return result.toString();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // unregister change listener
        DessertApplication.instance.unregisterRunningDaemonsChangedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register change listener
        DessertApplication.instance.registerRunningDaemonsChangedListener(this);

        if (DessertApplication.instance.getRunningDaemon() == null) {
            onDaemonStopped();
        } else {
            onDaemonStarted(DessertApplication.instance.getRunningDaemon());
        }
    }
}
