package ru.ksu.edu.museum.mobile.client.capture.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import ru.ksu.edu.museum.mobile.client.R;

public class ErrorDialog extends DialogFragment {
    private static final String ARG_MSG = "message";

    private static Activity activity;

    public static ErrorDialog newInstance(String message)
    {
        Bundle args = new Bundle();
        args.putString(ARG_MSG, message);

        ErrorDialog result = new ErrorDialog();
        result.setArguments(args);

        return result;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        activity = getActivity();

        return new AlertDialog.Builder(activity)
                .setMessage(getArguments().getString(ARG_MSG))
                .setPositiveButton(R.string.ok, new PositiveListener())
                .create();
    }

    private class PositiveListener implements DialogInterface.OnClickListener
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            activity.finish();
        }
    }
}
