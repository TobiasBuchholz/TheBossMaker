package de.pmaclothing.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.ImageView;
import android.widget.Toast;
import de.pmaclothing.activities.FaceDetectorActivity;
import de.pmaclothing.facedetect.R;
import de.pmaclothing.interfaces.OnBitmapTaskListener;
import de.pmaclothing.utils.*;
import de.pmaclothing.task.BitmapWorkerTask;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 14.07.13 | Time: 13:37
 */
public class BossFragment extends Fragment {
    private static final String     LOG_TAG                 = BossFragment.class.getSimpleName();

    private static final int        FACE_SPACE              = 220;
    public static final int         PROGRESS_PADDING        = 40;

    private FaceDetectorActivity    mActivity;

    private ImageZoomView           mImageViewFace;
    private ImageView               mImageViewBackground;
    private ImageZoomView           mImageViewProgress;

    private Bitmap                  mBitmapFace;
    private Bitmap                  mBitmapBackground;

    private GestureDetector         mGestureDetector;
    private DisplayMetrics          mDisplayMetrics;
    private ProgressDialog          mProgressDialog;

    private int                     mResourceId;
    private int                     mBackgroundNumber;
    private int                     mProgressPadding;

    /** Position for displaying the image on the screen. The midpoint of the screen represents x = 0 | y = 0. */
    private Point                   mFacePosition;
    /** Position for saving the image. The upper left corner represents x = 0 | y = 0. */
    private Point                   mFaceSavingPosition;
    private BitmapTransformer       mBitmapTransformer;

    public static BossFragment instantiate(final Context context, final String fname, final int backgroundNumber) {
        final BossFragment fragment = (BossFragment) instantiate(context, fname);
        fragment.mResourceId = BossFragmentPagerAdapter.mBackgroundIds[backgroundNumber];
        fragment.mBackgroundNumber = backgroundNumber;
        return fragment;
    }

    public void applyBackgroundChange(int backgroundPos) {
        mBackgroundNumber = backgroundPos;
        mBitmapBackground = BitmapFactory.decodeResource(getResources(), BossFragmentPagerAdapter.mBackgroundIds[mBackgroundNumber]);
        mImageViewBackground.setImageBitmap(mBitmapBackground);
        mImageViewFace.setPosition(mFacePosition);
        mImageViewProgress.setPosition(mFacePosition.x + mProgressPadding, mFacePosition.y + mProgressPadding);
        mImageViewFace.centerImage();
    }

    public void rotateFaceImage(final float degrees) {
        mImageViewFace.rotate(degrees, mBitmapFace.getWidth() / 2, mBitmapFace.getHeight() / 2);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (FaceDetectorActivity) getActivity();
        initDisplayDimensions();
        initProgressDialog();
    }

    private void initDisplayDimensions() {
        mDisplayMetrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        mProgressPadding = (int) (PROGRESS_PADDING * mDisplayMetrics.density);
    }

    private void initProgressDialog() {
        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setMessage(getString(R.string.please_wait));
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)   {
        final View view = inflater.inflate(R.layout.boss_fragment, container, false);
        initImageViews(view);
        return view;
    }

    private void initImageViews(final View view) {
        mBitmapBackground = BitmapFactory.decodeResource(getResources(), mResourceId);
        mImageViewFace = (ImageZoomView) view.findViewById(R.id.image_view_face);
        mImageViewBackground = (ImageView) view.findViewById(R.id.image_view_background);
        mImageViewProgress = (ImageZoomView) view.findViewById(R.id.image_view_progress_circle);
        mImageViewProgress.startRotateAnimation();
        mImageViewBackground.setImageBitmap(mBitmapBackground);

        setupTouchListener();
        setupViewTreeObserver();
    }

