package com.vs_unusedappremover;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.vs_unusedappremover.actions.SortActionProvider;
import com.vs_unusedappremover.common.GA;
import com.vs_unusedappremover.data.ApplicationCollection;
import com.vs_unusedappremover.data.Applications;
import com.vs_unusedappremover.data.Applications.Filter;
import com.vs_unusedappremover.data.OrderBy;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

import java.util.Collection;
import java.util.List;

public class AppsFragment extends ListFragment {
	
	private static final String TAG = AppsFragment.class.getSimpleName();
	
	private static final String ARG_FILTER = "AppsFragment.filter";
	private static final String ARG_ORDER = "AppsFragment.sort";

	private enum Actions {
		REMOVE,
		LAUNCH,
		SEE_IN_PLAY_STORE,
		DONT_NOTIFY
	}
		
	private ApplicationsAdapter adapter;
	private ApplicationCollection applicationCollection;
	private OrderBy order = OrderBy.TIME_UNUSED;
	private Applications.Filter filter = Applications.Filter.DOWNLOADED;
	private boolean isCustomView;
	private UpdateDataTask updateTask;
	
	public static AppsFragment create(Applications.Filter show, OrderBy order) {
		Bundle args = new Bundle();
		putParameters(args, show, order);
		AppsFragment fragment = new AppsFragment();
		fragment.setArguments(args);
		return fragment;
	}
	
	private static void putParameters(Bundle toBundle, Applications.Filter show, OrderBy order) {
		toBundle.putInt(ARG_FILTER, show.ordinal());
		toBundle.putInt(ARG_ORDER, order.ordinal());
	}
		
	private void readParameters(Bundle args) {
		if (args.containsKey(ARG_FILTER)) {
			int filterIndex = args.getInt(ARG_FILTER);
			filter = Applications.Filter.values()[filterIndex];
		}
		
		if (args.containsKey(ARG_ORDER)) {
			int orderIndex = args.getInt(ARG_ORDER);
			order = OrderBy.values()[orderIndex];
		}
		
		isCustomView = (filter == Filter.UNUSED);
	}

	@Override
	public void onCreate(Bundle savedState) {		
		super.onCreate(savedState);
		
		applicationCollection = MyApplication.getInstance().getApplications();
		Bundle state = (savedState == null) ? getArguments() : savedState;		
		readParameters(state);
		
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (isCustomView) {
			return inflater.inflate(R.layout.list_unused, container, false);		
		}
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);

