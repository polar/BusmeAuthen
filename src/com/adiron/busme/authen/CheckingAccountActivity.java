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

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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

/**
 * Activity which displays login screen to the user.
 */
public class CheckingAccountActivity extends Activity {
	private final static String LOGTAG = CheckingAccountActivity.class.getName();
	
	public final static int RESULT_AUTHENTICATION_FAILED = RESULT_FIRST_USER + 0;
	public final static int RESULT_SERVER_FAILED = RESULT_FIRST_USER + 1;

	/** Time the alert dialog waits before it dismisses itself. */
	private static final long EXIT_DIALOG_DISMISS_TIME = 5000;

	private UserLoginTask mAuthTask;

    private Button cancelButton;
	private TextView municipalityView;
	private ProgressBar checkProgressBar;


	private String accountType;

	private String accountPassword;

	private String accountName;

	private String accountAuthToken;

	private String municipalityUrl;

	private String municipalityName;

	private LoginAuthenticator loginAuthenticator;

	private String accountEmail;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle icicle) {
        Log.i(LOGTAG, "onCreate(" + icicle + ")");
        super.onCreate(icicle);
		Log.d(LOGTAG, "CheckingAccountActivity(pid="+Binder.getCallingPid()+", uid="+Binder.getCallingUid()+")");

        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.check_activity);
        
        getWindow().setFeatureDrawableResource(
                Window.FEATURE_LEFT_ICON, android.R.drawable.ic_input_add);

        checkProgressBar = (ProgressBar) findViewById(R.id.checkProgressBar);
        cancelButton = (Button) findViewById(R.id.checkCancelButton);
        municipalityView = (TextView) findViewById(R.id.municipalityView);

		cancelButton.setOnClickListener(new CancelClickListener());
		checkProgressBar.setIndeterminate(true);
		
		Intent intent = getIntent();
		accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
		accountEmail = intent.getStringExtra(Authenticator.KEY_ACCOUNT_EMAIL);
		accountPassword = intent.getStringExtra(AccountManager.KEY_PASSWORD);
		accountType = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
		municipalityName = intent.getStringExtra(Authenticator.KEY_MUNICIPALITY_NAME);
		municipalityUrl = intent.getStringExtra(Authenticator.KEY_MUNICIPALITY_URL);

		municipalityView.setText(municipalityName);
		loginAuthenticator = Authenticator.getLoginAuthenticator();
		
		mAuthTask = new UserLoginTask();
		mAuthTask.execute();
    }

	private class CancelClickListener implements OnClickListener
	{
		@Override public void onClick(View pView)
		{
			if (mAuthTask != null) {
				mAuthTask.cancel(true);
			}
			CheckingAccountActivity.this.setResult(RESULT_CANCELED);
			CheckingAccountActivity.this.finish();
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
    		dialogB.setMessage(R.string.check_okay);
    		break;
    	case RESULT_AUTHENTICATION_FAILED:
    		dialogB.setMessage(R.string.check_authen_failed);
    		break;
    	case RESULT_SERVER_FAILED:
    		dialogB.setMessage(R.string.check_server_failed);
    		break;
    	}
    	dialogB.setPositiveButton(R.string.check_pos_button_label, new DialogInterface.OnClickListener() {
    		@Override
    		public void onClick(DialogInterface dialog, int which) {
    			CheckingAccountActivity.this.finish();
    		}
    	});
    	return dialogB.create();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
    	super.onResume();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    private Intent performLogin() {
    	Intent result = new Intent();
        try {
			accountAuthToken = loginAuthenticator.login(accountEmail, accountPassword, municipalityUrl);
        	Log.i(LOGTAG, "UserLoginTask.doInBackground: Successful" );
			result.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
			result.putExtra(Authenticator.KEY_ACCOUNT_EMAIL, accountEmail);
			result.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
			result.putExtra(AccountManager.KEY_AUTHTOKEN, accountAuthToken);
			result.putExtra("RESULT_CODE", RESULT_OK);
			return result;
        } catch (SecurityException e1) {
        	Log.i(LOGTAG, "UserLoginTask.doInBackgroun: Failed to Authenticate at server.");
        	result.putExtra("RESULT_CODE", RESULT_AUTHENTICATION_FAILED);
        	result.putExtra(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_REQUEST);
        	result.putExtra(AccountManager.KEY_ERROR_MESSAGE, e1.getMessage());
        	return result;
        } catch (IOException ex) {
            Log.e(LOGTAG, "UserLoginTask.doInBackground: failed to authenticate");
        	result.putExtra("RESULT_CODE", RESULT_SERVER_FAILED);
        	result.putExtra(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_NETWORK_ERROR);
        	result.putExtra(AccountManager.KEY_ERROR_MESSAGE, ex.getMessage());
            return result;
        }
    }
    
    private void onAuthenticationResult(Intent result) {
        Log.i(LOGTAG, "onAuthenticationResult(" + accountName + ", " + result + ")");
		final int code = result.getIntExtra("RESULT_CODE", 0);
        this.setResult(code, result);
        
        // Dialog will finish if clicked. Otherwise, timer will do it.
		CheckingAccountActivity.this.showDialog(code);
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				CheckingAccountActivity.this.dismissDialog(code);
				CheckingAccountActivity.this.finish();
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

    private class UserLoginTask extends AsyncTask<Void, Void, Intent> {
    	
        @Override
        protected Intent doInBackground(Void... params) {
        	return performLogin();
        }

        @Override
        protected void onPostExecute(final Intent result) {
            onAuthenticationResult(result);
        }

        @Override
        protected void onCancelled() {
            onAuthenticationCancel();
        }
    }

}