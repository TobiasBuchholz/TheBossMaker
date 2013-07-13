package de.pmaclothing.view;

import android.widget.Button;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 08.07.13 | Time: 21:39
 */
public class CustomFontButton extends Button {
    public CustomFontButton(Context context) {
        super(context);
        setFont();
    }
    public CustomFontButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFont();
    }
    public CustomFontButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFont();
    }

    private void setFont() {
        Typeface font = Typeface.createFromAsset(getContext().getAssets(), "frizquadratabt.ttf");
        setTypeface(font, Typeface.NORMAL);
    }
}