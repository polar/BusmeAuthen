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

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
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

import com.appsolut.adapter.collections.CollectionsAdapter;

/**
 * Activity which displays login screen to the user.
 */
public class AddAccountActivity extends AccountAuthenticatorActivity {
	private final static String LOGTAG = AddAccountActivity.class.getName();

	private AccountManager accountManager;
	
	private TextView messageView;
    private EditText passwordEditText;

    private EditText emailEditText;
    
    private Button performButton;
    private Button cancelButton;

	private TextView bottomMessageView;

	private Spinner municipalitySpinner;

	private ArrayList<String> municipalities;

	private ArrayList<String> municipalityUrls;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle icicle) {
        Log.i(LOGTAG, "onCreate(" + icicle + ")");
        super.onCreate(icicle);
        accountManager = AccountManager.get(this);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.login_activity);
        
        getWindow().setFeatureDrawableResource(
                Window.FEATURE_LEFT_ICON, android.R.drawable.ic_input_add);

        // We are handed a list of available municipality ids from the Authenticator.
        municipalities = getIntent().getStringArrayListExtra(Authenticator.KEY_MUNICIPALITIES);
        municipalityUrls = getIntent().getStringArrayListExtra(Authenticator.KEY_MUNICIPALITY_URLS);
        municipalitySpinner = (Spinner) findViewById(R.id.municipalitySpinner);
        
        messageView = (TextView) findViewById(R.id.messageView);
        performButton = (Button) findViewById(R.id.performButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        emailEditText = (EditText) findViewById(R.id.emailEditText);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        bottomMessageView = (TextView) findViewById(R.id.message_bottom);
        

        SpinnerAdapter adapter = new CollectionsAdapter<String>(this, R.layout.login_activity_spinner_text_view, municipalities);
        municipalitySpinner.setAdapter(adapter);
        messageView.setText(R.string.login_activity_message_add_account);
        bottomMessageView.setText(R.string.login_acitivity_bottom_message_instructions);
        
        performButton.setOnClickListener(new PerformClickListener());
		cancelButton.setOnClickListener(new CancelClickListener());
    }
    
	private class PerformClickListener implements OnClickListener
	{

		@Override public void onClick(View pView)
		{
			final String username = emailEditText.getText().toString();
			final String password = passwordEditText.getText().toString();
			final int municipalityIdx = municipalitySpinner.getSelectedItemPosition();
            final String municipalityUrl = municipalityUrls.get(municipalityIdx);
            
			final Bundle accountData = new Bundle();
			accountData.putString(Authenticator.KEY_MUNICIPALITY_URL, municipalityUrl);
			final Account account = new Account(username, Authenticator.ACCOUNT_TYPE);
			Log.d(LOGTAG, "Adding AccountExplicity(uid="+Binder.getCallingUid()+", name="+account.name+", type="+account.type+", url="+municipalityUrl);
			accountManager.addAccountExplicitly(account, password, accountData);
			
			Intent intent = new Intent(AddAccountActivity.this, CheckingAccountActivity.class);
			intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
			intent.putExtra(Authenticator.KEY_MUNICIPALITY_URL, accountManager.getUserData(account, Authenticator.KEY_MUNICIPALITY_URL));

			Log.d(LOGTAG, "Starting CheckingAccountActivity(pid="+Binder.getCallingPid()+", uid="+Binder.getCallingUid()+", name="+account.name+", url="+accountManager.getUserData(account, Authenticator.KEY_MUNICIPALITY_URL));
			AddAccountActivity.this.startActivityForResult(intent, 100);
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Bundle result = new Bundle();
		if (resultCode == CheckingAccountActivity.RESULT_CANCELED) {
			AddAccountActivity.this.setAccountAuthenticatorResult(null);
		} else if (resultCode == CheckingAccountActivity.RESULT_SERVER_FAILED) {
			result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
			AddAccountActivity.this.setAccountAuthenticatorResult(result);
		} else if (resultCode == CheckingAccountActivity.RESULT_OK) {
			result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
			AddAccountActivity.this.setAccountAuthenticatorResult(result);
		}
		AddAccountActivity.this.finish();
	};
	
	private class CancelClickListener implements OnClickListener
	{
		@Override public void onClick(View pView)
		{
			// Will send ERROR_CODE_CANCELED.
			AddAccountActivity.this.setAccountAuthenticatorResult(null);
			AddAccountActivity.this.finish();
		}
	}
//    /*
//     * {@inheritDoc}
//     */
//    @Override
//    protected Dialog onCreateDialog(int id) {
//        final ProgressDialog dialog = new ProgressDialog(this);
//        dialog.setMessage(getText(R.string.ui_activity_authenticating));
//        dialog.setIndeterminate(true);
//        dialog.setCancelable(true);
//        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            public void onCancel(DialogInterface dialog) {
//                Log.i(LOGTAG, "user cancelling authentication");
//                if (mAuthTask != null) {
//                    mAuthTask.cancel(true);
//                }
//            }
//        });
//        // We save off the progress dialog in a field so that we can dismiss
//        // it later. We can't just call dismissDialog(0) because the system
//        // can lose track of our dialog if there's an orientation change.
//        mProgressDialog = dialog;
//        return dialog;
//    }

    @Override
    protected void onResume() {
    	super.onResume();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
//    /**
//     * Handles onClick event on the Submit button. Sends username/password to
//     * the server for authentication. The button is configured to call
//     * handleLogin() in the layout XML.
//     *
//     * @param view The Submit button for which this method is invoked
//     */
//    public void handleLogin(View view) {
//        if (mRequestNewAccount) {
//            mUsername = usernameEditText.getText().toString();
//        }
//        mPassword = passwordEditText.getText().toString();
//        if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword)) {
//            messageView.setText(getMessage());
//        } else {
//            // Show a progress dialog, and kick off a background task to perform
//            // the user login attempt.
//            showProgress();
//            mAuthTask = new UserLoginTask();
//            mAuthTask.execute();
//        }
//    }
//
//    /**
//     * Called when response is received from the server for confirm credentials
//     * request. See onAuthenticationResult(). Sets the
//     * AccountAuthenticatorResult which is sent back to the caller.
//     *
//     * @param result the confirmCredentials result.
//     */
//    private void finishConfirmCredentials(boolean result) {
//        Log.i(LOGTAG, "finishConfirmCredentials()");
//        final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
//        mAccountManager.setPassword(account, mPassword);
//        final Intent intent = new Intent();
//        intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
//        setAccountAuthenticatorResult(intent.getExtras());
//        setResult(RESULT_OK, intent);
//        finish();
//    }
//
//    /**
//     * Called when response is received from the server for authentication
//     * request. See onAuthenticationResult(). Sets the
//     * AccountAuthenticatorResult which is sent back to the caller. We store the
//     * authToken that's returned from the server as the 'password' for this
//     * account - so we're never storing the user's actual password locally.
//     *
//     * @param result the confirmCredentials result.
//     */
//    private void finishLogin(String authToken) {
//
//        Log.i(LOGTAG, "finishLogin()");
//        final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
//        if (mRequestNewAccount) {
//            mAccountManager.addAccountExplicitly(account, mPassword, null);
//        } else {
//            mAccountManager.setPassword(account, mPassword);
//        }
//        final Intent intent = new Intent();
//        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
//        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
//        setAccountAuthenticatorResult(intent.getExtras());
//        setResult(RESULT_OK, intent);
//        finish();
//    }
//
//    /**
//     * Called when the authentication process completes (see attemptLogin()).
//     *
//     * @param authToken the authentication token returned by the server, or NULL if
//     *            authentication failed.
//     */
//    public void onAuthenticationResult(String authToken) {
//
//        boolean success = ((authToken != null) && (authToken.length() > 0));
//        Log.i(LOGTAG, "onAuthenticationResult(" + success + ")");
//
//        // Our task is complete, so clear it out
//        mAuthTask = null;
//
//        // Hide the progress dialog
//        hideProgress();
//
//        if (success) {
//            if (!mConfirmCredentials) {
//                finishLogin(authToken);
//            } else {
//                finishConfirmCredentials(success);
//            }
//        } else {
//            Log.e(LOGTAG, "onAuthenticationResult: failed to authenticate");
//            if (mRequestNewAccount) {
//                // "Please enter a valid username/password.
//                messageView.setText(getText(R.string.login_activity_loginfail_text_both));
//            } else {
//                // "Please enter a valid password." (Used when the
//                // account is already in the database but the password
//                // doesn't work.)
//                messageView.setText(getText(R.string.login_activity_loginfail_text_pwonly));
//            }
//        }
//    }
//
//    public void onAuthenticationCancel() {
//        Log.i(LOGTAG, "onAuthenticationCancel()");
//
//        // Our task is complete, so clear it out
//        mAuthTask = null;
//
//        // Hide the progress dialog
//        hideProgress();
//    }
//
//    /**
//     * Returns the message to be displayed at the top of the login dialog box.
//     */
//    private CharSequence getMessage() {
//        getString(R.string.app_name);
//        if (TextUtils.isEmpty(mUsername)) {
//            // If no username, then we ask the user to log in using an
//            // appropriate service.
//            final CharSequence msg = getText(R.string.login_activity_newaccount_text);
//            return msg;
//        }
//        if (TextUtils.isEmpty(mPassword)) {
//            // We have an account but no password
//            return getText(R.string.login_activity_loginfail_text_pwmissing);
//        }
//        return null;
//    }
//
//    /**
//     * Shows the progress UI for a lengthy operation.
//     */
//    private void showProgress() {
//        showDialog(0);
//    }
//
//    /**
//     * Hides the progress UI for a lengthy operation.
//     */
//    private void hideProgress() {
//        if (mProgressDialog != null) {
//            mProgressDialog.dismiss();
//            mProgressDialog = null;
//        }
//    }
//
//    /**
//     * Represents an asynchronous task used to authenticate a user against the
//     * SampleSync Service
//     */
//    public class UserLoginTask extends AsyncTask<Void, Void, String> {
//
//        @Override
//        protected String doInBackground(Void... params) {
//            // We do the actual work of authenticating the user
//            // in the NetworkUtilities class.
//            try {
//                return NetworkUtilities.authenticate(mUsername, mPassword);
//            } catch (Exception ex) {
//                Log.e(LOGTAG, "UserLoginTask.doInBackground: failed to authenticate");
//                Log.i(LOGTAG, ex.toString());
//                return null;
//            }
//        }
//
//        @Override
//        protected void onPostExecute(final String authToken) {
//            // On a successful authentication, call back into the Activity to
//            // communicate the authToken (or null for an error).
//            onAuthenticationResult(authToken);
//        }
//
//        @Override
//        protected void onCancelled() {
//            // If the action was canceled (by the user clicking the cancel
//            // button in the progress dialog), then call back into the
//            // activity to let it know.
//            onAuthenticationCancel();
//        }
//    }

}