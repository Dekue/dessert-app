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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import de.fuberlin.dessert.R;

public class CommandOptionBoolean extends CommandOption {

    private final class CheckBoxWatcher implements OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            CommandOptionBoolean.this.setInternalValue(isChecked);
            CommandOptionBoolean.this.setValue(isChecked ? trueValue : falseValue);
        }
    }

    private static final String DEFAULT_TRUE = Boolean.TRUE.toString();
    private static final String DEFAULT_FALSE = Boolean.FALSE.toString();

    private final String trueValue;
    private final String falseValue;

    private boolean internalValue;

    public CommandOptionBoolean(String name, String description, String trueValue, String falseValue) {
        super(name, description);
        this.trueValue = trueValue == null ? DEFAULT_TRUE : trueValue;
        this.falseValue = falseValue == null ? DEFAULT_FALSE : falseValue;
        this.internalValue = false;
        this.setValue(this.falseValue);
    }

    @Override
    public View getView(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.config_boolean_element, null);

        CheckBox valueView = (CheckBox) view.findViewById(R.id.CheckBox);
        valueView.setChecked(getInternalValue());
        valueView.setText(description);
        valueView.setOnCheckedChangeListener(new CheckBoxWatcher());

        return view;
    }

    private boolean getInternalValue() {
        return this.internalValue;
    }

    private void setInternalValue(boolean value) {
        this.internalValue = value;
    }
}
