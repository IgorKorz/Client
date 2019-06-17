package ru.ksu.edu.museum.mobile.client.opengl;

import android.content.Context;
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

import java.util.Arrays;
import java.util.List;

public class OpenGLSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final int TEXT_SIZE = 50;
    private static final int COLOR = Color.RED;

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
        paint.setColor(COLOR);
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

    public void draw(int[] ids, List<PointF[]> corners) {
        Canvas canvas = getHolder().lockCanvas();

        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            Rect rect = new Rect(
                    getLeft() + (getRight() - getLeft()) / 3,
                    getTop() + (getBottom() - getTop()) / 3,
                    getRight() - (getRight() - getLeft()) / 3,
                    getBottom() - (getBottom() - getTop()) / 3
            );
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(rect, paint);

            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText("Detected ids:", rect.left, rect.centerY(), paint);
            canvas.drawText(Arrays.toString(ids), rect.left, rect.centerY() + TEXT_SIZE, paint);

            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    public void clear() {
        Canvas canvas = getHolder().lockCanvas();

        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            getHolder().unlockCanvasAndPost(canvas);
        }
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

    private Rect generateRect(List<PointF[]> corners) {
        Rect[] rectCorners = new Rect[4];
        int i = 0;

        for (PointF[] rectsF : corners) {
            int cornerLeft = (int) Math.abs(rectsF[3].y - rectsF[0].y);
            int cornerTop = (int) Math.abs(rectsF[0].x - rectsF[1].x);
            int cornerRight = (int) Math.abs(rectsF[1].y - rectsF[2].y);
            int cornerBottom = (int) Math.abs(rectsF[2].x - rectsF[3].x);
            rectCorners[i++] = new Rect(cornerLeft, cornerTop, cornerRight, cornerBottom);
        }

        return new Rect(Math.abs(rectCorners[3].centerY() - rectCorners[0].centerY()),
                Math.abs(rectCorners[0].centerX() - rectCorners[1].centerX()),
                Math.abs(rectCorners[1].centerY() - rectCorners[2].centerY()),
                Math.abs(rectCorners[2].centerX() - rectCorners[3].centerX()));
    }
}

