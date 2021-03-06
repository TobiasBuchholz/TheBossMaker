package de.pmaclothing.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory.Options;
import android.media.ExifInterface;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import de.pmaclothing.actionbar.CustomActionBarActivity;
import de.pmaclothing.facedetect.R;
import de.pmaclothing.listeners.ImageViewGestureListener;
import de.pmaclothing.utils.Constants;
import de.pmaclothing.utils.FileHelper;
import de.pmaclothing.view.ImageZoomView;
import de.pmaclothing.view.WrappingSlidingDrawer;

public class FaceDetectorActivity extends CustomActionBarActivity {
    public static final String      INTENT_EXTRA_FROM_CAMERA = "de.pmaclothing.facedetector.fromCamera";

    private static final String     LOG_TAG = FaceDetectorActivity.class.getSimpleName();
	private static final String     MIME_TYPE_IMAGE_JPG = "image/jpeg";
	
	private static final int        REQUEST_CODE_CHOOSE_BACKGROUND = 100;
	
	private static final int        MAX_FACES = 1;
	private static final int        FACE_SPACE = 220;
	
	private static final int        SEEKBAR_MODE_BRIGHTNESS = 0;
	private static final int        SEEKBAR_MODE_CONTRAST = 1;
	private static final int        SEEKBAR_MODE_ROTATION = 2;
	private static final int        SEEKBAR_MODE_SATURATION = 3;

	private static final int        ITEM_OVERFLOW_CHOOSE_BACKGROUND = 0;
	private static final int        ITEM_OVERFLOW_SAVE = 1;
	private static final int        ITEM_OVERFLOW_SHARE = 2;
	private static final int        ITEM_OVERFLOW_ORDER = 3;

	public static final int[]       mBackgroundIds = { R.drawable.sample_background_4, R.drawable.sample_background_2, R.drawable.sample_background_3, R.drawable.screen_test_background, R.drawable.sample_background_4 };

	private View                    mFaceContainerGrip;
    private ImageZoomView           mImageViewFace;
	private ImageView               mImageViewBackground;
	private Bitmap                  mBitmapFace;
	private Bitmap                  mBitmapBackground;
	private Button                  mLastSeekBarButton;
	private ProgressDialog          mProgressDialog;
	private SeekBar                 mSeekBar;
    private WrappingSlidingDrawer   mSlidingDrawer;
	
	private int                     mSeekBarMode = SEEKBAR_MODE_BRIGHTNESS;
	private int                     mCurrentBackgroundPos = 0;
	private int                     mFaceSpace;
	private int[]                   mSeekBarProgressStates = new int[] {50, 0, 50, 50};
	private int[]                   mOriginalPixels;
	
	private boolean                 mOverflowOpen = false;

	private ArrayList<Point>        mFacePositions = new ArrayList<Point>();
	private ArrayList<Point>        mFaceSavingPositions = new ArrayList<Point>();
	
	private Thread                  mBitmapCompressThread;
	
	private Uri                     mImageUri;
	
	private Animation               mAnimationOverflowIn;
	private Animation               mAnimationOverflowOut;

