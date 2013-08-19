package de.pmaclothing.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 16.08.13 | Time: 13:17
 */
public class FaceAdjustmentBar extends SeekBar {
    public static final int MODE_BRIGHTNESS = 0;
    public static final int MODE_CONTRAST   = 1;
    public static final int MODE_ROTATION   = 2;
    public static final int MODE_SATURATION = 3;

    private int[]           mProgressStates = new int[] {50, 0, 50, 50};
    private int             mMode           = MODE_BRIGHTNESS;

    public FaceAdjustmentBar(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public int getProgressState(final int mode) {
        return mProgressStates[mode];
    }

    public void setProgressState(final int progress) {
        mProgressStates[mMode] = progress;
    }

    public void setProgressMode(final int mode) {
        mMode = mode;
    }

    public void setProgress() {
        setProgress(mProgressStates[mMode]);
    }

    public boolean isMode(final int mode) {
        return mMode == mode;
    }
}
