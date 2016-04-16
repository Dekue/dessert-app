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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import de.fuberlin.dessert.R;

public class CommandOptionList extends CommandOption {

    protected final String[] values;

    public CommandOptionList(String name, String description, String[] values) {
        super(name, description);
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Argument 'values' MUST NOT be empty.");
        }

        this.values = values;
        setValue("");
    }

    @Override
    public View getView(final LayoutInflater inflater) {
        final View view = inflater.inflate(R.layout.config_list_element, null);

        final TextView descriptionView = (TextView) view.findViewById(R.id.Description);
        descriptionView.setText(description);

        final EditText valueView = (EditText) view.findViewById(R.id.EditBox);
        valueView.setText(getValue());
        valueView.addTextChangedListener(new TextEditWatcher(this));
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
}
