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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources.NotFoundException;
import android.util.Log;
import de.fuberlin.dessert.DessertApplication;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.Utils;
import de.fuberlin.dessert.model.ApplicationVersion;
import de.fuberlin.dessert.model.LibraryVersion;
import de.fuberlin.dessert.model.daemon.DaemonInfo;

/**
 * Collection of tasks concerning files on the Android system.
 * 
 * @author Ramin Baradari
 */
public class FileTasks {

    private static final class DaemonDirectoryFilter implements FileFilter {

        @Override
        public boolean accept(File file) {
            if (!file.isDirectory()) {
                return false;
            }

            File propFile = new File(file, DAEMON_PROPERTY_FILENAME);
            if (!propFile.exists() || !propFile.isFile() || !propFile.canRead()) {
                Log.w(LOG_TAG, "Sub directory does not contain a readable property file: " + file.getAbsolutePath());
                return false;
            }

            File tmplFile = new File(file, DAEMON_CONFIG_TEMPLATE_FILENAME);
            if (!tmplFile.exists() || !tmplFile.isFile() || !tmplFile.canRead()) {
                Log.w(LOG_TAG, "Sub directory does not contain a readable configuration template file: " + file.getAbsolutePath());
                return false;
            }

            File launcherFile = new File(file, DAEMON_LAUNCHER_XML_FILENAME);
            if (!launcherFile.exists() || !launcherFile.isFile() || !launcherFile.canRead()) {
                Log.w(LOG_TAG, "Sub directory does not contain a readable launcher configuration file: " + file.getAbsolutePath());
                return false;
            }

            File managerFile = new File(file, DAEMON_MANAGER_XML_FILENAME);
            if (!managerFile.exists() || !managerFile.isFile() || !managerFile.canRead()) {
                Log.w(LOG_TAG, "Sub directory does not contain a readable manager configuration file: " + file.getAbsolutePath());
                return false;
            }

            File binaryFile = new File(file, DAEMON_BINARY_FILENAME);
            if (!binaryFile.exists() || !binaryFile.isFile() || !binaryFile.canRead()) {
                Log.w(LOG_TAG, "Sub directory does not contain a readable binary file: " + file.getAbsolutePath());
                return false;
            }

            return true;
        }
    }

    private static final String LOG_TAG = "DESSERT -> FileTasks";

    private static final String START_DAEMON_SCRIPT = "start_daemon.sh";

    private static final String DEFAULT_FULL_MODE = "777";
    private static final String DEFAULT_LIBRARY_MODE = "777";

    private static final String LIB_PROPERTY_FILENAME = "libraries.properties";
    private static final String LIB_PROPERTY_PREFIX_LIB = "lib.";
    private static final String LIB_PROPERTY_PREFIX_SOURCE_NAME = LIB_PROPERTY_PREFIX_LIB + "source.name.";
    private static final String LIB_PROPERTY_PREFIX_TARGET_NAME = LIB_PROPERTY_PREFIX_LIB + "target.name.";
    private static final String LIB_PROPERTY_PREFIX_TARGET_VERSION = LIB_PROPERTY_PREFIX_LIB + "target.version.";
    private static final String LIB_PROPERTY_LIBRARY_VERSION = LIB_PROPERTY_PREFIX_LIB + "version";

    private static final String DAEMON_PROPERTY_FILENAME = "daemon.properties";
    private static final String DAEMON_CONFIG_TEMPLATE_FILENAME = "config.template";
    private static final String DAEMON_LAUNCHER_XML_FILENAME = "launcher.xml";
    private static final String DAEMON_MANAGER_XML_FILENAME = "manager.xml";
    private static final String DAEMON_ICON_FILENAME = "icon.png";
    private static final String DAEMON_BINARY_FILENAME = "daemon";

    private static final String LIBRARIES_VERSION_FILE = "version";
    private static final String LIBRARIES_VERSION_DEV_TAG = "dev";

    /**
     * Calls the daemon startup script for the given <code>daemonFile</code>
     * with the <code>configFile</code>.
     * 
     * @param daemonFile daemon binary to start
     * @param configFile configuration file to start with
     * @return <code>true</code> if all went well
     */
    public static boolean callStartScript(File daemonFile, File configFile) {
        File scriptFile = new File(getDaemonsDir(), START_DAEMON_SCRIPT);

        int result = NativeTasks.runShellScript(
                scriptFile,
                new String[] { daemonFile.getAbsolutePath(), configFile.getAbsolutePath(), getLibrariesDir().getAbsolutePath() },
                true);
        return result == 0;
    }

