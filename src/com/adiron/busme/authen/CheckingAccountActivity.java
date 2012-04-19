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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
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

	private static final String PARAM_LOGIN = "muni_admin[email]";
	private static final String PARAM_PASSWORD = "muni_admin[password]";

	private AccountManager accountManager;
	private UserLoginTask mAuthTask;

    private Button cancelButton;
	private TextView municipalityView;
	private ProgressBar checkProgressBar;

	private BasicHttpContext localHttpContext;

	private DefaultHttpClient httpClient;

	private String currentAuthToken;

	private String csrfParam;

	private String csrfToken;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle icicle) {
        Log.i(LOGTAG, "onCreate(" + icicle + ")");
        super.onCreate(icicle);
        accountManager = AccountManager.get(this);
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
		String login = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
		String municipalityUrl = intent.getStringExtra(Authenticator.KEY_MUNICIPALITY_URL);

		Account account = null;
		Account[] accounts = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
		Log.d(LOGTAG, "CheckingAccountActivity(pid="+Binder.getCallingPid()+", uid="+Binder.getCallingUid()+") found " + accounts.length + " accounts");
		for(Account a : accounts) {
			Log.d(LOGTAG, "CheckingAccountActivity.looking at (name="+a.name+", type="+a.type+")");
			String muniUrl = accountManager.getUserData(a, Authenticator.KEY_MUNICIPALITY_URL);
			Log.d(LOGTAG, "CheckingAccountActivity.and got (name="+a.name+", type="+a.type+", url="+muniUrl+")");
			if (a.name.equals(login) && municipalityUrl.equals(muniUrl)) {
				account = a;
			}
		}
		if (account != null) {

			Log.d(LOGTAG, "CheckingAccountActivity(uid="+Binder.getCallingUid()+", name="+account.name+", type="+account.type+")");
			initializeHttpClient();
			
			String municipality = accountManager.getUserData(account, Authenticator.KEY_MUNICIPALITY_URL);
			municipalityView.setText(municipality);
			
			mAuthTask = new UserLoginTask(account);
			mAuthTask.execute(municipality);
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
    
    public void onAuthenticationResult(Account account, int result) {
        Log.i(LOGTAG, "onAuthenticationResult(" + account.name + ", " + result + ")");

        // Our task is complete, so clear it out
        mAuthTask = null;

        this.setResult(result);
        this.finish();
    }

    public void onAuthenticationCancel() {
        Log.i(LOGTAG, "onAuthenticationCancel()");

        // Our task is complete, so clear it out
        mAuthTask = null;

        this.setResult(RESULT_CANCELED);
        this.finish();
    }

    private class UserLoginTask extends AsyncTask<String, Void, Integer> {
    	Account account;
    	
    	UserLoginTask(Account account) {
    		this.account = account;
    	}
    	
        @Override
        protected Integer doInBackground(String... params) {
            try {
            	// We can get this from the account, but we pass it in.
            	String municipalityUrl = params[0];
    			login(account, municipalityUrl);
    			return RESULT_OK;
            } catch (SecurityException e1) {
            	Log.i(LOGTAG, "UserLoginTask.doInBackgroun: Failed to Authenticate at server.");
            	return RESULT_AUTHENTICATION_FAILED;
            } catch (IOException ex) {
                Log.e(LOGTAG, "UserLoginTask.doInBackground: failed to authenticate");
                Log.i(LOGTAG, ex.toString());
                return RESULT_SERVER_FAILED;
            }
        }

        @Override
        protected void onPostExecute(final Integer success) {
			Log.d(LOGTAG, "login postExcecute returns " + success);
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

	private void getCSRFToken(String url) throws ClientProtocolException, IOException, XmlPullParserException {
		HttpGet request = new HttpGet(url);
		HttpResponse resp = httpClient.execute(request, localHttpContext);
		Header[] headers = resp.getAllHeaders();
		for (Header h : headers) {
			Log.d(LOGTAG, "Header: " + h.getName() + " " + h.getValue());
		}
		HttpEntity ent = resp.getEntity();
		InputStream in = ent.getContent();
		XmlPullParserFactory parserF = XmlPullParserFactory.newInstance();
		XmlPullParser parser = parserF.newPullParser();
		parser.setInput(new InputStreamReader(in));
		//parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
		parser.next();
		boolean inHead = false;
		while(parser.getEventType() != XmlPullParser.END_DOCUMENT && (csrfParam == null || csrfToken == null)) {
			if (parser.getEventType() == XmlPullParser.START_TAG) {
				if ("head".equalsIgnoreCase(parser.getName())) {
					inHead = true;
				} else if (inHead &&  "meta".equalsIgnoreCase(parser.getName())) {
					if ("csrf-param".equals(parser.getAttributeValue(null, "name"))) {
						csrfParam = parser.getAttributeValue(null, "content");
					} else if ("csrf-token".equals(parser.getAttributeValue(null, "name"))) {
						csrfToken = parser.getAttributeValue(null, "content");
					}
				}
			}
			parser.next();
		}
		ent.consumeContent();
		if (csrfParam == null || csrfToken == null) {
			throw new XmlPullParserException("No CSRF Token");
		}
	}
	
	private String changeType(String url, String newType) throws MalformedURLException {
		URL nurl = new URL(url);
		String path = nurl.getPath();
		String query = nurl.getQuery();
		String proto = nurl.getProtocol();
		int port = nurl.getPort();
		String host = nurl.getHost();
		String auth = nurl.getAuthority();
		
		int typeIdx = path.lastIndexOf(".");
		String type = (typeIdx < 0 ? null : path.substring(typeIdx +1));
		path = (typeIdx < 0 ? path : path.substring(0,typeIdx));
		
		type = newType;
		
		StringBuilder sb = new StringBuilder();
		sb.append(proto == null ? "http" : proto);
		sb.append("://");
		sb.append(host);
		sb.append(port < 0 ? "" : ":" + port);
		sb.append(auth == null ? "" : "@" + auth);
		sb.append(path == null ? "/" : "/" + path);
		sb.append(type == null ? "" : "." + type);
		sb.append(query == null ? "" : "?" + query);
		
		return new URL(sb.toString()).toExternalForm();
	}
	
	public void login(Account account, String url) throws ClientProtocolException, IOException, SecurityException {
		Log.d(LOGTAG, "CheckingAccountActivity.login(uid="+Binder.getCallingUid()+", name="+account.name+", type="+account.type+", url="+url+")");

		// TODO: The password should be encrypted.
		String password = accountManager.getPassword(account);
		
		final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		try {
			getCSRFToken(url);
			params.add(new BasicNameValuePair(csrfParam, csrfToken));
		} catch (XmlPullParserException e1) {
			throw new IOException(e1.getMessage());
		}
		params.add(new BasicNameValuePair(PARAM_LOGIN, account.name));
		params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
		HttpEntity entity;
		try {
			entity = new UrlEncodedFormEntity(params);
		} catch (UnsupportedEncodingException e) {
			// this should never happen.
			throw new IllegalStateException(e);
		}
		
		HttpPost request = new HttpPost(changeType(url, "json"));
		request.setEntity(entity);
		HttpResponse resp = httpClient.execute(request,localHttpContext);
		Header[] headers = resp.getAllHeaders();
		for (Header h : headers) {
			Log.d(LOGTAG, "Header: " + h.getName() + " " + h.getValue());
		}
		HttpEntity ent = resp.getEntity();
		ent.consumeContent();
		Log.d(LOGTAG, resp.getStatusLine().getStatusCode() + " " + resp.getStatusLine().getReasonPhrase());
		if (400 <= resp.getStatusLine().getStatusCode()) {
			throw new SecurityException("Authentication Failed.");
		} else {
			findAndStoreAuthToken(account, headers);
		}
	}
	
	private void findAndStoreAuthToken(Account account, Header[] headers) {
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
					Log.d(LOGTAG, "We must replace auth_token " + splits[0]);
					currentAuthToken = splits[0];
					accountManager.setAuthToken(account, Constants.AUTHTOKEN_TYPE, currentAuthToken);
				}
			}
		}
	}

}