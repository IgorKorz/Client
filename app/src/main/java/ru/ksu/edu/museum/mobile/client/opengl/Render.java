package ru.ksu.edu.museum.mobile.client.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Render implements GLSurfaceView.Renderer {
    private static final int GL_TEXTURE = GLES20.GL_TEXTURE_2D;

    private final float[] verticesData = {
            -0.5f, 0.5f, 0.0f,
            0.0f, 0.0f,
            -0.5f, -0.5f, 0.0f,
            0.0f, 1.0f,
            0.5f, -0.5f, 0.0f,
            1.0f, 1.0f,
            0.5f, 0.5f, 0.0f,
            1.0f, 0.0f
    };
    private final short[] indicesData = {
            0, 1, 2, 0, 2, 3
    };

    private Context context;
    private int programObject;
    private int posLoc;
    private int texCoordLoc;
    private int samplerLoc;
    private int textureId;
    private int width;
    private int height;
    private FloatBuffer vertices;
    private ShortBuffer indices;

    public Render(Context context) {
        this.context = context;
        vertices = ByteBuffer.allocateDirect(verticesData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertices.put(verticesData).position(0);

        indices = ByteBuffer.allocateDirect(indicesData.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        indices.put(indicesData).position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        String vShaderStr = "attribute vec4 a_position;\n"
                        + "attribute vec2 a_texCoord;  \n"
                        + "varying vec2 v_texCoord;    \n"
                        + "void main() {               \n"
                        + "   gl_Position = a_position;\n"
                        + "   v_texCoord = a_texCoord; \n"
                        + "}                           \n";

        String fShaderStr = "precision mediump float;                \n"
                + "varying vec2 v_texCoord;                          \n"
                + "uniform sampler2D s_texture;                      \n"
                + "void main() {                                     \n"
                + "  gl_FragColor = texture2D(s_texture, v_texCoord);\n"
                + "}                                                 \n";
        programObject = ESShader.loadProgram(vShaderStr, fShaderStr);
        posLoc = GLES20.glGetAttribLocation(programObject, "a_position");
        texCoordLoc = GLES20.glGetAttribLocation(programObject, "a_texCoord");
        samplerLoc = GLES20.glGetUniformLocation(programObject, "s_texture");
        textureId = createTexture();

        GLES20.glClearColor(0f, 0f, 0f, 0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glViewport(0, 0, width, height);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(programObject);

        vertices.position(0);

        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT,
                false, 5 * 4, vertices);
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glEnableVertexAttribArray(texCoordLoc);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE, textureId);
        GLES20.glUniform1i(samplerLoc, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indices);
    }

    private int createTexture() {
        int[] textureId = new int[1];
        byte[] pixels = new byte[] {
                127, 0, 0,
                0, 127, 0,
                0, 0, 127,
                127, 127, 0
        };
        ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(4 * 3);
        pixelBuffer.put(pixels).position(0);

        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GL_TEXTURE, textureId[0]);
        GLES20.glTexImage2D(GL_TEXTURE, 0, GLES20.GL_RGB,
                2, 2, 0,
                GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, pixelBuffer);
        GLES20.glTexParameteri(GL_TEXTURE, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        return textureId[0];
    }
}