    /**
     * Clears the temporary directory by deleting all it's content (not
     * recursive)
     */
    public static void clearTemporaryDirectory() {
        clearDirectory(getTemporaryDir());
    }

    /**
     * Get the binary file of this daemon directory
     * 
     * @param directory daemon directory
     * @return binary file of daemon directory
     */
    public static File getBinaryFile(File directory) {
        return new File(directory, DAEMON_BINARY_FILENAME);
    }

    /**
     * Get the configuration file of this daemon directory
     * 
     * @param directory daemon directory
     * @return configuration file of daemon directory
     */
    public static File getConfigFile(File directory) {
        return new File(directory, DAEMON_LAUNCHER_XML_FILENAME);
    }

    /**
     * Get the configuration template file of this daemon directory
     * 
     * @param directory daemon directory
     * @return configuration template of daemon directory
     */
    public static File getConfigTemplateFile(File directory) {
        return new File(directory, DAEMON_CONFIG_TEMPLATE_FILENAME);
    }

    /**
     * Get a list of files pointing to directories containing valid
     * installations of daemons.
     * 
     * @return list of daemon directories
     */
    public static List<File> getDirectoriesForInstalledDaemons() {
        List<File> result = new ArrayList<>();
        Collections.addAll(result, getDaemonsDir().listFiles(new DaemonDirectoryFilter()));
        return result;
    }

    /**
     * Get the icon file of this daemon directory
     * 
     * @param directory daemon directory
     * @return configuration file of daemon directory
     */
    public static File getIconFile(File directory) {
        return new File(directory, DAEMON_ICON_FILENAME);
    }

