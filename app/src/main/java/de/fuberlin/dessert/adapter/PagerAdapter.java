package de.fuberlin.dessert.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import de.fuberlin.dessert.activity.TabDaemonRepositoryActivity;
import de.fuberlin.dessert.activity.TabInstalledDaemonsActivity;
import de.fuberlin.dessert.activity.TabRunningDaemonActivity;

public class PagerAdapter extends FragmentStatePagerAdapter {
	final int mNumOfTabs;

	public PagerAdapter(FragmentManager fm, int NumOfTabs) {
		super(fm);
		this.mNumOfTabs = NumOfTabs;
	}

	@Override
	public Fragment getItem(int position) {

		switch (position) {
			case 0:
				return new TabDaemonRepositoryActivity();
			case 1:
				return new TabInstalledDaemonsActivity();
			case 2:
				return new TabRunningDaemonActivity();
			default:
				return null;
		}
	}

	@Override
	public int getCount() {
		return mNumOfTabs;
	}
}
