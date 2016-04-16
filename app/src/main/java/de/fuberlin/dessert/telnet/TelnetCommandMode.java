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
package de.fuberlin.dessert.telnet;

import java.util.EnumSet;

import android.util.Log;

/**
 * Representation of the modes of the telnet interface. Can be used to specify
 * which mode must be set to run a specific command.
 * <p>
 * You can use an <code>EnumSet<TelnetCommandMode></code> to create a set of
 * modes.
 */
public enum TelnetCommandMode {
    DEFAULT,
    PRIVILEGED,
    CONFIG;

    private static final String LOG_TAG = "DESSERT -> TelnetCommandMode";

    public static TelnetCommandMode parseString(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }

        try {
            return TelnetCommandMode.valueOf(value);
        } catch (Exception e) {
            // we can ignore this one
            Log.w(LOG_TAG, "Unrecognized");
        }

        return null;
    }

    public static EnumSet<TelnetCommandMode> parseStringField(String values) {
        EnumSet<TelnetCommandMode> result = EnumSet.noneOf(TelnetCommandMode.class);

        if (values != null) {
            for (String token : values.split("\\|")) {
                TelnetCommandMode mode = TelnetCommandMode.parseString(token);
                if (mode != null) {
                    result.add(mode);
                }
            }
        }

        return result;
    }
}
