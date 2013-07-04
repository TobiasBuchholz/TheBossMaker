package de.pmaclothing.listeners;

import de.pmaclothing.activities.FaceDetectorActivity;
import android.app.Activity;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class ImageViewGestureListener extends SimpleOnGestureListener {
	private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	
    private Activity mActivity;
    
    public ImageViewGestureListener(Activity activity) {
    	mActivity = activity;
    }
    
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		if(mActivity instanceof FaceDetectorActivity) {
            // TODO
//			((FaceDetectorActivity) mActivity).toggleAdjustFaceContainer();
		}
		return false;
	}
	
	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}
}