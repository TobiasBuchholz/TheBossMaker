package de.pmaclothing.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

public class FileHelper {
    public static final String  LOG_TAG         = FileHelper.class.getSimpleName();
    public static final int     IS_DIRECTORY    = -1;
    public static final int     IS_FILE         = 1;
    public static final int     IS_NOTHING      = 0;
    public static final String  SUFFIX_NOMEDIA  = ".nomedia";

    public static boolean createNoMediaDirectory(final File directory) {
        directory.mkdirs();
        if (!isExisting(directory.getAbsolutePath() + SUFFIX_NOMEDIA)) {
            tryCreateNoMediaFile(directory.getAbsolutePath());
        }
        return directory.exists() && directory.isDirectory();
    }

    private static void tryCreateNoMediaFile(final String baseDataFolderPath) {
        try {
            createNoMediaFile(baseDataFolderPath);
        } catch (final IOException e) {
            Log.e(LOG_TAG, "Couldn't create directory " + baseDataFolderPath, e);
        }
    }

    private static void createNoMediaFile(final String baseDataFolderPath) throws IOException {
        if (new File(baseDataFolderPath, SUFFIX_NOMEDIA).createNewFile()) {
            Log.d(LOG_TAG, ".nomedia-file created");
        } else {
            Log.d(LOG_TAG, ".nomedia-file not created");
        }
    }

    /**
     * Checks whether a file or directory exists.
     * 
     * @param path Path to the file or directory.
     * @return 1 if file. <br>
     *         -1 if directory. <br>
     *         0 if nothing at all.
     */
    public static int exists(final String path) {
        if (path != null && path.length() > 0) {
            final File file = new File(path);
            if (file.exists()) {
                if (file.isFile()) {
                    return 1;
                }
                return -1;
            }
        }
        return 0;
    }
    
    /**
     * @param path The path to the desired file.
     * @return true if deletion of the file was successfully.
     */
    public static boolean deleteFile(final String path) {
    	if (path != null && path.length() > 0) {
            final File file = new File(path);
            return file.delete();
    	}
    	return false;
    }

    public static boolean saveBitmap(final Bitmap bitmap, final String fileName) {
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

    public static boolean isExisting(final String path) {
        return exists(path) != IS_NOTHING;
    }
}
