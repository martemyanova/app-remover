package com.vs_unusedappremover.common;

import android.os.Handler;

public abstract class HandlerTimer {
	
	private final Handler handler;
	private final long intervalMillis;
	private boolean isEnabled;
	
	public HandlerTimer(Handler handler, long intervalMillis) {
		this.handler = handler;
		this.intervalMillis = intervalMillis;
	}
	
	public void setEnabled(boolean value) {
		isEnabled = value;
		if (isEnabled) {
			timerRunnable.run();
		}
	}
	
	protected abstract void onTick();
	
	private final Runnable timerRunnable = new Runnable() {		
		@Override
		public void run() {
			onTick();
			if (isEnabled) {
				handler.removeCallbacks(this);
				handler.postDelayed(this, intervalMillis);
			}
		}
	};
}

