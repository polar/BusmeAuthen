package com.adiron.busme.authen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.adiron.busme.api.BuspassAPI;
import com.adiron.busme.api.DiscoverAPIVersion1;
import com.adiron.busme.api.DiscoverAPIVersion1.Master;
import com.appsolut.adapter.collections.CollectionsAdapter;
import com.google.android.maps.MapActivity;

public class LocationAccountActivity extends MapActivity {

	public LocationAccountActivity() {
		// TODO Auto-generated constructor stub
	}

	BackgroundThread backgroundThread;
	Handler uiHandler;
	BuspassAPI municipalityAPI;
	
	public Handler getBackgroundHandler() {
		return backgroundThread.getHandler();
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#isLocationDisplayed()
	 */
	@Override
	protected boolean isLocationDisplayed() {
		// TODO Auto-generated method stub
		return super.isLocationDisplayed();
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		
		backgroundThread = new BackgroundThread();
		backgroundThread.start();
		
		uiHandler = new Handler();
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onGetMapDataSource()
	 */
	@Override
	protected int onGetMapDataSource() {
		// TODO Auto-generated method stub
		return super.onGetMapDataSource();
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onNewIntent(android.content.Intent)
	 */
	@Override
	public void onNewIntent(Intent arg0) {
		// TODO Auto-generated method stub
		super.onNewIntent(arg0);
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

    public void showMunicipalitiesDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new MunicipalitiesDialogFragment();
        //dialog.show(getSupportFragmentManager(), "NoticeDialogFragment");
    }

	Spinner municipalitySpinner;
	
    List<Master> masters;

	// Given Data
	ArrayList<String> municipalities;
	ArrayList<String> municipalityUrls;

	DiscoverAPIVersion1 discoverAPI;
    
	final static int DIALOG_IN_PROGRESS = 2;
	final static int DIALOG_NETWORK_PROBLEM = 1;
	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_IN_PROGRESS:
			ProgressDialog dialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
			dialog.setTitle("Contacting Server");
			dialog.setMessage("Trying to get municipalities near you");
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface arg0) {
					LocationAccountActivity.this.finish();
				}
			});
			return dialog;
		case DIALOG_NETWORK_PROBLEM:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Contacting Server");
			builder.setMessage("Trying to get municipalities near you");
			builder.setCancelable(true);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
					
				}
            	
            });
	        AlertDialog alert = builder.create();
	        return alert;
		}
		return null;
	}
	
	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_IN_PROGRESS:
		}
	}
	
	private List<Master> getMunicipalities(Double lon, Double lat) throws IOException {
		// This process makes two successive web calls.
		boolean ret = discoverAPI.get();
		if (ret) {
			List<Master> result = discoverAPI.discover(lon, lat);
			return result;
		} else {
			return null;
		}
	}

	private void assignMasters(List<Master> masters) {
    	municipalities = new ArrayList<String>();
    	municipalityUrls = new ArrayList<String>();
	    if (masters != null) {
	    	for(Master m : masters) {
	    		municipalities.add(m.name);
	    		municipalityUrls.add(m.apiUrl);
	    	}
	    }
	    // GUI Data
	    SpinnerAdapter adapter = 
	    		new CollectionsAdapter<String>(this, 
	    				R.layout.login_activity_spinner_text_view, 
	    				municipalities);
	    municipalitySpinner.setAdapter(adapter);
	    LocationAccountActivity.this.masters = masters;
	}


	private class GetMastersTask extends AsyncTask<Double, List<Master>, List<Master>> {
    	
        @Override
        protected List<Master> doInBackground(Double... params) {
        	try {
        		List<Master> result = getMunicipalities(params[0],params[1]);
        		return result;
        	} catch (Exception e) {
        		return null;
        	}
        }

        @Override
        protected void onPostExecute(final List<Master> result) {
            removeDialog(DIALOG_IN_PROGRESS);
            assignMasters(result);
        }

        @Override
        protected void onCancelled() {
        }
    }

}
