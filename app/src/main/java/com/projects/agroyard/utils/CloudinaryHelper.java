package com.projects.agroyard.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.projects.agroyard.constants.Constants;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryHelper {
    private static final String TAG = "CloudinaryHelper";
    private static boolean isInitialized = false;

    public interface CloudinaryUploadCallback {
        void onSuccess(String url, String publicId);
        void onError(String message);
        void onProgress(int progress);
    }

    public static void initCloudinary(Context context) {
        if (isInitialized) return;

        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", Constants.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", Constants.CLOUDINARY_API_KEY);
            config.put("api_secret", Constants.CLOUDINARY_API_SECRET);
            config.put("secure", "true");
            
            MediaManager.init(context, config);
            isInitialized = true;
            Log.d(TAG, "Cloudinary initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Cloudinary", e);
        }
    }

    public static void uploadImage(Context context, Uri imageUri, String fileName, final CloudinaryUploadCallback callback) {
        if (!isInitialized) {
            initCloudinary(context);
        }

        // Create request options
        Map<String, Object> options = new HashMap<>();
        options.put("folder", Constants.CLOUDINARY_PRODUCT_FOLDER);
        options.put("public_id", fileName);
        options.put("resource_type", "image");

        // Upload the image
        String requestId = MediaManager.get().upload(imageUri)
                .options(options)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Upload started: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int progress = (int) ((bytes * 100) / totalBytes);
                        Log.d(TAG, "Upload progress: " + progress + "%");
                        callback.onProgress(progress);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("url");
                        String secureUrl = (String) resultData.get("secure_url");
                        String publicId = (String) resultData.get("public_id");
                        
                        Log.d(TAG, "Upload successful: " + secureUrl);
                        callback.onSuccess(secureUrl, publicId);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Upload error: " + error.getDescription());
                        callback.onError(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.d(TAG, "Upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch();
    }
} 