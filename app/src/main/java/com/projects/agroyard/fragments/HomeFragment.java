
package com.projects.agroyard.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.projects.agroyard.R;
import com.projects.agroyard.adapters.ImageCarouselAdapter;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.model.Slide;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_TYPE = "userType";
    
    private TextView welcomeText;
    private TextView temperatureText;
    private TextView humidityText;
    private TextView windText;
    private TextView recommendedCropsText;
    private View notificationIconContainer;
    private TextView notificationBadge;
    private GridLayout categoryGrid;
    
    private String userType;
    private String userName;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get user type from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userType = prefs.getString(KEY_USER_TYPE, Constants.FARMER); // Default to Farmer if not set
        userName = prefs.getString("userName", "User"); // Default to "User" if not set
        
        // Debug log for user type
        Log.d("HomeFragment", "User Type: " + userType + ", User Name: " + userName);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Initialize views
        welcomeText = view.findViewById(R.id.welcome_text);
        temperatureText = view.findViewById(R.id.temperature_text);
        humidityText = view.findViewById(R.id.humidity_text);
        windText = view.findViewById(R.id.wind_text);
        recommendedCropsText = view.findViewById(R.id.recommended_crops_text);
        notificationIconContainer = view.findViewById(R.id.notification_icon_container);
        notificationBadge = view.findViewById(R.id.notification_badge);
        categoryGrid = view.findViewById(R.id.category_grid);

        // Set weather information
        temperatureText.setText("Temperature: 28°C");
        humidityText.setText("Humidity: 65%");
        windText.setText("Wind: 12 km/h");
        recommendedCropsText.setText("Recommended Crops:\n• Wheat\n• Rice\n• Tomatoes");
        
        // Set notification count
        updateNotificationBadge(3);
        
        // Add notification click listener
        notificationIconContainer.setOnClickListener(v -> {
            showNotifications();
        });
        
        // Set click listener for profile icon
        view.findViewById(R.id.profile_icon).setOnClickListener(v -> {
            navigateToFragment(new ProfileFragment());
        });

        // Configure UI based on user type
        configureUIForUserType(view);
        
        return view;
    }
    
    private void configureUIForUserType(View view) {
        // Set welcome message based on user type
        welcomeText.setText("Welcome, " + (userType.equals(Constants.MEMBER) ? 
                                        userName + " Member" : 
                                        "Farmer " + userName));
        
        // Clear existing UI elements from the grid
        categoryGrid.removeAllViews();
        
        // Inflate the appropriate UI elements based on user type
        if (userType.equals(Constants.FARMER)) {
            setupFarmerUI(view);
        } else if (userType.equals(Constants.MEMBER)) {
            setupMemberUI(view);
        }
    }
    
    private void setupFarmerUI(View view) {
        // Add Crop Info card
        addCategoryCard(R.drawable.ic_file_text, "Crop Info", R.drawable.circle_primary_light, 
                v -> navigateToFragment(new CropInfoFragment()));
        
        // Add Bid Monitor card
        addCategoryCard(R.drawable.ic_bid_monitor, "Bid Monitor", R.drawable.circle_green_light, 
                v -> navigateToFragment(new BidMonitorFragment()));
        
        // Add Upload Product card
        addCategoryCard(R.drawable.ic_upload_product, "Upload Product", R.drawable.circle_blue_light, 
                v -> navigateToFragment(new UploadProductFragment()));
        
        // Add Delivery Requests card
        addCategoryCard(R.drawable.ic_truck, "Delivery Requests", R.drawable.circle_yellow_light, 
                v -> navigateToFragment(new DeliveryRequestsFragment()));
        
        // Add Receipts card
        addCategoryCard(R.drawable.ic_transaction, "Receipts", R.drawable.circle_red_light, 
                v -> navigateToFragment(new ReceiptsFragment()));
    }
    
    private void setupMemberUI(View view) {
        // Add Products card
        addCategoryCard(R.drawable.ic_products, "Products", R.drawable.circle_yellow_light, 
                v -> navigateToFragment(new ProductsFragment()));
        
        // Add Betting card with extra prominence
        addCategoryCard(R.drawable.ic_betting, "Active Bidding", R.drawable.circle_red_light, 
                v -> navigateToFragment(new BiddingFragment())); //Betting Fragment
        
        // Add Payment card
        addCategoryCard(R.drawable.ic_payment, "Payment", R.drawable.circle_blue_light, 
                v -> navigateToFragment(new PaymentFragment()));
        
        // Add Delivery card
        addCategoryCard(R.drawable.ic_delivery, "Delivery", R.drawable.circle_primary_light, 
                v -> navigateToFragment(new DeliveryFragment()));

        // Add Receipts card
        addCategoryCard(R.drawable.ic_transaction, "Receipts", R.drawable.circle_green_light, 
                v -> navigateToFragment(new ReceiptsFragment()));
    }
    
    private void addCategoryCard(int iconResourceId, String title, int backgroundResourceId, View.OnClickListener clickListener) {
        CardView cardView = (CardView) getLayoutInflater().inflate(R.layout.item_category_card, categoryGrid, false);
        
        // Configure the card
        ImageView iconView = cardView.findViewById(R.id.category_icon);
        TextView titleView = cardView.findViewById(R.id.category_title);
        View iconBackground = cardView.findViewById(R.id.icon_background);
        
        iconView.setImageResource(iconResourceId);
        titleView.setText(title);
        iconBackground.setBackgroundResource(backgroundResourceId);
        
        // Set click listener
        cardView.setOnClickListener(clickListener);
        
        // Add parameters for grid layout
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(8, 8, 8, 8);
        
        cardView.setLayoutParams(params);
        
        // Add to the grid
        categoryGrid.addView(cardView);
    }
    
    /**
     * Updates the notification badge count
     * If count is 0, hides the badge
     */
    private void updateNotificationBadge(int count) {
        if (count > 0) {
            notificationBadge.setVisibility(View.VISIBLE);
            notificationBadge.setText(String.valueOf(count));
        } else {
            notificationBadge.setVisibility(View.GONE);
        }
    }
    
    /**
     * Shows the notifications screen
     */
    private void showNotifications() {
        // Navigate to the NotificationsFragment
        navigateToFragment(new NotificationsFragment());
    }

    private void navigateToFragment(Fragment fragment) {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}