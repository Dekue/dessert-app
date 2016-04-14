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

import de.fuberlin.dessert.Utils;

public class ApplicationVersion implements Comparable<ApplicationVersion> {
    protected final int major;
    protected final int minor;
    protected final int revision;
    protected final String extra;
    protected final String versionString;

    public static final ApplicationVersion EMPTY_VERSION = new ApplicationVersion(-1, -1, -1, "", "-");
    public static final ApplicationVersion FAULTY_VERSION = new ApplicationVersion(-2, -2, -2, "", "<error>");

    public static ApplicationVersion getVersionFromString(String versionString) {
        if (versionString == null || versionString.length() == 0) {
            return EMPTY_VERSION;
        }
        String[] parts = versionString.split("(\\.|-)");

        if (parts.length < 3) {
            return FAULTY_VERSION;
        }

        return new ApplicationVersion(
                Utils.safelyParseInteger(parts[0]),
                Utils.safelyParseInteger(parts[1]),
                Utils.safelyParseInteger(parts[2]),
                parts.length > 3 ? parts[3] : "");
    }

    private ApplicationVersion(int major, int minor, int revision, String extra) {
        this.major = major;
        this.minor = minor;
        this.revision = revision;
        this.extra = extra;
        this.versionString = buildVersionString(major, minor, revision, extra);
    }

    private ApplicationVersion(int major, int minor, int revision, String extra, String versionString) {
        this.major = major;
        this.minor = minor;
        this.revision = revision;
        this.extra = extra;
        this.versionString = versionString;
    }

    @Override
    public int compareTo(ApplicationVersion other) {
        int tmp;

        if (this == other) {
            return 0;
        }

        if (other == null) {
            return 1;
        }

        if ((tmp = compareVersionNumber(this.major, other.major)) != 0) {
            return tmp;
        }

        if ((tmp = compareVersionNumber(this.minor, other.minor)) != 0) {
            return tmp;
        }

        if ((tmp = compareVersionNumber(this.revision, other.revision)) != 0) {
            return tmp;
        }

        return this.extra.compareTo(other.extra);
    }

    /**
     * @return extra chars of this application version
     */
    public String getExtra() {
        return extra;
    }

    /**
     * @return major version of this application version
     */
    public int getMajor() {
        return major;
    }

    /**
     * @return minor version of this application version
     */
    public int getMinor() {
        return minor;
    }

    /**
     * @return revision of this application version
     */
    public int getRevision() {
        return revision;
    }

    /**
     * Checks if this object is compatible with the given version mask.
     * <p>
     * The mask can be defined as the regexp
     * "(*|[0-9]+).(*|[0-9]+).(*|[0-9]+)(-(*|[0-9]+).(*|[0-9]+).(*|[0-9]+))?"
     * where '.' and '*' are to be treated as literals.
     * 
     * @param versionMask mask to satisfy
     * @return <code>true</code> iff this object satisfies the given version
     *         mask
     */
    public boolean isCompatible(String versionMask) {
        if (versionMask.contains("-")) {
            // range check
            String parts[] = versionMask.split("(\\.|\\-)");
            if (parts.length != 6) {
                return false;
            }

            boolean rollover = false;
            // lower bound            
            if (compareVersionWithMaskPart(revision, parts[2]) < 0) {
                rollover = true;
            } else {
                rollover = false;
            }
            if (compareVersionWithMaskPart((rollover ? minor - 1 : minor), parts[1]) < 0) {
                rollover = true;
            } else {
                rollover = false;
            }
            if (compareVersionWithMaskPart((rollover ? major - 1 : major), parts[0]) < 0) {
                return false;
            }

            // upper bound
            if (compareVersionWithMaskPart(revision, parts[5]) > 0) {
                rollover = true;
            } else {
                rollover = false;
            }
            if (compareVersionWithMaskPart((rollover ? minor + 1 : minor), parts[4]) > 0) {
                rollover = true;
            } else {
                rollover = false;
            }
            if (compareVersionWithMaskPart((rollover ? major + 1 : major), parts[3]) > 0) {
                return false;
            }

        } else {
            // simple mask
            String parts[] = versionMask.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            // check parts
            if (compareVersionWithMaskPart(major, parts[0]) != 0) {
                return false;
            }
            if (compareVersionWithMaskPart(minor, parts[1]) != 0) {
                return false;
            }
            if (compareVersionWithMaskPart(revision, parts[2]) != 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return versionString;
    }

    private String buildVersionString(int mjr, int mnr, int rev, String xtr) {
        StringBuilder sb = new StringBuilder();

        sb.append(mjr).append('.').append(mnr).append('.').append(rev);
        if (!Utils.isStringEmpty(xtr)) {
            sb.append('-').append(xtr);
        }

        return sb.toString();
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

    private int compareVersionWithMaskPart(int version, String part) {
        if (!"*".equals(part)) {
            int partValue = Utils.safelyParseInteger(part, -1);
            if (version == partValue) {
                return 0;
            } else if (version > partValue) {
                return 1;
            } else {
                return -1;
            }
        }

        return 0;
    }

}
