package com.projects.agroyard.utils;

import android.content.Context;
import android.util.Log;

import com.projects.agroyard.constants.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Helper utility for managing image loading and verification
 */
public class ImageHelper {
    private static final String TAG = "ImageHelper";
    private static final String CHECK_IMAGE_URL = Constants.DB_URL_BASE + "check_image.php?filename=";
    private static final OkHttpClient client = new OkHttpClient();

    /**
     * Checks if an image exists on the server and returns details about it
     * @param context Android context
     * @param imageFilename The filename of the image to check
     * @param callback Callback to handle the result
     */
    public static void checkImageExistence(Context context, String imageFilename, ImageCheckCallback callback) {
        if (imageFilename == null || imageFilename.isEmpty()) {
            callback.onResult(false, "No filename provided", null);
            return;
        }

        String url = CHECK_IMAGE_URL + imageFilename;
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error checking image existence", e);
                callback.onResult(false, "Network error: " + e.getMessage(), null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onResult(false, "Server error: " + response.code(), null);
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);

                    boolean exists = jsonResponse.getBoolean("file_exists");
                    String emulatorUrl = jsonResponse.getString("emulator_url");

                    JSONObject details = new JSONObject();
                    details.put("exists", exists);
                    details.put("emulator_url", emulatorUrl);
                    details.put("browser_url", jsonResponse.getString("browser_url"));
                    details.put("physical_path", jsonResponse.getString("physical_path"));

                    if (exists && jsonResponse.has("file_details")) {
                        details.put("file_details", jsonResponse.getJSONObject("file_details"));
                    }

                    callback.onResult(exists, exists ?
                                    "Image exists on the server" :
                                    "Image doesn't exist on the server",
                            details);

                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing image check response", e);
                    callback.onResult(false, "Error parsing server response: " + e.getMessage(), null);
                }
            }
        });
    }

    /**
     * Constructs a full URL for accessing an image from the emulator
     * @param filename The image filename
     * @return The full URL for the image
     */
    public static String getEmulatorImageUrl(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        return Constants.DB_URL_BASE + "uploads/" + filename;
    }

    /**
     * Callback interface for image check operations
     */
    public interface ImageCheckCallback {
        void onResult(boolean exists, String message, JSONObject details);
    }
} 