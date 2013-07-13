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

public class SplashScreenActivity extends Activity {
	private final static String LOG_TAG = SplashScreenActivity.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash_screen);
		
		prepareBackgroundGridImages();
		
		new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
            	SplashScreenActivity.this.finish();
            	SplashScreenActivity.this.startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
            }
        }, 3000);
	}

	private void prepareBackgroundGridImages() {
		new AsyncTask<Void, Void, Void> (){

			@Override
			protected Void doInBackground(Void... params) {
				final String path = Constants.PMA_BOSSES_FILE_PATH + Constants.FILE_PATH_THUMBS;
				
				final File thumbsDir = new File(path);
				if(!thumbsDir.isDirectory()) {
					thumbsDir.mkdirs();
				}
				
				final File[] files = thumbsDir.listFiles();
				if(files != null && files.length < FaceDetectorActivity.mBackgroundIds.length) {
					for(int resId : FaceDetectorActivity.mBackgroundIds) {
						final Options options = new Options();
				        options.inSampleSize = 4;
				        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId, options);
						
						try {
							final FileOutputStream fos = new FileOutputStream(path + resId + Constants.SUFFIX_JPEG);
							bitmap.compress(CompressFormat.JPEG, 100, fos);
							fos.flush();
							fos.close();
						} catch (Exception e) {
							Log.e(LOG_TAG, e.getMessage() + e);
						}
					}
				}
				return null;
			}
		}.execute();
	}
}
