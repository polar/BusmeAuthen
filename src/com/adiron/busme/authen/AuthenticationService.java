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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Service to handle Account authentication. It instantiates the authenticator
 * and returns its IBinder.
 */
public class AuthenticationService extends Service {

    private static final String LOGTAG = "AuthenticationService";

	private static final Object ACTION_SYNCADAPTER_INTENT = "android.content.SyncAdapter";

	private Authenticator authenticator;
    
    
    @Override
    public void onCreate() {
    	super.onCreate();
        //if (Log.isLoggable(LOGTAG, Log.VERBOSE)) {
            Log.d(LOGTAG, "Busme Authentication Service started.");
        	authenticator = new Authenticator(this);
       // }
    }
    
    @Override
    public void onDestroy() {
        if (Log.isLoggable(LOGTAG, Log.VERBOSE)) {
            Log.v(LOGTAG, "Busme Authentication Service stopped.");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        //if (Log.isLoggable(LOGTAG, Log.VERBOSE)) {
            Log.d(LOGTAG, "getBinder()...  returning the AccountAuthenticator binder for intent "
                    + intent);
        //}
        if (AccountManager.ACTION_AUTHENTICATOR_INTENT.equals(intent.getAction())) {
        	return authenticator.getIBinder();
        } else if (ACTION_SYNCADAPTER_INTENT.equals(intent.getAction())) {
        	return new SyncAdapter(this, false).getSyncAdapterBinder();
        }
        return null;
    }
    
    /**
     *  We need a fake SyncAdapter to satisfy the AccountManager Framework.
     * @author polar
     *
     */
    private final class SyncAdapter extends AbstractThreadedSyncAdapter {

        private SyncAdapter(Context context, boolean autoInitialize) {
            super(context, autoInitialize);
            Log.d(LOGTAG, "new SyncAdapter()");
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority,
                ContentProviderClient provider, SyncResult syncResult) {
            Log.d(LOGTAG, "onPerformSync(" + account.name + ", " + authority + " ...)");
        }
    }
}