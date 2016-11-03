package com.example.videodecodeonglsurface;

import java.io.File;
<<<<<<< HEAD
import java.io.FileDescriptor;

import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;
import android.content.res.AssetFileDescriptor;

/**
 * DecoderThread.java
 * decoder thread helper for DecoderCore
=======

import android.opengl.GLSurfaceView;
import android.view.Surface;

/**
 * DecoderThread.java
 * decoder thread for decoding
>>>>>>> origin/master
 * Author: xuzebin
 * Created on: 12/07/2016
 */

public class DecoderThread extends DecoderCore implements Runnable {
<<<<<<< HEAD
	private static final String TAG = "DecoderThread";
	
	DecoderThread(File videoFile, Surface outputSurface, SpeedControlCallback cb) {
		super(videoFile, outputSurface, cb);	
	}
	
	DecoderThread(AssetFileDescriptor afd, Surface outputSurface, SpeedControlCallback cb) {
		super(afd, outputSurface, cb);	
=======
	
	DecoderThread(File videoFile, Surface outputSurface) {
		super(videoFile, outputSurface);	
>>>>>>> origin/master
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
<<<<<<< HEAD
		long start = System.currentTimeMillis();
		doDecode(); //core components for decoding
		Log.i(TAG, "decode time: " + (System.currentTimeMillis() - start) / 1000.0f + "s");
		dumpVideoInfo();
		
		Log.i(TAG, "thread end..");

=======
		doDecode();	//core components for decoding
		
		dumpVideoInfo();
>>>>>>> origin/master
	}
	
	void startPlaying() {
		new Thread(this, "MyDecoder").start();
	}
	
	void stopPlaying() {
<<<<<<< HEAD
		stopThread = true;
//		release();//release the decoding resources.		
	}
	

=======
		release();//release the decoding resources.
	}
>>>>>>> origin/master
}
