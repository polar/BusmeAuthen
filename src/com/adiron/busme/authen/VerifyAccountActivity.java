/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.adiron.busme.authen;

import java.io.IOException;

import com.adiron.busme.api.BuspassAPI;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity that displays a screen to the user that has pop up diaglogs
 * concerning the verification of the account. This is called by the
 * AccountActivity.
 * 
 * The Intents are:
 * AccountManager.KEY_ACCOUNT_NAME, String Required
 * AccountManager.KEY_PASSWORD, String Required
 * AccountManager.KEY_ACCOUNT_TYPE, String Required
 * Authenticator.KEY_MUNICIPALITY_NAME, String Required
 * Authenticator.KEY_MUNICIPALITY_UR, String Required
 * Authenticator.KEY_ACCOUNT_EMAIL, String Required
 * 
 */
public class VerifyAccountActivity extends AccountAuthenticatorActivity {
	private final static String LOGTAG = VerifyAccountActivity.class.getName();
	
	public final static int RESULT_AUTHENTICATION_FAILED = RESULT_FIRST_USER + 0;
	public final static int RESULT_SERVER_FAILED = RESULT_FIRST_USER + 1;

	/** Time the alert dialog waits before it dismisses itself. */
	private static final long EXIT_DIALOG_DISMISS_TIME = 5000;

	private UserLoginTask mAuthTask;

