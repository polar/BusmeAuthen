package com.adiron.busme.authen;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;

public class MunicipalitiesDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(inflater.inflate(R.layout.login_activity, null));
        return builder.create();
    }
    
    interface MuncipalitiesDialogListener {
    	public void onSelect(DialogFragment dialog);
    	public void onCancel(DialogFragment dialog);
    }
    
    MuncipalitiesDialogListener listener;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
        	listener = (MuncipalitiesDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MuncipalitiesDialogListener");
        }
    }
}
