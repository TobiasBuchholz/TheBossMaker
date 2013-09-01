package de.pmaclothing.utils;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 01.09.13 | Time: 12:22
 */
public class BitmapTransformValues {
    public final int   mBrightness;
    public final float mContrast;
    public final float mSaturation;

    public BitmapTransformValues(final int brightness, final float contrast, final float saturation) {
        mBrightness = brightness;
        mContrast = contrast;
        mSaturation = saturation;
    }
}