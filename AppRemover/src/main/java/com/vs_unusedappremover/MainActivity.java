package com.vs_unusedappremover;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;

import com.vs_unusedappremover.common.GA;
import com.vs_unusedappremover.data.Applications;
import com.vs_unusedappremover.data.OrderBy;

public class MainActivity extends ActionBarActivity {

	private static final String EXTRA_PAGE = "page";
	private static final String EXTRA_FROM_NOTIFICATION = "from notification";
	
	private static enum Pages {
		DOWNLOADED(R.string.title_downloaded_applications) {
			@Override
			public Fragment createFragment() {
				return AppsFragment.create(Applications.Filter.DOWNLOADED, OrderBy.TIME_UNUSED);
			}
		},
		
		UNUSED(R.string.title_unused_applications) {
			@Override
			public Fragment createFragment() {
				return AppsFragment.create(Applications.Filter.UNUSED, OrderBy.SIZE);
			}			
		};
		
		public final int titleResId;
		
		public abstract Fragment createFragment(); 
		
		private Pages(int titleResId) {
			this.titleResId = titleResId;
		}
	}

    public static Intent showUnusedFromNotificationIntent(Context context) {
		Intent i = new Intent(context, MainActivity.class);
		i.putExtra(EXTRA_PAGE, Pages.UNUSED.ordinal());
		i.putExtra(EXTRA_FROM_NOTIFICATION, true);
		return i;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(sectionsPagerAdapter);
		int item = getIntent().getIntExtra(EXTRA_PAGE, Pages.DOWNLOADED.ordinal());		
		viewPager.setCurrentItem(item);
		
		if (getIntent().getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
			GA.event("MainActivity", "Start from notification");
		}
	}

	@Override
	protected void onStart() {		
		super.onStart();
		GA.onActivityStart(this);
	}
	
	@Override
    protected void onPostCreate(Bundle savedInstanceState) {    	
    	super.onPostCreate(savedInstanceState);
    	
    	boolean isRestored = (savedInstanceState != null);
    	if (!isRestored) {
            boolean permissionRequestShown = RequestPermissionDialog.showIfNeeded(this);
            if (!permissionRequestShown) {
                RateThisAppDialog.showIfNeeded(this);
            }
    	}
    }
	
	@Override
	protected void onStop() {
		GA.onActivityStop(this);
		super.onStop();
	}
			
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return getResources().getString(Pages.values()[position].titleResId);
		}
		
		@Override
		public Fragment getItem(int position) {
			return Pages.values()[position].createFragment();
		}

		@Override
		public int getCount() {
			return Pages.values().length;
		}
	}
}
