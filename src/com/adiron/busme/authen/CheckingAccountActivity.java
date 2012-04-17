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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity which displays login screen to the user.
 */
public class CheckingAccountActivity extends Activity {
	private final static String LOGTAG = CheckingAccountActivity.class.getName();
	
	public final static int RESULT_AUTHENTICATION_FAILED = RESULT_FIRST_USER + 0;
	public final static int RESULT_SERVER_FAILED = RESULT_FIRST_USER + 1;

	private static final String PARAM_LOGIN = "muni_user[name]";
	private static final String PARAM_PASSWORD = "muni_user[password]";

	private AccountManager accountManager;
	private UserLoginTask mAuthTask;

    private Button cancelButton;
	private TextView municipalityView;
	private ProgressBar checkProgressBar;

	private BasicHttpContext localHttpContext;

	private DefaultHttpClient httpClient;

	private String currentAuthToken;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle icicle) {
        Log.i(LOGTAG, "onCreate(" + icicle + ")");
        super.onCreate(icicle);
        accountManager = AccountManager.get(this);

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
		String login = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

		Account account = null;
		Account[] accounts = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
		for(Account a : accounts) {
			if (a.name.equals(login)) {
				account = a;
			}
		}
		if (account != null) {
			initializeHttpClient();
			
			String municipality = accountManager.getUserData(account, Authenticator.KEY_MUNICIPALITY_URL);
			municipalityView.setText(municipality);
			
			mAuthTask = new UserLoginTask(account);
			mAuthTask.doInBackground(municipality);
		} else {
			Toast.makeText(this, R.string.check_activity_account_not_found_message, Toast.LENGTH_LONG);
			setResult(RESULT_CANCELED);
			finish();
		}
    }
    
	private class CancelClickListener implements OnClickListener
	{
		@Override public void onClick(View pView)
		{
			mAuthTask.cancel(true);
			CheckingAccountActivity.this.setResult(RESULT_CANCELED);
			CheckingAccountActivity.this.finish();
		}
	}
	
    @Override
    protected void onResume() {
    	super.onResume();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    public void onAuthenticationResult(Account account, Boolean success) {
        Log.i(LOGTAG, "onAuthenticationResult(" + account.name + ", " + success + ")");

        // Our task is complete, so clear it out
        mAuthTask = null;

        if (success) {
            Log.e(LOGTAG, "onAuthenticationResult: authenticated and stored auth token");
            this.setResult(RESULT_OK);
        } else {
            Log.e(LOGTAG, "onAuthenticationResult: failed to authenticate");
            this.setResult(RESULT_AUTHENTICATION_FAILED);
        }
        this.finish();
    }

    public void onAuthenticationCancel() {
        Log.i(LOGTAG, "onAuthenticationCancel()");

        // Our task is complete, so clear it out
        mAuthTask = null;

        this.setResult(RESULT_CANCELED);
        this.finish();
    }

    private class UserLoginTask extends AsyncTask<String, Void, Boolean> {
    	Account account;
    	
    	UserLoginTask(Account account) {
    		this.account = account;
    	}
    	
        @Override
        protected Boolean doInBackground(String... params) {
            try {
            	// We can get this from the account, but we pass it in.
            	String municipality = params[0];
    			return login(account, "http://localhost:3000/municipalities/"+municipality+"/sign_in");
            } catch (Exception ex) {
                Log.e(LOGTAG, "UserLoginTask.doInBackground: failed to authenticate");
                Log.i(LOGTAG, ex.toString());
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            onAuthenticationResult(account, success);
        }

        @Override
        protected void onCancelled() {
            onAuthenticationCancel();
        }
    }
    
	private HttpClient initializeHttpClient() {
		  DefaultHttpClient httpclient = new DefaultHttpClient();
		  BasicHttpContext localContext = new BasicHttpContext();
		  localHttpContext = localContext;
		  return httpClient = httpclient;
	}

	public boolean login(Account account, String url) throws ClientProtocolException, IOException {
		Log.d(LOGTAG, "LOGIN URL: " + url);

		// TODO: The password should be encrypted.
		String password = accountManager.getPassword(account);
		
		final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(PARAM_LOGIN, account.name));
		params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
		HttpEntity entity;
		try {
			entity = new UrlEncodedFormEntity(params);
		} catch (UnsupportedEncodingException e) {
			// this should never happen.
			throw new IllegalStateException(e);
		}
		HttpPost request = new HttpPost(url);
		request.setEntity(entity);
		HttpResponse resp = httpClient.execute(request,localHttpContext);
		Header[] headers = resp.getAllHeaders();
		for (Header h : headers) {
			Log.d(LOGTAG, "Header: " + h.getName() + " " + h.getValue());
		}
		HttpEntity ent = resp.getEntity();
		ent.consumeContent();
		if (400 > resp.getStatusLine().getStatusCode()) {
			return false;
		} else {
			return findAndStoreAuthToken(account, headers);
		}
	}
	
	private boolean findAndStoreAuthToken(Account account, Header[] headers) {
		boolean found = false;
		for (Header h : headers) {
			// We must go through them all because there could be multiple
			// set-cookie for the same cookie. In fact, some version of Rails, puts a 
			// Set-Cookie auth_token=; header before another with
			// the actual token.
			if (h.getName().equals("Set-Cookie")
					&& h.getValue().startsWith("auth_token=")) {
				// Set-Cookie auth_token=4abec3341; path=/; expires=.....
				String[] splits = h.getValue().split("=");
				splits = splits[1].split(";");
				if (splits[0].equals(currentAuthToken)) {
					// This should never happen.
					Log.d(LOGTAG, "Logged in with right token");
				} else {
					found = true;
					Log.d(LOGTAG, "We must replace auth_token " + splits[0]);
					currentAuthToken = splits[0];
					accountManager.setAuthToken(account, Constants.AUTHTOKEN_TYPE, currentAuthToken);
				}
			}
		}
		return found;
	}

}