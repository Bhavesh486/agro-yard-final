package com.projects.agroyard.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;

public class BottomNavigationFragment extends Fragment {
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_TYPE = "userType";
    
    private String userType;

    public BottomNavigationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get user type from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userType = prefs.getString(KEY_USER_TYPE, Constants.FARMER); // Default to Farmer if not set
        
        // Debug log for user type
        Log.d("BottomNavigation", "User Type: " + userType);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bottom_navigation, container, false);

        BottomNavigationView bottomNavigationView = view.findViewById(R.id.bottom_navigation);
        
        // Update the payment text based on user type
        if (userType.equals(Constants.FARMER)) {
            // Change menu item title programmatically
            bottomNavigationView.getMenu().findItem(R.id.n_payment).setTitle(R.string.payment_history);
        } else {
            bottomNavigationView.getMenu().findItem(R.id.n_payment).setTitle(R.string.payment);
        }
        
        bottomNavigationView.setOnItemSelectedListener(navListener);

        // Load HomeFragment by default
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        return view;
    }

    private final BottomNavigationView.OnItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.n_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.n_profile) {
                    selectedFragment = new ProfileFragment();
                } else if (itemId == R.id.n_payment) {
                    // Show different payment screens based on user type
                    if (userType.equals(Constants.FARMER)) {
                        selectedFragment = new PaymentHistoryFragment();
                    } else {
                        selectedFragment = new PaymentFragment();
                    }
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                }
                return true;
            };

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}
