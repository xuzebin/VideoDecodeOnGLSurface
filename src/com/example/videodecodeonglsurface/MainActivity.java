package com.example.videodecodeonglsurface;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import com.example.videodecodeonglsurface.DecoderCore;
import com.example.videodecodeonglsurface.MyGLSurfaceView;
import com.example.videodecodeonglsurface.R;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.opengl.EGLContext;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

public class MainActivity extends Activity implements SurfaceTexture.OnFrameAvailableListener {
	private static final String TAG = "MainActivity";
	private MyGLSurfaceView glSurfaceView;
	private SurfaceTexture surfaceTexture;
	DecoderThread decoder; //done on a decoder thread to avoid block in UI
	
	//a flag to notify the renderer to start drawing frames once the decoder is ready
	//TODO: add a handler in renderer to handle this message.
	boolean playingFlag;
	
	AssetFileDescriptor afd;
	Surface surface;
	SpeedController controller;
	
	
	//hardcode, read from external storage
//	private static final String filePath = 
//			Environment.getExternalStorageDirectory().toString() 
//			+ "/DCIM/Camera/" + "SLOW_MOTION_1280_720.mp4";
//			+ "/DCIM/Camera/VCameraDemo/" + "1443600911857.mp4";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		glSurfaceView = (MyGLSurfaceView) findViewById(R.id.glsurfaceview);

		playingFlag = false;

	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();

	}
	
	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();

	}
	
	@Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
		if (decoder != null) {
			decoder.stopPlaying();
			decoder = null;
		}
		if (glSurfaceView.renderer != null) {
	        glSurfaceView.stopRendering();
		}
    }
	
	//prepare before playing every video.
	public void prepareVideo() {
		//read video from res/raw/
	   	afd = getResources().openRawResourceFd(R.raw.test);

		surfaceTexture = glSurfaceView.getSurfaceTexture();//get surfacetexture created in renderer
		
		//Register a callback when a new frame is available to the SurfaceTexture
		surfaceTexture.setOnFrameAvailableListener(this);
			
		surface = new Surface(surfaceTexture);//get the surface to be used for decoding output
			
		controller = new SpeedController();
		decoder = new DecoderThread(afd, surface, controller);//initialize all the decoding stuff here.
	}
	
	public void startVideo() {
		if (decoder != null) stopVideo();

		prepareVideo();
		decoder.startPlaying();//start the video decoding thread here
			
		playingFlag = true;
	}

	@Override
	public void onFrameAvailable(SurfaceTexture surfaceTexture) {
		glSurfaceView.requestRender();	
	}
	//check if it is ready to play video
	public boolean isPlaying() {
		return playingFlag;
	}
	
	public void click_to_play(View argu) {
		startVideo();
		
	}
	
	public void click_to_stop(View argu) {
		stopVideo();//stop the video
	}
	
	public void stopVideo() {
		if (decoder != null) {
			decoder.stopPlaying();//delete the thread
			decoder = null;
			playingFlag = false;
		}
	
	}
	
}
