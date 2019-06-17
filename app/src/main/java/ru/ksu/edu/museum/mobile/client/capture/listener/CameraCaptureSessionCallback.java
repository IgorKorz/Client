package ru.ksu.edu.museum.mobile.client.capture.listener;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.util.Log;

import ru.ksu.edu.museum.mobile.client.capture.CameraFragment;

import static android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE;
import static android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;

public class CameraCaptureSessionCallback extends CameraCaptureSession.StateCallback {
    private static final String TAG = "CaptureSessionCallback";
    private static final String FAILED = "Failed!";

    private final CameraFragment owner;

    public CameraCaptureSessionCallback(CameraFragment owner) {
        this.owner = owner;
    }

    @Override
    public void onConfigured(CameraCaptureSession session) {
        if (owner.getCameraDevice() != null) {
            owner.setCaptureSession(session);

            owner.getPreviewRequestBuilder()
                    .set(CONTROL_AF_MODE, CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            owner.setAutoFlash(owner.getPreviewRequestBuilder());
            owner.buildPreviewRequest();

            try {
                owner.setCaptureSessionRepeatingRequest();
            } catch (CameraAccessException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    @Override
    public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
        owner.showToast(FAILED);
    }
}
