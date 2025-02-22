package ru.ksu.edu.museum.mobile.client.capture;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class AutoFitTextureView extends TextureView {
    private static final String AE_NEGATIVE_SIZE_MSG = "Size cannot be negative!";

    private int ratioWidth = 0;
    private int ratioHeight = 0;

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public void setAspectRatio(int width, int height) {
        if (width == 0 || height == 0) {
            throw new IllegalArgumentException(AE_NEGATIVE_SIZE_MSG);
        }

        ratioWidth = width;
        ratioHeight = height;

        requestLayout();
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
}
