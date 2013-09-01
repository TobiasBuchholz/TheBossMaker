package de.pmaclothing.utils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 01.09.13 | Time: 22:23
 */
public class BitmapSharingTask extends AsyncTask<Bitmap, Void, Intent> {
    private static final String     LOG_TAG = BitmapSharingTask.class.getSimpleName();
    private static final String     MIME_TYPE_IMAGE_JPG     = "image/jpeg";

    private Uri                     mImageUri;
    private Callback                mCallback;

    public BitmapSharingTask(final Callback callback) {
        mCallback = callback;
    }

    @Override
    protected Intent doInBackground(final Bitmap... params) {
        final Bitmap bitmap = params[0];

        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Keep Yourself");
        shareIntent.setType(MIME_TYPE_IMAGE_JPG);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "some text bla bla");


        String imagePath = Constants.PMA_BOSSES_FILE_PATH + Constants.KEEPER_TO_SHARE_PNG;

        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(imagePath));
        } catch (final FileNotFoundException e) {
            imagePath = null;
            Log.e(LOG_TAG, "Error at compressing image to share: " + e.getMessage(), e);
        }

        if (imagePath != null) {
            mImageUri = Uri.parse(Constants.PREFIX_FILE_PATH + imagePath);
        }

        if (mImageUri != null) {
            shareIntent.putExtra(Intent.EXTRA_STREAM, mImageUri);
            mImageUri = null;
        }

        return shareIntent;
    }

    @Override
    protected void onPostExecute(final Intent intent) {
        super.onPostExecute(intent);
        if(mCallback != null) {
            mCallback.onTaskFinish(intent);
        }
    }

    public interface Callback {
        public void onTaskFinish(final Intent shareIntent);
    }
}
