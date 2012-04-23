package com.adiron.busme.authen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import android.os.Binder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

/**
 * 
 */
class Authenticator extends AbstractAccountAuthenticator {
    private static final String LOGTAG = "Authenticator";

    /**
     * The Account Type for Busme Accounts.
     */
    public static final String ACCOUNT_TYPE = "com.adiron.busme.account";
    
    /**
     * The Authentication Token Type for Busme Accounts.
     */
	public static final String AUTHTOKEN_TYPE = "com.adiron.busme.WebAuthToken";

	/**
	 * This is the identifier where we store preference for Busme
	 * */
	public static final String PREFERENCES_NAME = "com.adiron.busme";
	
	/** 
	 * This is the identifier which denotes the JSON string containing 
	 * Municipality IDs and JSON Login URLs.
	 */
	public static final String PREF_MUNICIPALITIES = "com.adiron.busme.municipalities";

	// Package relevant Intent and Bundle Keys
	static final String KEY_MUNICIPALITY_URL = "MunicipalityUrl";
	static final String KEY_MUNICIPALITY_NAME = "MunicpalityName";
	static final String KEY_MUNICIPALITIES = "MunicipalityNames";
	static final String KEY_MUNICIPALITY_URLS = "MunicipalityUrls";
	static final String KEY_AUTHTOKEN_TYPE = "AuthTokenType";
	static final String KEY_ACCOUNT_DATA = "AccountData";
	static final String KEY_ACCOUNT_EMAIL = "Email";
	static final String KEY_AUTHENTICATOR_ACTION = "Action";

	static final String ACTION_ADD_ACCOUNT = "AddAccount";
	static final String ACTION_UPDATE_ACCOUNT = "UpdateAccount";

	/**
	 * We have one login Authenticator. The login Authenticator handles
     * authentication specifics, such as contacting a server and verifying credentials,
     * and retrieving an authentication token.
	 */
	private static LoginAuthenticator loginAuthenticator;

	/**
	 * The login Authenticator handles authentication specifics, such as 
	 * contacting a server and verifying credentials, and retrieving an authentication token. 
     * The default loginAuthenticator is the WebAuthenticator.
	 */
    public static LoginAuthenticator getLoginAuthenticator() {
		if (loginAuthenticator == null) {
			loginAuthenticator = new WebAuthenticator();
		}
		return loginAuthenticator;
	}

    /**
     * Sets the loginAuthenticator for this class/service. The login Authenticator handles
     * authentication specifics, such as contacting a server and verifying credentials,
     * and retrieving an authentication token.
     */
	public static void setLoginAuthenticator(LoginAuthenticator loginAuthenticator) {
		Authenticator.loginAuthenticator = loginAuthenticator;
	}

    // Authentication Service context
    private final Service mContext;

	private ArrayList<String> municipalities;

	private ArrayList<String> municipalityUrls;

	public Authenticator(Service context) {
        super(context);
        Log.d(LOGTAG, "new Authenticator(uid="+Binder.getCallingUid()+") for com.adiron.busme");
        mContext = context;
        getMunicipalityPrefs();
    }
    
	private void getMunicipalityPrefs() {
		SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
		String muniUrlString = prefs.getString(PREF_MUNICIPALITIES, "[[\"Syracuse\",\"http://192.168.99.2:3000/muni_admins/sign_in.json?master_id=4f69cdf5a0490542ec000311\"]]");
		municipalities = new ArrayList<String>();
		municipalityUrls = new ArrayList<String>();
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
	}
	/**
	 * This addAccount sets up the AddAccountActivity to collect data from the user and
	 * tries to verify it and retrieve an Authentication Token. We only deal with one
	 * AccountType which is "com.adiron.busme".
	 */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle loginOptions) {
        Log.d(LOGTAG, "addAccount(uid=" + Binder.getCallingUid() + ", type=" + accountType + ", tokenType=" + authTokenType + ")");
		
        final Intent intent = new Intent(mContext, AddAccountActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        intent.putExtra(Authenticator.KEY_MUNICIPALITIES, municipalities);
        intent.putExtra(Authenticator.KEY_MUNICIPALITY_URLS, municipalityUrls);
        // TODO: This action may come in useful in verification.
        intent.putExtra(Authenticator.KEY_AUTHENTICATOR_ACTION, Authenticator.ACTION_ADD_ACCOUNT);
        intent.putExtra(Authenticator.KEY_AUTHENTICATOR_ACTION, Authenticator.ACTION_UPDATE_ACCOUNT);
        
        // As per Android Framework, sending back a KEY_INTENT will launch an activity.
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
        final Bundle result = new Bundle();

        // If the caller requested an authToken type we don't support, then
        // return an error
        if (!authTokenType.equals(Authenticator.AUTHTOKEN_TYPE)) {
            result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_ARGUMENTS);
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            response.onResult(result);
            return result;
        }

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(mContext);
        final String password = am.getPassword(account);
        if (password != null) {
            final String url = am.getUserData(account, Authenticator.KEY_MUNICIPALITY_URL);
        	String authToken;
			try {
				authToken = getLoginAuthenticator().login(account.name, password, url);
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE);
                result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
                response.onResult(result);
                return result;
			} catch (SecurityException e) {
	            result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_REQUEST);
	            result.putString(AccountManager.KEY_ERROR_MESSAGE, e.getMessage());
	            return result;
			} catch (IOException e) {
	            result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_NETWORK_ERROR);
	            result.putString(AccountManager.KEY_ERROR_MESSAGE, e.getMessage());
	            return result;
			}
        }
        
        ArrayList<String> munis = new ArrayList<String>();
        munis.add(am.getUserData(account, Authenticator.KEY_MUNICIPALITY_NAME));
        ArrayList<String> muniUrls = new ArrayList<String>();
        muniUrls.add(am.getUserData(account, Authenticator.KEY_MUNICIPALITY_URL));

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity panel.
        final Intent intent = new Intent(mContext, AddAccountActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(Authenticator.KEY_ACCOUNT_EMAIL, am.getUserData(account, Authenticator.KEY_ACCOUNT_EMAIL));
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        intent.putExtra(Authenticator.KEY_MUNICIPALITIES, munis);
        intent.putExtra(Authenticator.KEY_MUNICIPALITY_URLS, muniUrls);
        // TODO: This action may come in useful in verification.
        intent.putExtra(Authenticator.KEY_AUTHENTICATOR_ACTION, Authenticator.ACTION_UPDATE_ACCOUNT);
        
        // As per Android Framework, sending back a KEY_INTENT will launch an activity.
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