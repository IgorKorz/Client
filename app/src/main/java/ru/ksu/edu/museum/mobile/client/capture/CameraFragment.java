package ru.ksu.edu.museum.mobile.client.capture;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import ru.ksu.edu.museum.mobile.client.R;
import ru.ksu.edu.museum.mobile.client.capture.dialog.ConfirmationDialog;
import ru.ksu.edu.museum.mobile.client.capture.dialog.ErrorDialog;
import ru.ksu.edu.museum.mobile.client.capture.listener.CameraCaptureListener;
import ru.ksu.edu.museum.mobile.client.capture.listener.CameraCaptureSessionCallback;
import ru.ksu.edu.museum.mobile.client.capture.listener.CameraCaptureStillPictureSessionCallback;
import ru.ksu.edu.museum.mobile.client.capture.listener.CameraStateListener;
import ru.ksu.edu.museum.mobile.client.capture.listener.CaptureSurfaceTextureListener;
import ru.ksu.edu.museum.mobile.client.capture.listener.ImageAvailableListener;
import ru.ksu.edu.museum.mobile.client.network.UserClient;
import ru.ksu.edu.museum.mobile.client.opengl.OpenGLSurfaceView;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;

public class CameraFragment extends Fragment {
    public static final int REQUEST_CAMERA_PERMISSION = 1;

    //region private constants
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final String TAG = "CameraFragment";
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    private static final String ERROR_NOT_FIND_ANY_SUITABLE_SIZE_MSG =
            "Couldn't find any suitable preview size";
    private static final String ERROR_INVALID_ROTATION_PATTERN =
            "Display rotation is invalid: ";
    private static final String RE_CAMERA_OPENING_TIME_OUT_MSG =
            "Time out waiting to lock cmaera opening.";
    private static final String RE_CAMERA_OPENING_INTERRUPTED_MSG =
            "Interrupted while trying to lock camera opening.";
    private static final String RE_CAMERA_CLOSING_INTERRUPTED_MSG =
            "Interrupted while trying to lock camera closing.";
    private static final String HANDLER_NAME = "CameraBackground";
    private static final String ISE_TESTURE_IS_NULL = "Texture is null.";
    private static final String AD_INFO_MSG =
            "It's mobile application for KSU (ksu.edu.ru) AR-Museum";
    //endregion

    //region fields
    private static CameraFragment instance;
    private UserClient userClient;
    private CaptureSurfaceTextureListener surfaceTextureListener;
    private String cameraId;
    private AutoFitTextureView textureView;
    private OpenGLSurfaceView surfaceView;
    private Size previewSize;
    private CameraStateListener stateCallback;
    private HandlerThread backgroundThread;
    private ImageReader imageReader;
    private ImageAvailableListener onImageAvailableListener;
    private boolean flashSupported;
    private int sensorOrientation;
    private CaptureRequest.Builder stillCaptureBuilder;
    private CameraCaptureSession captureSession;
    private CameraDevice cameraDevice;
    private Handler backgroundHandler;
//    private File file;
    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest previewRequest;
    private CameraState State = CameraState.PREVIEW;
    private Semaphore cameraOpenCloseLock = new Semaphore(1);
    private CameraCaptureListener captureCallback;
    private BaseLoaderCallback loaderCallback = new LoaderCallback(getContext());
    private Thread takePictureThread;
    private PictureTaker pictureTaker = new PictureTaker();
    //endregion

    private CameraFragment() {
    }

    public static CameraFragment getInstance() {
        if (instance == null) {
            instance = new CameraFragment();
        }

        return instance;
    }

    //region override methods
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == REQUEST_CAMERA_PERMISSION &&
//                (grantResults.length != 1
//                        || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
//            ErrorDialog.newInstance(getString(R.string.request_permission))
//                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
//        }
//    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateCallback = new CameraStateListener(this);
        surfaceTextureListener = new CaptureSurfaceTextureListener(this);

        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_camera_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        textureView = view.findViewById(R.id.texture);
        surfaceView = view.findViewById(R.id.opengl_texture);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

//        file = new File(Objects.requireNonNull(getActivity())
//                .getFilesDir(), "frame.jpeg");
//
//        if (!file.exists()) {
//            file.mkdir();
//        }

