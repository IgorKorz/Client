package org.opencv.android;

import android.content.Context;

public class LoaderCallback extends BaseLoaderCallback {
    public LoaderCallback(Context context) {
        super(context);
    }

    @Override
    public void onManagerConnected(int status) {
        if (status != LoaderCallbackInterface.SUCCESS) {
            super.onManagerConnected(status);
        }
    }
}
