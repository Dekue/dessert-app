package de.fuberlin.dessert.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import de.fuberlin.dessert.activity.TabDaemonRepositoryActivity;
import de.fuberlin.dessert.activity.TabInstalledDaemonsActivity;
import de.fuberlin.dessert.activity.TabRunningDaemonActivity;

public class PagerAdapter extends FragmentStatePagerAdapter {
	int mNumOfTabs;

	public PagerAdapter(FragmentManager fm, int NumOfTabs) {
		super(fm);
		this.mNumOfTabs = NumOfTabs;
	}

	@Override
	public Fragment getItem(int position) {

		switch (position) {
			case 0:
				TabDaemonRepositoryActivity tab1 = new TabDaemonRepositoryActivity();
				return tab1;
			case 1:
				TabInstalledDaemonsActivity tab2 = new TabInstalledDaemonsActivity();
				return tab2;
			case 2:
				TabRunningDaemonActivity tab3 = new TabRunningDaemonActivity();
				return tab3;
			default:
				return null;
		}
	}

	@Override
	public int getCount() {
		return mNumOfTabs;
	}
}
