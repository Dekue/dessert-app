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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import de.fuberlin.dessert.event.CommandResultEventListener;
import de.fuberlin.dessert.telnet.TelnetCommand;
import de.fuberlin.dessert.telnet.TelnetCommandMode;
import de.fuberlin.dessert.telnet.TelnetJob;

public class CommandTelnetJob implements TelnetJob {

    private final CommandResultEventListener resultListener;
    private final LinkedList<TelnetCommand> commands;
    private final List<String> results;
    private volatile boolean isStarted;

    public CommandTelnetJob(CommandResultEventListener resultListener) {
        this.resultListener = resultListener;
        this.commands = new LinkedList<TelnetCommand>();
        this.results = new ArrayList<String>();
    }

    public void addCommand(String commandString, EnumSet<TelnetCommandMode> modes) {
        if (isStarted) {
            throw new IllegalStateException("Cannot add anymore setter commands after the processing is started");
        }
        commands.offer(new TelnetCommand(commandString, modes));
    }

    @Override
    public boolean hasMoreCommands() {
        return !commands.isEmpty();
    }

    @Override
    public TelnetCommand nextCommand() {
        return commands.poll();
    }

    @Override
    public void onAborted() {
        if (resultListener != null) {
            resultListener.onAborted();
        }
    }

    @Override
    public void onCompleted() {
        if (resultListener != null) {
            resultListener.onCompleted(results.toArray(new String[results.size()]));
        }
    }

    @Override
    public void onError() {
        if (resultListener != null) {
            resultListener.onError();
        }
    }

    @Override
    public void onResult(String[] resultValues, TelnetCommand command) {
        for (String string : resultValues) {
            results.add(string);
        }
    }

    @Override
    public void onStart() {
        isStarted = true;
    }
}
