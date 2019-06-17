package ru.ksu.edu.museum.mobile.client.opengl;

import android.opengl.GLES20;
import android.util.Log;

public class ESShader {
	private static final String TAG = "ESShader";
	private static final String ERROR_LINKING_MSG = "Error linking program:";

	public static int loadShader(int type, String shaderSrc) {
		int shader;
		int[] compiled = new int[1];
		shader = GLES20.glCreateShader(type);

		if (shader == 0) return 0;

		GLES20.glShaderSource(shader, shaderSrc);
		GLES20.glCompileShader(shader);
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);

		if (compiled[0] == 0) {
			Log.e(TAG, GLES20.glGetShaderInfoLog(shader));

			GLES20.glDeleteShader(shader);

			return 0;
		}

		return shader;
	}

	public static int loadProgram(String vertShaderSrc, String fragShaderSrc) {
		int vertexShader;
		int fragmentShader;
		int programObject;
		int[] linked = new int[1];
		vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertShaderSrc);

		if (vertexShader == 0) return 0;

		fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragShaderSrc);

		if (fragmentShader == 0) {
			GLES20.glDeleteShader(vertexShader);

			return 0;
		}

		programObject = GLES20.glCreateProgram();

		if (programObject == 0) return 0;

		GLES20.glAttachShader(programObject, vertexShader);
		GLES20.glAttachShader(programObject, fragmentShader);
		GLES20.glLinkProgram(programObject);
		GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0);

		if (linked[0] == 0) {
			Log.e(TAG, ERROR_LINKING_MSG);
			Log.e(TAG, GLES20.glGetProgramInfoLog(programObject));

			GLES20.glDeleteProgram(programObject);

			return 0;
		}

		GLES20.glDeleteShader(vertexShader);
		GLES20.glDeleteShader(fragmentShader);

		return programObject;
	}
}
