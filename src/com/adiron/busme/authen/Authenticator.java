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
	public static final String AUTHTOKEN_TYPE = "com.adiron.busme.AuthTokenType";

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

	public Authenticator(Service context) {
        super(context);
        Log.d(LOGTAG, "new Authenticator(uid="+Binder.getCallingUid()+") for com.adiron.busme");
        mContext = context;
    }
    
	/**
	 * This addAccount sets up the AddAccountActivity to collect data from the user and
	 * tries to verify it and retrieve an Authentication Token. We only deal with one
	 * AccountType which is "com.adiron.busme".
	 * 
	 * This method is called by the Android Settings Account Manager for adding an account.
	 * Settings > Accounts & Sync > Add Account > BusmeAuth
	 *    Launches the AccountActivity with "Update Account" 
	 */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle loginOptions) {
        Log.d(LOGTAG, "addAccount(uid=" + Binder.getCallingUid() + ", type=" + accountType + ", tokenType=" + authTokenType + ")");
		
        final Intent intent = new Intent(mContext, AccountActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
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

    /**
     * This is run in the background.
     */
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle loginOptions) throws NetworkErrorException {
        Log.v(LOGTAG, "getAuthToken(name " + account.name);
        final Bundle result = new Bundle();

        // If the caller requested an authToken type we don't support, then
        // return an error
        if (!authTokenType.equals(Authenticator.AUTHTOKEN_TYPE)) {
            result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_ARGUMENTS);
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            Log.v(LOGTAG, "getAuthToken() = invalid authTokenType");
            response.onResult(result);
            return result;
        }

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(mContext);
        /*final String password = am.getPassword(account);
        if (password != null) {
            final String url = am.getUserData(account, Authenticator.KEY_MUNICIPALITY_URL);
        	String authToken;
			try {
	            Log.v(LOGTAG, "getAuthToken() going by login authenticator.");
				authToken = getLoginAuthenticator().login(account.name, password, url);
	            Log.v(LOGTAG, "getAuthToken() going by login authenticator. got token " + authToken);
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
        }*/
        String muni = am.getUserData(account, Authenticator.KEY_MUNICIPALITY_NAME);
        String muniUrl = am.getUserData(account, Authenticator.KEY_MUNICIPALITY_URL);
        // This is a list of one each, which is given to the AccountActivity.
        // This signifies that we are looking to update the account through
        // the Settings > Account & Sync > some Busme account
        // This is the name of the Master and its API URL, not its login URL.
        ArrayList<String> munis = new ArrayList<String>();
        munis.add(muni);
        ArrayList<String> muniUrls = new ArrayList<String>();
        muniUrls.add(muniUrl);


        Log.v(LOGTAG, "getAuthToken() = sending data to AccountActivity");
        Log.v(LOGTAG, "getAuthToken() muni = " + muni);
        Log.v(LOGTAG, "getAuthToken() muniUrl = " + muniUrl);
        
        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity panel.
        final Intent intent = new Intent(mContext, AccountActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        intent.putExtra(Authenticator.KEY_MUNICIPALITIES, munis);
        intent.putExtra(Authenticator.KEY_MUNICIPALITY_URLS, muniUrls);
        // TODO: This action may come in useful in verification.
        intent.putExtra(Authenticator.KEY_AUTHENTICATOR_ACTION, Authenticator.ACTION_UPDATE_ACCOUNT);
        
        // As per Android Framework, sending back a KEY_INTENT will launch an activity.
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        Log.v(LOGTAG, "getAuthToken() returning bundle " + bundle);
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