		adapter = new ApplicationsAdapter(getActivity());
        ListView l = getListView();
        l.setDivider(getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
		l.setAdapter(adapter);
		l.setOnItemClickListener(onItemClick);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {		
		super.onSaveInstanceState(outState);
		putParameters(outState, filter, order);
	}
	
	@Override
	public void onResume() {		
		super.onResume();		
		applicationCollection.addObserver(dataObserver);		
		dataObserver.onChanged();
	}
	
	@Override
	public void onPause() {
		applicationCollection.removeObserver(dataObserver);
		
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		stopDataUpdateIfNeeded();
		super.onDestroy();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.activity_main, menu);
	    
	    MenuItem sort = menu.findItem(R.id.menu_sort);
	    if (sort != null) {
	    	SortActionProvider p = (SortActionProvider) MenuItemCompat.getActionProvider(sort);
	    	p.setOrder(order);
	    	p.setOnSortSelectedListener(onSortChanged);
	    }
	    
	    MenuItem share = menu.findItem(R.id.menu_share);
	    if (share != null) {
	    	Activity activity = getActivity();
            ShareActionProvider p = (ShareActionProvider) MenuItemCompat.getActionProvider(share);
	    	
	    	Resources res = activity.getResources();
	    	String appName = res.getString(R.string.app_name);
	    	
	    	String url = "https://play.google.com/store/apps/details?id=" + activity.getPackageName();
	    	
	    	Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
	        shareIntent.setType("text/plain");
	        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, appName);
	        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, url);
            p.setShareIntent(shareIntent);
	    }
	}	
	
	private void uninstallPackage(String packageName) {	
       Uri packageURI = Uri.parse("package:" + packageName);
       startActivity(new Intent(Intent.ACTION_DELETE, packageURI));
    }
		
	private void onListItemClick(final AppEntry item, View view) {
		final Activity activity = getActivity();
		final QuickAction quickAction = new QuickAction(activity, QuickAction.HORIZONTAL);

		Resources res = activity.getResources();
        final PackageManager pm = activity.getPackageManager();
				
        quickAction.addActionItem(new ActionItem(
				Actions.REMOVE.ordinal(), 
				getString(R.string.action_remove), 
				scaleToActionSize(ContextCompat.getDrawable(activity, R.drawable.ic_delete_white_48dp))));
		
        quickAction.addActionItem(new ActionItem(
				Actions.LAUNCH.ordinal(),
				getString(R.string.action_launch),
				AppIcon.buildUrl(item.info.packageName)));
		
		final Intent openInPlayStoreIntent = getOpenInPlayStoreIntent(pm, item.info.packageName);		
		List<ResolveInfo> infos = pm.queryIntentActivities(openInPlayStoreIntent, 0);
		
		if (infos.size() > 0) {			
			quickAction.addActionItem(new ActionItem(
					Actions.SEE_IN_PLAY_STORE.ordinal(),
					getString(R.string.action_open_in_play_store),
					scaleToActionSize(infos.get(0).loadIcon(pm))));
		}
		
		quickAction.addActionItem(new ActionItem(
				Actions.DONT_NOTIFY.ordinal(),
				getString(R.string.action_dont_notify),
				ContextCompat.getDrawable(activity, R.drawable.ic_do_not_disturb_white_48dp)));
		       
		quickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				Actions action = Actions.values()[actionId];
				String packageName = item.info.packageName;
				
				switch (action) {
				case REMOVE : 
					uninstallPackage(packageName); 
					GA.event("Apps", "Uninstall");
					break;
				
				case LAUNCH :
					applicationCollection.notifyUsed(packageName, System.currentTimeMillis(), AppEntry.RanIn.FOREGROUND);
					Intent i = pm.getLaunchIntentForPackage(packageName);
					try {
						startActivity(i);
						GA.event("Apps", "Launch application");
					} catch (Exception e) {
						Toast.makeText(activity, R.string.toast_cant_launch_app, Toast.LENGTH_SHORT).show();
					}					
					break;
					
				case SEE_IN_PLAY_STORE :
					try {
						startActivity(Intent.createChooser(openInPlayStoreIntent, getString(R.string.action_launch)));
						GA.event("Apps", "Open in Play Market");
					} catch (Exception e) {
						Toast.makeText(activity, R.string.toast_cant_launch_app, Toast.LENGTH_SHORT).show();
					}					
					break;
					
				case DONT_NOTIFY:
					boolean willNotify = !item.notifyAbout;
					applicationCollection.setNotifyAbout(packageName, willNotify);
					GA.event("Apps", "Change notify/not notify");
					if (!willNotify) {
						Toast.makeText(activity, R.string.toast_dont_notify, Toast.LENGTH_SHORT).show();
					}
					break;
					
				default:
					Log.e(TAG, "TODO: Unknown action " + action);
				}
			}
		});
		
		quickAction.show(ApplicationViewHolder.getFromView(view).appIcon);
		GA.event("Apps", "Show application popup");
	}
		
	private Intent getOpenInPlayStoreIntent(PackageManager pm, String packageName) {
		
		Uri playUri = Uri.parse("market://details?id="+ packageName);
		Intent intent = new Intent(Intent.ACTION_VIEW, playUri);
		
		List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
		if (infos.size() > 0) return intent;
				
		playUri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName);
		return new Intent(Intent.ACTION_VIEW, playUri);		
	}
	
	private Drawable scaleToActionSize(Drawable d) {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		int size = (int)(QuickAction.ICON_SIZE_DP * metrics.density);
		Bitmap bmp = Bitmap.createScaledBitmap(drawableToBitmap(d), size, size, true);
		return new BitmapDrawable(getResources(), bmp);
	}

    private Bitmap drawableToBitmap(Drawable d) {
        if (d instanceof BitmapDrawable) {
            return ((BitmapDrawable)d).getBitmap();
        }
        Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        d.draw(canvas);
        return b;
    }
	
	private void stopDataUpdateIfNeeded() {
		if (updateTask != null && updateTask.getStatus() == Status.RUNNING) {
			updateTask.cancel(true);
		}
	}
		
	private final OnItemClickListener onItemClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			int index = position - getListView().getHeaderViewsCount();
			AppEntry app = adapter.getItem(index);
			onListItemClick(app, view);
		}
	};
	
	private final SortActionProvider.OnSortSelectedListener onSortChanged = 
			new SortActionProvider.OnSortSelectedListener() {		
		@Override
		public void onSortSelected(SortActionProvider source, OrderBy order) {
			AppsFragment.this.order = order;
			dataObserver.onChanged();
			GA.event("Apps", "Order by", order.toString());
		}
	};
	
	private final DataSetObserver dataObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
            View view = getView();
            if (view != null) {
                view.post(startUpdateData);
            }
		}
	};
	
	private final Runnable startUpdateData = new Runnable() {		
		@Override
		public void run() {
			Log.i(TAG, "Data changed");		
			stopDataUpdateIfNeeded();
			updateTask = new UpdateDataTask();
			updateTask.execute();
		}
	};
	
	private class UpdateDataTask extends AsyncTask<Void, Integer, Collection<AppEntry>> {

		@Override
		protected Collection<AppEntry> doInBackground(Void... params) {
			return applicationCollection.values(filter.create(), order);
		}
		
		@Override
		protected void onPostExecute(Collection<AppEntry> result) {
			if (getActivity() == null) return;			
			adapter.setApplications(result);
			if (!isCustomView) {
				setListShown(true);
			}
		}
	}
}
