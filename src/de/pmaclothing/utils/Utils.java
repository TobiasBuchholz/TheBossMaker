package de.pmaclothing.utils;

import android.os.AsyncTask;
import android.os.Build;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 01.09.13 | Time: 20:55
 */
public class Utils {
    public static <Params, Task extends AsyncTask<Params, ?, ?>> void executeBackgroundTask(final Task task, final Params... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            task.execute(params);
        }
    }
}
