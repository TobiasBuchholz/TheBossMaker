package de.pmaclothing.activities;

import java.io.File;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import de.pmaclothing.actionbar.CustomActionBarActivity;
import de.pmaclothing.facedetect.R;
import de.pmaclothing.utils.Constants;
import de.pmaclothing.utils.FileHelper;

public class MainActivity extends CustomActionBarActivity {
    private static final int TAKE_PICTURE_CODE = 100;
    private static final int CHOOSE_PICTURE_CODE = 101;
        
    private ProgressDialog mProgressDialog;
        
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initProgressDialog();
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK) {
			switch (requestCode) {
			case TAKE_PICTURE_CODE:
				launchFaceDetectorActivity(data, true);				
				break;
			case CHOOSE_PICTURE_CODE:
				launchFaceDetectorActivity(data, false);
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	public void onClickTakePictureButton(View view) {
		final String filePath = Constants.PMA_BOSSES_FILE_PATH;
        createDirIfNotExists(filePath);
        launchCamera(filePath);
	}

    private void createDirIfNotExists(final String filePath) {
        if(FileHelper.exists(filePath) != FileHelper.IS_DIRECTORY) {
            new File(filePath).mkdir();
        }
    }

    private void launchCamera(final String filePath) {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(filePath, Constants.ORIGINAL_JPG)));
        startActivityForResult(intent, TAKE_PICTURE_CODE);
    }

    public void onClickChoosePictureButton(View view) {
        final Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(intent, CHOOSE_PICTURE_CODE);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
			launchPmaSite();
                break;
            case R.id.menu_facebook_like:
            	launchFacebook();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onPause() {
    	mProgressDialog.dismiss();
    	super.onPause();
    }

	private void initProgressDialog() {
		mProgressDialog = new ProgressDialog(this);
	    mProgressDialog.setCancelable(false);
	    mProgressDialog.setMessage(getString(R.string.please_wait));
	}

	private void launchFaceDetectorActivity(final Intent intent, final boolean fromCamera){
		Intent faceDetectorIntent = new Intent(MainActivity.this, FaceDetectorActivity.class);
		faceDetectorIntent.putExtra(FaceDetectorActivity.INTENT_EXTRA_FROM_CAMERA, fromCamera);
		if(intent != null) {
			faceDetectorIntent.setData(intent.getData());
		}
		startActivity(faceDetectorIntent);
	}

	private void launchPmaSite() {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PMA_SHOP_URL));
		startActivity(intent);
	}

	private void launchFacebook() {
		mProgressDialog.show();
		new Thread() {
			public void run() {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.FACEBOOK_LIKE_URL));
				startActivity(intent);
			};
		}.start();
	}
}

