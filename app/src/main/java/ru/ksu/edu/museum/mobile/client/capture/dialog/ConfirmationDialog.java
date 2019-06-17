package ru.ksu.edu.museum.mobile.client.capture.dialog;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import ru.ksu.edu.museum.mobile.client.R;
import ru.ksu.edu.museum.mobile.client.capture.CameraFragment;

public class ConfirmationDialog extends DialogFragment {
    private static Fragment parent;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        parent = getParentFragment();

        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.request_permission)
                .setPositiveButton(R.string.ok, new PositiveListener())
                .setNegativeButton(R.string.cancel, new NegativeListener())
                .create();
    }

    private class PositiveListener implements DialogInterface.OnClickListener
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            parent.requestPermissions(new String[] { Manifest.permission.CAMERA },
                    CameraFragment.REQUEST_CAMERA_PERMISSION);
        }
    }

    private class NegativeListener implements DialogInterface.OnClickListener
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            Activity activity = parent.getActivity();

            if (activity != null)
            {
                activity.finish();
            }
        }
    }
}
