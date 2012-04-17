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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

/**
 * This class is an implementation of AbstractAccountAuthenticator for
 * authenticating accounts in the com.example.android.samplesync domain. The
 * interesting thing that this class demonstrates is the use of authTokens as
 * part of the authentication process. In the account setup UI, the user enters
 * their username and password. But for our subsequent calls off to the service
 * for syncing, we want to use an authtoken instead - so we're not continually
 * sending the password over the wire. getAuthToken() will be called when
 * SyncAdapter calls AccountManager.blockingGetAuthToken(). When we get called,
 * we need to return the appropriate authToken for the specified account. If we
 * already have an authToken stored in the account, we return that authToken. If
 * we don't, but we do have a username and password, then we'll attempt to talk
 * to the sample service to fetch an authToken. If that fails (or we didn't have
 * a username/password), then we need to prompt the user - so we create an
 * AuthenticatorActivity intent and return that. That will display the dialog
 * that prompts the user for their login information.
 */
class Authenticator extends AbstractAccountAuthenticator {
    private static final String LOGTAG = "Authenticator";

	static final String KEY_MUNICIPALITY_URL = "com.adiron.busme.municipality.url";
	static final String KEY_MUNICIPALITIES = "com.adiron.busme.municipalities";
	static final String KEY_MUNICIPALITY_URLS = "com.adiron.busme.municipality.urls";

	static final String PREF_MUNICIPALITIES = "com.adiron.busme.municipalities";
	static final String PREF_MUNICIPALITY_URLS = "com.adiron.busme.municipality.urls";

	private static final String KEY_AUTHTOKEN_TYPE = "com.adiron.busme.AuthTokenType";

	public static final String KEY_PASSWORD = "com.adiron.busme.Password";
	public static final String PREFERENCES_NAME = "com.adiron.busme";

    // Authentication Service context
    private final Service mContext;

    public Authenticator(Service context) {
        super(context);
        Log.d(LOGTAG, "new Authenticator() for com.adiron.busme");
        mContext = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle loginOptions) {
        Log.d(LOGTAG, "addAccount()");

		SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
		String muniUrlString = prefs.getString(PREF_MUNICIPALITIES, "[[\"Syracuse\",\"http://localhost:3000/muni_admins/sign_in?master_id=4f69cdf5a0490542ec000311\"]]");
		ArrayList<String> municipalities = new ArrayList<String>();
		ArrayList<String> municipalityUrls = new ArrayList<String>();
		try {
			JSONTokener tokener = new JSONTokener(muniUrlString);
			JSONArray array = new JSONArray(tokener);
			for(int i = 0; i < array.length(); i++) {
				JSONArray spec = array.getJSONArray(i);
				municipalities.add(spec.getString(0));
				municipalityUrls.add(spec.getString(1));
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        final Intent intent = new Intent(mContext, AddAccountActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        intent.putExtra(KEY_MUNICIPALITIES, municipalities);
        intent.putExtra(KEY_MUNICIPALITY_URLS, municipalityUrls);
        intent.putExtra("AUTHENTICATOR_ACTION", "addAccount");
        
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(
            AccountAuthenticatorResponse response, Account account, Bundle options) {
        Log.v(LOGTAG, "confirmCredentials()");
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        Log.v(LOGTAG, "editProperties()");
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle loginOptions) throws NetworkErrorException {
        Log.v(LOGTAG, "getAuthToken()");

        // If the caller requested an authToken type we don't support, then
        // return an error
        if (!authTokenType.equals(Constants.AUTHTOKEN_TYPE)) {
            final Bundle result = new Bundle();
            result.putInt(AccountManager.KEY_ERROR_CODE, 100);
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            response.onResult(result);
            //return result;
            return null;
        }

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        //final AccountManager am = AccountManager.get(mContext);
        //final String password = am.getPassword(account);
        //if (password != null) {
        final String authToken = NetworkUtilities.authenticate(account.name, "", "");
            if (!TextUtils.isEmpty(authToken)) {
                final Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
                result.putString(AccountManager.KEY_AUTHTOKEN, "");
                response.onResult(result);
                //return result;
                return null;
            }
        //}

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity panel.
        final Intent intent = new Intent(mContext, AddAccountActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
        intent.putExtra(KEY_AUTHTOKEN_TYPE, authTokenType);
        
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        // null means we don't support multiple authToken types
        Log.v(LOGTAG, "getAuthTokenLabel()");
        return null;
    }

    @Override
    public Bundle hasFeatures(
            AccountAuthenticatorResponse response, Account account, String[] features) {
        // This call is used to query whether the Authenticator supports
        // specific features. We don't expect to get called, so we always
        // return false (no) for any queries.
        Log.v(LOGTAG, "hasFeatures()");
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle loginOptions) {
        Log.v(LOGTAG, "updateCredentials()");
        return null;
    }

}