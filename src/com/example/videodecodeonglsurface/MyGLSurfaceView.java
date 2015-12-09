package com.example.videodecodeonglsurface;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.example.videodecodeonglsurface.MainActivity;
import com.example.videodecodeonglsurface.IVCGLLib;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;

public class MyGLSurfaceView extends GLSurfaceView {
	private static final String TAG = "MyGLSurfaceView";
	MainActivity context;
	MyRenderer renderer;
	
	int screenWidth, screenHeight;
	boolean playing;

	public MyGLSurfaceView(Context context, AttributeSet attri) {
		super(context, attri);
		init(context);
	}
	public MyGLSurfaceView(Context context) {
		super(context);
		init(context);
	}
	
	private void init(Context context) {

		this.context = (MainActivity) context;
		setEGLContextClientVersion(2);

		renderer = new MyRenderer();
		setRenderer(renderer);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		
		DisplayMetrics dm = new DisplayMetrics();   
		this.context.getWindowManager().getDefaultDisplay().getMetrics(dm); 
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels;
		
		playing = false;
		
	}
	
	public SurfaceTexture getSurfaceTexture() {
		return renderer.createSurfaceTexture();
//		queueEvent(new Runnable() {
//			@Override 
//			public void run() {
//                renderer.createSurfaceTexture();
//            }
//		});
	}
	
	void stopRendering() {
		renderer.release();
	}
	
	
	class MyRenderer implements GLSurfaceView.Renderer {
		private static final String TAG = "MyRenderer";
		private int textureId;
		SurfaceTexture mSurfaceTexture;
		
		private String vertShader;
		private String fragShader_Pre;
		private int programHandle;
		private int mPositionHandle;
		private int mTextureCoordHandle;
		
		FloatBuffer verticesBuffer, textureVerticesPreviewBuffer;
		private ShortBuffer drawListBuffer;
		
		// number of coordinates per vertex in this array
		private final int COORDS_PER_VERTEX = 2;
		
		private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
		
		private final float squareVertices[] = { // in counterclockwise order:
				-1.0f, 1.0f, 
				-1.0f, -1.0f, 
				1.0f, -1.0f, 
				1.0f, 1.0f
			 };
		private final float textureVerticesPreview[] = { // in counterclockwise order:
				0.0f, 1.0f, 
				1.0f, 1.0f, 
				1.0f, 0.0f, 
				0.0f, 0.0f 
			 };
		private final short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices
		
		
		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
			
//			context.startPlayingVideo();
			initTexture();
		}
		
		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height) {
			GLES20.glViewport(0, 0, width, height);
		}
		
		@Override
		public void onDrawFrame(GL10 arg0) {
			if (context.isPlaying()) {
				GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);		
				
				mSurfaceTexture.updateTexImage();
				draw();
			}
		}
		
		public SurfaceTexture createSurfaceTexture() {
			textureId = createVideoTexture();
			mSurfaceTexture = new SurfaceTexture(textureId);
			return mSurfaceTexture;
		}
	    private int createVideoTexture() {
	        int[] texture = new int[1];

	        GLES20.glGenTextures(1,texture, 0);
	        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
	        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
	             GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
	        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
	             GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
	        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
	             GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
	        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
	             GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

	        return texture[0];
	    }
	    
	    void initTexture() {
	    	verticesBuffer = IVCGLLib.glToFloatBuffer(squareVertices);
			textureVerticesPreviewBuffer = IVCGLLib
					.glToFloatBuffer(textureVerticesPreview);
			drawListBuffer = IVCGLLib.glToShortBuffer(drawOrder);
	    	
			vertShader = IVCGLLib.loadFromAssetsFile(
					"IVC_VShader_Preview.sh", getContext().getResources());
			fragShader_Pre = IVCGLLib.loadFromAssetsFile(
					"IVC_FShader_Preview.sh", getContext().getResources());

			programHandle = IVCGLLib.glCreateProgram(vertShader, fragShader_Pre);
	    	
	    	mPositionHandle = GLES20.glGetAttribLocation(programHandle, "position");
	    	mTextureCoordHandle = GLES20.glGetAttribLocation(programHandle, "inputTextureCoordinate");	
	    	
	    	
	    }
	    
	    void draw() {

			GLES20.glUseProgram(programHandle);

			GLES20.glEnableVertexAttribArray(mPositionHandle);
			GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, verticesBuffer);
			GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
			GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX,GLES20.GL_FLOAT, false, vertexStride, textureVerticesPreviewBuffer);
			
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);			
			GLES20.glUniform1i(GLES20.glGetUniformLocation(programHandle, "sampler2d"), 0);
   		    	
			IVCGLLib.glUseFBO(0, 0, screenWidth, screenHeight, false, 0, 0);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
			IVCGLLib.glCheckGlError("glDrawElements");	
			
			GLES20.glDisableVertexAttribArray(mPositionHandle);
			GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
	    }
	    
	    void release() {
	    	Log.d(TAG, "deleting program " + programHandle);
	        GLES20.glDeleteProgram(programHandle);
	        programHandle = -1;
	        
	        Log.d(TAG, "releasing SurfaceTexture");
	        if (mSurfaceTexture != null) {
	        	mSurfaceTexture.release();
	        	mSurfaceTexture = null;
	        }        
	    }
		
	}
}
