package ru.ksu.edu.museum.mobile.client;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.util.Log;

import ru.ksu.edu.museum.mobile.client.capture.CameraFragment;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String ERROR_OPENGL_ES_20_NOT_SUPPORTED =
            "OpenGL ES 2.0 not supported on device!";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActionBar() != null) {
            getActionBar().hide();
        }

        setContentView(R.layout.activity_camera);

        if (savedInstanceState == null && detectOpenGLES20()) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, CameraFragment.getInstance())
                    .commit();
        } else {
            Log.e(TAG, ERROR_OPENGL_ES_20_NOT_SUPPORTED);

            finish();
        }
    }

    private boolean detectOpenGLES20()
    {
        ActivityManager am =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        return (info.reqGlEsVersion >= 0x20000);
    }
}
