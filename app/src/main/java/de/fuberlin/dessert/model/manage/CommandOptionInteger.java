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
import android.widget.EditText;
import android.widget.TextView;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.Utils;

public class CommandOptionInteger extends CommandOption {

    private final int minValue;
    private final int maxValue;

    public CommandOptionInteger(String name, String description, String minValue, String maxValue) {
        super(name, description);
        this.minValue = Utils.safelyParseInteger(minValue, Integer.MIN_VALUE);
        this.maxValue = Utils.safelyParseInteger(maxValue, Integer.MAX_VALUE);
    }

    @Override
    public View getView(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.config_integer_element, null);

        TextView descriptionView = (TextView) view.findViewById(R.id.Description);

        StringBuilder sb = new StringBuilder(description);
        if (minValue != Integer.MIN_VALUE || maxValue != Integer.MAX_VALUE) {
            sb.append(" [")
                    .append(minValue == Integer.MIN_VALUE ? "-" : minValue)
                    .append(", ")
                    .append(maxValue == Integer.MAX_VALUE ? "-" : maxValue)
                    .append("]");
        }
        descriptionView.setText(sb.toString());

        EditText valueView = (EditText) view.findViewById(R.id.EditBox);
        valueView.setText(getValue());
        valueView.addTextChangedListener(new TextEditWatcher(this));

        return view;
    }
}
