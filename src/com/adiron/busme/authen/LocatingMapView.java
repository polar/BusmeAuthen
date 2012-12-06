package com.adiron.busme.authen;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class LocatingMapView extends MapView {
	LocationOverlay locationOverlay;
	

	public LocatingMapView(Context context, String apiKey) {
		super(context, apiKey);
		// TODO Auto-generated constructor stub
	}

	public LocatingMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
	    
		locationOverlay = new LocationOverlay(this.getContext(), this);
		getOverlays().add(locationOverlay);
	}

	public LocatingMapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}
	
	public GeoPoint getMyLocation() {
		return locationOverlay.getMyLocation();
	}
	
	Runnable runOnFirstFix;
	
	public void runOnFirstFix(Runnable r) {
		runOnFirstFix = r;
	}

}
