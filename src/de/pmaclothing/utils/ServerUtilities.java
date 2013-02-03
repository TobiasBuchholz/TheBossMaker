/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.pmaclothing.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

/**
 * Helper class used to communicate with the server for GCM.
 */
public final class ServerUtilities {
    private static final String LOG_TAG               = ServerUtilities.class.getSimpleName();
    
    private static final int    MAX_ATTEMPTS          = 5;
    private static final int    BACKOFF_MILLI_SECONDS = 2000;
    private static final Random random                = new Random();
    
    /**
     * Register this account/device pair within the server.
     * 
     * @param context The application context.
     * @param regId The id returned by GCM after a successfully registration with it.
     * @return whether the registration succeeded or not.
     */
    public static boolean sendBitmap(final Context context, final Bitmap bitmap) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(final Void... params) {
                final String serverUrl = Constants.SERVER_ADRESS + Constants.POST_BITMAP_PATHTH;
                
                final Map<String, String> requestParams = new HashMap<String, String>();
                requestParams.put("bitmap", getBitmapAsBase64(bitmap));
                long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
                
                // try to register with our server
                for (int i = 1; i <= MAX_ATTEMPTS; i++) {
                    Log.d(LOG_TAG, "Attempt #" + i + " to register");
                    try {
                        post(serverUrl, requestParams);
                        return true;
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Failed to post bitmap on attempt " + i, e);
                        if (i == MAX_ATTEMPTS) {
                            break;
                        }
                        try {
                            Log.d(LOG_TAG, "Sleeping for " + backoff + " ms before retry");
                            Thread.sleep(backoff);
                        } catch (final InterruptedException e1) {
                            // Activity finished before we complete - exit.
                            Log.d(LOG_TAG, "Thread interrupted: abort remaining retries!");
                            Thread.currentThread().interrupt();
                            return false;
                        }
                        // increase backoff exponentially
                        backoff *= 2;
                    }
                }
                return false;
            }
        }.execute();
        
        return false;
    }
    
    
    /**
     * Issue a POST request to the server.
     * 
     * @param endpoint POST address.
     * @param params request parameters.
     * @throws IOException propagated from POST.
     */
    private static void post(final String endpoint, final Map<String, String> params) throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        final StringBuilder bodyBuilder = new StringBuilder();
        final Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            final Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=').append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        final String body = bodyBuilder.toString();
        Log.v(LOG_TAG, "Posting '" + body + "' to " + url);
        final byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            final OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();
            // handle the response
            final int status = conn.getResponseCode();
            if (status == 503) {
                throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    private static String getBitmapAsBase64(Bitmap bitmap){
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    bitmap.compress(Bitmap.CompressFormat.PNG, 90, baos);
	    byte[] byteArray = baos.toByteArray();

	    return Base64.encodeToString(byteArray, Base64.DEFAULT);
	}
	
}