    private DisplayMetrics          mDisplayMetrics;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_face_detector);
		mLastSeekBarButton = (Button) findViewById(R.id.button_mode_brigthness);
        mLastSeekBarButton.setSelected(true);

        useHardwareAcceleration();
        initSlidingDrawer();
        initDisplayDimensions();
		initProgressDialog();
		mProgressDialog.show();
		initSeekBar();
		initImageViews();
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
	    final MenuInflater menuInflater = getMenuInflater();
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
	
    private void useHardwareAcceleration() {
        if (Build.VERSION.SDK_INT > 10) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
    }
	
	private void toggleButtons(final Button button) {
        button.setSelected(true);
        mLastSeekBarButton.setSelected(false);
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
	    mProgressDialog.setCancelable(true);
	    mProgressDialog.setMessage(getString(R.string.please_wait));
	}

	private void initImageViews() {
		mBitmapBackground = BitmapFactory.decodeResource(getResources(), mBackgroundIds[0]);
		
		mImageViewFace = (ImageZoomView) findViewById(R.id.image_view_face);
		mImageViewBackground = (ImageView) findViewById(R.id.image_view_background);
		mImageViewBackground.setImageBitmap(mBitmapBackground);

        final GestureDetector detector = new GestureDetector(this, new ImageViewGestureListener(this));
        final View.OnTouchListener listener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
                return detector.onTouchEvent(event);
			}
		};
		mImageViewBackground.setOnTouchListener(listener);
	}

    private void initSlidingDrawer() {
        mFaceContainerGrip = findViewById(R.id.face_container_grip);
        mSlidingDrawer = (WrappingSlidingDrawer) findViewById(R.id.sliding_drawer);
        mSlidingDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {
            @Override
            public void onDrawerClosed() {
                mFaceContainerGrip.setBackgroundResource(R.drawable.button_face_container_up);
            }
        });
        mSlidingDrawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener() {
            @Override
            public void onDrawerOpened() {
                mFaceContainerGrip.setBackgroundResource(R.drawable.button_face_container_down);
            }
        });
    }

	private void initAnimations() {
		mAnimationOverflowIn = AnimationUtils.loadAnimation(this, R.anim.scale_to_1);
		mAnimationOverflowOut = AnimationUtils.loadAnimation(this, R.anim.scale_to_0);
	}

	private void initSeekBar() {
		mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
		mSeekBar.setProgress(50);
		
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			int tempProgress = 50;
	    	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	    		if(mSeekBarMode == SEEKBAR_MODE_ROTATION) {
                    final int degrees = tempProgress - progress;
	    			mImageViewFace.rotate(degrees, mBitmapFace.getWidth()/2, mBitmapFace.getHeight()/2);
	    			tempProgress = progress;
	    			mSeekBarProgressStates[mSeekBarMode] = progress;
	    		} else {
	    			mSeekBarProgressStates[mSeekBarMode] = progress;
	    			
	    			final int brightness = (int) (mSeekBarProgressStates[SEEKBAR_MODE_BRIGHTNESS] * 2.55) - 127;
                    final float contrast = mSeekBarProgressStates[SEEKBAR_MODE_CONTRAST] / 10f;
                    final float saturation = mSeekBarProgressStates[SEEKBAR_MODE_SATURATION] / 50f;
					adjustPixelValues(brightness, contrast, saturation);
	    		}
	    	}
			
			public void onStartTrackingTouch(SeekBar seekBar) {}
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
	}

	private void initFacePositions() {
		mFacePositions.add(calcFacePosition(-110, -120));
		mFacePositions.add(calcFacePosition(-220, -190));
		mFacePositions.add(calcFacePosition(90, -150));
		mFacePositions.add(calcFacePosition(0, 0));
		
		mFaceSavingPositions.add(new Point(950, 950));
		mFaceSavingPositions.add(new Point(365, 510));
		mFaceSavingPositions.add(new Point(1105, 510));
		mFaceSavingPositions.add(new Point(950, 950));
	}

	private Point calcFacePosition(int distFromCenterX, int distFromCenterY) {
		float scaleFactor = (float) mImageViewBackground.getHeight() / (float) mBitmapBackground.getHeight();
		
		distFromCenterX = (int) (distFromCenterX * scaleFactor * mDisplayMetrics.density);
		distFromCenterY = (int) (distFromCenterY * scaleFactor * mDisplayMetrics.density);
		
		int resultX = mDisplayMetrics.widthPixels / 2 + distFromCenterX;
		int resultY = mImageViewBackground.getHeight() / 2 + distFromCenterY;
		
		return new Point(resultX, resultY);
	}

	private void initOverflowListView() {
		final ListView listView = (ListView) findViewById(R.id.listview_overflow);
		final String[] values = new String[] { getString(R.string.choose_background),
										 getString(R.string.save), 
										 getString(R.string.share)/*,
										 getString(R.string.order)*/ };
	
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item_overflow, R.id.list_item_textview, values);
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
					initFacePositions();
					mImageViewFace.setPosition(mFacePositions.get(mCurrentBackgroundPos).x, mFacePositions.get(mCurrentBackgroundPos).y);
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
            	final int eyeDistance = (int) faces[0].eyesDistance();
                final int cameraBitmapWidth = cameraBitmap.getWidth();
                final int cameraBitmapHeight = cameraBitmap.getHeight();
            	
            	// crop to face
            	int x = (int) (midPoint.x - (eyeDistance * 1.5));
            	int y = (int) (midPoint.y - (eyeDistance * 2.0));
                if(x < 0) x = 0;
                if(y < 0) y = 0;

				int cropWidth = (int) (eyeDistance * 3.5);
				int cropHeight = eyeDistance * 4;
                if(cropWidth + x > cameraBitmapWidth)   cropWidth = cameraBitmapWidth - x;
                if(cropHeight + y > cameraBitmapHeight) cropHeight = cameraBitmapHeight - y;

                float scaleFactor = (float) mImageViewBackground.getHeight() / (float) mBitmapBackground.getHeight();
				mFaceSpace = (int) (scaleFactor * FACE_SPACE * mDisplayMetrics.density);
				
				float faceScaleFactor = (float) mFaceSpace / cropWidth;
				 
				Bitmap faceBitmap;
				Matrix matrix = new Matrix();
				matrix.postScale(faceScaleFactor, faceScaleFactor);
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
		float scaleFactor = FACE_SPACE * mDisplayMetrics.density / (float) mBitmapFace.getWidth();
		
		mImageViewFace.getImageMatrix().getValues(matrixValues);
		matrixValues[Matrix.MTRANS_X] *= scaleFactor;
		matrixValues[Matrix.MTRANS_Y] *= scaleFactor;
		matrixValues[Matrix.MTRANS_X] += (mFaceSavingPositions.get(mCurrentBackgroundPos).x);
		matrixValues[Matrix.MTRANS_Y] += (mFaceSavingPositions.get(mCurrentBackgroundPos).y);
		matrixValues[Matrix.MSCALE_X] = scaleFactor;
		matrixValues[Matrix.MSCALE_Y] = scaleFactor;
		matrix.setValues(matrixValues);
		
		Canvas canvas = new Canvas(mergedBitmap);
		canvas.drawBitmap(mBitmapBackground, 0f, 0f, null);
		canvas.drawBitmap(mBitmapFace, matrix, null);
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
                    mImageUri = Uri.parse(Constants.PREFIX_FILE_PATH + imagePath);
                }
            }
        });
        mBitmapCompressThread.start();
    }
}
