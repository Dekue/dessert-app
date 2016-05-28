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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.support.annotation.NonNull;
import android.util.Log;
import de.fuberlin.dessert.Utils;
import de.fuberlin.dessert.telnet.jobs.SingleCommandTelnetJob;

/**
 * This is a priority queue system for a telnet connection.
 * <p>
 * You can queue {@link TelnetJob} objects with different priorities and they
 * get executed as soon as there are no more de.fuberlin.dessert.telnet.jobs
 * with a higher priority. There is no guarantee that the job will be executed
 * at all if more and more de.fuberlin.dessert.telnet.jobs of a higher priority
 * are inserted.
 * <p>
 * The scheduler must be primed by calling {@link #setConnectionDetails(int)}.
 * Any job executed after this call will be executed on the new connection as
 * specified by this call.
 * <p>
 * To force a disconnect of the scheduler, interrupt the currently executed job
 * and abort an outstanding job in the queue you can call {@link #disconnect()}.
 * To fully reset the scheduler you can call {@link #resetScheduler()}.
 */
public class TelnetScheduler {

    /**
     * Priorities of a scheduled job. Higher prioritized jobs are executed
     * earlier.
     */
    public enum Priority {
        /** Highest priority */
        HIGHEST,
        /** High priority */
        HIGH,
        /** Default priority */
        DEFAULT,
        /** Low priority */
        LOW,
        /** Lowest priority */
        LOWEST
    }

    private static final class JobWrapper implements Comparable<JobWrapper> {

        private final Priority priority;
        private final TelnetJob job;
        private final int id;

        public JobWrapper(TelnetJob job, Priority priority, int id) {
            this.job = job;
            this.priority = priority;
            this.id = id;
        }

        @Override
        public int compareTo(@NonNull JobWrapper other) {
            if (this == other) {
                return 0;
            }

            if (this.priority.ordinal() != other.priority.ordinal()) {
                return this.priority.ordinal() - other.priority.ordinal();
            }

            return this.id - other.id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            JobWrapper other = (JobWrapper) obj;
            if (id != other.id)
                return false;
            if (job == null) {
                if (other.job != null)
                    return false;
            } else if (!job.equals(other.job))
                return false;
            return priority == other.priority;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + id;
            result = prime * result + ((job == null) ? 0 : job.hashCode());
            result = prime * result + ((priority == null) ? 0 : priority.hashCode());
            return result;
        }

    }

    /**
     * Thread to run the actual job execution logic. Calls through to the outer
     * class.
     */
    private final class WorkerThread extends Thread {
        public WorkerThread() {
            super("TelnetScheduler-WorkerThread");
        }

        @Override
        public void run() {
            //noinspection InfiniteLoopStatement
            while (true) {
                Thread.interrupted();

                try {
                    // wait or get next job
                    TelnetJob job = getNextJob();
                    if (job == null) {
                        Log.w(LOG_TAG, "Queued job was null; skipping job");
                        continue;
                    }

                    // check if connection needs to established
                    boolean connectionOK = ensureConnection();
                    if (!connectionOK) {
                        Log.w(LOG_TAG, "Problem creating connection; skipping job");
                        continue;
                    }

                    // execute job
                    if (!executeJob(job)) {
                        Log.w(LOG_TAG, "Problem while executing job; skipping job");
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Got an exception in the worker thread loop", e);
                }
            }
        }
    }

    private static final String LOG_TAG = "TelnetScheduler";

    private static final String COMMAND_ENABLE = "enable";
    private static final String COMMAND_CONFIG = "configure terminal";
    private static final String COMMAND_DISABLE = "disable";
    private static final String COMMAND_EXIT = "exit";
    private static final String COMMAND_LOGOUT = "logout";
    private static final String COMMAND_SHUTDOWN = "shutdown";

    private static final int DEFAULT_CONNECTION_PORT = -1;

    private final Pattern PROMPT_PATTERN = Pattern.compile("^localhost:[0-9a-zA-z]{1,4}(>|#|\\(config\\)#)");

    private int port = DEFAULT_CONNECTION_PORT;

    /**
     * flag indicates that the connection details have changed and any further
     * de.fuberlin.dessert.telnet.jobs must be processed on the new connection
     */
    private boolean connectionDetailsChanged = false;

    /**
     * Internal queue and lock object for the scheduler
     */
    private final PriorityQueue<JobWrapper> queue = new PriorityQueue<>();
    private int lastJobID = 0;

    private final WorkerThread workerThread;

    private Socket socket = null;
    private InputStream incomingData;
    private OutputStream outgoingData;
    private TelnetCommandMode currentMode;

    // monitor objects
    private final Object queueLock = new Object();
    private final Object socketLock = new Object();

    public TelnetScheduler() {
        this.workerThread = new WorkerThread();
        this.workerThread.setDaemon(true);
    }

