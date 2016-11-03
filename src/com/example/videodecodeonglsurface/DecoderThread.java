package com.example.videodecodeonglsurface;

import java.io.File;
import java.io.FileDescriptor;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;
import android.content.res.AssetFileDescriptor;

/**
 * DecoderThread.java
 * decoder thread for decoding
 *
 * Author: xuzebin
 * Created on: 12/07/2016
 */

public class DecoderThread extends DecoderCore implements Runnable {

    private static final String TAG = "DecoderThread";
    
    public DecoderThread(File videoFile, Surface outputSurface, SpeedControlCallback cb) {
	super(videoFile, outputSurface, cb);	
    }
    
    public DecoderThread(AssetFileDescriptor afd, Surface outputSurface, SpeedControlCallback cb) {
	super(afd, outputSurface, cb);	
    }
	
    public DecoderThread(File videoFile, Surface outputSurface) {
	super(videoFile, outputSurface);	
    }

    @Override
    public void run() {
	long start = System.currentTimeMillis();
	doDecode(); //core components for decoding
	Log.i(TAG, "decode time: " + (System.currentTimeMillis() - start) / 1000.0f + "s");
	dumpVideoInfo();
    }
	
    public void startPlaying() {
	new Thread(this, "MyDecoder").start();
    }
	
    public void stopPlaying() {
	super.stopThread = true;
    }
}
