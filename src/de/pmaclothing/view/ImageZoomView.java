package de.pmaclothing.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;
import de.pmaclothing.facedetect.R;

/**
 * @see "http://code.google.com/p/android-image-process"
 */
public class ImageZoomView extends ImageView {
    
    private static final float     MAX_SCALE         = 25;
    private static final float     MIN_SCALE         = 0.7f;
    
    private GestureDetector        mDetector;
    
    private final Paint            mPaint;
    private Mode                   mMode             = Mode.DRAG;
    private float                  mStartX           = -1f;
    private float                  mStartY           = -1f;
    private float                  mScaleMax         = MAX_SCALE;
    private float                  mScaleMin         = MIN_SCALE;
    
    private final float[]          mMatrixValues     = new float[9];
    private final Matrix           mMatrix           = new Matrix();
    private float                  mScale            = mScaleMin;
    private Rect                   mImageBounds;
    
    private float                  mTranslateX       = Float.MAX_VALUE;
    private float                  mTranslateY       = Float.MAX_VALUE;
    private float                  mOldDist;
    private final PointF           mMidPoint         = new PointF();

    public enum Mode {
        ZOOM, DRAG, NONE
    }
    
    public ImageZoomView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        initGestureDetector();
    }
    
    private void initGestureDetector() {
        mDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener());
    	mDetector.setOnDoubleTapListener(new OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(final MotionEvent e) {
                Log.d("DEBUG", ":: onSingleTapConfirmed :: x:" + e.getX() + " y:" + e.getY());
                return false;
            }
            
            @Override
            public boolean onDoubleTapEvent(final MotionEvent e) {
                Toast.makeText(getContext(), "::onDoubleTapEvent::", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            @Override
            public boolean onDoubleTap(final MotionEvent e) {
                Toast.makeText(getContext(), "::onDoubleTap::", Toast.LENGTH_SHORT).show();
                mMatrix.getValues(mMatrixValues);
                if (mMatrixValues[Matrix.MSCALE_X] != mScaleMin) {
                    mMatrixValues[Matrix.MTRANS_X] = (getWidth() / 2) - ((mImageBounds.right * mScaleMin) / 2);
                    mMatrixValues[Matrix.MTRANS_Y] = (getHeight() / 2) - ((mImageBounds.bottom * mScaleMin) / 2);
                    mMatrixValues[Matrix.MSCALE_X] = mScaleMin;
                    mMatrixValues[Matrix.MSCALE_Y] = mScaleMin;
                    mScale = mScaleMin;
                } else {
                    mMatrixValues[Matrix.MTRANS_X] = (getWidth() / 2) - ((mImageBounds.right * mScaleMax) / 2);
                    mMatrixValues[Matrix.MTRANS_Y] = (getHeight() / 2) - ((mImageBounds.bottom * mScaleMax) / 2);
                    mMatrixValues[Matrix.MSCALE_X] = mScaleMax;
                    mMatrixValues[Matrix.MSCALE_Y] = mScaleMax;
                    mScale = mScaleMax;
                }
                mMatrix.setValues(mMatrixValues);
                centerImage();
                return false;
            }
        });
    }
    
    @Override
    public void setImageBitmap(final Bitmap bitmap) {
        float[] tempValues = new float[mMatrixValues.length];
        mMatrix.getValues(tempValues);

        super.setImageBitmap(bitmap);
        mMatrix.setValues(tempValues);
        setImageMatrix(mMatrix);
    }
    
    @Override
    public void setImageDrawable(final Drawable drawable) {
        if (drawable != null) {
            mImageBounds = drawable.getBounds();
        }
        super.setImageDrawable(drawable);
    }

    public void setImageBitmapWithTransition(final Bitmap bitmap) {
        final BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
        final Drawable placeHolderDrawable = getResources().getDrawable(R.drawable.shit);
        final TransitionDrawable td = new TransitionDrawable(new Drawable[] {placeHolderDrawable, bitmapDrawable});
        setImageDrawable(td);
        td.startTransition(500);
    }
    
    public void setPosition(final int x, final int y) {
    	LayoutParams params = new RelativeLayout.LayoutParams(getLayoutParams());
		params.setMargins(x, y, 0, 0);
		setLayoutParams(params);
    }

    public void setPosition(final Point position) {
        setPosition(position.x, position.y);
    }

    public void rotate(final float degrees, final int pivotX, final int pivotY) {
        mMatrix.getValues(mMatrixValues);
        mMatrix.preRotate(degrees, pivotX, pivotY);
        setImageMatrix(mMatrix);
    }
    
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        
        final int actionCode = event.getAction() & MotionEvent.ACTION_MASK;
        if (mMode == Mode.NONE) {
            if (mDetector.onTouchEvent(event)) {
                return true;
            }
        } 
        switch (actionCode) {
            case MotionEvent.ACTION_DOWN:
                checkMatrixMaxValues();
                mStartX = event.getX();
                mStartY = event.getY();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mMode = Mode.ZOOM;
                mOldDist = spacing(event);
                if (mOldDist > 10f) {
                    midPoint(mMidPoint, event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mMode.equals(Mode.DRAG)) {
                    handleDrag(event);
                } else if (mMode.equals(Mode.ZOOM)) {
                	handleZoom(event);
                }
                setImageMatrix(mMatrix);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mMode = Mode.DRAG;
                // change the start values according to the remaining pointer
                final int index = event.getActionIndex();
                if (index > -1 && index < event.getPointerCount()) {
                    if (index == 1) {
                        mStartX = event.getX(0);
                        mStartY = event.getY(0);
                    } else {
                        mStartX = event.getX(1);
                        mStartY = event.getY(1);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                setImageMatrix(mMatrix);
                mMode = Mode.DRAG;
                break;
        }
        return true;
    }
    
    private void handleDrag(final MotionEvent event) {
        final float diffX = event.getX() - mStartX;
        final float diffY = event.getY() - mStartY;
        mStartX = event.getX();
        mStartY = event.getY();
        
        mMatrix.getValues(mMatrixValues);
        mTranslateX = mMatrixValues[Matrix.MTRANS_X] + diffX;
        mTranslateY = mMatrixValues[Matrix.MTRANS_Y] + diffY;
        mMatrixValues[Matrix.MTRANS_X] = mTranslateX;
        mMatrixValues[Matrix.MTRANS_Y] = mTranslateY;
        mMatrix.setValues(mMatrixValues);
    }
    
    private void handleZoom(final MotionEvent event) {
        final float newDist = spacing(event);
        if (newDist > 10f) {
            final float scale = newDist / mOldDist;
            mOldDist = newDist;
            
            final Matrix tmpMatrix = new Matrix(mMatrix);
            tmpMatrix.postScale(scale, scale, mMidPoint.x, mMidPoint.y);
            final float[] tmpValues = new float[9];
            tmpMatrix.getValues(tmpValues);
            
            if (tmpValues[Matrix.MSCALE_X] <= mScaleMax && tmpValues[Matrix.MSCALE_X] >= mScaleMin) {
                mMatrix.postScale(scale, scale, mMidPoint.x, mMidPoint.y);
                mMatrix.getValues(mMatrixValues);
            }
        }
    }
    
    public void centerImage() {
        mMatrix.getValues(mMatrixValues);
        ensureZoomLevel();
        
        final boolean isImageTooHeight = mImageBounds.bottom * mScale > getHeight();
        final boolean isImageTooWidth = mImageBounds.right * mScale > getWidth();
        final boolean isTopInside = mMatrixValues[Matrix.MTRANS_Y] >= 0;
        final boolean isBottomInside = mMatrixValues[Matrix.MTRANS_Y] + (mImageBounds.bottom * mScale) <= getHeight();
        final boolean isLeftInside = mMatrixValues[Matrix.MTRANS_X] >= 0;
        final boolean isRightInside = mMatrixValues[Matrix.MTRANS_X] + (mImageBounds.right * mScale) <= getWidth();
        
        if (!isImageTooHeight) { // center vertical
            mMatrixValues[Matrix.MTRANS_Y] = (getHeight() - (mImageBounds.bottom * mScale)) / 2;
        }
        if (!isImageTooWidth) { // center horizontal
            mMatrixValues[Matrix.MTRANS_X] = (getWidth() - (mImageBounds.right * mScale)) / 2;
        }
        
        if (isImageTooHeight && isTopInside) {
            mMatrixValues[Matrix.MTRANS_Y] = 0;
        } else if (isImageTooHeight && isBottomInside) {
            mMatrixValues[Matrix.MTRANS_Y] = getHeight() - (mImageBounds.bottom * mScale);
        }
        
        if (isImageTooWidth && isLeftInside) {
            mMatrixValues[Matrix.MTRANS_X] = 0;
        } else if (isImageTooWidth && isRightInside) {
            mMatrixValues[Matrix.MTRANS_X] = getWidth() - (mImageBounds.right * mScale);
        }
        
        mMatrix.setValues(mMatrixValues);
        setImageMatrix(mMatrix);
    }
    
    private void ensureZoomLevel() {
        // ensure max zoom out to view width/height
        if (mMatrixValues[Matrix.MSCALE_X] > mScaleMax) {
            final Matrix tmpMatrix = new Matrix();
            final float[] tmpValues = new float[9];
            tmpMatrix.setValues(mMatrixValues);
            tmpMatrix
                    .postScale(mScaleMax / mMatrixValues[Matrix.MSCALE_X], mScaleMax / mMatrixValues[Matrix.MSCALE_X], mMidPoint.x, mMidPoint.y);
            tmpMatrix.getValues(tmpValues);
            mMatrixValues[Matrix.MTRANS_X] = tmpValues[Matrix.MTRANS_X];
            mMatrixValues[Matrix.MTRANS_Y] = tmpValues[Matrix.MTRANS_Y];
            
            mMatrixValues[Matrix.MSCALE_X] = mScaleMax;
            mMatrixValues[Matrix.MSCALE_Y] = mScaleMax;
        }
        // ensure max in zoom to MAX_ZOOM_LEVEL_IN
        if (mMatrixValues[Matrix.MSCALE_X] < mScaleMin) {
            mMatrixValues[Matrix.MSCALE_X] = mScaleMin;
            mMatrixValues[Matrix.MSCALE_Y] = mScaleMin;
        }
        mScale = mMatrixValues[Matrix.MSCALE_X];
    }
    
    private void checkMatrixMaxValues() {
        if (mTranslateX == Float.MAX_VALUE || mTranslateY == Float.MAX_VALUE) {
            mTranslateY = (getHeight() / 2) - ((mImageBounds.bottom * mScale) / 2);
            mTranslateX = (getWidth() / 2) - ((mImageBounds.right * mScale) / 2);
            mMatrix.getValues(mMatrixValues);
            mMatrixValues[Matrix.MTRANS_X] = mTranslateX;
            mMatrixValues[Matrix.MTRANS_Y] = mTranslateY;
        }
    }
    
    private float spacing(final MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            final float x = event.getX(0) - event.getX(1);
            final float y = event.getY(0) - event.getY(1);
            return FloatMath.sqrt(x * x + y * y);
        }
        return mOldDist;
        
    }
    
    private void midPoint(final PointF point, final MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            final float x = event.getX(0) + event.getX(1);
            final float y = event.getY(0) + event.getY(1);
            point.set(x / 2, y / 2);
        }
    }

    public void startRotateAnimation() {
        final Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_animation);
        startAnimation(animation);
    }
}
