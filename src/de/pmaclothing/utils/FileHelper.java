package de.pmaclothing.utils;

import java.io.File;

import android.os.Environment;

public class FileHelper {
	public static final int IS_DIRECTORY = -1;
	public static final int IS_FILE = 1;
	public static final int IS_NOTHING = 0;
	
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
}
