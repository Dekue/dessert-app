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
package de.fuberlin.dessert.model.manage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A ManageEntry represents a single element in a list of operations and
 * information of a running daemon.
 * <p>
 * Each entry is of a specific type and can create a view that represents this
 * entry.
 */
public abstract class ManageEntry {

    /**
     * Enumeration of the possible types of entries. Each entry of the same type
     * can share the same view with another entry of the same type.
     */
    public enum ManageEntryType {
        /** a simple entry to divide and describe sections */
        SPACER,
        /** a property entry that only has a getter */
        PROPERTY_GETTER_ONLY,
        /** a property entry with both a getter and a setter */
        PROPERTY_GETTER_SETTER,
        /** a command entry */
        COMMAND
    }

    protected final String description;

    /**
     * Constructs a new manage entry with the given <code>description/code>.
     * 
     * @param description the description of this entry
     */
    public ManageEntry(String description) {
        this.description = description;
    }

    /**
     * @return the description of this entry
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the type if this entry.
     * <p>
     * Subclasses must implement this method and return an entry type according
     * to the type of view the entry represents.
     * <p>
     * Entries with the same type will be offered existing views to reuse.
     * 
     * @return the type of this entry
     */
    abstract public ManageEntryType getType();

    /**
     * Get the current view for this manage entry. The entry might reuse an
     * older view <code>convertView</code>.
     * 
     * @param layoutInflater layout inflater to be used to create views from an
     *            XML definition
     * @param convertView an existing view to be reused; can be
     *            <code>null</code>
     * @param parent parent to optionally attach the view to
     * @return a newly created view or the reused <code>convertView</code>
     */
    abstract public View getView(LayoutInflater layoutInflater, View convertView, ViewGroup parent);

    /**
     * @return number of possible view types
     */
    public int getViewTypeCount() {
        return ManageEntryType.values().length;
    }
}
