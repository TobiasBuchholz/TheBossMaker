package de.pmaclothing.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.*;
import android.media.ExifInterface;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.Toast;
import de.pmaclothing.activities.FaceDetectorActivity;
import de.pmaclothing.facedetect.R;
import de.pmaclothing.utils.BitmapTransformValues;
import de.pmaclothing.utils.BitmapTransformer;
import de.pmaclothing.utils.Constants;
import de.pmaclothing.utils.FileHelper;
import de.pmaclothing.task.BitmapWorkerTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 14.07.13 | Time: 13:37
 */
public class BossFragment extends Fragment {
    private static final String     LOG_TAG                 = BossFragment.class.getSimpleName();

    private static final String     MIME_TYPE_IMAGE_JPG     = "image/jpeg";
    private static final int        MAX_FACES               = 1;
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

    private int                     mFaceSpace;
    private int                     mResourceId;
    private int                     mBackgroundNumber;
    private int[]                   mOriginalPixels;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        initDisplayDimensions();
        initProgressDialog();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)   {
        final View view = inflater.inflate(R.layout.boss_fragment, container, false);
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initImageViews(view);

        if(shouldProcessImage()) {
            startImageProcessing();
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

    public void loadBitmap(final String imagePath, final ImageView imageView) {
        final BitmapWorkerTask task = new BitmapWorkerTask(getActivity(), imageView);
        final Bitmap placeHolderBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.app_logo_48);
        final AsyncDrawable asyncDrawable = new AsyncDrawable(getResources(), placeHolderBitmap, task);
        imageView.setImageDrawable(asyncDrawable);
        task.execute(imagePath);
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

    private void startImageProcessing() {
        mProgressDialog.show();
        new AsyncTask<Void, Void, Boolean>() {
            Bitmap cameraBitmap;
            @Override
            protected Boolean doInBackground(Void... params) {
                boolean fromCamera = mActivity.getIntent().getBooleanExtra(FaceDetectorActivity.INTENT_EXTRA_FROM_CAMERA, false);

                if(fromCamera) {
                    cameraBitmap = getImageFromCamera();
                } else {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    cameraBitmap = getImageFromGalleryIntent();
                }
                mBitmapFace = findFaceAndCrop(cameraBitmap);

                if(mBitmapFace == null) {
                    return false;
                }

                saveBitmap(mBitmapFace, Constants.TEMP_FACE_PNG);
                return true;
            }

            @Override
            protected void onPostExecute(final Boolean result) {
                mProgressDialog.dismiss();
                if(result) {
                    mBitmapTransformer = new BitmapTransformer(mBitmapFace);
                    mImageViewFace.setImageBitmapWithTransition(mBitmapFace);
                    initAndSetFacePosition();
                } else {
                    Toast.makeText(mActivity, R.string.no_faces_found, Toast.LENGTH_LONG).show();
                    mActivity.finish();
                }
                FileHelper.deleteFile(Constants.PMA_BOSSES_FILE_PATH + Constants.ORIGINAL_JPG);
            }
        }.execute();
    }

    private Bitmap getImageFromCamera() {
        Bitmap cameraBitmap;
        final String imagePath = Constants.PMA_BOSSES_FILE_PATH + Constants.ORIGINAL_JPG;
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
        final Uri imageUri = mActivity.getIntent().getData();
        final String filePath = getImagePath(imageUri);
        final int orientation = getImageOrientation(imageUri);

        // wenns hier knallt, dann das hier mal probieren: http://stackoverflow.com/questions/9668430/decode-a-bitmap-rotated
        final Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        cameraBitmap = BitmapFactory.decodeFile(filePath, options);
        cameraBitmap = Bitmap.createBitmap(cameraBitmap, 0, 0, cameraBitmap.getWidth(), cameraBitmap.getHeight(), matrix, false);
        return cameraBitmap;
    }

    private int getImageOrientation(final Uri imageUri) {
        final String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};

        final Cursor cursor = mActivity.getContentResolver().query(imageUri, orientationColumn, null, null, null);
        int orientation = -1;
        if (cursor != null && cursor.moveToFirst()) {
            orientation = cursor.getInt(cursor.getColumnIndex(orientationColumn[0]));
        }
        cursor.close();
        return orientation;
    }


    /** if this doesn't work, check: http://stackoverflow.com/questions/8450539/images-taken-with-action-image-capture-always-returns-1-for-exifinterface-tag-or/8864367#8864367*/
    private int getImageOrientation(final String imagePath) {
        int orientation = 0;
        try {
            final ExifInterface exif = new ExifInterface(imagePath);
            final int orientationAttribute = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);

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

    private String getImagePath(final Uri imageUri) {
        String filePath = "";
        final String[] filePathColumn = {MediaStore.Images.Media.DATA};
        final Cursor cursor = mActivity.getContentResolver().query(imageUri, filePathColumn, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            filePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
        }
        cursor.close();
        return filePath;
    }

    private Bitmap findFaceAndCrop(final Bitmap cameraBitmap){
        if(cameraBitmap != null){
            int width = cameraBitmap.getWidth();
            int height = cameraBitmap.getHeight();

            FaceDetector detector = new FaceDetector(width, height, MAX_FACES);
            FaceDetector.Face[] faces = new FaceDetector.Face[MAX_FACES];
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
                final Matrix matrix = new Matrix();
                matrix.postScale(faceScaleFactor, faceScaleFactor);
                faceBitmap = Bitmap.createBitmap(cameraBitmap, x > 0 ? x : 0, y > 0 ? y : 0, cropWidth, cropHeight, matrix, false);
                return faceBitmap;
            }
        }
        return null;
    }

    public void rotateFaceImage(final float degrees) {
        mImageViewFace.rotate(degrees, mBitmapFace.getWidth()/2, mBitmapFace.getHeight()/2);
    }

    public void shareBitmap() {
        if(mImageUri == null) {
            mProgressDialog.show();
            compressCurrentBoss();
        }
        startActivity(Intent
                .createChooser(createShareIntent(), getString(R.string.share_chooser_text)));
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

    public boolean saveBoss() {
        final Bitmap bitmap = mergeBitmaps();
        return saveBitmap(bitmap, Constants.PREFIX_BOSS + System.currentTimeMillis() + Constants.SUFFIX_JPEG);
    }

    private boolean saveBitmap(final Bitmap bitmap, final String fileName) {
        boolean success = false;
        String filepath = Constants.PMA_BOSSES_FILE_PATH;
        if(FileHelper.exists(filepath) != FileHelper.IS_DIRECTORY) {
            new File(filepath).mkdir();
        }
        filepath += fileName;

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filepath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            success = true;
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, ":: saveBitmap ::" + e, e);
        } catch (IOException e) {
            Log.e(LOG_TAG, ":: saveBitmap ::" + e, e);
        } finally {
            if(fos != null) try { fos.close(); } catch (IOException e) { Log.e(LOG_TAG, ":: saveBitmap ::" + e, e); }
        }
        return success;
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

    public void applyBackgroundChange(int backgroundPos) {
        mBackgroundNumber = backgroundPos;
        mBitmapBackground = BitmapFactory.decodeResource(getResources(), BossFragmentPagerAdapter.mBackgroundIds[mBackgroundNumber]);
        mImageViewBackground.setImageBitmap(mBitmapBackground);
        mImageViewFace.setPosition(mFaceSavingPosition);
        mImageViewFace.centerImage();
    }
}


