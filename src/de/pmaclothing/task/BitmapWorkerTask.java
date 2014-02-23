package de.pmaclothing.task;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import de.pmaclothing.interfaces.OnBitmapTaskListener;
import de.pmaclothing.view.AsyncDrawable;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
    private static final String            LOG_TAG        = BitmapWorkerTask.class.getSimpleName();
    
    private final WeakReference<ImageView>  mImageViewReference;
    private OnBitmapTaskListener            mListener;
    private String                          mImagePath;

    public BitmapWorkerTask(final ImageView imageView) {
        mImageViewReference = new WeakReference<ImageView>(imageView);
    }

    public void setOnBitmapTaskListener(final OnBitmapTaskListener listener) {
        mListener = listener;
    }

    @Override
    protected Bitmap doInBackground(final String... params) {
        Bitmap bitmap = null;
        try {
            mImagePath = params[0];
            bitmap = decodeSampledBitmapFromDisk();
        } catch (final FileNotFoundException e) {
            Log.e(LOG_TAG, ":: doInBackground ::" + e.getMessage(), e);
        } catch (final OutOfMemoryError e) {
            Log.e(LOG_TAG, ":: doInBackground ::" + e.getMessage(), e);
        }

        return bitmap;
    }

    private Bitmap decodeSampledBitmapFromDisk() throws FileNotFoundException {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.RGB_565;
        return BitmapFactory.decodeFile(mImagePath, options);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            bitmap.recycle();
            bitmap = null;
        }

        if (bitmap != null) {
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

    public static boolean cancelPotentialWork(final String data, final ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.mImagePath;
            if (bitmapData != null && !bitmapData.equals(data)) {
                bitmapWorkerTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }
}
