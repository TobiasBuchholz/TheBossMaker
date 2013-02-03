package de.pmaclothing.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;
import de.pmaclothing.actionbar.ActionBarActivity;
import de.pmaclothing.facedetect.R;
import de.pmaclothing.listeners.ImageViewGestureListener;
import de.pmaclothing.utils.Constants;
import de.pmaclothing.utils.FileHelper;
import de.pmaclothing.view.ImageZoomView;

public class FaceDetectorActivity extends ActionBarActivity {
	public static final String INTENT_EXTRA_FROM_CAMERA = "de.pmaclothing.facedetector.fromCamera";
	
	private static final String LOG_TAG = FaceDetectorActivity.class.getSimpleName();
	private static final String MIME_TYPE_IMAGE_JPG = "image/jpeg";
	
	private static final int REQUEST_CODE_CHOOSE_BACKGROUND = 100;
	
	private static final int MAX_FACES = 1;
	private static final int FACE_SPACE = 430;
	
	private static final int SEEKBAR_MODE_BRIGHTNESS = 0;
	private static final int SEEKBAR_MODE_CONTRAST = 1;
	private static final int SEEKBAR_MODE_ROTATION = 2;
	private static final int SEEKBAR_MODE_SATURATION = 3;

	private static final int ITEM_OVERFLOW_CHOOSE_BACKGROUND = 0;
	private static final int ITEM_OVERFLOW_SAVE = 1;
	private static final int ITEM_OVERFLOW_SHARE = 2;
	private static final int ITEM_OVERFLOW_ORDER = 3;

	
	public static final int[] mBackgroundIds = { R.drawable.sample_background_1, R.drawable.sample_background_2, R.drawable.sample_background_3};

	private ImageZoomView mImageViewFace;
	private ImageView mImageViewBackground;
	private ImageView mImageViewBackgroundLeft;
	private ImageView mImageViewBackgroundRight;
	private Bitmap mBitmapFace;
	private Bitmap mBitmapBackground;
	private Button mLastSeekBarButton;
	private ProgressDialog mProgressDialog;
	private SeekBar mSeekBar;
	
	private int mSeekBarMode = SEEKBAR_MODE_BRIGHTNESS;
	private int mCurrentBackgroundPos = 0;
	private int mFaceSpace;
	private int[] mSeekBarProgressStates = new int[] {50, 0, 50, 50};
	private int[] mOriginalPixels;
	
	private boolean mOverflowOpen = false;
	private boolean mAdjustFaceContainerIn = true;

	private ArrayList<Point> mFacePositions = new ArrayList<Point>();
	private ArrayList<Point> mFaceSavingPositions = new ArrayList<Point>();
	
	private Thread mBitmapCompressThread;
	
	private Uri mImageUri;
	
	private Animation mAnimationOverflowIn;
	private Animation mAnimationOverflowOut;
	private Animation mAnimationSlideIn;
	private Animation mAnimationSlideOut;

