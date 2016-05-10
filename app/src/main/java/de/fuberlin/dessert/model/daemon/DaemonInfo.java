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
package de.fuberlin.dessert.model.daemon;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Properties;

import android.graphics.drawable.Drawable;
import de.fuberlin.dessert.Utils;

public class DaemonInfo {

    public final static class DaemonListComparator implements Comparator<DaemonInfo>, Serializable {

        @Override
        public int compare(DaemonInfo left, DaemonInfo right) {
            int tmp;

            if (left == right) {
                return 0;
            }

            if (right == null) {
                return 1;
            } else if (left == null) {
                return -1;
            }

            if ((tmp = left.name.compareToIgnoreCase(right.name)) != 0) {
                return tmp;
            }

            if ((tmp = left.version.compareTo(right.version)) != 0) {
                return tmp;
            }

            if ((tmp = left.applicationVersion.compareTo(right.applicationVersion)) != 0) {
                return tmp;
            }

            if ((tmp = left.libraryVersion - right.libraryVersion) != 0) {
                return tmp;
            }

            return 0;
        }

    }

    public static final DaemonListComparator DAEMON_LIST_COMPARATOR = new DaemonListComparator();

    public static final String PROPERTY_NAME = "daemon.name";
    public static final String PROPERTY_VERSION = "daemon.version";
    public static final String PROPERTY_APPLICATION_VERSION = "daemon.dessert.application.version";
    public static final String PROPERTY_LIBRARY_VERSION = "daemon.dessert.library.version";

    private static final String DEFAULT_EMPTY_NAME = "no-name-given-check-file";
    private static final String DEFAULT_EMPTY_VERSION = "-1";
    private static final String DEFAULT_EMPTY_APPLICATION_VERSION = "-1,-1,-1";
    private static final String DEFAULT_EMPTY_LIBRARY_VERSION = "-1";

    private final String name;
    private final String version;
    private final String applicationVersion;
    private final int libraryVersion;
    private final Drawable icon;

    DaemonInfo(DaemonInfo other) {
        this.name = other.name;
        this.version = other.version;
        this.applicationVersion = other.applicationVersion;
        this.libraryVersion = other.libraryVersion;
        this.icon = other.icon;
    }

    public DaemonInfo(Properties props, Drawable icon) {
        this(props.getProperty(PROPERTY_NAME, DEFAULT_EMPTY_NAME).trim(),
                props.getProperty(PROPERTY_VERSION, DEFAULT_EMPTY_VERSION),
                props.getProperty(PROPERTY_APPLICATION_VERSION, DEFAULT_EMPTY_APPLICATION_VERSION),
                props.getProperty(PROPERTY_LIBRARY_VERSION, DEFAULT_EMPTY_LIBRARY_VERSION),
                icon);
    }

    private DaemonInfo(String name, String version, String applicationVersion, int libraryVersion, Drawable icon) {
        this.name = name;
        this.version = version;
        this.applicationVersion = applicationVersion;
        this.libraryVersion = libraryVersion;
        this.icon = icon;
    }

    DaemonInfo(String name, String version, String applicationVersion, String libraryVersion, Drawable Icon) {
        this(name, version, applicationVersion, Utils.safelyParseInteger(libraryVersion, -1), Icon);
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public String getDaemonID() {

        return (name + '-' + version).replaceAll("\\\\", "\\\\");
    }

    public Drawable getIconDrawable() {
        return icon;
    }

    public int getLibraryVersion() {
        return libraryVersion;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}
