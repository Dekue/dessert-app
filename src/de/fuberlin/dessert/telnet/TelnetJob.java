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

/**
 * A holder of commands to be executed and the returned values.
 * <p>
 * Jobs are to be queued in a running {@link TelnetScheduler} and get executed
 * at some point in time.
 * <p>
 * The job contains a set number of commands. The service executes each command
 * in the order returned by {@link #nextCommand()}. After each call to
 * {@link #nextCommand()} and before any more calls to {@link #nextCommand()}
 * the service will supply the returned value of telnet command by calling the
 * {@link #onResult(String[], TelnetCommand)} callback method. After all
 * commands are executed, the {@link #hasMoreCommands()} returns
 * <code>false</code> and the {@link #onResult(String[], TelnetCommand)}
 * callback was called the service will call the {@link #onCompleted()} callback
 * to signal the job that the processing is completed.
 */
public interface TelnetJob {

    /**
     * Check if this job has more commands to execute.
     * 
     * @return <code>true</code> if there are more commands to be returned from
     *         {@link #nextCommand()}
     */
    public boolean hasMoreCommands();

    /**
     * Gets the next command in this job as long as {@link #hasMoreCommands()}
     * returns <code>true</code>. If {@link #hasMoreCommands()} returns
     * <code>false</code> the return value of this method is undefined.
     * 
     * @return the next command or undefined if there aren't anymore
     */
    public TelnetCommand nextCommand();

    /**
     * Callback method called by the telnet service when the job is abandoned
     * and will not be processed anymore.
     * <p>
     * Can only occur until {@link #onStart()} was called.
     */
    public void onAborted();

    /**
     * Callback method called by the telnet service when the job is successfully
     * executed and any result supplied to this job.
     */
    public void onCompleted();

    /**
     * Callback method called by the telnet service when an error occured while
     * executing this job. The job will be discarded and no outstanding command
     * will be processed.
     * <p>
     * Can only occur after {@link #onStart()} was called but before
     * {@link #onCompleted()}.
     */
    public void onError();

    /**
     * Callback method called by the telnet service when a single TelnetCommand
     * was executed and the return value from the telnet server was received.
     * 
     * @param resultValues the reponse lines as read from the telnet server
     * @param command the command that caused this return value; this object is
     *            the last object returned by {@link #nextCommand()}
     */
    public void onResult(String[] resultValues, TelnetCommand command);

    /**
     * Callback method called by the telnet service when the execution of this
     * job is started.
     * <p>
     * You can use this moment to prime any caches or other data structures that
     * are necessary in the processing but might be memory intensive while not
     * being processed.
     */
    public void onStart();
}
