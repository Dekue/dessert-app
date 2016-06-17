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
package de.fuberlin.dessert;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
//import java.io.RandomAccessFile;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//import java.util.regex.Pattern;
import org.xml.sax.SAXException;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import de.fuberlin.dessert.activity.SetupActivity;
import de.fuberlin.dessert.event.DaemonStartStopEventListener;
import de.fuberlin.dessert.model.ApplicationVersion;
import de.fuberlin.dessert.model.LibraryVersion;
import de.fuberlin.dessert.model.config.DaemonConfiguration;
import de.fuberlin.dessert.model.daemon.InstalledDaemonInfo;
import de.fuberlin.dessert.model.daemon.RepositoryDaemonInfo;
import de.fuberlin.dessert.model.daemon.RunningDaemonInfo;
import de.fuberlin.dessert.model.manage.ManageConfiguration;
import de.fuberlin.dessert.tasks.FileTasks;
import de.fuberlin.dessert.tasks.NativeTasks;
import de.fuberlin.dessert.tasks.XMLTasks;
import de.fuberlin.dessert.telnet.TelnetScheduler;

/**
 * This is the global application class. It serves as a central location to
 * access global services and functions.
 * <p>
 * You can always access the public static {@link #instance} to call any method
 * or use any of the services.
 * <p>
 * It is advised to schedule any asynchronous work in the {@link #taskExecutor}
 * instance. This is a single thread executor that will execute the scheduled
 * runnable objects one by one. This way you can easily do your work outside of
 * the UI while still making sure it's synchronized with any other work done
 * from another activity.
 * <p>
 * Furthermore there is the {@link #telnetScheduler} which is a priority queue
 * you can use to issue command jobs on the current telnet connection of the
 * running daemon (if any).
 */
public class DessertApplication extends Application {

    /**
     * Watch dog that checks for a given PID every second and if it stops
     * running it informs the authorities about it
     */
    private final class PIDWatchdog implements Runnable {
        private final int pid;

        public PIDWatchdog(int pid) {
            this.pid = pid;
        }

        @Override
        public void run() {
            if(Log.isLoggable(LOG_TAG, Log.DEBUG)) {
				Log.d(LOG_TAG, "Waiting for pid " + pid);
			}
            try {
                while (NativeTasks.isProcessRunning(pid, null)) {
                    Thread.sleep(1000); // sleep for 1 seconds each
                }
            } catch (InterruptedException e) {
                Log.w(LOG_TAG, "PID watcher was interrupted");
            }
	        if(Log.isLoggable(LOG_TAG, Log.DEBUG)) {
				Log.d(LOG_TAG, "Process finished pid " + pid);
			}
            setRunningDaemonStopped();
        }
    }

    private static final String LOG_TAG = "DessertApplication";

    private static final String REPO_INDEX_FILENAME = "index.xml";
    private static final String TEMP_CONFIG_FILENAME = "dessert.config";
    private static final String TEMP_PID_FILENAME = "dessert.pid";

    private static final String OPT_DAEMON_RUNNING_BOOLEAN = "running.daemon.state";
    private static final String OPT_DAEMON_ID_STRING = "running.daemon.state.id";
    private static final String OPT_DAEMON_PID_INTEGER = "running.daemon.state.pid";
    private static final String OPT_DAEMON_PORT_INTEGER = "running.daemon.state.port";

    private static final int MAX_DAEMON_START_WAIT_TIME = 2500;

    /** reference to the one and only running instance of the application */
    public static DessertApplication instance;
    /** task executor service */
    public static ExecutorService taskExecutor;
    /** telnet job scheduler */
    public static TelnetScheduler telnetScheduler;
    public static Drawable defaultDaemonIcon;

    // data cache
    private ApplicationVersion applicationVersion;
    private LibraryVersion libraryVersion;
    private Map<String, InstalledDaemonInfo> installedDaemons;
    private RunningDaemonInfo runningDaemon;

    private ManageConfiguration runningDaemonManageConfig;

    private final List<DaemonStartStopEventListener> daemonEventListeners;

    /**
     * Default constructor
     */
    public DessertApplication() {
        super();

        this.daemonEventListeners = new ArrayList<>();
    }

    /**
     * Clear the installed daemons cache
     */
    public synchronized void clearInstalledDaemonsCache() {
        if (installedDaemons != null) {
            installedDaemons.clear();
            installedDaemons = null;
        }
    }

