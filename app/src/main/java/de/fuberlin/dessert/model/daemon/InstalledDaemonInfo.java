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

import java.io.File;
import java.util.Properties;

import android.graphics.drawable.Drawable;

public class InstalledDaemonInfo extends DaemonInfo {
    protected final File directory;

    public InstalledDaemonInfo(DaemonInfo daemonInfo, File directory) {
        super(daemonInfo);
        this.directory = directory;
    }

    public InstalledDaemonInfo(Properties props, File directory, Drawable icon) {
        super(props, icon);
        this.directory = directory;
    }

    public File getDaemonDirectory() {
        return directory;
    }
}
