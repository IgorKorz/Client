package ru.ksu.edu.museum.mobile.client.capture.listener;

import android.graphics.PointF;
import android.media.Image;
import android.media.ImageReader;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ru.ksu.edu.museum.mobile.client.capture.CameraFragment;
import ru.ksu.edu.museum.mobile.client.network.UserClient;

public class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
    private static final String TAG = "ImageAvailableListener";

    private final CameraFragment owner;
//    private final File file;

//    public ImageAvailableListener(CameraFragment owner, File file) {
//        this.owner = owner;
//        this.file = file;
//    }

    public ImageAvailableListener(CameraFragment owner) {
        this.owner = owner;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
//        owner.getBackgroundHandler().post(new ImageSaver(reader.acquireNextImage(), file));
        owner.getBackgroundHandler().post(new ImageSender(reader.acquireNextImage()));
    }

//    private class ImageSaver implements Runnable {
//        private Image image;
//        private File file;
//
//        private ImageSaver(Image image, File file) {
//            this.image = image;
//            this.file = file;
//        }
//
//        @Override
//        public void run() {
//            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//            byte[] bytes = new byte[buffer.remaining()];
//
//            buffer.get(bytes);
//
//            try (FileOutputStream outputStream = new FileOutputStream(file)) {
//                outputStream.write(bytes);
//            } catch (IOException e) {
//                Log.e(TAG, e.getMessage());
//            } finally {
//                image.close();
//            }
//        }
//    }

    private class ImageSender implements Runnable {
        private static final double SCALE = 128;

        private Image image;
        private UserClient userClient = owner.getUserClient();
        private RandomParams drawParams = new RandomParams();

        private ImageSender(Image frame) {
            this.image = frame;
        }

        @Override
        public void run() {
            Mat frame = imageToMat(image);
            double newWidth = image.getWidth() / (image.getWidth() / SCALE);
            double newHeight = image.getHeight() / (image.getHeight() / SCALE);
            image.close();

            Mat resizedFrame = resizeMat(frame, newWidth, newHeight);
//            userClient.sendFrame(resizedFrame);
            owner.getActivity().runOnUiThread(new Drawer());
        }

        private Mat imageToMat(Image image) {
            Mat bufferMat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC1);
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            bufferMat.put(0, 0, bytes);
            return Imgcodecs.imdecode(bufferMat, Imgcodecs.IMREAD_COLOR);
        }

        private Mat resizeMat(Mat src, double newWidth, double newHeight) {
            Mat result = new Mat();
            Size newSize = new Size(newWidth, newHeight);
            Imgproc.resize(src, result, newSize);

            return result;
        }

        private class Drawer implements Runnable {
            @Override
            public void run() {
                owner.draw(drawParams.getIds(), drawParams.getPointFList());
            }
        }

        private class RandomParams {
            private Random r = new Random();
            private int bound = 10;

            private int[] getIds() {
                int[] ids = new int[r.nextInt(bound)];

                for (int i = 0; i < ids.length; i++) {
                    ids[i] = r.nextInt(bound);
                }

                return ids;
            }

            private List<PointF[]> getPointFList() {
                List<PointF[]> pointFList = new ArrayList<>();

                for (int i = 0; i < 4; i++) {
                    PointF[] pointsF = new PointF[4];

                    for (int j = 0; j < pointsF.length; j++) {
                        pointsF[j] = new PointF(r.nextFloat(), r.nextFloat());
                    }

                    pointFList.add(pointsF);
                }

                return pointFList;
            }
        }
    }
}