    private Button cancelButton;
	private TextView municipalityView;
	private ProgressBar checkProgressBar;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle icicle) {
        Log.i(LOGTAG, "onCreate(" + icicle + ")");
        super.onCreate(icicle);
		Log.d(LOGTAG, "CheckingAccountActivity(pid="+Binder.getCallingPid()+", uid="+Binder.getCallingUid()+")");

        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.verify_account_activity);
        
        getWindow().setFeatureDrawableResource(
                Window.FEATURE_LEFT_ICON, android.R.drawable.ic_input_add);

        checkProgressBar = (ProgressBar) findViewById(R.id.checkProgressBar);
        cancelButton = (Button) findViewById(R.id.checkCancelButton);
        municipalityView = (TextView) findViewById(R.id.municipalityView);

		cancelButton.setOnClickListener(new CancelClickListener());
		checkProgressBar.setIndeterminate(true);
    }
    
	public void onResume() {
		Log.d(LOGTAG, "onResume()");

		super.onResume();

		Intent intent = getIntent();
		Uri uri = intent.getData();
		if (uri != null && uri.toString().startsWith("busme://oauthresponse")) {
			this.respondWithCredentials(uri);
		} else {
			new UserLoginTask().execute(intent.getStringExtra(Authenticator.KEY_MUNICIPALITY_URL));
		}

	}

    private void respondWithCredentials(Uri uri) {
 	   Intent result = new Intent();
 	   Log.i(LOGTAG, "respondWithCredentials:" + uri);
 	   String accountAuthToken = uri.getQueryParameter("access_token");
 	   String accountName = uri.getQueryParameter("master");
 	   if (accountAuthToken != null && accountName != null) {
			result.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
			result.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE);
			result.putExtra(AccountManager.KEY_AUTHTOKEN, accountAuthToken);
			result.putExtra("RESULT_CODE", RESULT_OK);
			onActivityResult(RESULT_OK, result);
 	   } else {
			result.putExtra("RESULT_CODE", RESULT_AUTHENTICATION_FAILED);
			onActivityResult(RESULT_AUTHENTICATION_FAILED, result);
 	   }
 	   onAuthenticationResult(result);
 	}

	
	protected void onActivityResult( int resultCode, Intent data) {
		Log.d(LOGTAG, "onActivityResult(resultCode=" + resultCode + ", data=" + data);
		Bundle result = new Bundle();

		// Activity canceled by user.
		if (resultCode == VerifyAccountActivity.RESULT_CANCELED) {
			Log.d(LOGTAG, "CheckAcountActivity returns RESULT_CANCELED");
			result.putInt(AccountManager.KEY_ERROR_CODE,
					AccountManager.ERROR_CODE_CANCELED);
			result.putString(AccountManager.KEY_ERROR_MESSAGE,
					"Add Account Canceled");
			VerifyAccountActivity.this.setAccountAuthenticatorResult(result);
		}
		// Could not contact the authentication server or some other IO error.
		else if (resultCode == VerifyAccountActivity.RESULT_SERVER_FAILED) {
			Log.d(LOGTAG, "CheckAcountActivity returns RESULT_SERVER_FAILED");
			result.putInt(AccountManager.KEY_ERROR_CODE,
					data.getIntExtra(AccountManager.KEY_ERROR_CODE, 0));
			result.putString(AccountManager.KEY_ERROR_MESSAGE,
					data.getStringExtra(AccountManager.KEY_ERROR_MESSAGE));
			VerifyAccountActivity.this.setAccountAuthenticatorResult(result);
		}
		// The authentication failed on the server side.
		else if (resultCode == VerifyAccountActivity.RESULT_AUTHENTICATION_FAILED) {
			Log.d(LOGTAG,
					"CheckAcountActivity returns RESULT_AUTHENTICATION_FAILED");
			result.putInt(AccountManager.KEY_ERROR_CODE,
					data.getIntExtra(AccountManager.KEY_ERROR_CODE, 0));
			result.putString(AccountManager.KEY_ERROR_MESSAGE,
					data.getStringExtra(AccountManager.KEY_ERROR_MESSAGE));
			VerifyAccountActivity.this.setAccountAuthenticatorResult(null);

		}
		// The authentication was successful. Add the account and its AuthToken.
		else if (resultCode == VerifyAccountActivity.RESULT_OK) {
			Log.d(LOGTAG, "CheckAcountActivity returns RESULT_OK");
			String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			Account account = new Account(accountName, Authenticator.ACCOUNT_TYPE);
			String accountPassword = "Doesn't matter";
			
			
			// It seems whether we are adding or updating this call works just as
			// well. I assume that if we had other data, we may want to look it up
			// first.
			String authToken = data
					.getStringExtra(AccountManager.KEY_AUTHTOKEN);
			// We are adding the account
			AccountManager accountManager = AccountManager.get(this);
			accountManager.addAccountExplicitly(account, accountPassword,null);
			accountManager.setAuthToken(account, Authenticator.AUTHTOKEN_TYPE,
					authToken);

			// Required response by Android Framework.
			result.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
			result.putString(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE);
			VerifyAccountActivity.this.setAccountAuthenticatorResult(result);
		}
	};

	private class CancelClickListener implements OnClickListener
	{
		@Override public void onClick(View pView)
		{
			if (mAuthTask != null) {
				mAuthTask.cancel(true);
			}
			VerifyAccountActivity.this.setResult(RESULT_CANCELED);
			VerifyAccountActivity.this.finish();
		}
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected Dialog onCreateDialog(int code) {
    	Log.i(LOGTAG, "Building Dialog " + code);
    	final AlertDialog.Builder dialogB = new android.app.AlertDialog.Builder(this);
    	dialogB.setCancelable(false);
    	// We create different dialogs for the various code returned.
    	switch(code) {
    	case RESULT_OK:
    		dialogB.setMessage(R.string.verify_okay);
    		break;
    	case RESULT_AUTHENTICATION_FAILED:
    		dialogB.setMessage(R.string.verify_authen_failed);
    		break;
    	case RESULT_SERVER_FAILED:
    		dialogB.setMessage(R.string.verify_server_failed);
    		break;
    	}
    	dialogB.setPositiveButton(R.string.verify_pos_button_label, new DialogInterface.OnClickListener() {
    		@Override
    		public void onClick(DialogInterface dialog, int which) {
    			VerifyAccountActivity.this.finish();
    		}
    	});
    	return dialogB.create();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
    	Log.d(LOGTAG, "onPause()");
    	super.onPause();
    }
    @Override
    protected void onStart() {
    	Log.d(LOGTAG, "onStart()");
    	super.onStart();
    }
    @Override
    protected void onStop() {
    	Log.d(LOGTAG, "onStop()");
    	super.onStop();
    }
    @Override
    protected void onRestart() {
    	Log.d(LOGTAG, "onRestart()");
    	super.onRestart();
    }
    @Override
    protected void onDestroy() {
    	Log.d(LOGTAG, "onDestroy()");
    	super.onDestroy();
    }
    

    @Override
    protected void onNewIntent(Intent intent)
    {
 	   Log.i(LOGTAG, "Return Intent!");
    	Uri uri = intent.getData();
    	respondWithCredentials(uri);
    }
    
    
    private void onAuthenticationResult(Intent result) {
		final int code = result.getIntExtra("RESULT_CODE", 0);
        this.setResult(code, result);
        
        // Dialog will finish if clicked. Otherwise, timer will do it.
		VerifyAccountActivity.this.showDialog(code);
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				try {
					// The dialog could have already been destroyed.
					VerifyAccountActivity.this.dismissDialog(code);
					VerifyAccountActivity.this.finish();
				} catch (Exception e) {
					
				}
			}
		}, EXIT_DIALOG_DISMISS_TIME);
    }

    private void onAuthenticationCancel() {
        Log.i(LOGTAG, "onAuthenticationCancel()");
        // This should only get called from our cancel button. which will do the following
        // anyway.
        // this.setResult(RESULT_CANCELED);
        // this.finish();
    }

    private class UserLoginTask extends AsyncTask<String, Void, BuspassAPI> {
    	
        @Override
        protected BuspassAPI doInBackground(String... params) {
        	String apiUrl = params[0];
        	BuspassAPI api = BuspassAPI.getInstance(apiUrl);
        	try {
				if (api.get()) {
					return api;
				} else {
					return null;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
        }

        @Override
        protected void onPostExecute(final BuspassAPI busAPI) {
        	try {
    	    	final Intent redirectIntent = new Intent(Intent.ACTION_VIEW,
    	    	        Uri.parse(busAPI.loginUrl()));
    			Button buttonLogin = (Button)findViewById(R.id.goButton);
    			buttonLogin.setOnClickListener(new OnClickListener() {  
    				public void onClick(View v) {
    			    	startActivity(redirectIntent);
    				}
    			});
    		} catch (Exception e) {
    			Toast.makeText(VerifyAccountActivity.this, e.getMessage(),
    					Toast.LENGTH_LONG).show();
    			e.printStackTrace();
    		}
        }

        @Override
        protected void onCancelled() {
            onAuthenticationCancel();
        }
    }

}