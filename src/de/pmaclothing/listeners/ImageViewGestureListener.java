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
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    	Log.d("DEBUG", ":: onFling :: ");
        if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
            return false;
        // right to left swipe
        if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
        	if(mActivity instanceof FaceDetectorActivity) {
        		Toast.makeText(mActivity, "swipe left", Toast.LENGTH_SHORT).show();
        		((FaceDetectorActivity) mActivity).animateBackground(true);
        	}
        } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
        	if(mActivity instanceof FaceDetectorActivity) {
        		Toast.makeText(mActivity, "swipe right", Toast.LENGTH_SHORT).show();
        		((FaceDetectorActivity) mActivity).animateBackground(false);
        	}
        }
        return false;
    }

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		if(mActivity instanceof FaceDetectorActivity) {
			((FaceDetectorActivity) mActivity).toggleAdjustFaceContainer();
		}
		return false;
	}
	
	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}
}