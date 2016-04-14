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

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.fuberlin.dessert.DessertApplication;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.model.daemon.DaemonInfo;

public class DaemonListAdapter<T extends DaemonInfo> extends ArrayAdapter<T> {

    private static final StrikethroughSpan STRIKE_THRU_STYLE_SPAN = new StrikethroughSpan();

    private final LayoutInflater layoutInflater;
    private final boolean markInstalled;
    private final boolean showIcon;

    public DaemonListAdapter(Context context, List<T> objects, boolean markInstalled, boolean showIcon) {
        super(context, R.layout.daemon_list_item, objects);

        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.markInstalled = markInstalled;
        this.showIcon = showIcon;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // inflate view or reuse
        View itemView;
        if (convertView == null) {
            itemView = layoutInflater.inflate(R.layout.daemon_list_item, parent, false);
        } else {
            itemView = convertView;
        }

        // get text views
        ImageView iconView = (ImageView) itemView.findViewById(R.id.Icon);
        TextView upperLeftText = (TextView) itemView.findViewById(R.id.UpperLeftText);
        TextView upperRightText = (TextView) itemView.findViewById(R.id.UpperRightText);
        TextView lowerCenterText1 = (TextView) itemView.findViewById(R.id.LowerCenterText1);
        TextView lowerCenterText2 = (TextView) itemView.findViewById(R.id.LowerCenterText2);

        // get item and set text views
        T item = getItem(position);
        Drawable icon = item.getIconDrawable();
        upperLeftText.setText(item.getName(), TextView.BufferType.SPANNABLE);
        upperRightText.setText(item.getVersion(), TextView.BufferType.SPANNABLE);
        lowerCenterText1.setText("Library Version:\t\t\t" + item.getLibraryVersion(), TextView.BufferType.SPANNABLE);
        lowerCenterText2.setText("Application Version:\t" + item.getApplicationVersion(), TextView.BufferType.SPANNABLE);

        // format it if not compatible
        boolean libOK = DessertApplication.instance.getLibraryVersion().isCompatible(item.getLibraryVersion());
        boolean appOK = DessertApplication.instance.getApplicationVersion().isCompatible(item.getApplicationVersion());

        if (!libOK || !appOK) {
            Spannable nameSpan = (Spannable) upperLeftText.getText();
            nameSpan.setSpan(STRIKE_THRU_STYLE_SPAN, 0, nameSpan.length(), 0);
            Spannable versionSpan = (Spannable) upperRightText.getText();
            versionSpan.setSpan(STRIKE_THRU_STYLE_SPAN, 0, versionSpan.length(), 0);
        }

        if (!libOK) {
            Spannable span = (Spannable) lowerCenterText1.getText();
            span.setSpan(STRIKE_THRU_STYLE_SPAN, 0, span.length(), 0);
        }

        if (!appOK) {
            Spannable span = (Spannable) lowerCenterText2.getText();
            span.setSpan(STRIKE_THRU_STYLE_SPAN, 0, span.length(), 0);
        }

        // disable if this is already installed
        if (markInstalled && DessertApplication.instance.getInstalledDaemon(item.getDaemonID()) != null) {
            upperLeftText.setEnabled(false);
            upperRightText.setEnabled(false);
            lowerCenterText1.setEnabled(false);
            lowerCenterText2.setEnabled(false);
        } else {
            upperLeftText.setEnabled(true);
            upperRightText.setEnabled(true);
            lowerCenterText1.setEnabled(true);
            lowerCenterText2.setEnabled(true);
        }

        if (showIcon) {
            iconView.setVisibility(View.VISIBLE);
            if (icon == null) {
                iconView.setImageDrawable(DessertApplication.defaultDaemonIcon);
            } else {
                iconView.setImageDrawable(icon);
            }
        } else {
            iconView.setVisibility(View.GONE);
        }

        // return item view
        return itemView;
    }
}
