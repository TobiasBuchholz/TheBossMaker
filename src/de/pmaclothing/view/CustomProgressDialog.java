package de.pmaclothing.view;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import de.pmaclothing.facedetect.R;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 02.03.14 | Time: 14:38
 */
public class CustomProgressDialog extends Dialog {
    public CustomProgressDialog(final Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_progress_dialog);
        setCanceledOnTouchOutside(true);
        setCancelable(true);
    }

    @Override
    public void show() {
        startAnimation();
        super.show();
    }

    @Override
    public void dismiss() {
        findViewById(R.id.custom_progress_dialog_image).clearAnimation();
        super.dismiss();
    }

    private void startAnimation() {
        final Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_animation);
        findViewById(R.id.custom_progress_dialog_image).startAnimation(animation);
    }

    public void setMessage(final String message) {
        final TextView text = (TextView) findViewById(R.id.custom_progress_dialog_text);
        text.setText(message);
    }
}
