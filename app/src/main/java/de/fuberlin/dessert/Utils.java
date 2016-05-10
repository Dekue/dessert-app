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

import java.io.Closeable;
import java.net.Socket;

import android.util.Log;

/**
 * Collection of static utility functions
 * 
 * @author Ramin Baradari
 */
public class Utils {

    private static final String LOG_TAG = "DESSERT -> Utils";

    /**
     * Returns true if the given <code>str</code> is either <code>null</code> or
     * has a length of 0.
     * 
     * @param str string to check
     * @return <code>true</code> if the string is either <code>null</code> or has
     *         a length of 0
     */
    public static boolean isStringEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * Safely closes the given <code>closable</code> object. Swallows any
     * exception that might be thrown while closing.
     * 
     * @param closable may be null
     */
    public static void safelyClose(Closeable closable) {
        try {
            if (closable != null) {
                closable.close();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not safely close this resource: " + closable, e);
        }
    }

    /**
     * Safely closes the given <code>closable</code> Object. Swallows any
     * exception that might be thrown while closing.
     * 
     * @param closable may be null
     */
    public static void safelyClose(Socket closable) {
        try {
            if (closable != null) {
                closable.close();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not safely close this resource: " + closable, e);
        }
    }

    /**
     * Parses the given <code>str</code> to return a double value ignoring any
     * parsing exceptions. If the string could not be parsed the returned value
     * is <code>defaultValue</code>.
     * 
     * @param str string to parse
     * @param defaultValue default value in case the string could not be parsed
     * @return the parsed value or the <code>defaultValue</code> if parsing did
     *         not succeed
     */
    public static double safelyParseDouble(String str, double defaultValue) {
        double result = defaultValue;

        if (str == null || str.length() == 0) {
            result = defaultValue;
        } else {
            try {
                result = Double.parseDouble(str);
            } catch (NumberFormatException e) {
                // ignore exception
            }
        }

        return result;
    }

    /**
     * Parses the given <code>str</code> to return an integer value ignoring any
     * parsing exceptions. If the string could not be parsed the returned value
     * is <code>0</code>.
     * 
     * @param str string to parse
     * @return the parsed value or the value <code>0</code> if parsing did not
     *         succeed
     */
    public static int safelyParseInteger(String str) {
        return safelyParseInteger(str, 0);
    }

    /**
     * Parses the given <code>str</code> to return an integer value ignoring any
     * parsing exceptions. If the string could not be parsed the returned value
     * is <code>defaultValue</code>.
     * 
     * @param str string to parse
     * @param defaultValue default value in case the string could not be parsed
     * @return the parsed value or the <code>defaultValue</code> if parsing did
     *         not succeed
     */
    public static int safelyParseInteger(String str, int defaultValue) {
        int result = defaultValue;

        if (str == null || str.length() == 0) {
            result = defaultValue;
        } else {
            try {
                result = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                // ignore exception
            }
        }

        return result;
    }

    /**
     * Converts an array of strings into a single string with each string from
     * the array in a new line. It can also trim empty lines if
     * <code>trim</code> is set.
     * 
     * @param strings strings to convert
     * @param trim enabling of empty line trimming
     * @return concatenation of all elements in <code>strings</code> and
     *         optionally trimmed of empty elements
     */
    public static String toString(String[] strings, boolean trim) {
        if (strings == null || strings.length == 0) {
            return null;
        }

        // build result
        StringBuilder result = new StringBuilder();

        // find first and last index without an empty string
        int first = 0;
        while (trim && first < strings.length && (strings[first] == null || strings[first].trim().length() == 0)) {
            first++;
        }
        int last = strings.length - 1;
        while (trim && 0 <= last && (strings[last] == null || strings[last].trim().length() == 0)) {
            last--;
        }

        // append the strings in between 
        for (int i = first; i <= last; i++) {
            result.append(strings[i]);
            if (i != strings.length - 1) {
                result.append('\n');
            }
        }
        return result.toString();
    }

}
