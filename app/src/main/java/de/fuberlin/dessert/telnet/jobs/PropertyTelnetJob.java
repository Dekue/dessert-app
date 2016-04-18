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
package de.fuberlin.dessert.telnet.jobs;

import java.util.EnumSet;
import java.util.LinkedList;

import de.fuberlin.dessert.Utils;
import de.fuberlin.dessert.event.DataChangedEventListener;
import de.fuberlin.dessert.model.manage.ManageEntryProperty;
import de.fuberlin.dessert.telnet.TelnetCommand;
import de.fuberlin.dessert.telnet.TelnetCommandMode;
import de.fuberlin.dessert.telnet.TelnetJob;

public class PropertyTelnetJob implements TelnetJob {

    private final ManageEntryProperty entry;
    private final DataChangedEventListener changeListener;
    private final LinkedList<TelnetCommand> setterCommands;
    private final TelnetCommand getterCommand;
    private boolean isGetterSent;
    private volatile boolean isStarted;

    public PropertyTelnetJob(ManageEntryProperty entry, DataChangedEventListener changeListener, String commandString,
            EnumSet<TelnetCommandMode> modes) {
        this.entry = entry;
        this.changeListener = changeListener;
        this.setterCommands = new LinkedList<>();
        this.getterCommand = new TelnetCommand(commandString, modes);
    }

    public void addSetterCommand(String commandString, EnumSet<TelnetCommandMode> modes) {
        if (isStarted) {
            throw new IllegalStateException("Cannot add anymore setter commands after the processing is started");
        }
        setterCommands.offer(new TelnetCommand(commandString, modes));
    }

    @Override
    public boolean hasMoreCommands() {
        return !setterCommands.isEmpty() || !isGetterSent;
    }

    @Override
    public TelnetCommand nextCommand() {
        TelnetCommand result = setterCommands.poll();
        if (result == null && !isGetterSent) {
            result = getterCommand;
            isGetterSent = true;
        }
        return result;
    }

    @Override
    public void onAborted() {
        entry.setPropertyValue(null);
        entry.setQuerying(false);
        if (changeListener != null) {
            changeListener.onDataChanged();
        }
    }

    @Override
    public void onCompleted() {
        if (changeListener != null) {
            changeListener.onDataChanged();
        }
    }

    @Override
    public void onError() {
        entry.setPropertyValue("<error>");
        entry.setQuerying(false);
        if (changeListener != null) {
            changeListener.onDataChanged();
        }
    }

    @Override
    public void onResult(String[] resultValue, TelnetCommand command) {
        // we are only interested in the result of the getter command
        if (command != getterCommand) {
            return;
        }

        entry.setPropertyValue(Utils.toString(resultValue, true));
        entry.setQuerying(false);
    }

    @Override
    public void onStart() {
        isStarted = true;
    }
}
