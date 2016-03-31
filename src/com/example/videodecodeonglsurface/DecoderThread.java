package com.example.videodecodeonglsurface;

import java.io.File;
import java.io.FileDescriptor;

import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;
import android.content.res.AssetFileDescriptor;

/**
 * DecoderThread.java
 * decoder thread helper for DecoderCore
 * Author: xuzebin
 * Created on: 12/07/2016
 */

public class DecoderThread extends DecoderCore implements Runnable {
	private static final String TAG = "DecoderThread";
	
	DecoderThread(File videoFile, Surface outputSurface, SpeedControlCallback cb) {
		super(videoFile, outputSurface, cb);	
	}
	
	DecoderThread(AssetFileDescriptor afd, Surface outputSurface, SpeedControlCallback cb) {
		super(afd, outputSurface, cb);	
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		long start = System.currentTimeMillis();
		doDecode(); //core components for decoding
		Log.i(TAG, "decode time: " + (System.currentTimeMillis() - start) / 1000.0f + "s");
		dumpVideoInfo();
		
		Log.i(TAG, "thread end..");

	}
	
	void startPlaying() {
		new Thread(this, "MyDecoder").start();
	}
	
	void stopPlaying() {
		stopThread = true;
//		release();//release the decoding resources.		
	}
	

}
