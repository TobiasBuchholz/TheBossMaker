package de.pmaclothing.activities;

import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import de.pmaclothing.facedetect.R;
import de.pmaclothing.utils.Constants;
import de.pmaclothing.utils.FileHelper;
import de.pmaclothing.utils.Utils;
import de.pmaclothing.view.BossFragmentPagerAdapter;

public class SplashScreenActivity extends Activity {
	private final static String LOG_TAG = SplashScreenActivity.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash_screen);
		
		prepareBackgroundGridImagesAsync();
        deleteTempFace();
        launchMainActivityDelayed();
    }

    private void prepareBackgroundGridImagesAsync() {
        Utils.executeBackgroundTask(new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                prepareBackgroundGridImages();
                return null;
            }
        });
    }

    private void prepareBackgroundGridImages() {
        final String path = Constants.PMA_BOSSES_FILE_PATH + Constants.FILE_PATH_THUMBS;

        final File thumbsDir = new File(path);
        if(!thumbsDir.isDirectory()) {
            thumbsDir.mkdirs();
        }

        final File[] files = thumbsDir.listFiles();
        if(files != null && files.length < BossFragmentPagerAdapter.mBackgroundIds.length) {
            for(int resId : BossFragmentPagerAdapter.mBackgroundIds) {
                createBackgroundThumbnail(path, resId);
            }
        }
    }

    private void createBackgroundThumbnail(final String path, final int resId) {
        final Options options = new Options();
        options.inSampleSize = 4;
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId, options);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path + resId + Constants.SUFFIX_JPEG);
            bitmap.compress(CompressFormat.JPEG, 100, fos);
            fos.flush();
        } catch (Exception e) {
            Log.e(LOG_TAG, ":: createBackgroundThumbnail ::", e);
        } finally {
            Utils.closeOutputStream(fos);
        }
    }

    private void deleteTempFace() {
        FileHelper.deleteFile(Constants.PMA_BOSSES_FILE_PATH + Constants.TEMP_FACE_PNG);
    }

    private void launchMainActivityDelayed() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                launchMainActivity();
            }
        }, 3000);
    }

    private void launchMainActivity() {
        finish();
        startActivity(new Intent(this, MainActivity.class));
    }
}