    private void setupTouchListener() {
        final View.OnTouchListener listener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        };
        mImageViewBackground.setOnTouchListener(listener);
    }

    private void setupViewTreeObserver() {
        final ViewTreeObserver vto = mImageViewBackground.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                initAndSetFacePosition();
                return true;
            }
        });
    }

    private void initAndSetFacePosition() {
        initFacePosition();
        mImageViewFace.setPosition(mFacePosition);
        mImageViewProgress.setPosition(mFacePosition.x + mProgressPadding, mFacePosition.y + mProgressPadding);
    }

    private void initFacePosition() {
        mFacePosition = calcFaceOnScreenPosition(BossFragmentPagerAdapter.mFacePositions[mBackgroundNumber]);
        mFaceSavingPosition = BossFragmentPagerAdapter.mFaceSavingsPositions[mBackgroundNumber];
    }

    private Point calcFaceOnScreenPosition(final Point distFromCenter) {
        final float displayImageScale = (float) mImageViewBackground.getHeight() / (float) mBitmapBackground.getHeight();
        final int scaledX = (int) (distFromCenter.x * displayImageScale * mDisplayMetrics.density);
        final int scaledY = (int) (distFromCenter.y * displayImageScale * mDisplayMetrics.density);
        final int resultX = mImageViewBackground.getWidth() / 2 + scaledX;
        final int resultY = mImageViewBackground.getHeight() / 2 + scaledY;
        return new Point(resultX, resultY);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(shouldProcessImage()) {
            startFaceDetectorTask();
        }
    }

    private boolean shouldProcessImage() {
        return !tempImageExists() && mBackgroundNumber == 0;
    }

    private void startFaceDetectorTask() {
        final float scaleFactor = (float) mDisplayMetrics.heightPixels / (float) mBitmapBackground.getHeight();
        final int faceSpace = (int) (scaleFactor * FACE_SPACE * mDisplayMetrics.density);
        final Uri imageUri = mActivity.getIntent().getData();

        final FaceDetectorTask faceDetectorTask = new FaceDetectorTask(mActivity.getContentResolver(), faceSpace);
        final OnBitmapTaskListener listener = createOnFaceDetectorTaskListener();
        faceDetectorTask.setOnTaskListener(listener);
        Utils.executeBackgroundTask(faceDetectorTask, imageUri);
    }

    private OnBitmapTaskListener createOnFaceDetectorTaskListener() {
        return new OnBitmapTaskListener() {
            @Override
            public void onTaskFinishSuccess(final Bitmap bitmap) {
                mBitmapFace = bitmap;
                mBitmapTransformer = new BitmapTransformer(mBitmapFace);
                mImageViewFace.setImageBitmap(mBitmapFace);
            }

            @Override
            public void onTaskFinishFail() {
                Toast.makeText(mActivity, R.string.no_faces_found, Toast.LENGTH_LONG).show();
                mActivity.finish();
            }
        };
    }

    private boolean tempImageExists() {
        return FileHelper.exists(Constants.PMA_BOSSES_FILE_PATH + Constants.TEMP_FACE_PNG) == FileHelper.IS_FILE;
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {
            if(mBitmapFace == null && mImageViewFace != null && tempImageExists()) {
                loadFaceBitmapFromDisk();
            }

            if(mBitmapFace != null) {
                updatePixelValues();
            }
        }
    }

    private void showProgressDialog() {
        if(mProgressDialog != null && !mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    private void loadFaceBitmapFromDisk() {
        loadBitmap(Constants.PMA_BOSSES_FILE_PATH + Constants.TEMP_FACE_PNG, mImageViewFace);
    }

    public void loadBitmap(final String imagePath, final ImageView imageView) {
        final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        final AsyncDrawable asyncDrawable = new AsyncDrawable(getResources(), null, task);
        imageView.setImageDrawable(asyncDrawable);
        setListenerToBitmapWorkerTask(task);
        Utils.executeBackgroundTask(task, imagePath);
    }

    private void setListenerToBitmapWorkerTask(final BitmapWorkerTask task) {
        task.setOnBitmapTaskListener(new OnBitmapTaskListener() {
            @Override
            public void onTaskFinishSuccess(final Bitmap bitmap) {
                mImageViewFace.setImageBitmap(bitmap);
                mBitmapFace = bitmap.copy(bitmap.getConfig(), true);
                mBitmapTransformer = new BitmapTransformer(mBitmapFace);
                updatePixelValues();
            }

            @Override
            public void onTaskFinishFail() {}
        });
    }

    private void updatePixelValues() {
        final FaceAdjustmentBar faceAdjustmentBar = mActivity.getFaceAdjustmentBar();
        adjustPixelValues(faceAdjustmentBar);
    }

    public void adjustPixelValues(final FaceAdjustmentBar bar) {
        final int brightness = (int) (bar.getProgressState(FaceAdjustmentBar.MODE_BRIGHTNESS) * 2.55) - 127;
        final float contrast = bar.getProgressState(FaceAdjustmentBar.MODE_CONTRAST) / 10f;
        final float saturation = bar.getProgressState(FaceAdjustmentBar.MODE_SATURATION) / 50f;

        final BitmapTransformValues transformValues = new BitmapTransformValues(brightness, contrast, saturation);
        final Bitmap bitmap = mBitmapTransformer.getTransformedBitmap(mBitmapFace, transformValues);
        mImageViewFace.setImageBitmap(bitmap);
    }

    public void setGestureDetector(final GestureDetector detector) {
        mGestureDetector = detector;
    }

    public boolean saveBoss() {
        final Bitmap bitmap = mergeBitmaps();
        return FileHelper.saveBitmap(bitmap, Constants.PREFIX_BOSS + System.currentTimeMillis() + Constants.SUFFIX_JPEG);
    }

    private Bitmap mergeBitmaps() {
        Bitmap mergedBitmap = Bitmap.createBitmap(mBitmapBackground.getWidth(), mBitmapBackground.getHeight(), Bitmap.Config.ARGB_8888);
        Matrix matrix = new Matrix();
        float[] matrixValues = new float[9];
        float scaleFactor = FACE_SPACE * mDisplayMetrics.density / (float) mBitmapFace.getWidth();

        mImageViewFace.getImageMatrix().getValues(matrixValues);
        matrixValues[Matrix.MTRANS_X] *= scaleFactor;
        matrixValues[Matrix.MTRANS_Y] *= scaleFactor;
        matrixValues[Matrix.MTRANS_X] += (mFaceSavingPosition.x);
        matrixValues[Matrix.MTRANS_Y] += (mFaceSavingPosition.y);
        matrixValues[Matrix.MSCALE_X] = scaleFactor;
        matrixValues[Matrix.MSCALE_Y] = scaleFactor;
        matrix.setValues(matrixValues);

        Canvas canvas = new Canvas(mergedBitmap);
        canvas.drawBitmap(mBitmapBackground, 0f, 0f, null);
        canvas.drawBitmap(mBitmapFace, matrix, null);
        return mergedBitmap;
    }

    public void shareBitmap() {
        showProgressDialog();
        Utils.executeBackgroundTask(createBitmapSharingTask(), mergeBitmaps());
    }

    private BitmapSharingTask createBitmapSharingTask() {
        return new BitmapSharingTask(new BitmapSharingTask.Callback() {
                @Override
                public void onTaskFinish(final Intent shareIntent) {
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser_text)));
                    mProgressDialog.dismiss();
                }
            });
    }
}