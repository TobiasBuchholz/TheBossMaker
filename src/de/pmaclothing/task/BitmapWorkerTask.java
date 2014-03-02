package de.pmaclothing.task;

import android.content.Context;
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

public class BitmapWorkerTask<Param> extends AsyncTask<Param, Void, Bitmap> {
    private static final String       LOG_TAG        = BitmapWorkerTask.class.getSimpleName();

    private Context                   mContext;
    private WeakReference<ImageView>  mImageViewReference;
    private OnBitmapTaskListener      mListener;
    private String                    mImagePath;
    private int                       mImageResId;

    public BitmapWorkerTask(final Context context, final ImageView imageView) {
        mContext = context;
        mImageViewReference = new WeakReference<ImageView>(imageView);
    }

    public void setOnBitmapTaskListener(final OnBitmapTaskListener listener) {
        mListener = listener;
    }

    @Override
    protected Bitmap doInBackground(final Param... params) {
        Bitmap bitmap = null;
        try {
            bitmap = handleDecodeBitmap(bitmap, params);
        } catch (final FileNotFoundException e) {
            Log.e(LOG_TAG, ":: doInBackground ::" + e.getMessage(), e);
        } catch (final OutOfMemoryError e) {
            Log.e(LOG_TAG, ":: doInBackground ::" + e.getMessage(), e);
        }

        return bitmap;
    }

    private Bitmap handleDecodeBitmap(Bitmap bitmap, final Param[] params) throws FileNotFoundException {
        if(params instanceof String[]) {
            mImagePath = (String) params[0];
            bitmap = decodeSampledBitmapFromDisk();
        } else if(params instanceof Integer[]) {
            mImageResId = (Integer) params[0];
            bitmap = decodeBitmapFromResource();
        }
        return bitmap;
    }

    private Bitmap decodeSampledBitmapFromDisk() throws FileNotFoundException {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.RGB_565;
        return BitmapFactory.decodeFile(mImagePath, options);
    }

    private Bitmap decodeBitmapFromResource() {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.RGB_565;
        return BitmapFactory.decodeResource(mContext.getResources(), mImageResId, options);
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
                invokeListener(bitmap);
            }
        }
    }

    private void invokeListener(final Bitmap bitmap) {
        if(mListener != null) {
            mListener.onTaskFinishSuccess(bitmap);
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
}
