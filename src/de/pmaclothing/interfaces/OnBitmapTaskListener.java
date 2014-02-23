package de.pmaclothing.interfaces;

import android.graphics.Bitmap;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 01.09.13 | Time: 13:28
 */
public interface OnBitmapTaskListener {
    public void onTaskFinishSuccess(final Bitmap bitmap);
    public void onTaskFinishFail();
}