    /**
     * @return versions of the currently installed libraries
     */
    public static Map<String, LibraryVersion> getInstalledLibrariesVersions() {
        Map<String, LibraryVersion> result = new HashMap<>();

        // group 1 is the library name
        // group 2 is the library version
        final Pattern libraryPattern = Pattern.compile("^([^\\.]+)\\.so\\.(\\d+\\.\\d+\\.\\d+)$");

        String libraryFilenames[] = getLibrariesDir().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return libraryPattern.matcher(filename).matches();
            }
        });

        for (String filename : libraryFilenames) {
            Matcher matcher = libraryPattern.matcher(filename);
            if (matcher.matches()) {
                String libName = matcher.group(1);
                LibraryVersion libVersion = LibraryVersion.getVersionFromString(matcher.group(2));
                result.put(libName, libVersion);
            }
        }

        return result;
    }

    /**
     * @return version of the main library
     */
    public static LibraryVersion getLibraryVersion() {
        // first read the library file descriptions
        Properties libProps = new Properties();
        InputStream inputStream = null;
        try {
            AssetManager assetManager = DessertApplication.instance.getAssets();
            inputStream = assetManager.open(LIB_PROPERTY_FILENAME);
            libProps.load(inputStream);
        } catch (NotFoundException e) {
            Log.e(LOG_TAG, "Could not find the library description property file", e);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Could not read the library description properties", e);
        } finally {
            Utils.safelyClose(inputStream);
        }

        String versionString = libProps.getProperty(LIB_PROPERTY_LIBRARY_VERSION, null);
        return LibraryVersion.getVersionFromString(versionString);
    }

    /**
     * Get the management file of this daemon directory
     * 
     * @param directory daemon directory
     * @return management file of daemon directory
     */
    public static File getManageFile(File directory) {
        return new File(directory, DAEMON_MANAGER_XML_FILENAME);
    }

    /**
     * Get the directory for temporary files. The directory is in private mode.
     * 
     * @return directory for temporary files
     */
    public static File getTemporaryDir() {
        return DessertApplication.instance.getDir("tmp", Context.MODE_PRIVATE).getAbsoluteFile();
    }

    /**
     * Installs the daemon zipped in the given file <code>daemonZip</code> into
     * the directory according to the zipped daemon description.
     * <p>
     * The layout of the ZIP file must be flat. Each file that is part of the
     * daemon must be in the zip without a sub directory.
     * <p>
     * Any additional files in the ZIP file will be ignored.
     * <p>
     * Any files residing in the target directory (e.g. an existing installation
     * of the same daemon with the same version) will be overwritten.
     * 
     * @param daemonZip zip file with daemon
     * @return <code>true</code> if installation was successful
     */
    public static boolean installDaemonFromZip(File daemonZip) {
        try {
            ZipFile zipFile = new ZipFile(daemonZip, ZipFile.OPEN_READ);

            // get all entries first
            ZipEntry propertyEntry = zipFile.getEntry(DAEMON_PROPERTY_FILENAME);
            if (propertyEntry == null) {
                Log.d(LOG_TAG, "Missing file " + DAEMON_PROPERTY_FILENAME);
                return false;
            }
            ZipEntry binaryEntry = zipFile.getEntry(DAEMON_BINARY_FILENAME);
            if (binaryEntry == null) {
                Log.d(LOG_TAG, "Missing file " + DAEMON_BINARY_FILENAME);
                return false;
            }
            ZipEntry launcherEntry = zipFile.getEntry(DAEMON_LAUNCHER_XML_FILENAME);
            if (launcherEntry == null) {
                Log.d(LOG_TAG, "Missing file " + DAEMON_LAUNCHER_XML_FILENAME);
                return false;
            }
            ZipEntry templateEntry = zipFile.getEntry(DAEMON_CONFIG_TEMPLATE_FILENAME);
            if (templateEntry == null) {
                Log.d(LOG_TAG, "Missing file " + DAEMON_CONFIG_TEMPLATE_FILENAME);
                return false;
            }
            ZipEntry managerEntry = zipFile.getEntry(DAEMON_MANAGER_XML_FILENAME);
            if (managerEntry == null) {
                Log.d(LOG_TAG, "Missing file " + DAEMON_MANAGER_XML_FILENAME);
                return false;
            }
            ZipEntry iconEntry = zipFile.getEntry(DAEMON_ICON_FILENAME);
            if (iconEntry == null) {
                Log.w(LOG_TAG, "Missing file " + DAEMON_ICON_FILENAME);
            }

            // read property file
            Properties properties = new Properties();
            properties.load(zipFile.getInputStream(propertyEntry));

            // check if all properties exist
            if (properties.getProperty(DaemonInfo.PROPERTY_NAME, null) == null) {
                Log.d(LOG_TAG, "Missing entry in properties file " + DaemonInfo.PROPERTY_NAME);
                return false;
            }
            if (properties.getProperty(DaemonInfo.PROPERTY_VERSION, null) == null) {
                Log.d(LOG_TAG, "Missing entry in properties file " + DaemonInfo.PROPERTY_VERSION);
                return false;
            }
            if (properties.getProperty(DaemonInfo.PROPERTY_APPLICATION_VERSION, null) == null) {
                Log.d(LOG_TAG, "Missing entry in properties file " + DaemonInfo.PROPERTY_APPLICATION_VERSION);
                return false;
            }
            if (properties.getProperty(DaemonInfo.PROPERTY_LIBRARY_VERSION, null) == null) {
                Log.d(LOG_TAG, "Missing entry in properties file " + DaemonInfo.PROPERTY_LIBRARY_VERSION);
                return false;
            }

            // construct daemon info object to get the daemon id
            DaemonInfo daemonInfo = new DaemonInfo(properties, null);

            // get daemon directory
            File directory = new File(getDaemonsDir(), daemonInfo.getDaemonID());
            if(!directory.mkdir()) {
				Log.d(LOG_TAG, "Could not create daemon directory / directory existed before.");
			}

            // finally install the file           
            writeZipEntryToFile(zipFile, propertyEntry, getPropertyFile(directory));
            writeZipEntryToFile(zipFile, binaryEntry, getBinaryFile(directory));
            writeZipEntryToFile(zipFile, launcherEntry, getConfigFile(directory));
            writeZipEntryToFile(zipFile, templateEntry, getConfigTemplateFile(directory));
            writeZipEntryToFile(zipFile, managerEntry, getManageFile(directory));
            if (iconEntry != null) {
                writeZipEntryToFile(zipFile, iconEntry, getIconFile(directory));
            }

            // make binary executable
            NativeTasks.chmod(getBinaryFile(directory), DEFAULT_FULL_MODE, false);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error while reading zip file", e);
            return false;
        }

        return true;
    }

    /**
     * Installs the library files as packaged in the application into the
     * libraries directory.
     *
     * @throws IOException thrown if an I/O error occurred while installing the
     *             library files
     */
    public static void installLibraryFiles() throws IOException {
        File librariesDir = getLibrariesDir();

        // empty directory
        clearDirectory(librariesDir);

        // first read the library file descriptions
        Properties libProps = new Properties();
        InputStream inputStream = null;
        try {
            AssetManager assetManager = DessertApplication.instance.getAssets();
            inputStream = assetManager.open(LIB_PROPERTY_FILENAME);
            libProps.load(inputStream);
        } catch (NotFoundException e) {
            Log.e(LOG_TAG, "Could not find the library description property file", e);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Could not read the library description properties", e);
        } finally {
            Utils.safelyClose(inputStream);
        }

        // copy each library and create the necessary links
        for (Object key : libProps.keySet()) {
            if (key != null && key.toString().startsWith(LIB_PROPERTY_PREFIX_SOURCE_NAME)) {
                String libName = key.toString().substring(LIB_PROPERTY_PREFIX_SOURCE_NAME.length());

                String sourceName = libProps.getProperty(LIB_PROPERTY_PREFIX_SOURCE_NAME + libName);
                String targetName = libProps.getProperty(LIB_PROPERTY_PREFIX_TARGET_NAME + libName);
                String targetVersion = libProps.getProperty(LIB_PROPERTY_PREFIX_TARGET_VERSION + libName);

                if (sourceName == null || targetName == null || targetVersion == null) {
                    Log.w(LOG_TAG, "Library properties are incomplete for library: " + libName + ". skipping library entries");
                    continue;
                }

                File targetFile = new File(librariesDir, targetName + "." + targetVersion);
                installAssetFile(sourceName, targetFile);

                String targetPath = targetFile.getAbsolutePath();
                String targetPathShort = targetPath.substring(0, targetPath.lastIndexOf('.'));
                String targetPathShorter = targetPathShort.substring(0, targetPathShort.lastIndexOf('.'));
                String targetPathShortest = targetPathShorter.substring(0, targetPathShorter.lastIndexOf('.'));

                NativeTasks.chmod(new File(targetPath), DEFAULT_LIBRARY_MODE, false);
                NativeTasks.ln(new File(targetPath), new File(targetPathShorter), false);
                NativeTasks.ln(new File(targetPathShorter), new File(targetPathShortest), false);
            }
        }

        // write version tag here
        File versionFile = new File(librariesDir, LIBRARIES_VERSION_FILE);
        ApplicationVersion appVersion = DessertApplication.instance.getApplicationVersion();
        writeVersionToFile(appVersion, versionFile);
    }

    /**
     * Writes the daemon startup script as found in the raw file section of the
     * application apk.
     * 
     * @throws IOException thrown when an I/O error occurred while installing
     *             the script file
     */
    public static void installStartScript() throws IOException {
        File scriptFile = new File(getDaemonsDir(), START_DAEMON_SCRIPT);
        installRawFile(scriptFile);
        NativeTasks.chmod(scriptFile, DEFAULT_FULL_MODE, false);
    }

    /**
     * @return <code>true</code> if the currently libraries installation is
     *         outdated compared to th packaged files of the current application
     */
    public static boolean isLibrariesUpdateNeeded() {
        boolean result = true;

        File librariesDir = getLibrariesDir();

        // check if update is necessary
        File versionFile = new File(librariesDir, LIBRARIES_VERSION_FILE);
        ApplicationVersion appVersion = DessertApplication.instance.getApplicationVersion();
        ApplicationVersion installedLibVersion = readAppVersionFromFile(versionFile);
        if (installedLibVersion != null && !installedLibVersion.getExtra().equalsIgnoreCase(LIBRARIES_VERSION_DEV_TAG)
                && installedLibVersion.compareTo(appVersion) == 0) {
            Log.i(LOG_TAG, "No libraries update necessary");
            result = false;
        }

        return result;
    }

    /**
     * Read properties of daemon located in the directory <code>dir</code>
     * 
     * @param dir where to look for the daemon properties file
     * @return properties of the daemon or null if it could not be found
     */
    public static Properties readDaemonProperties(File dir) {
        return readPropertiesFile(getPropertyFile(dir));
    }

    /**
     * Reads the given <code>pidFile</code> to extract a PID number from it.
     * <p>
     * The given file is expected to contain an integer number on it's first
     * line. Anything else in the file is ignored and no further sanity check is
     * done.
     * 
     * @param pidFile file to scan for the PID
     * @return the PID as stored in the file
     * @throws FileNotFoundException thrown if the file could not be found
     * @throws IOException thrown if there was a problem while accessing the
     *             file
     */
    public static int readPIDFile(File pidFile) throws IOException {
        int result = -1;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(pidFile), 512);
            String line = reader.readLine();
            result = Utils.safelyParseInteger(line, -1);
        } finally {
            Utils.safelyClose(reader);
        }

        return result;
    }

    /**
     * Reads the property file <code>propertyFile</code>.
     * 
     * @param propertiesFile which file to load
     * @return the contained properties or null if the file could not be read
     */
    public static Properties readPropertiesFile(File propertiesFile) {
        Properties result = null;

        if (propertiesFile != null && propertiesFile.exists() && propertiesFile.isFile()) {
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(propertiesFile);
                result = new Properties();
                result.load(inputStream);
            } catch (NotFoundException e) {
                Log.e(LOG_TAG, "Could not find the property file", e);
                result = null;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Could not read the properties", e);
                result = null;
            } finally {
                Utils.safelyClose(inputStream);
            }
        } else {
            Log.e(LOG_TAG, "Could not find the property file: " + propertiesFile);
        }

        return result;
    }

    /**
     * Uninstalls the daemon found in the <code>daemonDirectory</code>.
     * <p>
     * Essentially this is just a lame delete content of directory and then
     * delete the directory itself method.
     * 
     * @param daemonDirectory daemon directory to uninstall the daemon from
     */
    public static void uninstallDaemon(File daemonDirectory) {
        clearDirectory(daemonDirectory);
		if(!daemonDirectory.delete())
			Log.d(LOG_TAG, "Could not delete daemon directory.");
    }

    /**
     * Writes the content of the given input stream <code>is</code> to the
     * <code>targetFile</code>.
     * 
     * @param is stream to read from
     * @param targetFile file to write to
     * @throws IOException thrown if an I/O error occurred
     */
    public static void writeInputStreamToFile(InputStream is, File targetFile) throws IOException {
        OutputStream outStream = null;
        try {
            outStream = new FileOutputStream(targetFile);
            byte buffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) > 0) {
                outStream.write(buffer, 0, bytesRead);
            }
        } finally {
            Utils.safelyClose(outStream);
        }
    }

    /**
     * Writes the the given <code>properties</code> to the
     * <code>propertiesFile</code>.
     * <p>
     * The same as calling
     * {@link #writePropertiesFile(File, Properties, String)} with a comment
     * string of <code>null</code>.
     *
     * @param propertiesFile file to write to
     * @param properties properties to write
     * @return <code>true</code> if all went well
     */
    public static boolean writePropertiesFile(File propertiesFile, Properties properties) {
        return writePropertiesFile(propertiesFile, properties, null);
    }

    /**
     * Writes the the given <code>properties</code> to the
     * <code>propertiesFile</code>.
     * 
     * @param propertiesFile file to write to
     * @param properties properties to write
     * @param comment a comment string to set at the top of the file
     * @return <code>true</code> if all went well
     */
    public static boolean writePropertiesFile(File propertiesFile, Properties properties, String comment) {
        boolean result = false;

        if (propertiesFile != null && !propertiesFile.exists() && properties != null) {
            OutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(propertiesFile);
                properties.store(outputStream, comment == null ? "" : comment);
                result = true;
            } catch (NotFoundException e) {
                Log.e(LOG_TAG, "Could not find the property file", e);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Could not write the properties", e);
            } finally {
                Utils.safelyClose(outputStream);
            }
        }

        return result;
    }

    /**
     * Writes the given <code>text</code> to the <code>targetFile</code>.
     * 
     * @param text string to write
     * @param targetFile file to write to
     * @throws IOException thrown if an I/O error occurred while writing to the
     *             file
     */
    public static void writeStringToFile(String text, File targetFile) throws IOException {
        FileWriter writer = null;
        try {
            if (targetFile.getParentFile() != null) {
				//noinspection ResultOfMethodCallIgnored
				targetFile.getParentFile().mkdirs();
            }
            writer = new FileWriter(targetFile, true);
            writer.write(text == null ? "" : text);
        } finally {
            Utils.safelyClose(writer);
        }
    }

    /**
     * Writes the content of the given <code>zipEntry</code> packed in the given
     * <code>zipFile</code> to the <code>targetFile</code> location.
     * 
     * @param zipFile source zip file
     * @param zipEntry entry in zip file to write
     * @param targetFile target file location
     * @throws IOException thrown when the source could not be read or the
     *             target could not be written to
     */
    private static void writeZipEntryToFile(ZipFile zipFile, ZipEntry zipEntry, File targetFile) throws IOException {
        try {
            writeInputStreamToFile(zipFile.getInputStream(zipEntry), targetFile);
        } finally {
            Utils.safelyClose((java.io.Closeable) null);
        }
    }

    private static void clearDirectory(File directory) {
        for (File file : directory.listFiles()) {
            if (!file.delete()) {
                Log.w(LOG_TAG, "Could not delete the file: " + file.getAbsolutePath());
            }
        }
    }

    private static File getDaemonsDir() {
		// TODO: Check if MODE_WORLD_WRITABLE can be replaced by MODE_PRIVATE without problems.
        return DessertApplication.instance.getDir("daemons", Context.MODE_PRIVATE).getAbsoluteFile();
    }

    private static File getLibrariesDir() {
		// TODO: Check if MODE_WORLD_WRITABLE can be replaced by MODE_PRIVATE without problems.
        return DessertApplication.instance.getDir("libraries", Context.MODE_PRIVATE).getAbsoluteFile();
    }

    /**
     * Get the property file of this daemon directory
     * 
     * @param directory daemon directory
     * @return property file of daemon directory
     */
    private static File getPropertyFile(File directory) {
        return new File(directory, DAEMON_PROPERTY_FILENAME);
    }

    private static void installAssetFile(String assetName, File targetFile) throws IOException {
		if(Log.isLoggable(LOG_TAG, Log.DEBUG)) {
			Log.d(LOG_TAG, "Installing asset file to: " + targetFile.getAbsolutePath());
        }

        InputStream inputStream = null;
        try {
            AssetManager assetManager = DessertApplication.instance.getAssets();
            inputStream = assetManager.open(assetName);
            writeInputStreamToFile(inputStream, targetFile);

        } finally {
            Utils.safelyClose(inputStream);
        }
    }

    /**
     * Installs the file identified by <code>sourceResourceID</code> from the
     * raw folder of the packaged application to the <code>targetFile</code>.
     *
     * @param targetFile target file to write to
     * @throws IOException thrown when an I/O error occurred while accessing the
     *             target file
     */
    private static void installRawFile(File targetFile) throws IOException {
	    if(Log.isLoggable(LOG_TAG, Log.DEBUG)) {
			Log.d(LOG_TAG, "Installing raw file to: " + targetFile.getAbsolutePath());
		}

        InputStream inputStream = null;
        try {
            inputStream = DessertApplication.instance.getResources().openRawResource(R.raw.start_daemon_script);
            writeInputStreamToFile(inputStream, targetFile);
        } finally {
            Utils.safelyClose(inputStream);
        }
    }

    /**
     * Reads an application version as stored in the <code>sourceFile</code> as
     * a single line containing the version string.
     * 
     * @param sourceFile file to read the version from
     * @return application version as stored in the <code>sourceFile</code>
     */
    private static ApplicationVersion readAppVersionFromFile(File sourceFile) {
        ApplicationVersion result = null;

        if (sourceFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(sourceFile), 512);
                result = ApplicationVersion.getVersionFromString(reader.readLine());
            } catch (IOException e) {
                Log.w(LOG_TAG, "Could not read library version file", e);
            } finally {
                Utils.safelyClose(reader);
            }
        }

        return result;
    }

    /**
     * Writes the string representation of th given <code>version</code> to the
     * <code>targetFile</code>. Will append the string instead of replacing the
     * file if <code>doAppend</code> is <code>true</code>.
     * 
     * @param version version to store in the target file
     * @param targetFile file to write to
     * @throws IOException thrown when an I/O error occurred while writing the
     *             file
     */
    private static void writeVersionToFile(ApplicationVersion version, File targetFile) throws IOException {
        FileWriter writer = null;
        try {
            writer = new FileWriter(targetFile, false);
            writer.write(version.toString());
        } finally {
            Utils.safelyClose(writer);
        }
    }

    private FileTasks() {
        // we don't need any instances of file task... this is just a collection of static methods
    }

}
