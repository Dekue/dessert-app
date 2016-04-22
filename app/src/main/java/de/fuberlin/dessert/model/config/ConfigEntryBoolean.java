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
import android.content.SharedPreferences.Editor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import de.fuberlin.dessert.R;

public class ConfigEntryBoolean extends ConfigEntry {

    protected final class CheckBoxWatcher implements OnCheckedChangeListener {
        private final SharedPreferences preferences;

        public CheckBoxWatcher(SharedPreferences preferences) {
            this.preferences = preferences;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            ConfigEntryBoolean.this.setInternalValue(preferences, isChecked);
            ConfigEntryBoolean.this.setValue(preferences, isChecked ? trueValue : falseValue);
        }
    }

    private static final String INTERNAL_NAME_SUFFIX = ".internalvalue";

    public static final String DEFAULT_TRUE = Boolean.TRUE.toString();
    public static final String DEFAULT_FALSE = Boolean.FALSE.toString();

    protected final String trueValue;
    protected final String falseValue;

    public ConfigEntryBoolean(String name, String defaultValue, String description, String trueValue, String falseValue) {
        super(name, defaultValue, description);
        this.trueValue = trueValue == null ? DEFAULT_TRUE : trueValue;
        this.falseValue = falseValue == null ? DEFAULT_FALSE : falseValue;
    }

    @Override
    public void ensureDefaultValue(SharedPreferences preferences, boolean force) {
        // there is one value with the name that is exposed to the outer world and an internal one with a lower case suffix
        // the internal value is the state of the checkbox while the external value is the actual value of the property
        if (force || !preferences.contains(getKey() + INTERNAL_NAME_SUFFIX)) {
            setInternalValue(preferences, Boolean.parseBoolean(defaultValue));
        }
        if (force || !preferences.contains(getKey())) {
            setValue(preferences, Boolean.parseBoolean(defaultValue) ? trueValue : falseValue);
        }
    }

    @Override
    public View getView(LayoutInflater inflater, SharedPreferences preferences) {
        View view = inflater.inflate(R.layout.config_boolean_element, null);

        CheckBox valueView = (CheckBox) view.findViewById(R.id.CheckBox);
        valueView.setChecked(getInternalValue(preferences));
        valueView.setText(description);
        valueView.setOnCheckedChangeListener(new CheckBoxWatcher(preferences));

        return view;
    }

    @Override
    public void readValueFromProperties(SharedPreferences preferences, Properties properties) {
        if (properties.containsKey(name)) {
            boolean value = Boolean.valueOf(properties.getProperty(name, "false"));

            setInternalValue(preferences, value);
            setValue(preferences, value ? trueValue : falseValue);
        }
    }

    @Override
    public void writeValueToProperties(SharedPreferences preferences, Properties properties) {
        // we write the internal true/false values here
        properties.setProperty(name, Boolean.toString(getInternalValue(preferences)));
    }

    protected boolean getInternalValue(SharedPreferences preferences) {
        return preferences.getBoolean(getKey() + INTERNAL_NAME_SUFFIX, Boolean.parseBoolean(defaultValue));
    }

    protected void setInternalValue(SharedPreferences preferences, boolean value) {
        Editor editor = preferences.edit();
        editor.putBoolean(getKey() + INTERNAL_NAME_SUFFIX, value);
        editor.apply();
    }
}
