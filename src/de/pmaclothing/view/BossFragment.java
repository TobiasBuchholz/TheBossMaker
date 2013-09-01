package de.pmaclothing.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.Toast;
import de.pmaclothing.activities.FaceDetectorActivity;
import de.pmaclothing.facedetect.R;
import de.pmaclothing.interfaces.FaceDetectorTaskListener;
import de.pmaclothing.utils.*;
import de.pmaclothing.task.BitmapWorkerTask;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 14.07.13 | Time: 13:37
 */
public class BossFragment extends Fragment {
    private static final String     LOG_TAG                 = BossFragment.class.getSimpleName();

    private static final String     MIME_TYPE_IMAGE_JPG     = "image/jpeg";
    private static final int        FACE_SPACE              = 220;

    private Activity                mActivity;

    private ImageZoomView           mImageViewFace;
    private ImageView               mImageViewBackground;

    private Bitmap                  mBitmapFace;
    private Bitmap                  mBitmapBackground;

    private GestureDetector         mGestureDetector;
    private DisplayMetrics          mDisplayMetrics;
    private ProgressDialog          mProgressDialog;
    private Thread                  mBitmapCompressThread;

    private Uri                     mImageUri;

    private int                     mResourceId;
    private int                     mBackgroundNumber;

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

    public void rotateFaceImage(final float degrees) {
        mImageViewFace.rotate(degrees, mBitmapFace.getWidth()/2, mBitmapFace.getHeight()/2);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        initDisplayDimensions();
        initProgressDialog();
    }

    private void initDisplayDimensions() {
        mDisplayMetrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
    }

    private void initProgressDialog() {
        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setCancelable(false);
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
        mImageViewBackground.setImageBitmap(mBitmapBackground);

        final View.OnTouchListener listener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        };
        mImageViewBackground.setOnTouchListener(listener);
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

    private void loadFaceBitmapFromDisk() {
        final Bitmap savedFaceBitmap = BitmapFactory.decodeFile(Constants.PMA_BOSSES_FILE_PATH + Constants.TEMP_FACE_PNG);
        mBitmapFace = savedFaceBitmap.copy(savedFaceBitmap.getConfig(), true);

        if(mBitmapFace != null) {
            mBitmapTransformer = new BitmapTransformer(mBitmapFace);
            mImageViewFace.setImageBitmapWithTransition(mBitmapFace);
            initAndSetFacePosition();
        }
    }

    private void updatePixelValues() {
        final FaceAdjustmentBar faceAdjustmentBar = ((FaceDetectorActivity) mActivity).getFaceAdjustemntBar();
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

    private void initAndSetFacePosition() {
        initFacePosition();
        mImageViewFace.setPosition(mFacePosition);
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

    public void setGestureDetector(final GestureDetector detector) {
        mGestureDetector = detector;
    }

    public void loadBitmap(final String imagePath, final ImageView imageView) {
        final BitmapWorkerTask task = new BitmapWorkerTask(getActivity(), imageView);
        final Bitmap placeHolderBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.app_logo_48);
        final AsyncDrawable asyncDrawable = new AsyncDrawable(getResources(), placeHolderBitmap, task);
        imageView.setImageDrawable(asyncDrawable);
        task.execute(imagePath);
    }

    private void startFaceDetectorTask() {
        mProgressDialog.show();
        float scaleFactor = (float) mDisplayMetrics.heightPixels / (float) mBitmapBackground.getHeight();
        final int faceSpace = (int) (scaleFactor * FACE_SPACE * mDisplayMetrics.density);
        final Uri imageUri = mActivity.getIntent().getData();

        final FaceDetectorTask faceDetectorTask = new FaceDetectorTask(mActivity.getContentResolver(), faceSpace);
        final FaceDetectorTaskListener listener = createOnFaceDetectorTaskListener();
        faceDetectorTask.setOnTaskListener(listener);
        Utils.executeBackgroundTask(faceDetectorTask, imageUri);
    }

    private FaceDetectorTaskListener createOnFaceDetectorTaskListener() {
        return new FaceDetectorTaskListener() {
            @Override
            public void onTaskFinishSuccess(final Bitmap bitmap) {
                mProgressDialog.dismiss();
                mBitmapFace = bitmap;
                mBitmapTransformer = new BitmapTransformer(mBitmapFace);
                mImageViewFace.setImageBitmapWithTransition(mBitmapFace);
                initAndSetFacePosition();
            }

            @Override
            public void onTaskFinishFail() {
                mProgressDialog.dismiss();
                Toast.makeText(mActivity, R.string.no_faces_found, Toast.LENGTH_LONG).show();
                mActivity.finish();
            }
            @Override
            public void onTaskError() {}
        };
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
        if(mImageUri == null) {
            mProgressDialog.show();
            compressCurrentBoss();
        }
        startActivity(Intent.createChooser(createShareIntent(), getString(R.string.share_chooser_text)));
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

    public void applyBackgroundChange(int backgroundPos) {
        mBackgroundNumber = backgroundPos;
        mBitmapBackground = BitmapFactory.decodeResource(getResources(), BossFragmentPagerAdapter.mBackgroundIds[mBackgroundNumber]);
        mImageViewBackground.setImageBitmap(mBitmapBackground);
        mImageViewFace.setPosition(mFaceSavingPosition);
        mImageViewFace.centerImage();
    }
}