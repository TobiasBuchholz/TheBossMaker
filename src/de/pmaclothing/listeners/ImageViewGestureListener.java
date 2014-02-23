package de.pmaclothing.listeners;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class ImageViewGestureListener extends SimpleOnGestureListener {
    public ImageViewGestureListener() {
    }

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return false;
	}
	
	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
        return true;
    }
}