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
	private static final String TAG = "DecoderCore";
	DecoderThread(File videoFile, Surface outputSurface) {
		super(videoFile, outputSurface);	
	}
	
	DecoderThread(AssetFileDescriptor afd, Surface outputSurface) {
		super(afd, outputSurface);	
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		long start = System.currentTimeMillis();
		doDecode();	//core components for decoding
		Log.i(TAG, "decode time: " + (System.currentTimeMillis() - start) / 1000.0f + "s");
		dumpVideoInfo();
	}
	
	void startPlaying() {
		new Thread(this, "MyDecoder").start();
	}
	
	void stopPlaying() {
		release();//release the decoding resources.
	}
}
