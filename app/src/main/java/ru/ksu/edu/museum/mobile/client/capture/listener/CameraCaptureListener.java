package ru.ksu.edu.museum.mobile.client.capture.listener;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;

import ru.ksu.edu.museum.mobile.client.capture.CameraFragment;

import static ru.ksu.edu.museum.mobile.client.capture.CameraState.*;

public class CameraCaptureListener extends CameraCaptureSession.CaptureCallback {
    private final CameraFragment owner;

    public CameraCaptureListener(CameraFragment owner) {
        this.owner = owner;
    }

    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                   TotalCaptureResult result) {
        process(result);
    }

    @Override
    public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                    CaptureResult partialResult) {
        process(partialResult);
    }

    private void process(CaptureResult result) {
        switch (owner.getState()) {
            case WAITING_LOCK: {
                Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);

                if (afState == null) {
                    owner.captureStillPicture();
                } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                        || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                        || afState == CaptureResult.CONTROL_AF_STATE_INACTIVE) {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);

                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        owner.setState(PICTURE_TAKEN);
                        owner.captureStillPicture();
                    } else {
                        owner.runPrecaptureSequence();
                    }
                }

                break;
            }

            case WAITING_PRECAPTURE: {
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);

                if (aeState == null ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                    owner.setState(WAITING_NON_PRECAPTURE);
                }

                break;
            }

            case WAITING_NON_PRECAPTURE: {
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);

                if (aeState == null ||
                        aeState != CaptureRequest.CONTROL_AE_STATE_PRECAPTURE) {
                    owner.setState(PICTURE_TAKEN);
                    owner.captureStillPicture();
                }

                break;
            }
        }
    }
}
