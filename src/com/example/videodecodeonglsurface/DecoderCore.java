package com.example.videodecodeonglsurface;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import android.content.res.AssetFileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

/**
 * DecoderCore.java
 * Author: xuzebin
 * Created on: 12/03/2016
 */

/**
 * core components for video decoder (do not consider audio, loop back).
 */
public class DecoderCore {
    private static final String TAG = "DecoderCore";
    
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private File mVideoFile;
    private AssetFileDescriptor mAssetFileDescriptor;
    private Surface mOutputSurface;
    private MediaExtractor extractor = null;
    private MediaCodec decoder = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private int sampleNumber;
    
    SpeedControlCallback mCallback;//control playback speed and (TODO: loop playback)
    
    protected boolean stopThread = false;//control play and stop
    
    private static final long SLEEP_SLICE = 10;

    /**
     * video playback control callback when rendering video frames.  The DecoderCore client must
     * provide one of these.
     */
    public interface SpeedControlCallback {
    	
        /**
         * Called immediately before the frame is rendered.
         * @param presentationTimeUsec The desired presentation time, in microseconds.
         */
        void controlTime(long presentationTimeUsec);

        /**
         * Called after the last frame of a looped movie has been rendered.  This allows the
         * callback to adjust its expectations of the next presentation time stamp.
         */
        void loopReset();
        
        /**
         * To avoid wrong fast play when replaying the video. 
         * Must be called before and after stopping the video
         * it calculates the time for sleeping and update the PTS
         * @param sleepTime time cost on sleeping, needs to be compensated
         */
        void updatePTS(long sleepTime);
    }