    /**
     * Enforces a disconnect to the telnet server and purges the queue.
     * <p>
     * The currently executed job will be interrupted and the onError handler
     * will be called. Any queued job will be removed from the queue and their
     * onAbort handler will be called.
     */
    public void disconnect() {
        resetSchedulerImpl(false);
    }

    public void enqueueJob(TelnetJob job) {
        enqueueJob(job, Priority.DEFAULT);
    }

    private void enqueueJob(TelnetJob job, Priority priority) {
        synchronized (queueLock) {
            queue.offer(new JobWrapper(job, priority, ++lastJobID));
            queueLock.notifyAll();
        }
    }

    public void enqueueShutdownCommand(Priority priority) {
        enqueueJob(new SingleCommandTelnetJob(COMMAND_SHUTDOWN, TelnetCommandMode.PRIVILEGED), priority);
    }

    /**
     * Enforces a disconnect to the telnet server, purges the queue and resets
     * the connection details.
     * <p>
     * The currently executed job will be interrupted and the onError handler
     * will be called. Any queued job will be removed from the queue and their
     * onAbort handler will be called.
     * <p>
     * Further the connection details like port and cliID will be reset.
     */
    public void resetScheduler() {
        resetSchedulerImpl(true);
    }

    public void setConnectionDetails(int port) {
        synchronized (queueLock) {
            this.port = port;
            this.connectionDetailsChanged = true;
        }
    }

    public void startScheduler() {
        this.workerThread.start();
    }

    private void changeCommandMode(EnumSet<TelnetCommandMode> modes) throws IOException {
        if (modes.contains(currentMode)) {
            Log.w(LOG_TAG, "Got called with the current mode... this should not happen");
            return;
        }

        // we need at most two command switches
        String firstCommand;
        String secondCommand = null;

        // decide which commands to call
        switch (currentMode) {
        case DEFAULT:
            firstCommand = COMMAND_ENABLE;
            if (!modes.contains(TelnetCommandMode.PRIVILEGED)) {
                secondCommand = COMMAND_CONFIG;
            }
            break;
        case PRIVILEGED:
            if (modes.contains(TelnetCommandMode.DEFAULT)) {
                firstCommand = COMMAND_DISABLE;
            } else {
                firstCommand = COMMAND_CONFIG;
            }
            break;
        case CONFIG:
            firstCommand = COMMAND_EXIT;
            if (!modes.contains(TelnetCommandMode.PRIVILEGED)) {
                secondCommand = COMMAND_DISABLE;
            }
            break;
        default:
            throw new IllegalArgumentException("Got unsupported telnet command mode " + currentMode);
        }

        sendCommand(firstCommand);
        readUntilPrompt();

        if (secondCommand != null) {
            sendCommand(secondCommand);
            readUntilPrompt();
        }
    }

    private void cutConnection() {
        synchronized (socketLock) {
            if (socket != null && socket.isConnected() && !socket.isOutputShutdown()) {
                try {
                    sendCommand(COMMAND_LOGOUT);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error while disconnecting from telnet server", e);
                }
            }
            Utils.safelyClose(socket);
        }
    }

    private boolean ensureConnection() {
        synchronized (socketLock) {
            // stop active connection if details are changed
            if (connectionDetailsChanged) {
                // disconnect here
                cutConnection();
                socket = null;
                incomingData = null;
                outgoingData = null;
                currentMode = null;
                connectionDetailsChanged = false;
            }

            // start connection if none is running
            if (socket == null) {
                try {
                    socket = new Socket("localhost", port);
                    incomingData = socket.getInputStream();
                    outgoingData = socket.getOutputStream();
                    readUntilPrompt();
                } catch (UnknownHostException e) {
                    Log.e(LOG_TAG, "Host not found", e);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error while creating telnet connection", e);
                }
            }
        }

        return socket != null;
    }

    private boolean executeJob(TelnetJob job) {
        signalJobStart(job);

        // iterate over the job and stuff
        while (job.hasMoreCommands()) {
            TelnetCommand command = job.nextCommand();

            try {
                // 1. enter the correct telnet mode            
                if (!command.isModeValid(currentMode)) {
                    changeCommandMode(command.getModes());
                    if (!command.isModeValid(currentMode)) {
                        throw new IllegalStateException("Still in wrong mode after switching modes");
                    }
                }

                // 2. send the command
                sendCommand(command.getCommand());

                // 3. read the output until the prompt
                List<String> commandResult = readUntilPrompt();

                // 4. signal the job command success and the result
                signalJobResult(job, commandResult.toArray(new String[commandResult.size()]), command);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error while executing command " + command.getCommand() + " @ " + command.getModes(), e);
                signalJobError(job);
                return false;
            }
        }

        signalJobCompleted(job);

        return true;
    }

