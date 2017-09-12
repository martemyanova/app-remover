package com.vs_unusedappremover.common;

public class AsyncResult<T> {

	private final Object waitMonitor = new Object();
	private T result;
	private boolean hasResult;
	
	public void set(T result) {
		synchronized (waitMonitor) {
			this.result = result;
			hasResult = true;
			waitMonitor.notify();
		}
	}
	
	public T get() {
		synchronized (waitMonitor) {
			while (!hasResult) {
				try {
					waitMonitor.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			return result;
		}
	}
}
