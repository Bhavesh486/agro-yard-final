package com.projects.agroyard.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager handles user session management including login state persistence.
 */
public class SessionManager {
    // Shared Preferences
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;
    
    // Shared Preferences file name
    private static final String PREF_NAME = "AgroYardSession";
    
    // Shared Preferences keys
    private static final String IS_LOGGED_IN = "IsLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_MOBILE = "mobile";
    private static final String KEY_PROFILE_IMAGE = "profileImage";
    
    // Constructor
    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }
    
    /**
     * Create login session
     */
    public void createLoginSession(String userId, String name, String email, String userType, String mobile, String profileImage) {
        // Store login status
        editor.putBoolean(IS_LOGGED_IN, true);
        
        // Store user data
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_USER_TYPE, userType);
        editor.putString(KEY_MOBILE, mobile);
        editor.putString(KEY_PROFILE_IMAGE, profileImage);
        
        // Commit changes
        editor.apply();
    }
    
    /**
     * Get stored session data
     */
    public String getUserId() {
        return pref.getString(KEY_USER_ID, "");
    }
    
    public String getName() {
        return pref.getString(KEY_NAME, "");
    }
    
    public String getEmail() {
        return pref.getString(KEY_EMAIL, "");
    }
    
    public String getUserType() {
        return pref.getString(KEY_USER_TYPE, "");
    }
    
    public String getMobile() {
        return pref.getString(KEY_MOBILE, "");
    }
    
    public String getProfileImage() {
        return pref.getString(KEY_PROFILE_IMAGE, "");
    }
    
    /**
     * Update specific user data
     */
    public void updateUserName(String name) {
        editor.putString(KEY_NAME, name);
        editor.apply();
    }
    
    public void updateUserEmail(String email) {
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }
    
    public void updateUserMobile(String mobile) {
        editor.putString(KEY_MOBILE, mobile);
        editor.apply();
    }
    
    public void updateProfileImage(String profileImage) {
        editor.putString(KEY_PROFILE_IMAGE, profileImage);
        editor.apply();
    }
    
    /**
     * Check login status
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(IS_LOGGED_IN, false);
    }
    
    /**
     * Clear session details and log out user
     */
    public void logoutUser() {
        // Clear all data from Shared Preferences
        editor.clear();
        editor.apply();
    }
} 