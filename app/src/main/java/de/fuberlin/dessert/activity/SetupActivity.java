/*******************************************************************************
 * Copyright 2010, Freie Universitaet Berlin (FUB). All rights reserved.
 *
 * These sources were developed at the Freie Universitaet Berlin,
 * Computer Systems and Telematics / Distributed, embedded Systems (DES) group
 * (http://cst.mi.fu-berlin.de, http://www.des-testbed.net)
 * -----------------------------------------------------------------------------
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
 * -----------------------------------------------------------------------------
 * For further information and questions please use the web site
 *        http://www.des-testbed.net
 ******************************************************************************/
package de.fuberlin.dessert.activity;

import java.io.File;
import java.util.Arrays;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.tasks.NativeTasks;

/**
 * global preferences activity
 */
public class SetupActivity extends PreferenceActivity {

	/**
	 * key to the preference of the port of the cli telnet interface of the
	 * running daemon
	 */
	public static final String KEY_CLI_PORT = "cliport";
	/**
	 * key to the preference of the mesh interface
	 */
	public static final String KEY_MESH_IF = "meshif";
	/**
	 * key to the preference of the system interface
	 */
	public static final String KEY_SYS_IF = "sysif";
	/**
	 * key to the preference of the system interface ip
	 */
	public static final String KEY_SYS_IP = "sysip";
	/**
	 * key to the preference of the system interface netmask
	 */
	public static final String KEY_SYS_MASK = "sysmask";
	/**
	 * key to the preference of the system interface mtu value
	 */
	public static final String KEY_SYS_MTU = "sysmtu";
	/**
	 * key to the preference of the repository url
	 */
	public static final String KEY_REPO_URL = "repourl";
	/**
	 * key to the preference of the path to the su command
	 */
	public static final String KEY_PATH_SU = "pathsu";
	/**
	 * key to the preference of the path to the sh command
	 */
	public static final String KEY_PATH_SH = "pathsh";
	/**
	 * key to the preference of the path to the ln command
	 */
	public static final String KEY_PATH_LN = "pathln";
	/**
	 * key to the preference of the path to the chmod command
	 */
	public static final String KEY_PATH_CHMOD = "pathchmod";
	/**
	 * key to the preference of the path to the kill command
	 */
	public static final String KEY_PATH_KILL = "pathkill";

	private static final String[] pathStrings = {
			KEY_PATH_SU,
			KEY_PATH_CHMOD,
			KEY_PATH_KILL,
			KEY_PATH_LN,
			KEY_PATH_SH
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefFragment()).commit();
	}

	public static class PrefFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

		@Override
		public void onResume() {
			super.onResume();
			getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onPause() {
			getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
			super.onPause();
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences preference, String key) {
			// managing paths for: su, sh, ln, chmod, kill
			String path = preference.getString(key, null);
			if (Arrays.asList(pathStrings).contains(key) && path != null) {
				File file = new File(path);
				if (!file.exists() && !file.isFile()) {
					// show dialog
					NativeTasks.setCheckedForNativeCommands(false);
					new AlertDialog.Builder(getActivity())
							.setCancelable(true)
							.setTitle(R.string.native_command_not_found_title)
							.setMessage(getString(R.string.native_command_not_found_msg, path))
							.setNeutralButton(R.string.ok, null)
							.show();
				}
			}
		}
	}
}
