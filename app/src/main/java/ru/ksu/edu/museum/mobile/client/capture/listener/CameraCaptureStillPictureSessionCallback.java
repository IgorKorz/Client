package ru.ksu.edu.museum.mobile.client.capture.listener;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Log;

import ru.ksu.edu.museum.mobile.client.capture.CameraFragment;

public class CameraCaptureStillPictureSessionCallback extends CameraCaptureSession.CaptureCallback {
//    private static final String TAG = "StillPictureCallback";
    private static final String MSG = "Image has been sent";

    private final CameraFragment owner;

    public CameraCaptureStillPictureSessionCallback(CameraFragment owner) {
        this.owner = owner;
    }

    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                   TotalCaptureResult result) {
        owner.showToast(MSG);

//        Log.d(TAG, owner.getFile().getAbsolutePath());

        owner.unlockFocus();
    }
}