    /**
     * @return global preferences for the application
     */
    public SharedPreferences getApplicationPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    /**
     * @return version of this application
     */
    public synchronized ApplicationVersion getApplicationVersion() {
        if (applicationVersion == null) {
            String versionName = "";

            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                versionName = packageInfo.versionName;
            } catch (NameNotFoundException e) {
                Log.e(LOG_TAG, "Could not get the versionName for the application", e);
            }
            applicationVersion = ApplicationVersion.getVersionFromString(versionName);
        }

        return applicationVersion;
    }

    /**
     * @param daemonID daemon of which to get the preferences
     * @return preferences for the daemon
     */
    public synchronized SharedPreferences getDaemonPreferences(String daemonID) {
        return getSharedPreferences(daemonID, 0);
    }

    /**
     * @param daemonID daemon to get installation information about
     * @return information about the installed daemon
     */
    public synchronized InstalledDaemonInfo getInstalledDaemon(String daemonID) {
        return readInstalledDaemons().get(daemonID);
    }

    /**
     * @return list of all currently installed daemons (might be cached)
     */
    public synchronized List<InstalledDaemonInfo> getInstalledDaemons() {
        return new ArrayList<>(readInstalledDaemons().values());
    }

    /**
     * @return get the versions of all currently installed libraries
     */
    public Map<String, LibraryVersion> getInstalledLibrariesVersions() {
        return FileTasks.getInstalledLibrariesVersions();
    }

    /**
     * @return the version of main library to be used in compatibility checks
     */
    public synchronized LibraryVersion getLibraryVersion() {
        if (libraryVersion == null) {
            libraryVersion = FileTasks.getLibraryVersion();
        }

        return libraryVersion;
    }

    /**
     * @return manage configuration for the currently running daemon
     */
    public synchronized ManageConfiguration getManageForRunningDaemon() {
        if (runningDaemon == null) {
            throw new IllegalStateException("Can not get the manage configuration if there is no running daemon");
        }

        if (runningDaemonManageConfig == null) {
            runningDaemonManageConfig = XMLTasks.readManageFile(FileTasks.getManageFile(runningDaemon.getDaemonDirectory()));
        }
        return runningDaemonManageConfig;
    }

    /**
     * Queries the configured repository location for the available daemons.
     * 
     * @return a list of the daemons in the configured repository
     * @throws IOException thrown when the repository could not be accessed
     * @throws SAXException thrown when the repository index could not be parsed
     */
    public List<RepositoryDaemonInfo> getRepositoryDaemons() throws IOException, SAXException {
        SharedPreferences globalPreferences = getApplicationPreferences();
        String repoString = globalPreferences.getString(SetupActivity.KEY_REPO_URL, null);

        if (repoString == null) {
            return Collections.emptyList();
        }

        List<RepositoryDaemonInfo> result = Collections.emptyList();
        InputStream indexStream = null;
        try {
            URL repoURL = new URL(repoString + "/" + REPO_INDEX_FILENAME);
            indexStream = repoURL.openStream();
            result = XMLTasks.readRepositoryIndex(indexStream, new URL(repoString));
        } finally {
            Utils.safelyClose(indexStream);
        }

        return result;
    }

    /**
     * @return the information about currently running daemon or
     *         <code>null</code> if none is running
     */
    public synchronized RunningDaemonInfo getRunningDaemon() {
        return runningDaemon;
    }

    /**
     * Checks if there is at least one installed daemon installed on the system.
     * 
     * @return <code>true</code> if at least one daemon is installed currently
     */
    public synchronized boolean hasDaemonsInstalled() {
        boolean result = false;

        List<File> daemonDirs = FileTasks.getDirectoriesForInstalledDaemons();
        if (!daemonDirs.isEmpty()) {
            result = true;
        }

        return result;
    }

    /**
     * Installs the daemon pack from resource location <code>url</code>.
     * 
     * @param url location of the pack
     * @return true if all went well
     */
    public synchronized boolean installDaemonFromURL(URL url) {
        boolean result = true;

        InputStream inputStream = null;
        File tmpFile = null;
        try {
            inputStream = url.openStream();

            tmpFile = new File(FileTasks.getTemporaryDir(), "tmp-daemon-zip-download.zip");
            FileTasks.writeInputStreamToFile(inputStream, tmpFile);
            result = installDaemonFromZip(tmpFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.safelyClose(inputStream);
            if (tmpFile != null) {
                if(!tmpFile.delete())
					Log.d(LOG_TAG, "Could not delete temporary file");
            }
        }

        return result;
    }

    /**
     * Installs a daemon from the given <code>daemonZip</code> file.
     * 
     * @param daemonZip zip from which to install a daemon
     * @return <code>true</code> if anything went well
     */
    public synchronized boolean installDaemonFromZip(File daemonZip) {
        clearInstalledDaemonsCache(); // to update any list
        return FileTasks.installDaemonFromZip(daemonZip);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // ensure default values are set
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

        // set instance reference
        DessertApplication.instance = this;
        DessertApplication.taskExecutor = Executors.newSingleThreadExecutor();
        DessertApplication.telnetScheduler = new TelnetScheduler();
        DessertApplication.telnetScheduler.startScheduler();
        DessertApplication.defaultDaemonIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.daemon_icon, null);

        // load running daemon state
        loadRunningDaemonState();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        Log.w(LOG_TAG, "onLowMemory called. Trying my best to free up as much as possible");

        // clear some stuff so the GC can free them
        synchronized (this) {
            applicationVersion = null;
            clearInstalledDaemonsCache();
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // get rid of instance reference
        DessertApplication.instance = null;
    }

    /**
     * Reads the configuration for the given <code>daemonID</code> string.
     * 
     * @param daemonID daemon to get the configuration
     * @return daemon configuration
     */
    public DaemonConfiguration readConfigForDaemon(String daemonID) {
        DaemonConfiguration result = null;

        InstalledDaemonInfo daemonInfo = getInstalledDaemon(daemonID);
        if (daemonInfo != null) {
            result = XMLTasks.readConfigFile(FileTasks.getConfigFile(daemonInfo.getDaemonDirectory()));
        }

        return result;
    }

    /**
     * Registers an listener to called whenever a daemon is either started or
     * stopped to run.
     * 
     * @param listener the listener to add
     */
    public void registerRunningDaemonsChangedListener(DaemonStartStopEventListener listener) {
        daemonEventListeners.add(listener);
    }

    /**
     * Starts the daemon identified by <code>daemonID</code>.
     * <p>
     * The process is as follows:
     * 
     * <pre>
     * 1. Gathers the global preferences, the daemon information and temporary file locations
     * 2. Creates an daemon configuration file from the daemon preferences and the global preferences
     * 3. Calls the daemon startup script to get the daemon running
     * 4. Waits some time for the PID file to appear
     * 5. Fills the running daemon info object
     * 6. Configures the scheduler for the newly started daemon
     * 7. Starts up a PID watch dog
     * </pre>
     * 
     * @param daemonID daemon to start
     * @return an daemon info object representing the started daemon or
     * @throws FileNotFoundException thrown when any of files to start the
     *             daemon could not be found
     * @throws IOException thrown when any I/O operation failed while starting
     *             the daemon
     * @throws Exception thrown if anything bad happens to the sad and lonely
     *             daemon process
     */
    public synchronized RunningDaemonInfo startDaemon(String daemonID) throws Exception {
        if (runningDaemon != null) {
            throw new IllegalStateException("Can not start a second daemon if there is already a running daemon");
        }

        InstalledDaemonInfo daemon = getInstalledDaemon(daemonID);

        SharedPreferences daemonPreferences = getDaemonPreferences(daemonID);
        SharedPreferences appPreferences = getApplicationPreferences();

        //
        // create config from template

        // find first unused port
        int cliPort = Utils.safelyParseInteger(appPreferences.getString(SetupActivity.KEY_CLI_PORT, "4518"), 4518);
        String sysif = appPreferences.getString(SetupActivity.KEY_SYS_IF, "tap0");
        String sysip = appPreferences.getString(SetupActivity.KEY_SYS_IP, "192.168.5.99");
        String sysmask = appPreferences.getString(SetupActivity.KEY_SYS_MASK, "255.255.255.0");
        String sysmtu = appPreferences.getString(SetupActivity.KEY_SYS_MTU, "1300");
        String meshif = appPreferences.getString(SetupActivity.KEY_MESH_IF, "tiwlan0");

        File daemonDirectory = daemon.getDaemonDirectory();
        File tmpDirectory = FileTasks.getTemporaryDir();

        File configTemplateFile = FileTasks.getConfigTemplateFile(daemonDirectory);
        File configFile = new File(tmpDirectory, TEMP_CONFIG_FILENAME);
        File pidFile = new File(tmpDirectory, TEMP_PID_FILENAME);

		if(!configFile.delete())
			Log.d(LOG_TAG, "Could not delete config file");
		if(!pidFile.delete())
			Log.d(LOG_TAG, "Could not delete pid file");

        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(configTemplateFile), 512);
            writer = new BufferedWriter(new FileWriter(configFile), 512);

            writer.write("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
            writer.write("! THIS IS A GENERATED CONFIGURATION FILE FOR DAEMON " + daemonID + "\n");
            writer.write("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
            writer.write("\n");

            writer.write("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
            writer.write("! GLOBAL VALUES\n");
            writer.write("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
            writer.write("\n");

            writer.write("no logging stderr\n");
            writer.write("no logging file\n");
            writer.write("no logging ringbuffer\n");
            writer.write("\n");
            writer.write("port " + Integer.toString(cliPort) + "\n");
            writer.write("pid " + pidFile.getAbsolutePath() + "\n");
            writer.write("\n");
            writer.write("interface sys " + sysif + " " + sysip + " " + sysmask + " " + sysmtu + "\n");
            writer.write("interface mesh " + meshif + "\n");
            writer.write("\n");

            writer.write("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
            writer.write("! DAEMON TEMPLATE VALUES\n");
            writer.write("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
            writer.write("\n");

            StringBuilder variableName = new StringBuilder(64);
            int b;
            boolean readingVariable = false;
            boolean gotEscape = false;
            while ((b = reader.read()) != -1) {
                char chr = (char) b;
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

                            // get from daemon prefs, global prefs or use fallback and don't replace
                            String value = daemonPreferences.getString(varName, null);
                            if (value == null) {
                                value = appPreferences.getString(varName, null);
                            }
                            if (value == null) {
                                value = "%" + varName + "%";
                            }

                            writer.write(value);
                            writer.write(chr); // need to write the latest character too
                        } else {
                            variableName.append(chr);
                        }
                    }
                } else {
                    // either we find the start of a variable or just push through
                    if (chr == '%') {
                        readingVariable = true;
                    } else {
                        writer.write(chr);
                    }
                }
            }

            writer.write("\n");
        } finally {
            Utils.safelyClose(writer);
            Utils.safelyClose(reader);
        }

        //
        // make config file readable
        NativeTasks.chmod(configFile, "666", false);

        // start daemon
        if (!FileTasks.callStartScript(FileTasks.getBinaryFile(daemonDirectory), configFile)) {
            throw new Exception(getString(R.string.start_daemon_script_error));
        }

        //
        try {
            int waitTime = MAX_DAEMON_START_WAIT_TIME;
            while (!pidFile.exists() && waitTime > 0) {
                wait(100);
                waitTime -= 100;
            }
        } catch (InterruptedException e) {
            Log.i(LOG_TAG, "PID wait loop interrupted", e);
        }

        if (!pidFile.exists()) {
            throw new FileNotFoundException("pid file was not found in a timely manner");
        }

        // make pid file readable
        NativeTasks.chmod(pidFile, "666", false);

        // read pid
        final int pid = FileTasks.readPIDFile(pidFile);

        // remember running daemon
        runningDaemon = new RunningDaemonInfo(daemon, pid, cliPort);
        saveRunningDaemonState();

        // re-configure telnet scheduler
        DessertApplication.telnetScheduler.setConnectionDetails(runningDaemon.getCLIPort());

        // start pid watchdog        
        new Thread(new PIDWatchdog(pid)).start();

		// return new running daemon info as inserted
		return runningDaemon;
    }

    /* Seems to be unused
	public synchronized boolean switchWiFiMode() {
		// Texas Instruments Transceiver?
		RandomAccessFile tiwlan = null;
		try {
			tiwlan = new RandomAccessFile("/system/etc/wifi/tiwlan.ini", "rw");
			Pattern p = Pattern.compile("^WiFiAdhoc = \\d.*$");
		} catch (FileNotFoundException e) {
			Log.i(LOG_TAG, "no tiwlan file found");
		}
		return false;
	}
	*/

    /**
     * Uninstall the daemon identified by <code>daemonInfo</code>. This will
     * essentially just remove any installed file of this daemon. It will not
     * remove any preferences for the daemon.
     * 
     * @param daemonInfo info object of the daemon to remove from the system
     */
    public synchronized void uninstallDaemon(InstalledDaemonInfo daemonInfo) {
        FileTasks.uninstallDaemon(daemonInfo.getDaemonDirectory());
        getDaemonPreferences(daemonInfo.getDaemonID()).edit().clear().commit();
        clearInstalledDaemonsCache(); // to update any list
    }

    /**
     * Removes a change listener from the application
     * 
     * @param listener the listener to be removed
     */
    public void unregisterRunningDaemonsChangedListener(DaemonStartStopEventListener listener) {
        daemonEventListeners.remove(listener);
    }

    /**
     * Reads the state of the running daemon (if any) as it was saved when the
     * application was last running
     */
    private synchronized void loadRunningDaemonState() {
        if (getApplicationPreferences().getBoolean(OPT_DAEMON_RUNNING_BOOLEAN, false)) {
            String daemonID = getApplicationPreferences().getString(OPT_DAEMON_ID_STRING, "");
            int pid = getApplicationPreferences().getInt(OPT_DAEMON_PID_INTEGER, -1);
            int port = getApplicationPreferences().getInt(OPT_DAEMON_PORT_INTEGER, -1);

            InstalledDaemonInfo daemon = getInstalledDaemon(daemonID);
            if (daemon != null && NativeTasks.isProcessRunning(pid, null)) {
                runningDaemon = new RunningDaemonInfo(daemon, pid, port);
                DessertApplication.telnetScheduler.setConnectionDetails(runningDaemon.getCLIPort());
                new Thread(new PIDWatchdog(pid)).start();
            }
        }
    }

    private List<InstalledDaemonInfo> readDaemonInfos() {
        List<InstalledDaemonInfo> result = new ArrayList<>();

        List<File> daemonDirs = FileTasks.getDirectoriesForInstalledDaemons();
        for (File daemonDir : daemonDirs) {
            Properties daemonProperties = FileTasks.readDaemonProperties(daemonDir);
            Drawable icon = Drawable.createFromPath(FileTasks.getIconFile(daemonDir).getAbsolutePath());
            InstalledDaemonInfo description = new InstalledDaemonInfo(daemonProperties, daemonDir, icon);
            result.add(description);
        }

        return result;
    }

    private synchronized Map<String, InstalledDaemonInfo> readInstalledDaemons() {
        if (installedDaemons == null) {
            // load daemons information
            List<InstalledDaemonInfo> list = readDaemonInfos();
            installedDaemons = new HashMap<>(list.size());

            for (InstalledDaemonInfo daemonInfo : list) {
                installedDaemons.put(daemonInfo.getDaemonID(), daemonInfo);
            }
        }

        return installedDaemons;
    }

    /**
     * Saves the information of the currently running daemon to be retrieved
     * when application starts again.
     */
    private synchronized void saveRunningDaemonState() {
        RunningDaemonInfo daemon = getRunningDaemon();
        Editor editor = getApplicationPreferences().edit();
        if (daemon == null) {
            editor.putBoolean(OPT_DAEMON_RUNNING_BOOLEAN, false);
            editor.putString(OPT_DAEMON_ID_STRING, "");
            editor.putInt(OPT_DAEMON_PID_INTEGER, -1);
            editor.putInt(OPT_DAEMON_PORT_INTEGER, -1);
        } else {
            editor.putBoolean(OPT_DAEMON_RUNNING_BOOLEAN, true);
            editor.putString(OPT_DAEMON_ID_STRING, daemon.getDaemonID());
            editor.putInt(OPT_DAEMON_PID_INTEGER, daemon.getPID());
            editor.putInt(OPT_DAEMON_PORT_INTEGER, daemon.getCLIPort());
        }
        editor.apply();
    }

    /**
     * Set the running daemon to be stopped, reset the scheduler and finally
     * signal any listener about the event
     */
    private synchronized void setRunningDaemonStopped() {
        DessertApplication.telnetScheduler.resetScheduler();
        runningDaemon = null;
        runningDaemonManageConfig = null;
        saveRunningDaemonState();
        for (DaemonStartStopEventListener listener : daemonEventListeners) {
            listener.onDaemonStopped();
        }
    }
}