        captureCallback = new CameraCaptureListener(this);
        onImageAvailableListener = new ImageAvailableListener(this);
        Resources res = getActivity().getResources();
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        userClient = new UserClient("admin",
                res.getString(R.string.server_host),
                res.getInteger(R.integer.server_port),
                ip);
        userClient.run();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, getContext(), loaderCallback);
        }

        startBackgroundThread();

        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();

        super.onPause();
    }
    //endregion

    //region getters and setters
    public void setCaptureSession(CameraCaptureSession captureSession) {
        this.captureSession = captureSession;
    }

    public CameraDevice getCameraDevice() {
        return cameraDevice;
    }

    public void setCameraDevice(CameraDevice cameraDevice) {
        this.cameraDevice = cameraDevice;
    }

    public CaptureRequest.Builder getPreviewRequestBuilder() {
        return previewRequestBuilder;
    }

    public CameraState getState() {
        return State;
    }

    public void setState(CameraState state) {
        State = state;
    }

    public Semaphore getCameraOpenCloseLock() {
        return cameraOpenCloseLock;
    }
    //endregion

    public void showToast(final String text) {
        final Activity activity = getActivity();

        if (activity != null) {
            activity.runOnUiThread(()
                    -> Toast.makeText(activity.getApplicationContext(), text, Toast.LENGTH_SHORT)
                    .show());
        }
    }

    public void openCamera(int width, int height) {
        if (getActivity() != null &&
                getActivity().checkSelfPermission(CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermission(CAMERA);

            return;
        }

        setUpCameraOutputs(width, height);
        configureTransform(width, height);

        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException(RE_CAMERA_OPENING_TIME_OUT_MSG);
            }

            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(RE_CAMERA_OPENING_INTERRUPTED_MSG, e);
        }
    }

    public void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();

            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }

            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }

            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(RE_CAMERA_CLOSING_INTERRUPTED_MSG, e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    public void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();

            if (texture == null) {
                throw new IllegalStateException(ISE_TESTURE_IS_NULL);
            }

            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            Surface surface = new Surface(texture);

            previewRequestBuilder
                    = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            List<Surface> surfaces = new ArrayList<>();
            surfaces.add(surface);
            surfaces.add(imageReader.getSurface());

            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSessionCallback(this),
                    null);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();

        if (textureView != null &&
                previewSize != null &&
                activity != null) {
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0,
                    viewWidth, viewHeight);
            RectF bufferRect = new RectF(0, 0,
                    previewSize.getHeight(), previewSize.getWidth());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();

            if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                bufferRect.offset(centerX - bufferRect.centerX(),
                        centerY - bufferRect.centerY());

                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

                float scale = Math.max((float) viewHeight / previewSize.getHeight(),
                        (float) viewWidth / previewSize.getWidth());

                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            } else if (rotation == Surface.ROTATION_180) {
                matrix.postRotate(180, centerX, centerY);
            }

            textureView.setTransform(matrix);
        }
    }

    public void runPrecaptureSequence() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);

            State = CameraState.WAITING_PRECAPTURE;

            captureSession.capture(previewRequestBuilder.build(), captureCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void captureStillPicture() {
        try {
            Activity activity = getActivity();

            if (activity != null && cameraDevice != null) {
                if (stillCaptureBuilder == null) {
                    stillCaptureBuilder = cameraDevice
                            .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                }

                stillCaptureBuilder.addTarget(imageReader.getSurface());
                stillCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                setAutoFlash(stillCaptureBuilder);

                int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

                stillCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

//                captureSession.stopRepeating();
                captureSession.capture(stillCaptureBuilder.build(),
                        new CameraCaptureStillPictureSessionCallback(this), null);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void unlockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);

            State = CameraState.PREVIEW;

            setAutoFlash(previewRequestBuilder);

            captureSession.capture(previewRequestBuilder.build(), captureCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (flashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF);
        }
    }

    public void buildPreviewRequest() {
        previewRequest = previewRequestBuilder.build();
    }

    public void setCaptureSessionRepeatingRequest() throws CameraAccessException {
        captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
    }

    public UserClient getUserClient() {
        return userClient;
    }

    public void draw(int[] ids, PointF[][] corners, double widthScale, double heightScale) {
        getActivity().runOnUiThread(surfaceView.getDrawer(ids, corners, widthScale, heightScale,
                getActivity().getResources()));
    }

    private static Size ChooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight,
                                          Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        for (Size option : choices) {
            if (option.getWidth() <= maxWidth &&
                    option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new SizesByAreaComparator());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new SizesByAreaComparator());
        } else {
            Log.e(TAG, ERROR_NOT_FIND_ANY_SUITABLE_SIZE_MSG);

            return choices[0];
        }
    }

    private void requestPermission(String permission) {
        if (shouldShowRequestPermissionRationale(permission)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[] {
                    permission,
                    INTERNET,
                    ACCESS_WIFI_STATE,
                    ACCESS_NETWORK_STATE
            }, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();

        if (activity != null) {
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            int maxImages = 2;

            try {
                for (int i = 0; i < manager.getCameraIdList().length; i++) {
                    String cameraId = manager.getCameraIdList()[i];
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        continue;
                    }

                    StreamConfigurationMap map = characteristics
                            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    if (map == null) {
                        continue;
                    }

                    Size largest = Collections.max(Arrays.asList(map.getOutputSizes(
                            ImageFormat.JPEG)), new SizesByAreaComparator());
                    imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                            ImageFormat.JPEG, maxImages);
                    imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);

                    int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                    sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    boolean swappedDimensions = false;

                    switch (displayRotation) {
                        case Surface.ROTATION_0:
                        case Surface.ROTATION_180: {
                            if (sensorOrientation == 90 || sensorOrientation == 270) {
                                swappedDimensions = true;
                            }

                            break;
                        }

                        case Surface.ROTATION_90:
                        case Surface.ROTATION_270: {
                            if (sensorOrientation == 0 || sensorOrientation == 180) {
                                swappedDimensions = true;
                            }

                            break;
                        }

                        default: {
                            Log.e(TAG, ERROR_INVALID_ROTATION_PATTERN + displayRotation);

                            break;
                        }
                    }

                    Point displaySize = new Point();

                    activity.getWindowManager().getDefaultDisplay().getSize(displaySize);

                    int rotatedPreviewWidth = width;
                    int rotatedPreviewHeight = height;
                    int maxPreviewWidth = displaySize.x;
                    int maxPreviewHeight = displaySize.y;

                    if (swappedDimensions) {
                        rotatedPreviewWidth = height;
                        rotatedPreviewHeight = width;
                        maxPreviewWidth = displaySize.y;
                        maxPreviewHeight = displaySize.x;
                    }

                    if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                        maxPreviewWidth = MAX_PREVIEW_WIDTH;
                    }

                    if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                        maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                    }

                    previewSize = ChooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                            rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                            maxPreviewHeight, largest);
                    int orientation = getResources().getConfiguration().orientation;

                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                        surfaceView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                    } else {
                        textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                        surfaceView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                    }

                    Boolean available = characteristics
                            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

                    if (available == null) {
                        flashSupported = false;
                    } else {
                        flashSupported = available;
                    }

                    this.cameraId = cameraId;

                    return;
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, e.getMessage());
            } catch (NullPointerException e) {
                ErrorDialog.newInstance(getString(R.string.camera_error))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread(HANDLER_NAME);
        backgroundThread.start();

        backgroundHandler = new Handler(backgroundThread.getLooper());

        pictureTaker.interrupted = false;
        takePictureThread = new Thread(pictureTaker);
        takePictureThread.start();
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();

        try {
            pictureTaker.interrupted = true;
            takePictureThread.join();
            takePictureThread = null;
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void takePicture() {
        lockFocus();
    }

    private void lockFocus() {
        if (captureSession != null) {
            try {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CaptureRequest.CONTROL_AF_TRIGGER_START);

                State = CameraState.WAITING_LOCK;

                captureSession.capture(previewRequestBuilder.build(), captureCallback,
                        backgroundHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private int getOrientation(int rotation) {
        return (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360;
    }

    private class PictureTaker implements Runnable {
        private boolean interrupted = false;

        @Override
        public void run() {
            while (!interrupted) {
                takePicture();
            }
        }
    }
}
