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

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.Utils;

public class ConfigEntryDecimal extends ConfigEntry {

    private final double minValue;
    private final double maxValue;

    public ConfigEntryDecimal(String name, String defaultValue, String description, String minValue, String maxValue) {
        super(name, defaultValue, description);
        this.minValue = Utils.safelyParseDouble(minValue, Double.NaN);
        this.maxValue = Utils.safelyParseDouble(maxValue, Double.NaN);
    }

    @Override
    public View getView(LayoutInflater inflater, SharedPreferences preferences) {
        View view = inflater.inflate(R.layout.config_decimal_element, null);

        TextView descriptionView = (TextView) view.findViewById(R.id.Description);

        StringBuilder sb = new StringBuilder(description);
        if (!Double.isNaN(minValue) || !Double.isNaN(maxValue)) {
            sb.append(" [")
                    .append(Double.isNaN(minValue) ? "-" : minValue)
                    .append(", ")
                    .append(Double.isNaN(maxValue) ? "-" : maxValue)
                    .append("]");
        }
        descriptionView.setText(sb.toString());

        EditText valueView = (EditText) view.findViewById(R.id.EditBox);
        valueView.setText(getValue(preferences));
        valueView.addTextChangedListener(new TextEditWatcher(preferences));

        return view;
    }

    @Override
    public void readValueFromProperties(SharedPreferences preferences, Properties properties) {
        if (properties.containsKey(name)) {
            double value = Utils.safelyParseDouble(properties.getProperty(name, ""), 0d);
            if (!Double.isNaN(minValue)) {
                value = Math.max(minValue, value);
            }
            if (!Double.isNaN(maxValue)) {
                value = Math.min(value, maxValue);
            }

            setValue(preferences, Double.toString(value));
        }
    }

}
