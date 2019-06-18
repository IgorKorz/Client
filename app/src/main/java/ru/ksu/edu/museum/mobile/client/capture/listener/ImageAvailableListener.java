package ru.ksu.edu.museum.mobile.client.capture.listener;

import android.graphics.PointF;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

import ru.ksu.edu.museum.mobile.client.capture.CameraFragment;
import ru.ksu.edu.museum.mobile.client.network.UserClient;

public class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
    private static final String TAG = "ImageAvailableListener";

    private final CameraFragment owner;
    private UserClient userClient;
    private Thread receiverThred;

    public ImageAvailableListener(CameraFragment owner) {
        this.owner = owner;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        if (userClient == null) {
            userClient = owner.getUserClient();
        }

        Image image = reader.acquireLatestImage();

        if (image == null) {
            return;
        }

        if (receiverThred == null) {
            double widthScale = image.getWidth() / 128.0;
            double heightScale = image.getHeight() / 128.0;
            receiverThred = new Thread(new Receiver(widthScale, heightScale));
            receiverThred.run();
        }

        Mat frame = imageToMat(image);
        image.close();

        if (frame.cols() > 0) {
            Mat resizedFrame = resizeMat(frame, 128, 128);
            userClient.sendFrame(resizedFrame);

//            Thread receiveThread = new Thread(new Receiver(widthScale, heightScale));
//            receiveThread.start();
//
//            byte[] data = userClient.receive();
//
//            if (data != null) {
//                try {
//                    String response = new String(data);
//                    int responseEnd = response.lastIndexOf('}');
//                    JSONObject responseObject = new JSONObject(response.substring(0, responseEnd + 1));
//
//                    if (responseObject.getInt("DetectedMarkersCount") > 0) {
//                        SceneData sceneData = parseResponse(responseObject);
//
//                        owner.draw(sceneData.ids, sceneData.corners, widthScale, heightScale);
//                    }
//                } catch (JSONException e) {
//                    Log.e(TAG, e.getMessage(), e);
//                }
//            }
        }
    }

    private Mat imageToMat(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Mat bufferMat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        bufferMat.put(0, 0, bytes);

        return Imgcodecs.imdecode(bufferMat, Imgcodecs.IMREAD_COLOR);
    }

    private Mat resizeMat(Mat src, double newWidth, double newHeight) {
        Mat result = new Mat();
        Size newSize = new Size(newWidth, newHeight);
        Imgproc.resize(src, result, newSize);

        return result;
    }

    private class SceneData {
        private int[] ids;
        private PointF[][] corners;
    }

    private class Receiver implements Runnable {
        private double widthScale;
        private double heightScale;

        public Receiver(double widthScale, double heightScale) {
            this.widthScale = widthScale;
            this.heightScale = heightScale;
        }

        @Override
        public void run() {
            while (true) {
                byte[] data = userClient.receive();

                if (data == null) continue;

                try {
                    String response = new String(data);
                    int responseEnd = response.lastIndexOf('}');
                    JSONObject responseObject = new JSONObject(response.substring(0, responseEnd + 1));

                    if (responseObject.getInt("DetectedMarkersCount") > 0) {
                        SceneData sceneData = parseResponse(responseObject);

                        owner.draw(sceneData.ids, sceneData.corners, widthScale, heightScale);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }

        private SceneData parseResponse(JSONObject response) throws JSONException {
            SceneData sceneData = new SceneData();
            JSONArray jsonIds = response.getJSONArray("Ids");
            int[] ids = new int[jsonIds.length()];

            for (int i = 0; i < jsonIds.length(); i++) {
                ids[i] = jsonIds.getInt(i);
            }

            sceneData.ids = ids;

            JSONArray jsonCorners = response.getJSONArray("Corners");
            PointF[][] corners = new PointF[jsonCorners.length()][4];

            for (int i = 0; i < jsonCorners.length(); i++) {
                JSONArray jsonSomeCorners = jsonCorners.getJSONArray(i);

                for (int j = 0; j < jsonSomeCorners.length(); j++) {
                    JSONObject corner = jsonSomeCorners.getJSONObject(j);
                    corners[i][j] = new PointF((float) corner.getDouble("X"),
                            (float) corner.getDouble("Y"));
                }
            }

            sceneData.corners = corners;

            return sceneData;
        }
    }
}
