package com.vs_unusedappremover.common;

public abstract class RunIfTimeElapsed  implements Runnable {
	
	private long lastRunMillis;
	
	public void runIfElapsed(long interval) {
		long time = System.currentTimeMillis();
		if (time - lastRunMillis > interval) {
			lastRunMillis = time;
			run();
		}
	}
}
