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
 * This activity gets information from the user and creates
 * a Busme Account for a particular Municipality if it authenticates.
 * 
 * Once it collects the required information from the user, this activity
 * invokes another activity, which the user can cancel, that goes off
 * to the authentication server to check the email/password combination.
 */
public class AddAccountActivity extends AccountAuthenticatorActivity {
	private final static String LOGTAG = AddAccountActivity.class.getName();
	
	// GUI
	private TextView messageView;
    private EditText passwordEditText;
    private EditText emailEditText;
    private Button performButton;
    private Button cancelButton;
	private TextView bottomMessageView;
	private Spinner municipalitySpinner;

	// Given Data
	private ArrayList<String> municipalities;
	private ArrayList<String> municipalityUrls;

	private AccountManager accountManager;
	
	// The following fields contains data received from the user or 
	// derived from input. 
	
	// The accountName becomes the name of the selected municipality.
	// There is only at most one Busme account per municipality name.
	private Account account;
	private String accountName;
	private Bundle accountData;
	private String accountPassword;
	private String accountEmail;
	private String municipalityName;
	private String municipalityUrl;

	private Object authenticatorAction;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle icicle) {
        Log.i(LOGTAG, "onCreate(" + icicle + ")");
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.login_activity);
        
        getWindow().setFeatureDrawableResource(
                Window.FEATURE_LEFT_ICON, android.R.drawable.ic_input_add);

        Intent intent = getIntent();
        
        // We are handed a list of available municipality names from the Authenticator
        // and their corresponding login URLs that the authenticator knows how to handle.
        // If we are updating credentials, both will contain a list of only one value.
        municipalities = intent.getStringArrayListExtra(Authenticator.KEY_MUNICIPALITIES);
        municipalityUrls = getIntent().getStringArrayListExtra(Authenticator.KEY_MUNICIPALITY_URLS);
        authenticatorAction = intent.getStringExtra(Authenticator.KEY_AUTHENTICATOR_ACTION);
        // If we are updating credentials, the email should be there.
        accountEmail = intent.getStringExtra(Authenticator.KEY_ACCOUNT_EMAIL);
        
        // GUI
        municipalitySpinner = (Spinner) findViewById(R.id.municipalitySpinner);
        messageView = (TextView) findViewById(R.id.messageView);
        
        performButton = (Button) findViewById(R.id.performButton);
        if (Authenticator.ACTION_ADD_ACCOUNT.equals(authenticatorAction)) {
        	performButton.setText(R.string.login_activity_perform_button_add);
        } else if (Authenticator.ACTION_UPDATE_ACCOUNT.equals(authenticatorAction)) {
        	performButton.setText(R.string.login_activity_perform_button_update);
        }
        
        cancelButton = (Button) findViewById(R.id.cancelButton);
        emailEditText = (EditText) findViewById(R.id.emailEditText);
        if (accountEmail != null) {
        	emailEditText.setText(accountEmail);
        }
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        bottomMessageView = (TextView) findViewById(R.id.message_bottom);

        // GUI Data
        SpinnerAdapter adapter = new CollectionsAdapter<String>(this, R.layout.login_activity_spinner_text_view, municipalities);
        municipalitySpinner.setAdapter(adapter);
        messageView.setText(R.string.login_activity_message_add_account);
        bottomMessageView.setText(R.string.login_acitivity_bottom_message_instructions);
        
        // GUI Functionality
        performButton.setOnClickListener(new PerformClickListener());
		cancelButton.setOnClickListener(new CancelClickListener());
		
		// We need the AccountManager at some point
        accountManager = AccountManager.get(this);
    }
    
    /**
     * This listener applies to a click on the performButton (Add Account).
     */
	private class PerformClickListener implements OnClickListener
	{
		@Override public void onClick(View pView)
		{
			final int municipalityIdx = municipalitySpinner.getSelectedItemPosition();
            municipalityName = municipalities.get(municipalityIdx);
            municipalityUrl = municipalityUrls.get(municipalityIdx);
            
			accountEmail = emailEditText.getText().toString();
			accountPassword = passwordEditText.getText().toString();
            
			accountData = new Bundle();
			accountData.putString(Authenticator.KEY_MUNICIPALITY_URL, municipalityUrl);
			accountData.putString(Authenticator.KEY_MUNICIPALITY_NAME, municipalityName);
			accountData.putString(Authenticator.KEY_ACCOUNT_EMAIL, accountEmail);
			
			// The account name as far as Android goes is the municipality name.
			accountName = municipalityName; // May not be unique enough.
			
			account = new Account(accountName, Authenticator.ACCOUNT_TYPE);
			// Okay its not proper to do it here, only after we check every thing
			// because there is no way to explicitly remove it. Therefore we must
			// transfer all information to the Checking Activity in an Intent.
			//accountManager.addAccountExplicitly(account, password, accountData);
			// We must wait until confirmation to add an account to the AccountManager.
			
			// Set up and activity to check the credentials before adding this account
			// to the AccountManager.
			Intent intent = new Intent(AddAccountActivity.this, CheckingAccountActivity.class);
			intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
			intent.putExtra(Authenticator.KEY_ACCOUNT_EMAIL, accountEmail);
			
			//TODO: I wonder if passing the password in an Intent is unsafe, i.e. reading the process table?
			intent.putExtra(AccountManager.KEY_PASSWORD, accountPassword);
			intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE);
	        intent.putExtra(Authenticator.KEY_MUNICIPALITY_NAME, municipalityName);
	        intent.putExtra(Authenticator.KEY_MUNICIPALITY_URL, municipalityUrl);

			Log.d(LOGTAG, "Starting CheckingAccountActivity(pid="+Binder.getCallingPid()+
					", uid="+Binder.getCallingUid()+", name="+account.name+
					", url="+accountManager.getUserData(account, Authenticator.KEY_MUNICIPALITY_URL)+
					", muniName="+accountManager.getUserData(account, Authenticator.KEY_MUNICIPALITY_NAME));
			AddAccountActivity.this.startActivityForResult(intent, 100);
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(LOGTAG, "onActivityResult(requestCode=" + requestCode
				+ ", resultCode=" + resultCode + ", data=" + data);
		Bundle result = new Bundle();

		// Activity canceled by user.
		if (resultCode == CheckingAccountActivity.RESULT_CANCELED) {
			Log.d(LOGTAG, "CheckAcountActivity returns RESULT_CANCELED");
			result.putInt(AccountManager.KEY_ERROR_CODE,
					AccountManager.ERROR_CODE_CANCELED);
			result.putString(AccountManager.KEY_ERROR_MESSAGE,
					"Add Account Canceled");
			AddAccountActivity.this.setAccountAuthenticatorResult(result);
		}
		// Could not contact the authentication server or some other IO error.
		else if (resultCode == CheckingAccountActivity.RESULT_SERVER_FAILED) {
			Log.d(LOGTAG, "CheckAcountActivity returns RESULT_SERVER_FAILED");
			result.putInt(AccountManager.KEY_ERROR_CODE,
					data.getIntExtra(AccountManager.KEY_ERROR_CODE, 0));
			result.putString(AccountManager.KEY_ERROR_MESSAGE,
					data.getStringExtra(AccountManager.KEY_ERROR_MESSAGE));
			AddAccountActivity.this.setAccountAuthenticatorResult(result);
		}
		// The authentication failed on the server side.
		else if (resultCode == CheckingAccountActivity.RESULT_AUTHENTICATION_FAILED) {
			Log.d(LOGTAG,
					"CheckAcountActivity returns RESULT_AUTHENTICATION_FAILED");
			result.putInt(AccountManager.KEY_ERROR_CODE,
					data.getIntExtra(AccountManager.KEY_ERROR_CODE, 0));
			result.putString(AccountManager.KEY_ERROR_MESSAGE,
					data.getStringExtra(AccountManager.KEY_ERROR_MESSAGE));
			AddAccountActivity.this.setAccountAuthenticatorResult(null);

		}
		// The authentication was successful. Add the account and its AuthToken.
		else if (resultCode == CheckingAccountActivity.RESULT_OK) {
			Log.d(LOGTAG, "CheckAcountActivity returns RESULT_OK");

			// It seems whether we are adding or updating this call works just as
			// well. I assume that if we had other data, we may want to look it up
			// first.
			String authToken = data
					.getStringExtra(AccountManager.KEY_AUTHTOKEN);
			// We are adding the account
			accountManager.addAccountExplicitly(account, accountPassword,
					accountData);
			accountManager.setAuthToken(account, Authenticator.AUTHTOKEN_TYPE,
					authToken);

			// Required response by Android Framework.
			result.putString(AccountManager.KEY_ACCOUNT_NAME,
					data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
			result.putString(AccountManager.KEY_ACCOUNT_TYPE,
					data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
			AddAccountActivity.this.setAccountAuthenticatorResult(result);
		}
		AddAccountActivity.this.finish();
	};
	
	private class CancelClickListener implements OnClickListener
	{
		@Override public void onClick(View pView)
		{
			// Using null here sends ERROR_CODE_CANCELED as per Android Fremwork documentation.
			AddAccountActivity.this.setAccountAuthenticatorResult(null);
			AddAccountActivity.this.finish();
		}
	}
}