    private TelnetJob getNextJob() {
        TelnetJob result = null;
        synchronized (queueLock) {
            // wait for a job
            while (queue.isEmpty()) {
                try {
                    queueLock.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }

            // get job from queue
            JobWrapper wrapper = queue.poll();
            if (wrapper != null) {
                result = wrapper.job;
            }
        }

        return result;
    }

    /**
     * Read from the current connection
     * 
     * @return the list of lines read as result from the last command run
     * @throws IOException
     */
    private List<String> readUntilPrompt() throws IOException {
        List<String> result = new ArrayList<>();

        synchronized (socketLock) {
            if (socket == null || socket.isClosed() || !socket.isConnected()) {
                throw new IllegalStateException("Must be connected to a telnet server");
            }

            if (socket.isInputShutdown()) {
                throw new IllegalStateException("Incoming stream must not be closedo");
            }

            // read loop.. data comes as 8bit values containing 7bit ascii with a leading 0 bit     
            boolean discardedPreviousPrompt = false; // first line is always the last prompt; we discard this one
            boolean gotCR = false; // last character was a CR
            boolean foundPrompt = false;
            StringBuilder sb = new StringBuilder(128);
            for (int b = incomingData.read(); b != -1 && !foundPrompt; b = incomingData.read()) {
                // handle CR without the following LF
                if (gotCR && b != 0x0A) {
                    sb.append((char) 0x0D);
                    gotCR = false;
                }

                switch (b) {
                case 0x0D:
                    // must wait for the next character because this can be a line ending CR LF
                    gotCR = true;
                    break;
                case 0x0A:
                    // it's either an encoded line ending or just a LF as is
                    if (gotCR) {
                        if (discardedPreviousPrompt) {
                            result.add(sb.toString());
                        } else {
                            discardedPreviousPrompt = true;
                        }
                        sb.setLength(0);
                        gotCR = false;
                    } else {
                        sb.append((char) 0x0A);
                    }
                    break;
                case '>':
                case '#':
                    sb.append((char) b);

                    // check if this is a prompt
                    Matcher matcher = PROMPT_PATTERN.matcher(sb.toString());
                    if (matcher.matches()) {
                        // this is the prompt; now set the new mode
                        foundPrompt = true;

                        String modeString = matcher.group(1);
                        if (">".equals(modeString)) {
                            currentMode = TelnetCommandMode.DEFAULT;
                        } else if ("#".equals(modeString)) {
                            currentMode = TelnetCommandMode.PRIVILEGED;
                        } else if ("(config)#".equals(modeString)) {
                            currentMode = TelnetCommandMode.CONFIG;
                        } else {
                            Log.e(LOG_TAG, "Found unsupported prompt: " + sb.toString());
                        }
                    }
                    break;
                default:
                    sb.append((char) b);
                    break;
                }
            }
        }

        return result;
    }

    private void resetSchedulerImpl(boolean resetDetails) {
        synchronized (queueLock) {
            // empty queue
            while (!queue.isEmpty()) {
                signalJobAborted(queue.poll().job);
            }

            // stop worker thread
            workerThread.interrupt();

            // cut connection
            cutConnection();

            if (resetDetails) {
                this.port = DEFAULT_CONNECTION_PORT;
                this.connectionDetailsChanged = false; // we don't signal a change until the details are set again
            } else {
                this.connectionDetailsChanged = true; // we signal a change but leave the values as is for any further queue item
            }

            // notify anyone that might be waiting on the queue
            queueLock.notifyAll();
        }
    }

    private void sendCommand(String command) throws IOException {
        synchronized (socketLock) {
            if (socket == null || socket.isClosed() || !socket.isConnected()) {
                throw new IllegalStateException("Must be connected to a telnet server");
            }

            if (socket.isOutputShutdown()) {
                throw new IllegalStateException("Outgoing stream must not be closedo");
            }

            byte[] bytes;
            try {
                bytes = command.getBytes("ASCII");
            } catch (UnsupportedEncodingException e) {
                Log.e(LOG_TAG, "ASCII encoding is not supported", e);
                return;
            }

            outgoingData.write(bytes);
            outgoingData.write(0x0D);
            outgoingData.write(0x0A);
            outgoingData.flush();
        }
    }

    private void signalJobAborted(TelnetJob job) {
        try {
            job.onAborted();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Caught exception in onAborted callback", e);
        }
    }

    private void signalJobCompleted(TelnetJob job) {
        try {
            job.onCompleted();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Caught exception in onCompleted callback", e);
        }
    }

    private void signalJobError(TelnetJob job) {
        try {
            job.onError();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Caught exception in onError callback", e);
        }
    }

    private void signalJobResult(TelnetJob job, String[] resultValue, TelnetCommand command) {
        try {
            job.onResult(resultValue, command);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Caught exception in onStart callback", e);
        }
    }

    private void signalJobStart(TelnetJob job) {
        try {
            job.onStart();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Caught exception in onStart callback", e);
        }
    }
}
