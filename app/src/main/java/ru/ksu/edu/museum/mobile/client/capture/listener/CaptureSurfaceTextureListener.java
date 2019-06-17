package ru.ksu.edu.museum.mobile.client.capture.listener;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

import ru.ksu.edu.museum.mobile.client.capture.CameraFragment;

public class CaptureSurfaceTextureListener implements TextureView.SurfaceTextureListener {
    private final CameraFragment owner;

    public CaptureSurfaceTextureListener(CameraFragment owner) {
        this.owner = owner;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        owner.openCamera(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        owner.configureTransform(width, height);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}
