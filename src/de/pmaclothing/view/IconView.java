package de.pmaclothing.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import de.pmaclothing.facedetect.R;

/**
 * User: Mirko Haeberlin
 * Date: 25.04.13 | Time: 11:44
 */
public class IconView extends TextView {
    public static final String FONT_PMX = "pmx-client.ttf";

    private static Typeface     mFont;

    /**
     * Returns the Typeface from the given context with the given name typeface
     * @param context Context to get the assets from
     * @param typeface name of the ttf file
     * @return Typeface from the given context with the given name
     */
    public static Typeface getTypeface(Context context, String typeface) {
        if (mFont == null) {
            mFont = Typeface.createFromAsset(context.getAssets(), typeface);
        }
        return mFont;
    }

    public static Typeface getCurrentTypeface(Context context) {
        if (mFont == null) {
            mFont = Typeface.createFromAsset(context.getAssets(), FONT_PMX);
        }
        return mFont;
    }

    public IconView(Context context) {
        super(context);
        setTypeface(IconView.getTypeface(context, FONT_PMX));
    }

    public IconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTypeface(IconView.getTypeface(context, FONT_PMX));
        setText(getIconString(context, attrs));
    }

    private String getIconString(Context context, AttributeSet attrs){
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IconView, 0, 0);
        try{
            return a.getString(R.styleable.IconView_icon);
        } finally {
            a.recycle();
        }
    }

    public void setIcon(final String icon){
        setText(icon);
    }

    public void setIcon(final int iconResId){
        setText(getContext().getString(iconResId));
    }

    public void startRotateAnimation() {
        final Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_animation);
        startAnimation(animation);
    }

    public void stopRotateAnimation() {
        clearAnimation();
    }
}
