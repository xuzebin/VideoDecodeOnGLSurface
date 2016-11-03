package com.example.videodecodeonglsurface;

import android.util.Log;
import com.example.videodecodeonglsurface.DecoderCore.SpeedControlCallback;

public class SpeedController implements SpeedControlCallback {
    private static final String TAG = "SpeedController";
    private static final long ONE_MILLION = 1000000L;

    private long mPrevPresentUsec;
    private long mPrevMonoUsec;
    private boolean mLoopReset;
    

    //runs on decoder thread
    @Override public void controlTime(long presentationTimeUsec) {
	// TODO Auto-generated method stub
	if (mPrevMonoUsec == 0) {
            // Latch current values, then return immediately.
            mPrevMonoUsec = System.nanoTime() / 1000;
            mPrevPresentUsec = presentationTimeUsec;
        } else {
            // Compute the desired time delta between the previous frame and this frame.
            long frameDelta;
            if (mLoopReset) {
                mPrevPresentUsec = presentationTimeUsec - ONE_MILLION / 30;
                mLoopReset = false;
            }
            
            frameDelta = presentationTimeUsec - mPrevPresentUsec;//time delta
            if (frameDelta < 0) {
                Log.w(TAG, "Weird, video times went backward");
                frameDelta = 0;
            } else if (frameDelta == 0) {
                // This suggests a possible bug in movie generation.
                Log.i(TAG, "Warning: current frame and previous frame had same timestamp");
            } else if (frameDelta > 10 * ONE_MILLION) {
                Log.i(TAG, "Inter-frame pause was " + (frameDelta / ONE_MILLION) +
		      "sec, capping at 5 sec");
                frameDelta = 5 * ONE_MILLION;
            }

            long desiredUsec = mPrevMonoUsec + frameDelta;  // when we want to wake up
            long nowUsec = System.nanoTime() / 1000;
            while (nowUsec < (desiredUsec - 100) /*&& mState == RUNNING*/) {
                long sleepTimeUsec = desiredUsec - nowUsec;
                if (sleepTimeUsec > 500000) {
                    sleepTimeUsec = 500000;
                }
                try {
                    long startNsec = System.nanoTime();
                    
                    Thread.sleep(sleepTimeUsec / 1000, (int) (sleepTimeUsec % 1000) * 1000);
        
                    long actualSleepNsec = System.nanoTime() - startNsec;
                    Log.d(TAG, "sleep=" + sleepTimeUsec + " actual=" + (actualSleepNsec/1000) +
			  " diff=" + (Math.abs(actualSleepNsec / 1000 - sleepTimeUsec)) +
			  " (usec)");
  
                } catch (InterruptedException ie) {}
                nowUsec = System.nanoTime() / 1000;
            }

            // Advance times using calculated time values, not the post-sleep monotonic
            // clock time, to avoid drifting.
            mPrevMonoUsec += frameDelta;
            mPrevPresentUsec += frameDelta;
	}
    }

    @Override public void loopReset() {
	// TODO Auto-generated method stub
	mLoopReset = true;
    }

    @Override public void updatePTS(long sleepTime) {
    }
}
