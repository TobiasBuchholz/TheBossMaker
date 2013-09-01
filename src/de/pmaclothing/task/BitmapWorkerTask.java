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
import de.pmaclothing.view.AsyncDrawable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

public class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
    private static final String            LOG_TAG        = BitmapWorkerTask.class.getSimpleName();
    
    public static int                      IN_SIZE_NONE   = -1;
    public static int                      IN_SIZE_LOW    = 4;
    public static int                      IN_SIZE_MEDIUM = 2;
    public static int                      IN_SIZE_FULL   = 1;
    

    private final WeakReference<ImageView> mImageViewReference;
    private boolean                        mFade;
    private Context                        mContext;

    public BitmapWorkerTask(final Context context, final ImageView imageView) {
        mContext = context;
        mImageViewReference = new WeakReference<ImageView>(imageView);
    }
    
    @Override
    protected Bitmap doInBackground(final Integer... params) {
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

    private Bitmap decodeSampledBitmapFromDisk(final int resId) throws FileNotFoundException {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.RGB_565;
        final Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), resId);
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
                setImageBitmap(imageView, bitmap);
            }
        }
    }

    public void enableTransition() {
        mFade = true;
    }

    public void disableTransition() {
        mFade = false;
    }

    private void setImageBitmap(final ImageView imageView, final Bitmap bitmap) {
        if (mFade) {
            final BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap);
            final TransitionDrawable td = new TransitionDrawable(new Drawable[] { new ColorDrawable(android.R.color.transparent), bitmapDrawable});
            imageView.setImageDrawable(td);
            td.startTransition(200);
        } else {
            imageView.setImageBitmap(bitmap);
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
