package com.adiron.busme.authen;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class LocationOverlay extends MyLocationOverlay {
	private final static String LOGTAG = LocationOverlay.class.getName();
	
    GestureDetector gestureDetector;
    MapView mapView;

	public LocationOverlay(Context context, MapView mapView) {
		super(context, mapView);
		this.mapView = mapView;
    	OnGestureListener lis = new SimpleOnGestureListener();
		gestureDetector = new GestureDetector(context, lis);
		gestureDetector.setOnDoubleTapListener(new MyOnDoubleTapListener());
	}

    private class MyOnDoubleTapListener implements GestureDetector.OnDoubleTapListener {

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			Log.d(LOGTAG, "DOUBLE TAP!!");
			mapView.getController().animateTo(LocationOverlay.this.getMyLocation());
			return true;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			Log.d(LOGTAG, "DOUBLE TAP EVENT!!");
			return false;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			Log.d(LOGTAG, "SINGE TAP CONFIRMED!!");
			return false;
		}
    }
	
	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		gestureDetector.onTouchEvent(e);
		return false;
	}
}
