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
package de.fuberlin.dessert.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import de.fuberlin.dessert.model.manage.ManageConfiguration;
import de.fuberlin.dessert.model.manage.ManageEntry;
import de.fuberlin.dessert.model.manage.ManageEntry.ManageEntryType;

public class ManageConfigurationListAdapter extends BaseAdapter {

    private final LayoutInflater layoutInflater;
    private ManageConfiguration configuration;

    public ManageConfigurationListAdapter(Context context) {
        this(context, null);
    }

    public ManageConfigurationListAdapter(Context context, ManageConfiguration configuration) {
        super();

        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.configuration = configuration;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    public void clear() {
        setValues(null);
    }

    @Override
    public int getCount() {
        int result = 0;
        if (configuration != null) {
            result += configuration.getEntriesCount();
        }
        return result;
    }

    @Override
    public ManageEntry getItem(int position) {
        if (position < 0 || position >= getCount()) {
            throw new IndexOutOfBoundsException("position must be between 0 and " + getCount() + " but was " + position);
        }
        return configuration.getEntry(position);
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= getCount()) {
            throw new IndexOutOfBoundsException("position must be between 0 and " + getCount() + " but was " + position);
        }
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= getCount()) {
            throw new IndexOutOfBoundsException("position must be between 0 and " + getCount() + " but was " + position);
        }
        return configuration.getEntry(position).getType().ordinal();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position < 0 || position >= getCount()) {
            throw new IndexOutOfBoundsException("position must be between 0 and " + getCount() + " but was " + position);
        }
        return configuration.getEntry(position).getView(layoutInflater, convertView, parent);
    }

    @Override
    public int getViewTypeCount() {
        return ManageEntryType.values().length;
    }

    @Override
    public boolean isEnabled(int position) {
        if (position < 0 || position >= getCount()) {
            throw new IndexOutOfBoundsException("position must be between 0 and " + getCount() + " but was " + position);
        }
        return configuration.getEntry(position).getType() != ManageEntryType.SPACER;
    }

    public void setValues(ManageConfiguration configuration) {
        this.configuration = configuration;
        notifyDataSetChanged();
    }
}
