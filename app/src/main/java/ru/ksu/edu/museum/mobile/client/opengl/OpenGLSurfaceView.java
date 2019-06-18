package ru.ksu.edu.museum.mobile.client.opengl;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

import ru.ksu.edu.museum.mobile.client.R;

public class OpenGLSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final int TEXT_SIZE = 50;
    private static final int TEXT_COLOR = Color.RED;
    private static final int RECT_COLOR = Color.GREEN;

    private int ratioWidth;
    private int ratioHeight;
    private StringBuilder stringBuilder;
    private Paint paint;

    public OpenGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        stringBuilder = new StringBuilder();
        paint = new Paint();
        paint.setTextSize(TEXT_SIZE);

        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSPARENT);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void setAspectRatio(int width, int height) {
        ratioWidth = width;
        ratioHeight = height;

        requestLayout();
    }

    public void draw(int[] ids, PointF[][] corners, double widthScale, double heightScale,
                     Resources resources) {
//        if (ids == null || corners == null) {
//            return;
//        }
        clear();

        Canvas canvas = getHolder().lockCanvas();

        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            for (int i = 0; i < ids.length; i++) {
                PointF[] rectCorners = corners[i];
                Rect rect = generateRect(rectCorners, widthScale, heightScale);
                paint.setColor(RECT_COLOR);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(rect, paint);

                Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.images);
                paint.setColor(TEXT_COLOR);
                canvas.drawBitmap(bitmap, rect.left, rect.top, paint);
            }
        }

        getHolder().unlockCanvasAndPost(canvas);
    }

    public Drawer getDrawer(int[] ids, PointF[][] corners, double widhtScale, double heightScale,
                            Resources resources) {
        return new Drawer(ids, corners, widhtScale, heightScale, resources);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height);
        } else if (width < (float) height * ratioWidth / (float) ratioHeight) {
            setMeasuredDimension(width, width * ratioHeight / ratioWidth);
        } else {
            setMeasuredDimension(height * ratioWidth / ratioHeight, height);
        }
    }

    private Rect generateRect(PointF[] corners, double widthScale, double heightScale) {
        return new Rect(
                (int) (corners[0].x * widthScale),
                (int) (corners[0].y * heightScale),
                (int) (corners[2].x * widthScale),
                (int) (corners[2].y * heightScale)
        );
    }

    private void clear() {
        Canvas canvas = getHolder().lockCanvas();

        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }

        getHolder().unlockCanvasAndPost(canvas);
    }

    private class Drawer implements Runnable {
        private int[] ids;
        private PointF[][] corners;
        private double widthScale;
        private double heightScale;
        private Resources resources;

        private Drawer(int[] ids, PointF[][] corners, double widthScale, double heightScale,
                       Resources resources) {
            this.ids = ids;
            this.corners = corners;
            this.widthScale = widthScale;
            this.heightScale = heightScale;
            this.resources = resources;
        }

        @Override
        public void run() {
            draw(ids, corners, widthScale, heightScale, resources);
        }
    }
}

