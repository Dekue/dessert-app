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
package de.fuberlin.dessert.model.config;

import java.util.Properties;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import de.fuberlin.dessert.R;

public class ConfigEntryList extends ConfigEntry {

    private final String[] values;

    public ConfigEntryList(String name, String defaultValue, String description, String[] values) {
        super(name, defaultValue, description);
        this.values = values;

        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Argument 'values' MUST NOT be empty.");
        }
    }

    @Override
    public View getView(final LayoutInflater inflater, final SharedPreferences preferences) {
        final View view = inflater.inflate(R.layout.config_list_element, null);

        final TextView descriptionView = (TextView) view.findViewById(R.id.Description);
        descriptionView.setText(description);

        final EditText valueView = (EditText) view.findViewById(R.id.EditBox);
        valueView.setText(getValue(preferences));
        valueView.addTextChangedListener(new TextEditWatcher(preferences));
        valueView.setFocusable(false);
        valueView.setFocusableInTouchMode(false);
        valueView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View vw) {
                new AlertDialog.Builder(inflater.getContext())
                        .setTitle(R.string.choose_value)
                        .setCancelable(true)
                        .setItems(values, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                valueView.setText(values[which]);
                            }
                        })
                        .show();
            }
        });

        return view;
    }

    @Override
    public void readValueFromProperties(SharedPreferences preferences, Properties properties) {
        if (properties.containsKey(name)) {
            String value = properties.getProperty(name, "");

            boolean exists = false;
            for (String element : values) {
                if (value.equals(element)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                value = defaultValue;
            }

            setValue(preferences, value);
        }
    }
}
