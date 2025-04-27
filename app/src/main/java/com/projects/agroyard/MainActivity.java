package com.projects.agroyard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.projects.agroyard.fragments.BottomNavigationFragment;
import com.projects.agroyard.fragments.LoginUsers;
import com.projects.agroyard.fragments.NotificationsFragment;
import com.projects.agroyard.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        handleNotificationIntent(getIntent());

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            if (isUserLoggedIn()) {
                loadFragment(new BottomNavigationFragment());  // Load home if logged in
            } else {
                loadFragment(new LoginUsers());  // Load login screen if not logged in
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && "OPEN_NOTIFICATION_FRAGMENT".equals(intent.getAction())) {
            openNotificationFragment();
        }
    }

    private void openNotificationFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new NotificationsFragment())
                .addToBackStack(null)
                .commit();
    }

    public void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private boolean isUserLoggedIn() {
        // Use SessionManager to check if user is logged in
        return sessionManager.isLoggedIn();
    }

    public void logoutUser() {
        // Clear session data
        sessionManager.logoutUser();
        
        // Navigate back to login screen
        loadFragment(new LoginUsers());
    }
}
