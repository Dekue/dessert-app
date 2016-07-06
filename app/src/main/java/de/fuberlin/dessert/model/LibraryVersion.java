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
package de.fuberlin.dessert.model;

import android.support.annotation.NonNull;
import de.fuberlin.dessert.HashCode;
import de.fuberlin.dessert.Utils;

/**
 * ApplicationVersion of a native as described here <a href=
 * "http://www.nondot.org/sabre/Mirrored/libtool-2.1a/libtool_6.html#SEC35"
 * >http://www.nondot.org/sabre/Mirrored/libtool-2.1a/libtool_6.html#SEC35</a>.
 * 
 * @author R.Baradari
 */
public class LibraryVersion implements Comparable<LibraryVersion> {

    private final int current;
    private final int revision;
    private final int age;
    private final String versionString;

    /** marker object to identify empty versions of an library */
    private static final LibraryVersion EMPTY_VERSION = new LibraryVersion(-1, -1, -1, "-");
    /** marker object to identify faulty versions of an library */
    private static final LibraryVersion FAULTY_VERSION = new LibraryVersion(-2, -2, -2, "<error>");

    /**
     * Parse the given <code>versionString</code> and create a new
     * LibraryVersion from it.
     * <p>
     * If the given string is empty or <code>null</code> the returned object is
     * the globally unique object {@link #EMPTY_VERSION}. If the given string is
     * faulty and an error occurs while parsing the returned object is the
     * globally unique object {@link #FAULTY_VERSION}.
     * 
     * @param versionString string to parse
     * @return object representing the parsed version string or
     *         {@link #EMPTY_VERSION} or {@link #FAULTY_VERSION}
     */
    public static LibraryVersion getVersionFromString(String versionString) {
        if (versionString == null || versionString.length() == 0) {
            return EMPTY_VERSION;
        }
        String[] parts = versionString.split("\\.");

        if (parts.length < 2) {
            return FAULTY_VERSION;
        }

        return new LibraryVersion(
                Utils.safelyParseInteger(parts[0]),
                Utils.safelyParseInteger(parts[1]),
                Utils.safelyParseInteger(parts[2]));
    }

    private LibraryVersion(int current, int revision, int age) {
        this.current = current;
        this.revision = revision;
        this.age = age;
        this.versionString = buildVersionString(current, revision, age);
    }

    private LibraryVersion(int current, int revision, int age, String versionString) {
        this.current = current;
        this.revision = revision;
        this.age = age;
        this.versionString = versionString;
    }

	@Override public int hashCode() {
		int result = HashCode.SEED;
		result = HashCode.hash(result, current);
		result = HashCode.hash(result, revision);
		result = HashCode.hash(result, age);
		result = HashCode.hash(result, versionString);
		return result;
	}

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LibraryVersion)) return false;

        LibraryVersion that = (LibraryVersion) other;
        return this.versionString.equals(that.versionString);
    }

    @Override
    public int compareTo(@NonNull LibraryVersion other) {
        int tmp;

        if (this == other) {
            return 0;
        }

        if ((tmp = compareVersionNumber(this.current, other.current)) != 0) {
            return tmp;
        }

        if ((tmp = compareVersionNumber(this.revision, other.revision)) != 0) {
            return tmp;
        }

        if ((tmp = compareVersionNumber(this.age, other.age)) != 0) {
            return tmp;
        }

        return 0;
    }

    /**
     * Checks if this object is compatible with the given version number.
     * 
     * @param expectedVersion version number to satisfy
     * @return <code>true</code> iff this object satisfies the given version
     *         number
     */
    public boolean isCompatible(int expectedVersion) {
        return expectedVersion <= current && expectedVersion >= current - age;
    }

    @Override
    public String toString() {
        return versionString;
    }

    private String buildVersionString(int major, int minor, int iteration) {
        return String.valueOf(major) + '.' + minor + '.' + iteration;
    }

    private int compareVersionNumber(int left, int right) {
        if (left < 0 && right < 0) {
            return 0;
        }

        if (right < 0 || left > right) {
            return 1;
        }

        if (left < 0 || left < right) {
            return -1;
        }
        return 0;
    }
}
