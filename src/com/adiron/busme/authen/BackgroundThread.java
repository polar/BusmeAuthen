package com.adiron.busme.authen;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * This thread handles all the background work. Use the handler
 * to post Runnables to it.
 * @author polar
 *
 */
public class BackgroundThread extends Thread {
	static final String LOGTAG = "BackgroundThread";
	private Handler handler;
	
	BackgroundThread() {
		this.setName("BackgroundThread");
	}
	
	public Handler getHandler() {
		// NB: Dangerous I know, but we really don't want to work up a sync scheme for this.
		// It gets called immediately after Thread.start();
		while(handler == null);
		return handler;
	}
	
	@Override
	public void run() {
		try {
			Log.d(LOGTAG, "started.");
			Looper.prepare();
			
			handler = new Handler();
			
			Looper.loop();
		} catch (Throwable t) {
			Log.e(LOGTAG, "halted due to error", t);
		}
	}
	
	public void pleaseStop() {
		handler.post(new Runnable() {
			public void run() {
				Log.i(LOGTAG, "BackgroundThread stop");
			    Looper.myLooper().quit();
			}
		});
	}
}
