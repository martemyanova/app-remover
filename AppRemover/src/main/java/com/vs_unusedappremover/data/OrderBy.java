package com.vs_unusedappremover.data;

import java.text.Collator;
import java.util.Comparator;

import com.vs_unusedappremover.AppEntry;
import com.vs_unusedappremover.R;

public enum OrderBy implements Comparator<AppEntry> {
	
	TIME_UNUSED(R.string.order_by_time_unused_action_bar,
			R.string.order_by_time_unused_menu) {
		@Override
		public int compare(AppEntry e1, AppEntry e2) {
			long time1 = Math.max(e1.installTime, e1.lastUsedTime);
			long time2 = Math.max(e2.installTime, e2.lastUsedTime);			
			if (time1 < time2) return -1;
			if (time1 > time2) return 1;
			return NAME.compare(e1, e2);
		}
	},
	
	NAME(R.string.order_by_name_action_bar, 
			R.string.order_by_name_menu) {
		@Override
		public int compare(AppEntry e1, AppEntry e2) {			
			return Collator.getInstance().compare(e1.label, e2.label);
		}
	},
	
	SIZE(R.string.order_by_size_action_bar, 
			R.string.order_by_size_menu) {
		@Override
		public int compare(AppEntry e1, AppEntry e2) {
			if (e1.size < e2.size) return 1;
			if (e1.size > e2.size) return -1;
			return 0;
		}
	};
	
	public final int shortTextResId;
	public final int fullTextResId;
		
	private OrderBy(int textResId, int fullTextResId) {
		this.shortTextResId = textResId;
		this.fullTextResId = fullTextResId;		
	}
}
