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
package de.fuberlin.dessert.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Pattern;

import android.content.SharedPreferences;
import android.util.Log;
import de.fuberlin.dessert.DessertApplication;
import de.fuberlin.dessert.Utils;
import de.fuberlin.dessert.activity.SetupActivity;
import de.fuberlin.dessert.model.ApplicationVersion;

/**
 * Collection of native tasks implemented as a native library libNativeTask.so
 * 
 * @author Ramin Baradari
 */
public class NativeTasks {

    private static final String LOG_TAG = "DESSERT -> NativeTasks";

    private static final String OPT_CHECKED_NATIVE_COMMANDS = "checked.native.commands";
    private static final String OPT_GUESSED_NATIVE_COMMANDS = "guessed.native.commands";

    private static final String DEFAULT_PROGRAM_PATH = "/dev/null";
    private static final String PROCESS_PATH = "/proc";
    private static final String PROCESS_CMDLINE_FILE = "cmdline";

    private static final String[] PATH_PREFIX_ALTERNATIVES = { "/system/xbin/", "/system/bin/", "/system/sbin/", "/sbin/", "/xbin/" };
    private static final String[][] PATH_COMMAND_NAMES_AND_KEYS = {
            { "sh", SetupActivity.KEY_PATH_SH },
            { "su", SetupActivity.KEY_PATH_SU },
            { "ln", SetupActivity.KEY_PATH_LN },
            { "kill", SetupActivity.KEY_PATH_KILL },
            { "chmod", SetupActivity.KEY_PATH_CHMOD } };

    static {
        try {
            Log.i("NativeTask", "Trying to load libNativeTask.so");
            System.loadLibrary("NativeTask");
        } catch (UnsatisfiedLinkError ule) {
            Log.e("NativeTask", "Could not load libNativeTask.so");
        }
    }

    /**
     * Checks the configured paths to the commands and returns an array of any
     * command that is not found
     * 
     * @return array of command paths that are not found
     */
    public static String[] checkCommandPaths() {
        ArrayList<String> paths = new ArrayList<>();
        paths.add(getPathToCHMOD());
        paths.add(getPathToKILL());
        paths.add(getPathToLN());
        paths.add(getPathToSH());
        paths.add(getPathToSU());
        Collections.sort(paths);

        for (Iterator<String> it = paths.iterator(); it.hasNext();) {
            File path = new File(it.next());
            if (path.exists() && path.isFile()) {
                it.remove();
            }
        }

        return paths.isEmpty() ? null : paths.toArray(new String[paths.size()]);
    }

    /**
     * Change the file system mode of the given <code>file</code> to the given
     * <code>mode</code>. The process be can optionally run as a super user.
     * 
     * @param file file to change modes
     * @param mode mode string to set for the file
     * @param runAsSU if <code>true</code> runs the process as super user
     * @return the return code of the chmod process
     */
    public static int chmod(File file, String mode, boolean runAsSU) {
        String sb = getPathToCHMOD() + " " + mode +
                " " + file.getAbsolutePath();

        return NativeTasks.runCommand(sb, runAsSU);
    }

    /**
     * Create a subprocess.
     * 
     * @param command The command to execute
     * @return the PID of the subprocess
     */
    public static int createSubprocess(String command) {
        return createSubprocess(command, null, null, null);
    }

    /**
     * Create a subprocess.
     * 
     * @param command The command to execute
     * @param arg0 The first argument to the command, may be null
     * @return the PID of the subprocess
     */
    public static int createSubprocess(String command, String arg0) {
        return createSubprocess(command, arg0, null, null);
    }

    /**
     * Create a subprocess.
     * 
     * @param command The command to execute
     * @param arg0 The first argument to the command, may be null
     * @param arg1 the second argument to the command, may be null
     * @return the PID of the subprocess
     */
    public static int createSubprocess(String command, String arg0, String arg1) {
        return createSubprocess(command, arg0, arg1, null);
    }

    /**
     * Create a subprocess.
     * 
     * @param command The command to execute
     * @param arg0 The first argument to the command, may be null
     * @param arg1 the second argument to the command, may be null
     * @param arg2 the second argument to the command, may be null
     * @return the PID of the subprocess
     */
    public static native int createSubprocess(String command, String arg0, String arg1, String arg2);

    /**
     * Retrieves a string containing the value of the environment variable whose
     * name is specified as argument <code>key</code>. If the requested variable
     * is not part of the environment list, the function returns
     * <code>null</code>.
     * 
     * @param key
     * @return value of the enviromnent variable or <code>null</code>
     */
    public static native String getEnvironmentValue(String key);

