package com.adiron.busme.authen;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * The Busme Service to handle Account authentication. It instantiates the authenticator
 * and returns its IBinder. We have a stub SyncAdapter since there is a bug
 * prior to Android 2.3 that requires it.
 */
public class AuthenticationService extends Service {
    private static final String LOGTAG = "AuthenticationService";

	private static final Object ACTION_SYNCADAPTER_INTENT = "android.content.SyncAdapter";

	private Authenticator authenticator;
    
    @Override
    public void onCreate() {
    	super.onCreate();
        if (Log.isLoggable(LOGTAG, Log.VERBOSE)) {
            Log.d(LOGTAG, "Busme Authentication Service started.");
        }
    	authenticator = new Authenticator(this);
    }
    
    @Override
    public void onDestroy() {
        if (Log.isLoggable(LOGTAG, Log.VERBOSE)) {
            Log.v(LOGTAG, "Busme Authentication Service stopped.");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (Log.isLoggable(LOGTAG, Log.VERBOSE)) {
            Log.d(LOGTAG, "getBinder()...  returning the AccountAuthenticator binder for intent "
                    + intent);
        }
        if (AccountManager.ACTION_AUTHENTICATOR_INTENT.equals(intent.getAction())) {
        	return authenticator.getIBinder();
        } else if (ACTION_SYNCADAPTER_INTENT.equals(intent.getAction())) {
        	return new SyncAdapter(this, false).getSyncAdapterBinder();
        }
        return null;
    }
    
    /**
     *  We supply a stub SyncAdapter to satisfy the AccountManager Framework prior to
     *  Android 2.3.
     */
    private final class SyncAdapter extends AbstractThreadedSyncAdapter {

        private SyncAdapter(Context context, boolean autoInitialize) {
            super(context, autoInitialize);
            if (Log.isLoggable(LOGTAG, Log.VERBOSE)) {
            	Log.d(LOGTAG, "new SyncAdapter()");
            }
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority,
                ContentProviderClient provider, SyncResult syncResult) {
            if (Log.isLoggable(LOGTAG, Log.VERBOSE)) {
            	Log.d(LOGTAG, "onPerformSync(" + account.name + ", " + authority + " ...)");
            }
        }
    }
}