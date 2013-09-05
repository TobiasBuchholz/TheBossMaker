package de.pmaclothing.task;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import de.pmaclothing.facedetect.R;
import de.pmaclothing.interfaces.OnBitmapTaskListener;
import de.pmaclothing.view.AsyncDrawable;
import de.pmaclothing.view.ImageZoomView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
    private static final String            LOG_TAG        = BitmapWorkerTask.class.getSimpleName();
    
    public static int                      IN_SIZE_NONE   = -1;
    public static int                      IN_SIZE_LOW    = 4;
    public static int                      IN_SIZE_MEDIUM = 2;
    public static int                      IN_SIZE_FULL   = 1;
    

    private final WeakReference<ImageView> mImageViewReference;
    private Context                        mContext;
    private OnBitmapTaskListener           mListener;

    public BitmapWorkerTask(final Context context, final ImageView imageView) {
        mContext = context;
        mImageViewReference = new WeakReference<ImageView>(imageView);
    }

    public void setOnBitmapTaskListener(final OnBitmapTaskListener listener) {
        mListener = listener;
    }

    @Override
    protected Bitmap doInBackground(final String... params) {
        Bitmap bitmap = null;
        try {
            bitmap = decodeSampledBitmapFromDisk(params[0]);
        } catch (final FileNotFoundException e) {
            Log.e(LOG_TAG, ":: doInBackground ::" + e.getMessage(), e);
        } catch (final OutOfMemoryError e) {
            Log.e(LOG_TAG, ":: doInBackground ::" + e.getMessage(), e);
        }

        return bitmap;
    }

    private Bitmap decodeSampledBitmapFromDisk(final String imagePath) throws FileNotFoundException {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.RGB_565;
        final Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            bitmap.recycle();
            bitmap = null;
        }

        if (mImageViewReference != null && bitmap != null) {
            final ImageView imageView = mImageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
            if (this == bitmapWorkerTask && imageView != null) {
                if(mListener != null) {
                    mListener.onTaskFinishSuccess(bitmap);
                }
            }
        }
    }

    public static BitmapWorkerTask getBitmapWorkerTask(final ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

//    public static boolean cancelPotentialWork(final String data, final ImageView imageView) {
//        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
//
//        if (bitmapWorkerTask != null) {
//            final String bitmapData = bitmapWorkerTask.mImagePath;
//            if (bitmapData != data) {
//                bitmapWorkerTask.cancel(true);
//            } else {
//                return false;
//            }
//        }
//        return true;
//    }
}
