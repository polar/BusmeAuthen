package com.adiron.busme.authen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.adiron.busme.api.DiscoverAPIVersion1;
import com.adiron.busme.api.DiscoverAPIVersion1.Master;
import com.appsolut.adapter.collections.CollectionsAdapter;

/**
 * This activity gets information from the user and creates
 * a Busme Account for a particular Municipality if it authenticates.
 * <p>
 * Once it collects the required information from the user, this activity
 * invokes another activity, which the user can cancel, that goes off
 * to the authentication server to check the email/password combination
 * for a particular Municipality.
 * <p>
 * It takes the following intents:
 * <ul>
 * <li>Authenticator.KEY_MUNICIPALITY_NAMES, ArrayList<String>, Required
 * <li>Authenticator.KEY_MUNICIPALITY_URLS, ArrayList<String>, Required
 * <li>AccountManager.KEY_ACCOUNT_EMAIL, String  - Optional
 * <li>AccountManager.KEY_ACCOUNT_TYPE, String - Required
 * <li>Authenticator.KEY_AUTHENTICATOR_ACTION, String, Required values:
 * <ul>
 *  <li>Authenticator.ACTION_ADD_ACCOUNT
 *  <li>Authenticator.ACTION_UPDATE_ACCOUNT
 * </ul>
 * </ul>
 * It returns the AccountManagerResponse as per the Android Framework.
 */
public class AccountActivity extends AccountAuthenticatorActivity {
	private final static String LOGTAG = AccountActivity.class.getName();
	
	public static String BUSME_API_URL = "http://adiron.com:3002/apis/d1";
	
	// GUI
	private TextView messageView;
    private Button performButton;
    private Button cancelButton;
	private TextView bottomMessageView;
	private Spinner municipalitySpinner;

	// Given Data
	private ArrayList<String> municipalities;
	private ArrayList<String> municipalityUrls;


	private Object authenticatorAction;

	DiscoverAPIVersion1 discoverAPI;
	
    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle icicle) {
        Log.i(LOGTAG, "onCreate(" + icicle + ")");
        super.onCreate(icicle);
        
        discoverAPI = DiscoverAPIVersion1.getInstance(BUSME_API_URL);
        
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.login_activity);
        
        getWindow().setFeatureDrawableResource(
                Window.FEATURE_LEFT_ICON, android.R.drawable.ic_input_add);

        Intent intent = getIntent();
        
        authenticatorAction = intent.getStringExtra(Authenticator.KEY_AUTHENTICATOR_ACTION);
        
        // GUI
        municipalitySpinner = (Spinner) findViewById(R.id.municipalitySpinner);
        messageView = (TextView) findViewById(R.id.messageView);
        
        performButton = (Button) findViewById(R.id.performButton);
        if (Authenticator.ACTION_ADD_ACCOUNT.equals(authenticatorAction)) {
        	performButton.setText(R.string.login_activity_perform_button_add);
        } else if (Authenticator.ACTION_UPDATE_ACCOUNT.equals(authenticatorAction)) {
        	performButton.setText(R.string.login_activity_perform_button_update);
        }
        
        cancelButton = (Button) findViewById(R.id.cancelButton);
        bottomMessageView = (TextView) findViewById(R.id.message_bottom);

        messageView.setText(R.string.login_activity_message_add_account);
        bottomMessageView.setText(R.string.login_acitivity_bottom_message_instructions);
        
        // GUI Functionality
        performButton.setOnClickListener(new PerformClickListener());
		cancelButton.setOnClickListener(new CancelClickListener());

    	LocationManager locationManager = (LocationManager) getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
    	Criteria myCriteria = new Criteria();
    	myCriteria.setAccuracy(Criteria.ACCURACY_FINE);
    	myCriteria.setPowerRequirement(Criteria.POWER_LOW);
    	// let Android select the right location provider for you
    	String myProvider = locationManager.getBestProvider(myCriteria, true); 
    	// finally require updates at -at least- the desired rate
    	long minTimeMillis = 600000; // 600,000 milliseconds make 10 minutes
    	lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    	locationManager.requestLocationUpdates(myProvider,minTimeMillis,0,locationListener);
    	if (lastLocation != null) {
    		new GetMastersTask().execute(lastLocation.getLongitude(), lastLocation.getLatitude());
    	}
    	showDialog(DIALOG_IN_PROGRESS);
    }
    
    List<Master> masters;
    
    Location lastLocation = null;
    
    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
        	lastLocation = location;
        	if (lastLocation != null && masters == null) {
        		new GetMastersTask().execute(lastLocation.getLongitude(), lastLocation.getLatitude());
        	}
        }

		@Override
		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub
			
		}
    };
    
    
    /**
     * This listener applies to a click on the performButton (Add Account).
     */
	private class PerformClickListener implements OnClickListener
	{
		@Override public void onClick(View pView)
		{
			final int municipalityIdx = municipalitySpinner.getSelectedItemPosition();
            String municipalityName = municipalities.get(municipalityIdx);
            String municipalityUrl = municipalityUrls.get(municipalityIdx);
            
			// The account name as far as Android goes is the municipality name.
			String accountName = municipalityName; // May not be unique enough.
			
			// Set up and activity to check the credentials before adding this account
			// to the AccountManager.
			Intent intent = new Intent(AccountActivity.this, VerifyAccountActivity.class);
			intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
			intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE);
	        intent.putExtra(Authenticator.KEY_MUNICIPALITY_NAME, municipalityName);
	        intent.putExtra(Authenticator.KEY_MUNICIPALITY_URL, municipalityUrl);

			Log.d(LOGTAG, "Starting CheckingAccountActivity(pid="+Binder.getCallingPid()+
					", uid="+Binder.getCallingUid()+", name="+accountName);
			AccountActivity.this.startActivity(intent);
			Log.d(LOGTAG, "Started next Activity and now finishing.");
			AccountActivity.this.finish();
		}
	}
	
	private class CancelClickListener implements OnClickListener
	{
		@Override public void onClick(View pView)
		{
			// Using null here sends ERROR_CODE_CANCELED as per Android Fremwork documentation.
			AccountActivity.this.setAccountAuthenticatorResult(null);
			AccountActivity.this.finish();
		}
	}

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
					AccountActivity.this.finish();
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
	    AccountActivity.this.masters = masters;
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