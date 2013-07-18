package de.pmaclothing.actionbar;

import android.app.ActionBar;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import de.pmaclothing.facedetect.R;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 09.07.13 | Time: 09:42
 */
public class CustomActionBarActivity extends ActionBarActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT > 10) {
            final ActionBar actionBar = this.getActionBar();
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);

            LayoutInflater inflator = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflator.inflate(R.layout.action_bar, null);

            //if you need to customize anything else about the text, do it here.
            //I'm using a custom TextView with a custom font in my layout xml so all I need to do is set title
            ((TextView)v.findViewById(R.id.title)).setText(this.getTitle());

            //assign the view to the actionbar
            actionBar.setCustomView(v);
        }
    }
}