    public DecoderCore(File videoFile, Surface outputSurface, SpeedControlCallback callback) {
    	mVideoFile = videoFile;
    	mOutputSurface = outputSurface;
    	mCallback = callback;

    	sampleNumber = 0;
    	
    	try {
	    prepareDecode();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    
    public DecoderCore(AssetFileDescriptor assetFileDescriptor, Surface outputSurface, SpeedControlCallback callback) {
    	mAssetFileDescriptor = assetFileDescriptor;
    	mOutputSurface = outputSurface;
    	mCallback = callback;
    	sampleNumber = 0;
    	
    	try {
	    prepareDecode();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public DecoderCore(File videoFile, Surface outputSurface) {
    	mVideoFile = videoFile;
    	mOutputSurface = outputSurface;
    	sampleNumber = 0;
    	
    	try {
	    prepareDecode();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public void prepareDecode() throws IOException {
    	Log.d(TAG, "preparing decoder");
    	
        //begin
        extractor = new MediaExtractor();
//		extractor.setDataSource(mVideoFile.toString());//load video from external storage
        //load video from res/raw/
	extractor.setDataSource(mAssetFileDescriptor.getFileDescriptor(), 
				mAssetFileDescriptor.getStartOffset(), mAssetFileDescriptor.getLength());
    	//check if video file can be read.
        if (!mVideoFile.canRead()) {
	    Log.w(TAG, "Unable to read " + mVideoFile);
            return;
        }
        //begin
        extractor = new MediaExtractor();
		extractor.setDataSource(mVideoFile.toString());
     
        // Select the first video track we find, ignore the rest.
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
            	extractor.selectTrack(i);//select the track
            	
            	Log.d(TAG, "mime: " + mime);

            	mVideoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
            	mVideoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
            	
            	//create a decoder with MediaCodec
                decoder = MediaCodec.createDecoderByType(mime);
                decoder.configure(format, mOutputSurface, null, 0); //output goes to mOutputSurface
//                    decoder.configure(format, null, null, 0);//output does not go to surface
                Log.d(TAG, "format: " + format);
                break;
            }
        }
        
        if (decoder == null) {
	    Log.w(TAG, "video track not found");
	    return;
        }
        
        decoder.start();
        Log.d(TAG, "decoder prepared...");    			
    }
    
    protected boolean doDecode() {
    	Log.d(TAG, "begin decoding...");
    	sampleNumber = 0;
    	final int TIMEOUT_USEC = 10000;
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();

        boolean outputDone = false;
        boolean inputDone = false;
        long startTime = System.currentTimeMillis();
        while (!outputDone) {
	    if (stopThread) {//stop playing
		Log.d(TAG, "stop thread");
		release();
		return false;
	    }
			
	    //deal with inputBuffer
	    if (!inputDone) {
		//Retrieve the index of an input buffer to be filled with valid data 
            	//or -1 if no such buffer is currently available.
            	int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
            	if (inputBufIndex >= 0) {
		    ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];//retrieve the available buffer to be filled 
            		
		    //Retrieve the current encoded sample 
		    //and store it in the byte buffer starting at the given offset.
                    int sampleSize = extractor.readSampleData(inputBuf, 0);
                    if (sampleSize < 0) {
                    	// End of stream -- send empty frame with EOS flag set.
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
						 MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                    	//submit it to the codec.
                    	long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufIndex, 0, sampleSize,
						 presentationTimeUs, 0 /*flags*/);
       
                        sampleNumber++;//count the input sample number
                    	extractor.advance();//Advance to the next sample.
                    	
                    	Log.i(TAG, "sumitted frame " + sampleNumber + ", size=" + sampleSize + ", pts=" + presentationTimeUs);
                    }
                    
            	} else {
		    Log.i(TAG, "input buffer not available");
            	}
	    }
        	
	    //deal with outputBuffer
	    if (!outputDone) {
		//Dequeue an output buffer, block at most TIMEOUT_USEC microseconds.
		//return the index of an output buffer that has been successfully decoded 
		int outputBufIndex = decoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

		switch(outputBufIndex) {
		case MediaCodec.INFO_TRY_AGAIN_LATER:
		    Log.d(TAG, "no output from decoder available");
		    break;
		case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:	
		    Log.d(TAG, "output buffer changed");
		    break;
		case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
		    Log.d(TAG, "output format changed to " + decoder.getOutputFormat());
		    break;
		default:
		    if (outputBufIndex < 0) {
			Log.d(TAG, "outputBufIndex < 0");
			return false;
		    }
        				
		    //a simple trick to control frame rate: sleep until it matches the PTS of video
		    
		    //        			while (mBufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startTime) {
		    //        				try {
		    //							Thread.sleep(10);
		    //						} catch (InterruptedException e) {
		    //							e.printStackTrace();
		    //						}
		    //        			}
    
		    //time control based on PTS
		    if (mBufferInfo.size != 0 && mCallback != null) {
			mCallback.controlTime(mBufferInfo.presentationTimeUs);
                    }
				
		    while (mBufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startTime) {
			try {
			    Thread.sleep(10);
			} catch (InterruptedException e) {
			    e.printStackTrace();
			}
		    }
	
		    //--------------------outputBufIndex >= 0---------------------------------
		    //send the buffer to the output surface.
		    //surface will return the buffer to the codec
		    //once the buffer is no longer used 
		    decoder.releaseOutputBuffer(outputBufIndex, true);
		    Log.d(TAG, "send buffer to surface, index=" + outputBufIndex);
            		
            		
		    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
			Log.d(TAG, "deocode end --- end of stream");
			/**
			 * stop the thread at the end of the video
			 * TODO: add loop playback if necessary
			 */
			stopThread = true;
			release();
			return false;
		    }
            		
            		//replay whenever true
//            		if (isReplay) {
//                    	extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
//                        inputDone = false;
//                        decoder.flush();    // reset decoder state
//                        mCallback.loopReset(); 
//                        isReplay = false;
//                        Log.i(TAG, "replay");
//                        break;
//                        
//            		}
//            		if (isStop) {
//            			int sleepLoop = 0;
//            			while (!isReplay) {
//                        	try {
//								Thread.sleep(SLEEP_SLICE);
//								sleepLoop++;
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//                        	Log.i(TAG, "stop looping : wait for replay" + sleepLoop);
//                        }       
//            			mCallback.updatePTS(sleepLoop * SLEEP_SLICE);
//            			extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
//                        inputDone = false;   
//                        decoder.flush();    // reset decoder state
//                        mCallback.loopReset(); 
//                        
//                        isStop = false;
//                        isReplay = false;
//                        break;
//            		}
		    
		    break;
		}
	    }


	    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
		Log.d(TAG, "deocode end --- end of stream");
		break;
	    }
 	
        }
        Log.d(TAG, "decoding end");
        return true;
    }
    
    void release() {
    	// release
        if (decoder != null) {
            decoder.stop();
            decoder.release();
            decoder = null;
        }
        if (extractor != null) {
            extractor.release();
            extractor = null;
        }
        Log.d(TAG, "resourses released");
    }
    
    
    //call at the end of decode
    void dumpVideoInfo() {
    	Log.i(TAG, "VideoWidth=" + mVideoWidth + ", VideoHeight=" + mVideoHeight);
    	Log.i(TAG, "Total frame number: " + sampleNumber);
    }

}