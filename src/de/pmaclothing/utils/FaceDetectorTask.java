package de.pmaclothing.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import de.pmaclothing.interfaces.FaceDetectorTaskListener;

import java.io.IOException;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 01.09.13 | Time: 13:09
 */
public class FaceDetectorTask extends AsyncTask<Uri, Void, Boolean> {
    private static final int            MAX_FACES               = 1;
    private ContentResolver             mContentResolver;

    private int                         mFaceSpace;
    private Bitmap                      mCameraBitmap;
    private Bitmap                      mFaceBitmap;
    private FaceDetectorTaskListener    mListener;

    public FaceDetectorTask(final ContentResolver contentResolver, final int faceSpace) {
        mContentResolver = contentResolver;
        mFaceSpace = faceSpace;
    }

    public void setOnTaskListener(FaceDetectorTaskListener listener) {
        mListener = listener;
    }

    @Override
    protected Boolean doInBackground(final Uri... params) {
        final Uri imageUri = params[0];
        if(imageUri == null) {
            mCameraBitmap = getImageFromCamera();
        } else {
            mCameraBitmap = getImageFromGallery(imageUri);
        }
        mFaceBitmap = findFaceAndCrop(mCameraBitmap);

        if(mFaceBitmap == null) {
            return false;
        }

        FileHelper.saveBitmap(mFaceBitmap, Constants.TEMP_FACE_PNG);
        return true;
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        if(success) {
            mListener.onTaskFinishSuccess(mFaceBitmap);
        } else {
            mListener.onTaskFinishFail();
        }
        FileHelper.deleteFile(Constants.PMA_BOSSES_FILE_PATH + Constants.ORIGINAL_JPG);
    }

    private Bitmap getImageFromCamera() {
        Bitmap cameraBitmap;
        final String imagePath = Constants.PMA_BOSSES_FILE_PATH + Constants.ORIGINAL_JPG;
        int orientation = getImageOrientation(imagePath);

        final Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
        cameraBitmap = BitmapFactory.decodeFile(imagePath);
        cameraBitmap = Bitmap.createBitmap(cameraBitmap, 0, 0, cameraBitmap.getWidth(), cameraBitmap.getHeight(), matrix, false);
        return cameraBitmap;
    }

    private Bitmap getImageFromGallery(final Uri imageUri) {
        Bitmap cameraBitmap;
        final String filePath = getImagePath(imageUri);
        final int orientation = getImageOrientation(imageUri);

        final Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        cameraBitmap = BitmapFactory.decodeFile(filePath, options);
        cameraBitmap = Bitmap.createBitmap(cameraBitmap, 0, 0, cameraBitmap.getWidth(), cameraBitmap.getHeight(), matrix, false);
        return cameraBitmap;
    }

    private int getImageOrientation(final Uri imageUri) {
        final String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};

        final Cursor cursor = mContentResolver.query(imageUri, orientationColumn, null, null, null);
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
        final Cursor cursor = mContentResolver.query(imageUri, filePathColumn, null, null, null);
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
}
