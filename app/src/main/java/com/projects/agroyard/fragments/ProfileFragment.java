package com.projects.agroyard.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.projects.agroyard.MainActivity;
import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.utils.SessionManager;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2; 

    private static final String PREFS_NAME = "UserPrefs";
    private String userType;
    private String userName;
    private SessionManager sessionManager;
    private View deliveryOption;

    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        
        // Initialize SessionManager
        sessionManager = new SessionManager(requireContext());
        
        // Get user info from SessionManager
        userType = sessionManager.getUserType();
        userName = sessionManager.getName();
        
        // Fallback to SharedPreferences if needed
        if (userName.isEmpty()) {
            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            userType = prefs.getString("userType", Constants.FARMER); // Default to Farmer if not set
            userName = prefs.getString("userName", "User"); // Default to "User" if not set
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // Set user name in profile header
        TextView profileNameText = view.findViewById(R.id.profile_name);
        profileNameText.setText(userName);
        
        // Get delivery option view
        deliveryOption = view.findViewById(R.id.option_delivery);
        
        // Show delivery option only for MEMBER user type
        if (Constants.MEMBER.equals(userType)) {
            deliveryOption.setVisibility(View.VISIBLE);
        } else {
            deliveryOption.setVisibility(View.GONE);
        }
        
        // Set up option click listeners
        setupOptionClickListeners(view);
        
        return view;
    }
    
    private void setupOptionClickListeners(View view) {
        // My Posts option
        view.findViewById(R.id.option_my_posts).setOnClickListener(v -> {
            navigateToMyPosts();
        });
        
        // My Profile option
        view.findViewById(R.id.option_my_profile).setOnClickListener(v -> {
            showMyProfile();
        });
        
        // Share This App option
        view.findViewById(R.id.option_share_app).setOnClickListener(v -> {
            shareApp();
        });
        
        // Delivery Address option
        deliveryOption.setOnClickListener(v -> {
            showDeliveryForm();
        });
        
        // Contact Us option
        view.findViewById(R.id.option_contact_us).setOnClickListener(v -> {
            contactUs();
        });
        
        // Guidelines Videos option
        view.findViewById(R.id.option_guidelines).setOnClickListener(v -> {
            showGuidelines();
        });
        
        // Privacy Policy option
        view.findViewById(R.id.option_privacy_policy).setOnClickListener(v -> {
            showPrivacyPolicy();
        });
        
        // About option
        view.findViewById(R.id.option_about).setOnClickListener(v -> {
            showAbout();
        });
        
        // Log Out option
        view.findViewById(R.id.option_logout).setOnClickListener(v -> {
            logOut();
        });
    }
    
    private void navigateToMyPosts() {
        // Navigate to My Posts fragment
        Fragment myPostsFragment = new MyPostsFragment();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, myPostsFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    
    private void showMyProfile() {
        // Navigate to My Profile fragment
        Fragment myProfileFragment = new MyProfileFragment();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, myProfileFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    
    private void shareApp() {
        // Create share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out Agro-Yard App");
        shareIntent.putExtra(Intent.EXTRA_TEXT, 
                "Hey! Check out this amazing app for farmers and agricultural businesses: https://play.google.com/store/apps/details?id=com.projects.agroyard");
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
    
    private void showDeliveryForm() {
        // Only allow members to access this feature
        if (!Constants.MEMBER.equals(userType)) {
            Toast.makeText(requireContext(), "This feature is only available to Member accounts", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Navigate to Delivery Form fragment
        Fragment deliveryFragment = new DeliveryFragment();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, deliveryFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    
    private void contactUs() {
        // Open email app with pre-filled email
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:support@agroyard.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Support Request from Agro-Yard App");
        
        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), "No email apps installed", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showGuidelines() {
        // Navigate to Guidelines fragment (to be implemented)
        Toast.makeText(requireContext(), "Guidelines - Coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void showPrivacyPolicy() {
        // Navigate to Privacy Policy fragment (to be implemented)
        Toast.makeText(requireContext(), "Privacy Policy - Coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void showAbout() {
        // Navigate to About fragment
        Fragment aboutFragment = new About();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, aboutFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    
    private void logOut() {
        // Clear user session using SessionManager
        sessionManager.logoutUser();
        
        // Also clear SharedPreferences for backward compatibility
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to login fragment
        Fragment loginFragment = new LoginUsers();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, loginFragment);
        transaction.commit();
    }
}