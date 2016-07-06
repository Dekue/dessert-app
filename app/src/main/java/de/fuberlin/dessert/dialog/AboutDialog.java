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
package de.fuberlin.dessert.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;
import de.fuberlin.dessert.DessertApplication;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.model.LibraryVersion;

/**
 * About dialog...
 */
class AboutDialog extends AlertDialog {

    /**
     * Creates a new about dialog ready to be show()n.
     * 
     * @param context context in which to create the view of this dialogs
     */
    public AboutDialog(Context context) {
        super(context, R.style.dialog_theme);

        View aboutView = getLayoutInflater().inflate(R.layout.about_dialog, null);
        setView(aboutView);
        setTitle(R.string.about);
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        setButton(BUTTON_NEUTRAL, getContext().getString(R.string.close), (DialogInterface.OnClickListener) null);

        TextView appVersionText = (TextView) aboutView.findViewById(R.id.AppVersionText);
        appVersionText.setText(DessertApplication.instance.getApplicationVersion().toString());

        Map<String, LibraryVersion> installedLibs = DessertApplication.instance.getInstalledLibrariesVersions();
        List<String> sortedLibNames = new ArrayList<>(installedLibs.keySet());
        Collections.sort(sortedLibNames);

        int nameLength = 0;
        for (String libName : sortedLibNames) {
            nameLength = Math.max(nameLength, libName.length());
        }
        nameLength += 4; // spaces between name and version
        char[] tabs = new char[(int) Math.ceil((nameLength + 5) / 2d)]; // magic conversion between spaces and tabs
        Arrays.fill(tabs, '\t');

        StringBuilder libVersion = new StringBuilder();
        boolean first = true;
        for (String libName : sortedLibNames) {
            if (first) {
                first = false;
            } else {
                libVersion.append("\n");
            }
            libVersion.append(libName);
            libVersion.append(tabs, 0, (int) Math.ceil((nameLength - libName.length() - 1) / 2d)); // align by using tabs equal to the amount of remaining spaces
            libVersion.append(installedLibs.get(libName).toString());
        }

        TextView libVersionText = (TextView) aboutView.findViewById(R.id.LibVersionText);
        libVersionText.setText(libVersion.toString());
    }
}
