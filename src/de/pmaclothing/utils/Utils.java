package de.pmaclothing.utils;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 01.09.13 | Time: 20:55
 */
public class Utils {
    private static final String LOG_TAG = Utils.class.getSimpleName();

    public static <Params, Task extends AsyncTask<Params, ?, ?>> void executeBackgroundTask(final Task task, final Params... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            task.execute(params);
        }
    }

    public static void closeInputStream(final InputStream inputStream) {
        if(inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, ":: closeInputStream ::" + e, e);
            }
        }
    }

    public static void closeOutputStream(final OutputStream outputStream) {
        if(outputStream != null) {
            try {
                outputStream.close();
                outputStream.flush();
            } catch (IOException e) {
                Log.e(LOG_TAG, ":: closeOutputStream ::" + e, e);
            }
        }
    }
}
