package ru.ksu.edu.museum.mobile.client.capture.listener;

import android.app.Activity;
import android.hardware.camera2.CameraDevice;

import ru.ksu.edu.museum.mobile.client.capture.CameraFragment;

public class CameraStateListener extends CameraDevice.StateCallback {
    private final CameraFragment owner;

    public CameraStateListener(CameraFragment owner) {
        this.owner = owner;
    }

    @Override
    public void onDisconnected(CameraDevice camera) {
        owner.getCameraOpenCloseLock().release();

        camera.close();

        owner.setCameraDevice(null);
    }

    @Override
    public void onError(CameraDevice camera, int error) {
        owner.getCameraOpenCloseLock().release();

        camera.close();

        owner.setCameraDevice(null);

        Activity activity = owner.getActivity();

        if (activity != null) {
            activity.finish();
        }
    }

    @Override
    public void onOpened(CameraDevice camera) {
        owner.getCameraOpenCloseLock().release();
        owner.setCameraDevice(camera);
        owner.createCameraPreviewSession();
    }
}
