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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;

public abstract class ConfigEntry {

    protected final class TextEditWatcher implements TextWatcher {
        private final SharedPreferences preferences;

        public TextEditWatcher(SharedPreferences preferences) {
            this.preferences = preferences;
        }

        @Override
        public void afterTextChanged(Editable string) {
            ConfigEntry.this.setValue(preferences, string.toString());
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // no-op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // no-op
        }
    }

    protected final String name;
    protected final String description;
    protected final String defaultValue;

    public ConfigEntry(String name, String defaultValue, String description) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public void ensureDefaultValue(SharedPreferences preferences, boolean force) {
        if (force || !preferences.contains(getKey())) {
            setValue(preferences, defaultValue);
        }
    }

    public String getValue(SharedPreferences preferences) {
        return preferences.getString(getKey(), null);
    }

    abstract public View getView(LayoutInflater layouInflater, SharedPreferences preferences);

    abstract public void readValueFromProperties(SharedPreferences preferences, Properties properties);

    public void setValue(SharedPreferences preferences, String value) {
        Editor editor = preferences.edit();
        editor.putString(getKey(), value);
        editor.commit();
    }

    public void writeValueToProperties(SharedPreferences preferences, Properties properties) {
        properties.setProperty(name, getValue(preferences));
    }

    /**
     * @return the lookup key of this config entry for use in the preference
     *         objects
     */
    protected String getKey() {
        return name.toUpperCase();
    }
}