    /**
     * Guesses the paths to the native commands and sets them to the application
     * global preferences
     */
    public static void guessAndSetNativeCommandPaths() {
        SharedPreferences prefs = DessertApplication.instance.getApplicationPreferences();

        // for each command
        for (String[] nameAndKey : PATH_COMMAND_NAMES_AND_KEYS) {
            String name = nameAndKey[0];
            String key = nameAndKey[1];

            // try each path prefix
            for (String prefix : PATH_PREFIX_ALTERNATIVES) {
                File commandFile = new File(prefix, name);
                if (commandFile.exists() && commandFile.isFile()) {
                    // and set to prefs if found
                    prefs.edit().putString(key, commandFile.getAbsolutePath()).apply();
                    break;
                }
            }
        }
    }

    /**
     * @return <code>true</code> if
     *         {@link #setCheckedForNativeCommands(boolean)} was already called
     *         for the current application version with the value
     *         <code>true</code>
     */
    public static boolean isCheckedForNativeCommands() {
        String lastCheck = DessertApplication.instance.getApplicationPreferences().getString(OPT_CHECKED_NATIVE_COMMANDS, "");
        return DessertApplication.instance.getApplicationVersion().compareTo(ApplicationVersion.getVersionFromString(lastCheck)) == 0;
    }

    /**
     * @return <code>true</code> if {@link #setNativeCommandsGuessed(boolean)}
     *         was already called the value <code>true</code>
     */
    public static boolean isNativeCommandsGuessed() {
        return DessertApplication.instance.getApplicationPreferences().getBoolean(OPT_GUESSED_NATIVE_COMMANDS, false);
    }

