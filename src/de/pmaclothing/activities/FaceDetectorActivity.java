package de.pmaclothing.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
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
import de.pmaclothing.view.BossFragmentPagerAdapter;
import de.pmaclothing.view.WrappingSlidingDrawer;

public class FaceDetectorActivity extends CustomActionBarActivity {
    public static final String      INTENT_EXTRA_FROM_CAMERA = "de.pmaclothing.facedetector.fromCamera";

    private static final String     LOG_TAG = FaceDetectorActivity.class.getSimpleName();

	private static final int        REQUEST_CODE_CHOOSE_BACKGROUND = 100;

	private static final int        SEEKBAR_MODE_BRIGHTNESS = 0;
	private static final int        SEEKBAR_MODE_CONTRAST = 1;
	private static final int        SEEKBAR_MODE_ROTATION = 2;
	private static final int        SEEKBAR_MODE_SATURATION = 3;

	private static final int        ITEM_OVERFLOW_CHOOSE_BACKGROUND = 0;
	private static final int        ITEM_OVERFLOW_SAVE = 1;
	private static final int        ITEM_OVERFLOW_SHARE = 2;
	private static final int        ITEM_OVERFLOW_ORDER = 3;

	private View                    mFaceContainerGrip;
	private Button                  mLastSeekBarButton;
	private SeekBar                 mSeekBar;
    private WrappingSlidingDrawer   mSlidingDrawer;
	
	private int                     mSeekBarMode = SEEKBAR_MODE_BRIGHTNESS;
	private int[]                   mSeekBarProgressStates = new int[] {50, 0, 50, 50};

	private boolean                 mOverflowOpen = false;
	private Animation               mAnimationOverflowIn;
	private Animation               mAnimationOverflowOut;

    private ViewPager               mViewPager;
    BossFragmentPagerAdapter        mFragmentPagerAdapter;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_face_detector);
		mLastSeekBarButton = (Button) findViewById(R.id.button_mode_brigthness);
        mLastSeekBarButton.setSelected(true);

        useHardwareAcceleration();

        initViewPager();
        initSlidingDrawer();
		initSeekBar();
		initOverflowListView();
		initAnimations();
	}

    private void initViewPager() {
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mFragmentPagerAdapter = new BossFragmentPagerAdapter(getSupportFragmentManager(), this);
        mFragmentPagerAdapter.setGestureDetector(new GestureDetector(this, new ImageViewGestureListener()));
        mViewPager.setAdapter(mFragmentPagerAdapter);
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == REQUEST_CODE_CHOOSE_BACKGROUND) {
			if(resultCode == RESULT_OK) {
				int backgroundPos = data.getIntExtra(ChooseBackgroundActivity.EXTRA_CHOSEN_BACKGROUND_POS, 0);
				mFragmentPagerAdapter.getCurrentFragment().applyBackgroundChange(backgroundPos);
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
    protected void onDestroy() {
        super.onDestroy();
        FileHelper.deleteFile(Constants.PMA_BOSSES_FILE_PATH + Constants.TEMP_FACE_PNG);
    }

	public void onClickButtonModeBrightness(final View view) {
		toggleButtons((Button) view, SEEKBAR_MODE_BRIGHTNESS);
	}
	
	public void onClickButtonModeContrast(final View view) {
		toggleButtons((Button) view, SEEKBAR_MODE_CONTRAST);
	}
	
	public void onClickButtonModeRotation(final View view) {
		toggleButtons((Button) view, SEEKBAR_MODE_ROTATION);
	}
	
	public void onClickButtonModeSaturation(final View view) {
		toggleButtons((Button) view, SEEKBAR_MODE_SATURATION);
	}
	
    private void useHardwareAcceleration() {
        if (Build.VERSION.SDK_INT > 10) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
    }

    private void toggleButtons(final Button button, final int mode) {
        mSeekBarMode = mode;
        mSeekBar.setProgress(mSeekBarProgressStates[mode]);

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

    private void initSlidingDrawer() {
        mFaceContainerGrip = findViewById(R.id.face_container_grip);
        mSlidingDrawer = (WrappingSlidingDrawer) findViewById(R.id.sliding_drawer);
        mSlidingDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {
            @Override
            public void onDrawerClosed() {
                mFaceContainerGrip.setBackgroundResource(R.drawable.button_face_container_up_selector);
            }
        });
        mSlidingDrawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener() {
            @Override
            public void onDrawerOpened() {
                mFaceContainerGrip.setBackgroundResource(R.drawable.button_face_container_down_selector);
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
                    mFragmentPagerAdapter.getCurrentFragment().rotateFaceImage(degrees);
	    			tempProgress = progress;
	    			mSeekBarProgressStates[mSeekBarMode] = progress;
	    		} else {
	    			mSeekBarProgressStates[mSeekBarMode] = progress;
	    			
	    			final int brightness = (int) (mSeekBarProgressStates[SEEKBAR_MODE_BRIGHTNESS] * 2.55) - 127;
                    final float contrast = mSeekBarProgressStates[SEEKBAR_MODE_CONTRAST] / 10f;
                    final float saturation = mSeekBarProgressStates[SEEKBAR_MODE_SATURATION] / 50f;
					mFragmentPagerAdapter.getCurrentFragment().adjustPixelValues(brightness, contrast, saturation);
	    		}
	    	}
			
			public void onStartTrackingTouch(SeekBar seekBar) {}
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
	}

    private void initOverflowListView() {
		final ListView listView = (ListView) findViewById(R.id.listview_overflow);
		final String[] values = new String[] { getString(R.string.choose_background),
										 getString(R.string.save), 
										 getString(R.string.share),
										 getString(R.string.order) };
	
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
                    final boolean success = mFragmentPagerAdapter.getCurrentFragment().saveBoss();
                    Toast.makeText(FaceDetectorActivity.this, "Saving " + (success ? "was successful" : "failed"), Toast.LENGTH_LONG).show();
                    break;
				case ITEM_OVERFLOW_SHARE: 
					mFragmentPagerAdapter.getCurrentFragment().shareBitmap();
					break;
				case ITEM_OVERFLOW_ORDER:
					startActivity(new Intent(FaceDetectorActivity.this, OrderActivity.class));
					break;
				}
			}
		});
	}

	private void launchChooseBackgroundActivity() {
		Intent intent = new Intent(FaceDetectorActivity.this, ChooseBackgroundActivity.class);
		startActivityForResult(intent, REQUEST_CODE_CHOOSE_BACKGROUND);
	}

}
