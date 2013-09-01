package de.pmaclothing.view;

import java.lang.ref.WeakReference;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import de.pmaclothing.task.BitmapWorkerTask;

/**
 * Drawable which gets loaded in the background and shows a placeholder.
 * 
 * @author tobiasbuchholz
 */
public class AsyncDrawable extends BitmapDrawable {
    private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;
    
    /**
     * @param res The resource.
     * @param placeHolderBitmap The bitmap to show before the real one is downloaded.
     * @param bitmapWorkerTask The task to load the real bitmap.
     */
    public AsyncDrawable(final Resources res, final Bitmap placeHolderBitmap, final BitmapWorkerTask bitmapWorkerTask) {
        super(res, placeHolderBitmap);
        bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
    }
    
    /**
     * @return the refering BitmapWorkerTask of the drawable.
     */
    public BitmapWorkerTask getBitmapWorkerTask() {
        return bitmapWorkerTaskReference.get();
    }
}