    /**
     * Test if the process with the given <code>pid</code> is still running.
     * <p>
     * You can supply an optional <code>regex</code> string to check if the pid
     * might be a reused one. The cmdline of the pid is tested if it matches the
     * regular expression <code>regex</code>.
     * 
     * @param pid the pid to check for
     * @param regex substring of the command line to test if the pid may be
     *            reused; may be null
     * @return <code>true</code> if a process with the <code>pid</code> is
     *         currently running
     */
    public static boolean isProcessRunning(int pid, String regex) {
        File pidDirectory = new File(PROCESS_PATH, Integer.toString(pid));

        if (!pidDirectory.exists() || !pidDirectory.isDirectory()) {
            return false;
        }

        if (regex != null) {
            File cmdlineFile = new File(pidDirectory, PROCESS_CMDLINE_FILE);
            if (cmdlineFile.exists() || cmdlineFile.canRead()) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(cmdlineFile), 512);
                    String line = reader.readLine();
                    if (!Pattern.matches(regex, line)) {
                        Log.d(LOG_TAG, "PID " + pid + " was reused; cmdline: " + line);
                        return false;
                    }
                } catch (FileNotFoundException e) {
                    Log.e(LOG_TAG, "cmdline file not found although it was here second ago", e);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "cmdline file not readable although it was a second ago", e);
                } finally {
                    Utils.safelyClose(reader);
                }
            } else {
                Log.w(LOG_TAG, "cmdline file does not exists for pid " + cmdlineFile.getAbsolutePath());
            }
        }

        return true;
    }

    /**
     * Issues a kill of the given <code>pid</code> by calling the native command
     * <i>kill</i>.
     * 
     * @param pid pid to kill
     * @param runAsSU if <code>true</code> runs the process as super user
     * @return <code>true</code> if the result code was 0 otherwise
     *         <code>false</code>
     */
    public static boolean killProcess(int pid, boolean runAsSU) {
        int result = NativeTasks.runCommand(getPathToKILL() + " " + Integer.toString(pid), runAsSU);

        return result == 0;
    }

    /**
     * Creates a symbolic <code>linkFile</code> pointing to the
     * <code>targetFile</code> by calling the native command <i>ln</i>.
     * 
     * @param targetFile link file to create
     * @param linkFile file to link to
     * @param runAsSU if <code>true</code> runs the process as super user
     * @return the return code of the called command
     */
    public static int ln(File targetFile, File linkFile, boolean runAsSU) {
        String sb = getPathToLN() + " " + targetFile.getAbsolutePath() +
                " " + linkFile.getAbsolutePath();

        return NativeTasks.runCommand(sb, runAsSU);
    }

    /**
     * Invokes the command processor to execute the given <code>command</code>.
     * Once the command execution has terminated, the processor gives the
     * control back to the program, returning an int value, whose interpretation
     * is system-dependent.
     * 
     * @param command command to be processed
     * @return return code of the executed command
     */
    public static native int runCommand(String command);

    /**
     * Invokes the command processor to execute the given <code>command</code>.
     * Once the command execution has terminated, the processor gives the
     * control back to the program, returning an int value, whose interpretation
     * is system-dependent.
     * 
     * @param command to be processed
     * @param runAsSU if <code>true</code> runs the process as super user
     * @return return code of the executed command
     */
    public static int runCommand(String command, boolean runAsSU) {
        StringBuilder sb = new StringBuilder();

        if (runAsSU && command != null) {
            sb.append(getPathToSU());
            sb.append(" -c '");
            sb.append(command);
            sb.append("'");
        } else {
            sb.append(command);
        }

        return NativeTasks.runCommand(sb.toString());
    }

    /**
     * Runs the shell script <code>scriptFile</code> with the given arguments
     * <code>args</code>. The process be can optionally run as a super user.
     * 
     * @param scriptFile the script file to execute
     * @param args arguments to pass to the script
     * @param runAsSU if <code>true</code> runs the process as super user
     * @return the return code of the shell script
     */
    public static int runShellScript(File scriptFile, String[] args, boolean runAsSU) {
        StringBuilder sb = new StringBuilder(getPathToSH());
        sb.append(" ").append(scriptFile.getAbsolutePath());

        if (args != null) {
            for (String arg : args) {
                sb.append(" ").append(arg);
            }
        }

        return NativeTasks.runCommand(sb.toString(), runAsSU);
    }

    /**
     * Set a mark that the native commands have been checked for the current
     * version.
     * 
     * @param isChecked is the check done
     */
    public static void setCheckedForNativeCommands(boolean isChecked) {
        SharedPreferences prefs = DessertApplication.instance.getApplicationPreferences();
        prefs.edit().putString(OPT_CHECKED_NATIVE_COMMANDS,
                        isChecked ? DessertApplication.instance.getApplicationVersion().toString() : "").apply();
    }

    /**
     * Set a mark that the native commands have been guessed
     * 
     * @param isChecked is the check done
     */
    public static void setNativeCommandsGuessed(boolean isChecked) {
        SharedPreferences prefs = DessertApplication.instance.getApplicationPreferences();
        prefs.edit().putBoolean(OPT_GUESSED_NATIVE_COMMANDS, isChecked).apply();
    }

    /**
     * Causes the calling thread to wait for the process associated with the
     * receiver to finish executing.
     * <p>
     * Only works for immediate processes created by calling any of the
     * <code>createSubprocess</code> functions.
     * 
     * @param pid pid to wait on
     * @return The exit value of the Process being waited on
     */
    public static native int waitFor(int pid);

    /**
     * @return path to chmod command as stored in the application preferences
     */
    private static String getPathToCHMOD() {
        SharedPreferences prefs = DessertApplication.instance.getApplicationPreferences();
        return prefs.getString(SetupActivity.KEY_PATH_CHMOD, DEFAULT_PROGRAM_PATH);
    }

    /**
     * @return path to kill command as stored in the application preferences
     */
    private static String getPathToKILL() {
        SharedPreferences prefs = DessertApplication.instance.getApplicationPreferences();
        return prefs.getString(SetupActivity.KEY_PATH_KILL, DEFAULT_PROGRAM_PATH);
    }

    /**
     * @return path to ln command as stored in the application preferences
     */
    private static String getPathToLN() {
        SharedPreferences prefs = DessertApplication.instance.getApplicationPreferences();
        return prefs.getString(SetupActivity.KEY_PATH_LN, DEFAULT_PROGRAM_PATH);
    }

    /**
     * @return path to sh command as stored in the application preferences
     */
    private static String getPathToSH() {
        SharedPreferences prefs = DessertApplication.instance.getApplicationPreferences();
        return prefs.getString(SetupActivity.KEY_PATH_SH, DEFAULT_PROGRAM_PATH);
    }

    /**
     * @return path to su command as stored in the application preferences
     */
    private static String getPathToSU() {
        SharedPreferences prefs = DessertApplication.instance.getApplicationPreferences();
        return prefs.getString(SetupActivity.KEY_PATH_SU, DEFAULT_PROGRAM_PATH);
    }

    /**
     * Hidden constructor
     */
    public NativeTasks() {
        // not allowed because this is only a collection of static methods
    }
}
