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
import android.widget.ImageView;
import android.widget.TextView;
import de.fuberlin.dessert.R;

public class ManageEntryProperty extends ManageEntry {

    protected final CommandLine getterCommand;
    protected final CommandLine[] setterCommands;
    protected final CommandOption[] setterCommandOptions;
    protected String currentValue;
    protected boolean isQuerying;

    public ManageEntryProperty(String description, CommandLine getterCommand, CommandLine[] setterCommands,
            CommandOption[] setterCommandOptions) {
        super(description);

        this.getterCommand = getterCommand;
        this.setterCommands = setterCommands;
        this.setterCommandOptions = setterCommandOptions;
    }

    public CommandLine getGetterCommand() {
        return getterCommand;
    }

    public CommandOption[] getSetterCommandOptions() {
        return setterCommandOptions;
    }

    public CommandLine[] getSetterCommands() {
        return setterCommands;
    }

    @Override
    public ManageEntryType getType() {
        ManageEntryType result = null;
        if (setterCommands == null || setterCommands.length == 0) {
            result = ManageEntryType.PROPERTY_GETTER_ONLY;
        } else {
            result = ManageEntryType.PROPERTY_GETTER_SETTER;
        }
        return result;
    }

    @Override
    public View getView(LayoutInflater layoutInflater, View convertView, ViewGroup parent) {
        // inflate view or reuse
        View view;
        if (convertView == null) {
            view = layoutInflater.inflate(R.layout.manage_property_element, parent, false);

            int imageSize = view.findViewById(R.id.Description).getHeight();
            ImageView imageView = (ImageView) view.findViewById(R.id.SetterMarker);
            imageView.setMinimumHeight(imageSize);
            imageView.setMinimumWidth(imageSize);
        } else {
            view = convertView;
        }

        // set description
        TextView descriptionView = (TextView) view.findViewById(R.id.Description);
        descriptionView.setText(description);

        // set marker icons
        if (getType() == ManageEntryType.PROPERTY_GETTER_SETTER) {
            view.findViewById(R.id.SetterMarker).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.SetterMarker).setVisibility(View.GONE);
        }

        if (isQuerying) {
            view.findViewById(R.id.QueryingMarker).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.QueryingMarker).setVisibility(View.GONE);
        }

        // set current value
        String value = null;
        if (currentValue == null) {
            value = layoutInflater.getContext().getString(R.string.value_unknown);
        } else {
            value = currentValue;
        }

        TextView valueView = (TextView) view.findViewById(R.id.ValueField);
        valueView.setText(value);

        return view;
    }

    public void setPropertyValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public void setQuerying(boolean isQuerying) {
        this.isQuerying = isQuerying;
    }
}