	private DisplayMetrics mDisplayMetrics;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_face_detector);
		setActionBarHomeIcon(R.drawable.ic_home_back);
		mLastSeekBarButton = (Button) findViewById(R.id.button_mode_brigthness);
		
		initDisplayDimensions(); 
		initProgressDialog();
		mProgressDialog.show();
		initSeekBar();
		initImageViews();
		initFacePositions();
		initOverflowListView();
		initAnimations();
		
		startImageProcessing();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == REQUEST_CODE_CHOOSE_BACKGROUND) {
			if(resultCode == RESULT_OK) {
				int backgroundPos = data.getIntExtra(ChooseBackgroundActivity.EXTRA_CHOSEN_BACKGROUND_POS, 0);
				applyBackgroundChange(backgroundPos);
				toggleOverflow();
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater menuInflater = getMenuInflater();
	    menuInflater.inflate(R.menu.facedetector, menu);
	    
	    return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            setResult(RESULT_OK);
	            finish();
	            break;
	        case R.id.menu_overflow:
			toggleOverflow();
	            break;
	    }
	    return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onKeyDown(int keycode, KeyEvent e) {
	    switch(keycode) {
	        case KeyEvent.KEYCODE_MENU:
	            toggleAdjustFaceContainer();
	            return true;
	    }
	    return super.onKeyDown(keycode, e);
	}
	
	public void onClickButtonModeBrightness(final View view) {
		mSeekBarMode = SEEKBAR_MODE_BRIGHTNESS;
		mSeekBar.setProgress(mSeekBarProgressStates[SEEKBAR_MODE_BRIGHTNESS]);
		toggleButtons((Button) view);
	}
	
	public void onClickButtonModeContrast(final View view) {
		mSeekBarMode = SEEKBAR_MODE_CONTRAST;
		mSeekBar.setProgress(mSeekBarProgressStates[SEEKBAR_MODE_CONTRAST]);
		toggleButtons((Button) view);
	}
	
	public void onClickButtonModeRotation(final View view) {
		mSeekBarMode = SEEKBAR_MODE_ROTATION;
		mSeekBar.setProgress(mSeekBarProgressStates[SEEKBAR_MODE_ROTATION]);
		toggleButtons((Button) view);
	}
	
	public void onClickButtonModeSaturation(final View view) {
		mSeekBarMode = SEEKBAR_MODE_SATURATION;
		mSeekBar.setProgress(mSeekBarProgressStates[SEEKBAR_MODE_SATURATION]);
		toggleButtons((Button) view);
	}
	
	public void onClickButtonToggleFaceContainer(final View view) {
		toggleAdjustFaceContainer();
	}
	
	public void animateBackground(final boolean swipeLeft) {
		if(swipeLeft) {
			mImageViewBackgroundRight.startAnimation(AnimationUtils.loadAnimation(this, R.anim.background_image_right_in));
//			mImageViewBackground.startAnimation(AnimationUtils.loadAnimation(this, R.anim.background_image_left_out));
		} else {
			mImageViewBackgroundLeft.startAnimation(AnimationUtils.loadAnimation(this, R.anim.background_image_left_in));
//			mImageViewBackground.startAnimation(AnimationUtils.loadAnimation(this, R.anim.background_image_right_out));
		}
	}
	
	public void toggleAdjustFaceContainer() {
		final View container = findViewById(R.id.adjust_face_container);
		final View button = findViewById(R.id.button_toggle_face_container);
		
		mAnimationSlideIn.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			@Override
			public void onAnimationEnd(Animation animation) {
				container.setVisibility(View.VISIBLE);
				button.setBackgroundResource(R.drawable.adjust_face_container_down);
			}
		});
		
		mAnimationSlideOut.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			@Override
			public void onAnimationEnd(Animation animation) {
				container.setVisibility(View.GONE);
				button.setBackgroundResource(R.drawable.adjust_face_container_up);
			}
		});
		
		if(mAdjustFaceContainerIn) {
			container.startAnimation(mAnimationSlideOut);
			button.startAnimation(mAnimationSlideOut);
			mAdjustFaceContainerIn = false;
		} else {
			container.startAnimation(mAnimationSlideIn);
			button.startAnimation(mAnimationSlideIn);
			mAdjustFaceContainerIn = true;
			container.setVisibility(View.VISIBLE);
		}
	}

	private void toggleButtons(final Button button) {
		button.setBackgroundColor(Color.DKGRAY);
		mLastSeekBarButton.setBackgroundColor(Color.GRAY);
		mLastSeekBarButton = button;
	}
	
	private void toggleOverflow() {
		View view = findViewById(R.id.listview_overflow_container);
		if(mOverflowOpen) {
			view.startAnimation(mAnimationOverflowOut);
			view.setVisibility(View.INVISIBLE);
			mOverflowOpen = false;
		} else {
			view.setVisibility(View.VISIBLE);
			view.startAnimation(mAnimationOverflowIn);
			mOverflowOpen = true;
		}
	}

	private void initDisplayDimensions() {
		 mDisplayMetrics = new DisplayMetrics();
		 getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
	}

	private void initProgressDialog() {
		mProgressDialog = new ProgressDialog(FaceDetectorActivity.this);
	    mProgressDialog.setCancelable(false);
	    mProgressDialog.setMessage(getString(R.string.please_wait));
	}

	private void initImageViews() {
		mBitmapBackground = BitmapFactory.decodeResource(getResources(), R.drawable.sample_background_1);
		
		mImageViewFace = (ImageZoomView) findViewById(R.id.image_view_face);
		mImageViewBackground = (ImageView) findViewById(R.id.image_view_background);
//		mImageViewBackgroundLeft = (ImageView) findViewById(R.id.image_view_background_left);
//		mImageViewBackgroundRight = (ImageView) findViewById(R.id.image_view_background_right);
	
		final GestureDetector detector = new GestureDetector(this, new ImageViewGestureListener(this));
		View.OnTouchListener listener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return detector.onTouchEvent(event);
			}
		};
		mImageViewBackground.setOnTouchListener(listener);
	}

	private void initAnimations() {
		mAnimationOverflowIn = AnimationUtils.loadAnimation(this, R.anim.scale_to_1);
		mAnimationOverflowOut = AnimationUtils.loadAnimation(this, R.anim.scale_to_0);
		mAnimationSlideIn = new TranslateAnimation(0, 0, 145, 0);
		mAnimationSlideOut = new TranslateAnimation(0, 0, 0, 145);
		mAnimationSlideIn.setDuration(200);
		mAnimationSlideOut.setDuration(200);
	}

	private void initSeekBar() {
		mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
		mSeekBar.setProgress(50);
		
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			int tempProgress = 50;
	    	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	    		if(mSeekBarMode == SEEKBAR_MODE_ROTATION) {
	    			int degrees = tempProgress - progress;
	    			mImageViewFace.rotate(degrees, mBitmapFace.getWidth()/2, mBitmapFace.getHeight()/2);
	    			tempProgress = progress;
	    			mSeekBarProgressStates[mSeekBarMode] = progress;
	    		} else {
	    			mSeekBarProgressStates[mSeekBarMode] = progress;
	    			
	    			int brightness = (int) (mSeekBarProgressStates[SEEKBAR_MODE_BRIGHTNESS] * 2.55) - 127;
					float contrast = mSeekBarProgressStates[SEEKBAR_MODE_CONTRAST] / 10f; 
					float saturation = mSeekBarProgressStates[SEEKBAR_MODE_SATURATION] / 50f;
					adjustPixelValues(brightness, contrast, saturation);
	    		}
	    	}
			
			public void onStartTrackingTouch(SeekBar seekBar) {}
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
	}

	private void initFacePositions() {
		float scaleX = (float) mDisplayMetrics.widthPixels / (float) (mBitmapBackground.getWidth()/2);
		mFaceSpace = (int) (scaleX * FACE_SPACE);
		
		mFacePositions.add(calcFacePosition(0, 0));
		mFacePositions.add(calcFacePosition(-225, 0));
		mFacePositions.add(calcFacePosition(270, -10));
		
		mFaceSavingPositions.add(new Point(400, 350));
		mFaceSavingPositions.add(new Point(400, 450));
		mFaceSavingPositions.add(new Point(250, 250));
	}

	private Point calcFacePosition(int distFromCenterX, int distFromCenterY) {
		float scaleFactor = (float) mDisplayMetrics.heightPixels / (float) mBitmapBackground.getHeight();
		distFromCenterX = (int) (distFromCenterX * scaleFactor * mDisplayMetrics.density);
		distFromCenterY = (int) (distFromCenterY * scaleFactor * mDisplayMetrics.density);
		
		int resultX = mDisplayMetrics.widthPixels /2 + distFromCenterX - mFaceSpace/2;
		int resultY = mDisplayMetrics.heightPixels /2 + distFromCenterY - mFaceSpace;
		
		return new Point(resultX, resultY);
	}

	private void initOverflowListView() {
		ListView listView = (ListView) findViewById(R.id.listview_overflow);
		String[] values = new String[] { getString(R.string.choose_background), 
										 getString(R.string.save), 
										 getString(R.string.share),
										 getString(R.string.order) };
	
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item_overflow, R.id.list_item_textview, values);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				switch (position) {
				case ITEM_OVERFLOW_CHOOSE_BACKGROUND: 
					launchChooseBackgroundActivity();
					break;
				case ITEM_OVERFLOW_SAVE: 
					Bitmap mergedBitmap = mergeBitmaps();
					saveBitmap(mergedBitmap);
					break;
				case ITEM_OVERFLOW_SHARE: 
					shareBitmap();
					break;
				case ITEM_OVERFLOW_ORDER:
					compressCurrentBoss();
					startActivity(new Intent(FaceDetectorActivity.this, OrderActivity.class));
					break;
				}
			}
		});
	}

	private void startImageProcessing() {
		new AsyncTask<Void, Void, Boolean>() {
			Bitmap cameraBitmap;
			@Override
			protected Boolean doInBackground(Void... params) {
				boolean fromCamera = getIntent().getBooleanExtra(INTENT_EXTRA_FROM_CAMERA, false);
				
				if(fromCamera) {
					cameraBitmap = getImageFromCamera();
				} else {
					Options options = new Options();
					options.inSampleSize = 2;
					 cameraBitmap = getImageFromGalleryIntent();
				}
				mBitmapFace = findFaceAndCrop(cameraBitmap);
				
				if(mBitmapFace == null) {
					return false;
				}				
				determineOriginalPixels();
				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if(result) {
					mImageViewFace.setImageBitmap(mBitmapFace);
					mImageViewFace.setPosition(mFacePositions.get(mCurrentBackgroundPos).x, mFacePositions.get(mCurrentBackgroundPos).y);
					mImageViewBackground.setImageBitmap(mBitmapBackground);
					mProgressDialog.dismiss();
				} else {
					Toast.makeText(FaceDetectorActivity.this, R.string.no_faces_found, Toast.LENGTH_LONG).show();
					FaceDetectorActivity.this.finish();
				}
				FileHelper.deleteFile(Constants.PMA_BOSSES_FILE_PATH + Constants.ORIGINAL_JPG);
			}
		}.execute();
	}
	
	private void determineOriginalPixels() {
		int width = mBitmapFace.getWidth();
		int height = mBitmapFace.getHeight();
		mOriginalPixels = new int[width * height];
		mBitmapFace.getPixels(mOriginalPixels, 0, width, 0, 0, width, height);
	}
	
	private Bitmap getImageFromCamera() {
		Bitmap cameraBitmap;
		String imagePath = Constants.PMA_BOSSES_FILE_PATH + Constants.ORIGINAL_JPG;
		int orientation = getImageOrientation(imagePath);

		// wenns hier knallt, dann das hier mal probieren: http://stackoverflow.com/questions/9668430/decode-a-bitmap-rotated
		Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
		cameraBitmap = BitmapFactory.decodeFile(imagePath);
		cameraBitmap = Bitmap.createBitmap(cameraBitmap, 0, 0, cameraBitmap.getWidth(), cameraBitmap.getHeight(), matrix, false);
		return cameraBitmap;
	}

	private Bitmap getImageFromGalleryIntent() {
		Bitmap cameraBitmap;
		Uri imageUri = getIntent().getData();
		String filePath = getImagePath(imageUri);
		int orientation = getImageOrientation(imageUri);
		
		// wenns hier knallt, dann das hier mal probieren: http://stackoverflow.com/questions/9668430/decode-a-bitmap-rotated
		Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
		cameraBitmap = BitmapFactory.decodeFile(filePath, options);
		cameraBitmap = Bitmap.createBitmap(cameraBitmap, 0, 0, cameraBitmap.getWidth(), cameraBitmap.getHeight(), matrix, false);
		return cameraBitmap;
	}

	private String getImagePath(Uri imageUri) {
		String filePath = "";
		String[] filePathColumn = {MediaStore.Images.Media.DATA};
		Cursor cursor = getContentResolver().query(imageUri, filePathColumn, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			 filePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
		}
		cursor.close();
		return filePath;
	}

	private int getImageOrientation(Uri imageUri) {
		String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};

		Cursor cursor = getContentResolver().query(imageUri, orientationColumn, null, null, null);
		int orientation = -1;
		if (cursor != null && cursor.moveToFirst()) {
			orientation = cursor.getInt(cursor.getColumnIndex(orientationColumn[0]));
		} 
		cursor.close();
		return orientation;
	}
	
	/** if this doesn't work, check: http://stackoverflow.com/questions/8450539/images-taken-with-action-image-capture-always-returns-1-for-exifinterface-tag-or/8864367#8864367*/
	private int getImageOrientation(String imagePath) {
		int orientation = 0;
		try {
			ExifInterface exif = new ExifInterface(imagePath);
			int orientationAttribute = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
			
			switch (orientationAttribute) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				orientation = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				orientation = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				orientation = 270;
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return orientation;
	}

	
	private Bitmap findFaceAndCrop(Bitmap cameraBitmap){
        if(null != cameraBitmap){
            int width = cameraBitmap.getWidth();
			int height = cameraBitmap.getHeight();
			
			FaceDetector detector = new FaceDetector(width, height, MAX_FACES);
            Face[] faces = new Face[MAX_FACES];
            int facesFound = detector.findFaces(cameraBitmap, faces);
            
            if(facesFound > 0) {
            	PointF midPoint = new PointF();
            	faces[0].getMidPoint(midPoint);
            	int eyeDistance = (int) faces[0].eyesDistance();
            	
            	// crop to face
            	int x = (int) (midPoint.x - (eyeDistance * 1.5));
            	int y = (int) (midPoint.y - (eyeDistance * 2.0));
				int cropWidth = (int) (eyeDistance * 3.5);
				int cropHeight = (int) (eyeDistance * 4);
				float scaleFactor = (float) mFaceSpace / cropWidth;
				 
				Bitmap faceBitmap;
				Matrix matrix = new Matrix();
				matrix.postScale(scaleFactor, scaleFactor);
				faceBitmap = Bitmap.createBitmap(cameraBitmap, x > 0 ? x : 0, y > 0 ? y : 0, cropWidth, cropHeight, matrix, false);
            	return faceBitmap;
            } 
        }
        return null;
    }

	private void adjustPixelValues(int brightness, float contrast, float saturation) {
		int width = mBitmapFace.getWidth();
		int height = mBitmapFace.getHeight();

		int[] pixels = new int[width * height];
		for(int pos=0; pos<pixels.length; pos++) {
			int argb = mOriginalPixels[pos];
			
			int r = (argb >> 16) & 0xff;
			int g = (argb >>  8) & 0xff;
			int b =  argb        & 0xff;
			
			// transformation to Y-Cb-Cr
			int ylum = (int) (0.299 * r + 0.587 * g + 0.114 * b);
			int cb   = (int) (-0.168736 * r - 0.331264 * g + 0.5 * b);
			int cr   = (int) (0.5 * r - 0.418688 * g - 0.081312 * b);

			// adjust brightness
			ylum += brightness;
			
			// adjust contrast
			if (contrast > 1) {
				ylum = (int) ((contrast) * (ylum - 127) + 127);
			}
			
			// adjust saturation
			cb = (int) (cb * saturation );
			cr = (int) (cr * saturation );
			
			// transformation back to RGB
			r = (int) (ylum + 1.402 * cr);
			g = (int) (ylum - 0.3441*cb - 0.7141*cr);
			b = (int) (ylum + 1.772*cb);
			
			if(r > 255) 	r = 255;
			if(g > 255) 	g = 255;
			if(b > 255)		b = 255;
			if(r < 0)		r = 0;
			if(g < 0)		g = 0;
			if(b < 0)		b = 0;
			
			argb = (0xFF<<24) | (r<<16) | (g<<8) | b;
			pixels[pos] = argb;
		}
		mBitmapFace.setPixels(pixels, 0, width, 0, 0, width, height);
		mImageViewFace.setImageBitmap(mBitmapFace);
	}
	
	private void applyBackgroundChange(int backgroundPos) {
		mCurrentBackgroundPos = backgroundPos;
		mBitmapBackground = BitmapFactory.decodeResource(getResources(), mBackgroundIds[mCurrentBackgroundPos]);
		mImageViewBackground.setImageBitmap(mBitmapBackground);
		mImageViewFace.setPosition(mFacePositions.get(mCurrentBackgroundPos).x, mFacePositions.get(mCurrentBackgroundPos).y);
		mImageViewFace.centerImage();
	}

	private Bitmap mergeBitmaps() {
		Bitmap mergedBitmap = Bitmap.createBitmap(mBitmapBackground.getWidth(), mBitmapBackground.getHeight(), Bitmap.Config.ARGB_8888);
		Matrix matrix = new Matrix();
		float[] matrixValues = new float[9];
	
		mImageViewFace.getImageMatrix().getValues(matrixValues);
		matrixValues[2] += mFaceSavingPositions.get(mCurrentBackgroundPos).x;
		matrixValues[5] += mFaceSavingPositions.get(mCurrentBackgroundPos).y;
		matrix.setValues(matrixValues);
		
		Canvas canvas = new Canvas(mergedBitmap);
		canvas.drawBitmap(mBitmapFace, matrix, null);
		canvas.drawBitmap(mBitmapBackground, 0f, 0f, null);
		return mergedBitmap;
	}

	private void saveBitmap(final Bitmap bitmap) {
		String filepath = Constants.PMA_BOSSES_FILE_PATH;
		if(FileHelper.exists(filepath) != FileHelper.IS_DIRECTORY) {
			new File(filepath).mkdir();
		}
		filepath += (Constants.PREFIX_BOSS + System.currentTimeMillis() + Constants.SUFFIX_PNG);
		
		try {
			FileOutputStream fos = new FileOutputStream(filepath);
			bitmap.compress(CompressFormat.PNG, 100, fos);
			fos.flush();
			fos.close();
			Toast.makeText(this, "Saving successfull", Toast.LENGTH_LONG).show();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void shareBitmap() {
		if(mImageUri == null) {
			mProgressDialog.show();
			compressCurrentBoss();
		}
		startActivity(Intent
		        .createChooser(createShareIntent(), getString(R.string.share_chooser_text)));
	}
	
	private void launchChooseBackgroundActivity() {
		Intent intent = new Intent(FaceDetectorActivity.this, ChooseBackgroundActivity.class);
		startActivityForResult(intent, REQUEST_CODE_CHOOSE_BACKGROUND);
	}

	private Intent createShareIntent() {
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Keep Yourself");
        shareIntent.setType(MIME_TYPE_IMAGE_JPG);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "some text bla bla");
        try {
            mBitmapCompressThread.join();
            if (mImageUri != null) {
                shareIntent.putExtra(Intent.EXTRA_STREAM, mImageUri);
                mImageUri = null;
            }
            mProgressDialog.dismiss();
        } catch (final InterruptedException e) {
            Log.e(LOG_TAG, "Error at joining the bitmap compressing thread: " + e.getMessage());
        }
        return shareIntent;
    }
	
	private void compressCurrentBoss() {
        mBitmapCompressThread = new Thread(new Runnable() {
			@Override
            public void run() {
				String imagePath = Constants.PMA_BOSSES_FILE_PATH + Constants.KEEPER_TO_SHARE_PNG;

				Bitmap bitmap = mergeBitmaps();
                try {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(imagePath));
                } catch (final FileNotFoundException e) {
                    imagePath = null;
                    Log.e(LOG_TAG, "Error at compressing image to share: " + e.getMessage(), e);
                }
                if (imagePath != null) {
                    mImageUri = Uri.parse(Constants.FILE_PATH + imagePath);
                }
            }
        });
        mBitmapCompressThread.start();
    }
}
