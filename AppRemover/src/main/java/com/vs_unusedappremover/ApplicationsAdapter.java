package com.vs_unusedappremover;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vs_unusedappremover.common.MillisecondsIn;
import com.vs_unusedappremover.data.Plural;

import java.util.ArrayList;
import java.util.Collection;

public class ApplicationsAdapter extends BaseAdapter {
	
	private final Context context;
	private final ElapsedTimeFormatter elapsedTime;
	private final UnknownUsageTimeFormatter unknownUsageTime;
	private ArrayList<AppEntry> items = new ArrayList<>();

	public ApplicationsAdapter(Context context) {
		this.context = context;
		this.elapsedTime = new ElapsedTimeFormatter(context);
		this.unknownUsageTime = new UnknownUsageTimeFormatter(context);
	}
	
	public void setApplications(Collection<AppEntry> applications) {
		items.clear();
		items.addAll(applications);
		notifyDataSetChanged();
	}
		
	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public AppEntry getItem(int at) {
		return items.get(at);
	}

	@Override
	public long getItemId(int at) {
		return at;
	}

	@Override
	public View getView(int index, View convertView, ViewGroup parent) {
		final ApplicationViewHolder holder = ApplicationViewHolder.createOrReuse(context, convertView);
		convertView = holder.rootView;
		Context context = convertView.getContext();
		
        final AppEntry entry = getItem(index);
        holder.appName.setText(entry.label);

		MyApplication.getInstance().picasso().load(AppIcon.buildUrl(entry.info.packageName)).into(holder.appIcon);

        holder.doNotNotify.setVisibility(entry.notifyAbout ? View.INVISIBLE : View.VISIBLE);
        
        String sizeStr = (entry.size != PackageSize.UNKNOWN) ? Formatter.formatFileSize(context, entry.size) : null;
        if (sizeStr != null) {
        	holder.appSize.setText(sizeStr);
        } else {        	
        	holder.appSize.setText(R.string.app_size_unknown);
        }       
        
        String lastUsedText;
		long unusedTime = entry.lastUsedTime - System.currentTimeMillis();
		if ((entry.lastUsedTime != 0) || unusedTime < MillisecondsIn.DAY * 365 * 3) {
        	lastUsedText = elapsedTime.format(entry.lastUsedTime, entry.ranIn);
        } else {
        	lastUsedText = unknownUsageTime.format(entry.installTime);
        }
        holder.lastUsed.setText(lastUsedText);
                
        return convertView;
	}
}

class ElapsedTimeFormatter {
	
	private final Resources res;
	private final Plural pluralRes;
	
	ElapsedTimeFormatter(Context context) {
		this.res = context.getResources();
		this.pluralRes = new Plural(res);
	}
		
	String format(long lastUsed, AppEntry.RanIn ranIn) {
		if (lastUsed == 0) return res.getString(R.string.havent_seen_app_running);
		
		long now = System.currentTimeMillis();
		long diff = now - lastUsed;

		boolean isToday = DateUtils.isToday(lastUsed);

		if (diff > MillisecondsIn.DAY || !isToday) {
			int count = (int)(diff / MillisecondsIn.DAY);
			count = Math.max(1, count);
			return lastUsed(ranIn, R.plurals.day_count, count);
		}
				
		if (diff > 12 * MillisecondsIn.HOUR) {
            switch (ranIn) {
                case BACKGROUND: return res.getString(R.string.ran_in_background_today);
                case FOREGROUND:
                default:
                    return res.getString(R.string.used_today);
            }
		}		
		
		if (diff > MillisecondsIn.HOUR) {
			int count = (int)(diff / MillisecondsIn.HOUR);
			return lastUsed(ranIn, R.plurals.hour_count, count);
		}
		
		if (diff > 5 * MillisecondsIn.MINUTE) {
			int count = (int)(diff / MillisecondsIn.MINUTE);
			return lastUsed(ranIn, R.plurals.minute_count, count);
		}

        switch (ranIn) {
            case BACKGROUND:
                return res.getString(R.string.ran_in_background_just);
            case FOREGROUND:
            default:
                return res.getString(R.string.used_just);
        }
	}
	
	private String lastUsed(AppEntry.RanIn ranIn, int unitsPluralId, int quantity) {
        final String ranInFormat;
        switch (ranIn) {
            case BACKGROUND:
                ranInFormat = res.getString(R.string.ran_in_background_X_ago); break;
            case FOREGROUND:
            default:
                ranInFormat = res.getString(R.string.used_by_user_X_ago);
        }
        final String timeLeft = pluralRes.format(unitsPluralId, quantity, quantity);
        return String.format(ranInFormat, timeLeft);
	}
}

class UnknownUsageTimeFormatter {
	private final long removerInstallTime = MyApplication.getInstance().getInstallTime();
	private final Resources res;
	private final Plural pluralRes;
	
	UnknownUsageTimeFormatter(Context context) {
		this.res =context.getResources(); 
		this.pluralRes = new Plural(res);
	}
	
	String format(long installTime) {
		long timeInstalled = Math.max(installTime, removerInstallTime);
		long timeLeft =  System.currentTimeMillis() - timeInstalled;		
		
		int days = (int)(timeLeft / MillisecondsIn.DAY);
		if (days > 0) {
			String daysString = pluralRes.format(R.plurals.day_count, days, days);
			String format = res.getString(R.string.unused_at_least_X); 
			return String.format(format, daysString);
		}
		return res.getString(R.string.havent_seen_app_running);
	}
}

class ApplicationViewHolder {
	
	final View rootView;
    final TextView appName;
    final ImageView appIcon;
    final TextView appSize;
    final TextView lastUsed;
    final ImageView doNotNotify;
    		
	static ApplicationViewHolder createOrReuse(Context context, View convertView) {
		if (convertView != null) {
			return getFromView(convertView);
		}
		return new ApplicationViewHolder(context);
	}
	
	static ApplicationViewHolder getFromView(View v) {
		return (ApplicationViewHolder)v.getTag();
	}
	
	private ApplicationViewHolder(Context context) {
		rootView = View.inflate(context, R.layout.list_item_application, null);
		appName = (TextView)rootView.findViewById(R.id.app_name);
        appIcon = (ImageView)rootView.findViewById(R.id.app_icon);
        appSize = (TextView)rootView.findViewById(R.id.app_size);
        lastUsed = (TextView)rootView.findViewById(R.id.app_last_used);
        doNotNotify = (ImageView)rootView.findViewById(R.id.app_dont_notify);
		Drawable drawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_do_not_disturb_black_24dp));
		DrawableCompat.setTint(drawable, 0x80000000);
		doNotNotify.setImageDrawable(drawable);
		rootView.setTag(this);
	}
}
