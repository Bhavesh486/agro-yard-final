package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.projects.agroyard.R;

public class CropDetailFragment extends Fragment {
    private ImageView cropImageView;
    private ImageView backButton;
    private TextView cropTitleTextView;
    private TextView temperatureTextView;
    private TextView humidityTextView;
    private TextView sunlightTextView;
    private TextView waterTextView;
    private TextView soilTypeTextView;
    private TextView soilPhTextView;
    private TextView soilNutrientsTextView;
    private TextView soilDepthTextView;
    private TextView growingSeasonTextView;
    private TextView descriptionTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crop_detail, container, false);
        
        // Initialize views
        cropImageView = view.findViewById(R.id.crop_image);
            backButton = view.findViewById(R.id.back_button);
        cropTitleTextView = view.findViewById(R.id.crop_title);
        temperatureTextView = view.findViewById(R.id.temperature_text);
        humidityTextView = view.findViewById(R.id.humidity_text);
        sunlightTextView = view.findViewById(R.id.sunlight_text);
        waterTextView = view.findViewById(R.id.water_text);
        soilTypeTextView = view.findViewById(R.id.soil_type_text);
        soilPhTextView = view.findViewById(R.id.soil_ph_text);
        soilNutrientsTextView = view.findViewById(R.id.soil_nutrients_text);
        soilDepthTextView = view.findViewById(R.id.soil_depth_text);
        growingSeasonTextView = view.findViewById(R.id.growing_season_text);
        descriptionTextView = view.findViewById(R.id.description_text);

        // Setup back button
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Set data from bundle
        Bundle bundle = getArguments();
        if (bundle != null) {
            String cropName = bundle.getString(CropInfoFragment.CROP_NAME, "");
            String cropType = bundle.getString(CropInfoFragment.CROP_TYPE, "");
            int cropImageResId = bundle.getInt(CropInfoFragment.CROP_IMAGE, 0);

            // Set crop title
            cropTitleTextView.setText(cropName);

            // Set crop image
            if (cropImageResId != 0) {
                try {
                    cropImageView.setImageResource(cropImageResId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Set crop details based on crop name
            setCropDetails(cropName, cropType);
        }

        return view;
    }

    private void setCropDetails(String cropName, String cropType) {
        // This would typically fetch data from a database or predefined sets
        // For demonstration, we're setting some sample data based on crop name
        switch (cropName.toLowerCase()) {
            case "rice":
                setRiceDetails();
                break;
            case "wheat":
                setWheatDetails();
                break;
            case "corn":
                setCornDetails();
                break;
            case "cotton":
                setCottonDetails();
                break;
            case "sugarcane":
                setSugarcaneDetails();
                break;
            default:
                setDefaultDetails();
                break;
        }
    }

    private void setRiceDetails() {
        temperatureTextView.setText("20-35°C");
        humidityTextView.setText("80-90%");
        sunlightTextView.setText("Full sun");
        waterTextView.setText("High (flooded conditions)");
        soilTypeTextView.setText("Clay or clay loam");
        soilPhTextView.setText("5.5-6.5");
        soilNutrientsTextView.setText("High in nitrogen");
        soilDepthTextView.setText("15-20 cm");
        growingSeasonTextView.setText("Kharif (monsoon) / Rabi");
        descriptionTextView.setText("Rice is a staple food crop in India, grown widely during monsoon season. It requires wet, humid conditions and proper water management for optimal growth. Two main varieties are grown: long-grain and short-grain rice.");
    }

    private void setWheatDetails() {
        temperatureTextView.setText("15-25°C");
        humidityTextView.setText("50-60%");
        sunlightTextView.setText("Full sun");
        waterTextView.setText("Moderate");
        soilTypeTextView.setText("Loamy or clay loam");
        soilPhTextView.setText("6.0-7.0");
        soilNutrientsTextView.setText("Nitrogen, phosphorus, potassium");
        soilDepthTextView.setText("15-25 cm");
        growingSeasonTextView.setText("Rabi (winter)");
        descriptionTextView.setText("Wheat is a major cereal grain cultivated in cooler regions of India during the winter season. It's a hardy crop that can be grown in various soil types but performs best in well-drained loamy soils with good fertility.");
    }

    private void setCornDetails() {
        temperatureTextView.setText("21-30°C");
        humidityTextView.setText("50-80%");
        sunlightTextView.setText("Full sun");
        waterTextView.setText("Medium to high");
        soilTypeTextView.setText("Well-drained loamy soil");
        soilPhTextView.setText("5.8-7.0");
        soilNutrientsTextView.setText("High in nitrogen and potassium");
        soilDepthTextView.setText("30-45 cm");
        growingSeasonTextView.setText("Kharif, Rabi, and Spring");
        descriptionTextView.setText("Corn (Maize) is a versatile crop grown in almost all regions of India. It's grown year-round in different seasons. Modern hybrid varieties offer high yields and are used for various purposes including food, animal feed, and industrial applications.");
    }

    private void setCottonDetails() {
        temperatureTextView.setText("21-30°C");
        humidityTextView.setText("50-60%");
        sunlightTextView.setText("Full sun");
        waterTextView.setText("Moderate");
        soilTypeTextView.setText("Deep, well-drained black soil");
        soilPhTextView.setText("5.5-8.0");
        soilNutrientsTextView.setText("Balanced NPK");
        soilDepthTextView.setText("90-120 cm");
        growingSeasonTextView.setText("Kharif (monsoon)");
        descriptionTextView.setText("Cotton is an important commercial crop in India, grown primarily for its fiber. It's typically planted at the beginning of the monsoon season and requires warm temperatures and moderate rainfall for optimal growth.");
    }

    private void setSugarcaneDetails() {
        temperatureTextView.setText("24-30°C");
        humidityTextView.setText("80-85%");
        sunlightTextView.setText("Full sun");
        waterTextView.setText("High");
        soilTypeTextView.setText("Loamy to clayey soil");
        soilPhTextView.setText("6.5-7.5");
        soilNutrientsTextView.setText("Rich in organic matter");
        soilDepthTextView.setText("90-100 cm");
        growingSeasonTextView.setText("Year-round (12-18 month cycle)");
        descriptionTextView.setText("Sugarcane is a perennial crop grown for its sugar content. It requires hot and humid climate with moderate rainfall. In India, it's grown in tropical and subtropical regions, with major production in states like Uttar Pradesh, Maharashtra, and Karnataka.");
    }

    private void setDefaultDetails() {
        temperatureTextView.setText("Varies by variety");
        humidityTextView.setText("Varies by variety");
        sunlightTextView.setText("Full sun to partial shade");
        waterTextView.setText("Moderate");
        soilTypeTextView.setText("Well-drained fertile soil");
        soilPhTextView.setText("6.0-7.0");
        soilNutrientsTextView.setText("Balanced NPK");
        soilDepthTextView.setText("15-30 cm");
        growingSeasonTextView.setText("Varies by variety");
        descriptionTextView.setText("Please check with local agricultural extension services for specific information about this crop in your region.");
    }
} 