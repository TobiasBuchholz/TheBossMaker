package de.pmaclothing.utils;

import android.graphics.Bitmap;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 01.09.13 | Time: 11:58
 */
public class BitmapTransformer {
    private int[]  mOriginalPixels;

    public BitmapTransformer(final Bitmap bitmap) {
        initOriginalPixels(bitmap);
    }

    private void initOriginalPixels(final Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        mOriginalPixels = new int[width * height];
        bitmap.getPixels(mOriginalPixels, 0, width, 0, 0, width, height);
    }

    public Bitmap getTransformedBitmap(final Bitmap srcBitmap, final BitmapTransformValues transformValues) {
        final int width = srcBitmap.getWidth();
        final int height = srcBitmap.getHeight();
        int[] pixels = new int[width * height];
        for(int pos = 0; pos < pixels.length; pos++) {
            int argb = mOriginalPixels[pos];

            int r = (argb >> 16) & 0xff;
            int g = (argb >>  8) & 0xff;
            int b =  argb        & 0xff;

            // transformation to Y-Cb-Cr
            int ylum = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            int cb   = (int) (-0.168736 * r - 0.331264 * g + 0.5 * b);
            int cr   = (int) (0.5 * r - 0.418688 * g - 0.081312 * b);

            // adjust brightness
            ylum += transformValues.mBrightness;

            // adjust contrast
            if (transformValues.mContrast > 1) {
                ylum = (int) ((transformValues.mContrast) * (ylum - 127) + 127);
            }

            // adjust saturation
            cb = (int) (cb * transformValues.mSaturation );
            cr = (int) (cr * transformValues.mSaturation );

            // transformation back to RGB
            r = (int) (ylum + 1.402 * cr);
            g = (int) (ylum - 0.3441 * cb - 0.7141 * cr);
            b = (int) (ylum + 1.772 * cb);

            if(r > 255) 	r = 255;
            if(g > 255) 	g = 255;
            if(b > 255)		b = 255;
            if(r < 0)		r = 0;
            if(g < 0)		g = 0;
            if(b < 0)		b = 0;

            argb = (0xFF<<24) | (r<<16) | (g<<8) | b;
            pixels[pos] = argb;
        }
        srcBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return srcBitmap;
    }
}
