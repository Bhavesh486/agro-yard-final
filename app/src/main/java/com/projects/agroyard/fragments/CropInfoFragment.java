package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.projects.agroyard.R;

import java.util.HashMap;
import java.util.Map;

public class CropInfoFragment extends Fragment {
    private ImageView backButton;
    private CardView tomatoesCard;
    private CardView wheatCard;
    private CardView applesCard;
    private CardView riceCard;
    private CardView potatoesCard;
    private CardView mangoesCard;
    private CardView cornCard;
    private CardView carrotsCard;
    private CardView bananasCard;
    private CardView orangesCard;
    private CardView grapesCard;
    private CardView watermelonCard;
    private CardView strawberryCard;
    private CardView pineappleCard;
    private CardView cherryCard;
    private CardView kiwiCard;
    private CardView papayaCard;
    private CardView guavaCard;
    private CardView onionsCard;
    private CardView cucumberCard;
    private CardView broccoliCard;
    private CardView eggplantCard;
    private CardView bellPepperCard;
    private CardView spinachCard;
    private CardView lettuceCard;
    private CardView cauliflowerCard;
    private CardView peasCard;
    private CardView garlicCard;
    private CardView barleyCard;
    private CardView oatsCard;
    private CardView milletCard;
    private CardView quinoaCard;
    private CardView sorghumCard;

    // Constants for crop types
    public static final String CROP_NAME = "crop_name";
    public static final String CROP_TYPE = "crop_type";
    public static final String CROP_IMAGE = "crop_image";
    
    // Data for each crop
    private final Map<String, Map<String, Object>> cropData = new HashMap<>();

    public CropInfoFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crop_info, container, false);
        
        // Initialize crop data
        initializeCropData();
        
        // Initialize views
        initializeViews(view);
        
        // Set click listeners
        setupClickListeners();
        
        // Setup back button
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
        
        return view;
    }

    private void initializeViews(View view) {
        try {
            backButton = view.findViewById(R.id.back_button);
            tomatoesCard = view.findViewById(R.id.tomatoes_card);
            wheatCard = view.findViewById(R.id.wheat_card);
            applesCard = view.findViewById(R.id.apples_card);
            riceCard = view.findViewById(R.id.rice_card);
            potatoesCard = view.findViewById(R.id.potatoes_card);
            mangoesCard = view.findViewById(R.id.mangoes_card);
            cornCard = view.findViewById(R.id.corn_card);
            carrotsCard = view.findViewById(R.id.carrots_card);
            
            // Initialize new fruit cards
            bananasCard = view.findViewById(R.id.bananas_card);
            orangesCard = view.findViewById(R.id.oranges_card);
            grapesCard = view.findViewById(R.id.grapes_card);
            watermelonCard = view.findViewById(R.id.watermelon_card);
            strawberryCard = view.findViewById(R.id.strawberry_card);
            pineappleCard = view.findViewById(R.id.pineapple_card);
            cherryCard = view.findViewById(R.id.cherry_card);
            kiwiCard = view.findViewById(R.id.kiwi_card);
            papayaCard = view.findViewById(R.id.papaya_card);
            guavaCard = view.findViewById(R.id.guava_card);
            
            // Initialize new vegetable cards
            onionsCard = view.findViewById(R.id.onions_card);
            cucumberCard = view.findViewById(R.id.cucumber_card);
            broccoliCard = view.findViewById(R.id.broccoli_card);
            eggplantCard = view.findViewById(R.id.eggplant_card);
            bellPepperCard = view.findViewById(R.id.bell_pepper_card);
            spinachCard = view.findViewById(R.id.spinach_card);
            lettuceCard = view.findViewById(R.id.lettuce_card);
            cauliflowerCard = view.findViewById(R.id.cauliflower_card);
            peasCard = view.findViewById(R.id.peas_card);
            garlicCard = view.findViewById(R.id.garlic_card);
            
            // Initialize new grain cards
            barleyCard = view.findViewById(R.id.barley_card);
            oatsCard = view.findViewById(R.id.oats_card);
            milletCard = view.findViewById(R.id.millet_card);
            quinoaCard = view.findViewById(R.id.quinoa_card);
            sorghumCard = view.findViewById(R.id.sorghum_card);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeCropData() {
        // Tomatoes data
        Map<String, Object> tomatoData = new HashMap<>();
        tomatoData.put(CROP_NAME, "Tomatoes");
        tomatoData.put(CROP_TYPE, "Vegetable");
        tomatoData.put(CROP_IMAGE, R.drawable.tomato_img);
        tomatoData.put("temperature", "21-24°C");
        tomatoData.put("humidity", "65-75%");
        tomatoData.put("sunlight", "6-8 hours daily");
        tomatoData.put("water", "Regular, consistent watering");
        tomatoData.put("soil_type", "Well-draining, loamy");
        tomatoData.put("soil_ph", "6.0-6.8");
        tomatoData.put("soil_nutrients", "Rich in organic matter");
        tomatoData.put("soil_depth", "At least 12 inches");
        tomatoData.put("growing_season", "March to June (planting), June to September (harvesting)");
        tomatoData.put("description", "Tomatoes are one of the most popular garden vegetables and come in many varieties.");
        cropData.put("tomatoes", tomatoData);

        // Wheat data
        Map<String, Object> wheatData = new HashMap<>();
        wheatData.put(CROP_NAME, "Wheat (Gehun)");
        wheatData.put(CROP_TYPE, "Grain");
        wheatData.put(CROP_IMAGE, R.drawable.weat_img);
        wheatData.put("temperature", "15-20°C");
        wheatData.put("humidity", "40-60%");
        wheatData.put("sunlight", "Full sun exposure");
        wheatData.put("water", "Moderate watering, more during flowering");
        wheatData.put("soil_type", "Loamy, well-draining");
        wheatData.put("soil_ph", "5.5-7.0");
        wheatData.put("soil_nutrients", "Nitrogen-rich soil");
        wheatData.put("soil_depth", "At least 15 inches");
        wheatData.put("growing_season", "October to November (winter wheat), March to April (spring wheat)");
        wheatData.put("description", "Wheat is one of the world's most important cereal crops, providing essential nutrients and calories.");
        cropData.put("wheat", wheatData);

        // Apples data
        Map<String, Object> applesData = new HashMap<>();
        applesData.put(CROP_NAME, "Apples");
        applesData.put(CROP_TYPE, "Fruit");
        applesData.put(CROP_IMAGE, R.drawable.apple_img);
        applesData.put("temperature", "15-22°C");
        applesData.put("humidity", "60-75%");
        applesData.put("sunlight", "6-8 hours daily");
        applesData.put("water", "Regular watering, especially in dry periods");
        applesData.put("soil_type", "Well-draining, loamy, moist soil");
        applesData.put("soil_ph", "6.0-7.0");
        applesData.put("soil_nutrients", "Moderate fertility");
        applesData.put("soil_depth", "Deep soil for extensive root system");
        applesData.put("growing_season", "Spring flowering, Fall harvesting (varies by variety)");
        applesData.put("description", "Apples are a popular fruit crop grown worldwide and come in thousands of varieties.");
        cropData.put("apples", applesData);

        // Rice data
        Map<String, Object> riceData = new HashMap<>();
        riceData.put(CROP_NAME, "Rice (Chawal)");
        riceData.put(CROP_TYPE, "Grain");
        riceData.put(CROP_IMAGE, R.drawable.rice_img);
        riceData.put("temperature", "20-35°C");
        riceData.put("humidity", "80-90%");
        riceData.put("sunlight", "Full sun exposure");
        riceData.put("water", "Submergence in water (paddy fields)");
        riceData.put("soil_type", "Clay soil that holds water well");
        riceData.put("soil_ph", "5.0-6.5");
        riceData.put("soil_nutrients", "High in organic matter");
        riceData.put("soil_depth", "Minimum of 10 inches");
        riceData.put("growing_season", "May to September (varies by region)");
        riceData.put("description", "Rice is a staple food for more than half of the world's population and grown in flooded fields.");
        cropData.put("rice", riceData);
        
        // Potatoes data
        Map<String, Object> potatoesData = new HashMap<>();
        potatoesData.put(CROP_NAME, "Potatoes");
        potatoesData.put(CROP_TYPE, "Vegetable");
        potatoesData.put(CROP_IMAGE, R.drawable.potato);
        potatoesData.put("temperature", "15-20°C");
        potatoesData.put("humidity", "60-80%");
        potatoesData.put("sunlight", "Full sun to partial shade");
        potatoesData.put("water", "Regular watering, especially during tuber formation");
        potatoesData.put("soil_type", "Loose, well-draining, sandy loam");
        potatoesData.put("soil_ph", "5.0-6.5");
        potatoesData.put("soil_nutrients", "High in potassium and phosphorus");
        potatoesData.put("soil_depth", "At least 12 inches for tuber development");
        potatoesData.put("growing_season", "Spring to fall; early, mid, and late-season varieties");
        potatoesData.put("description", "Potatoes are tubers that grow underground and are among the world's most important food crops.");
        cropData.put("potatoes", potatoesData);
        
        // Mangoes data
        Map<String, Object> mangoesData = new HashMap<>();
        mangoesData.put(CROP_NAME, "Mangoes");
        mangoesData.put(CROP_TYPE, "Fruit");
        mangoesData.put(CROP_IMAGE, R.drawable.mango);
        mangoesData.put("temperature", "24-30°C");
        mangoesData.put("humidity", "50-60%");
        mangoesData.put("sunlight", "Full sun exposure");
        mangoesData.put("water", "Regular when young, reduced during flowering, increased during fruit development");
        mangoesData.put("soil_type", "Deep, well-draining loamy soil");
        mangoesData.put("soil_ph", "5.5-7.5");
        mangoesData.put("soil_nutrients", "Rich in organic matter");
        mangoesData.put("soil_depth", "Deep soil for extensive root system");
        mangoesData.put("growing_season", "Flowering in winter, fruit development in spring, harvesting in summer");
        mangoesData.put("description", "Mangoes are tropical fruits known for their sweet flavor and juicy flesh. The fruits develop from green to yellow-red when ripe, with varieties showing different color patterns. They grow on trees that can reach heights of 30-100 feet and require warm climate to thrive.");
        cropData.put("mangoes", mangoesData);
        
        // Corn data
        Map<String, Object> cornData = new HashMap<>();
        cornData.put(CROP_NAME, "Corn (Makka)");
        cornData.put(CROP_TYPE, "Grain");
        cornData.put(CROP_IMAGE, R.drawable.placeholder_image);
        cornData.put("temperature", "18-25°C");
        cornData.put("humidity", "50-80%");
        cornData.put("sunlight", "Full sun exposure");
        cornData.put("water", "Regular watering, critical during silking and tasseling");
        cornData.put("soil_type", "Well-draining, fertile soil");
        cornData.put("soil_ph", "5.8-6.8");
        cornData.put("soil_nutrients", "High in nitrogen");
        cornData.put("soil_depth", "Deep soil for extensive root system");
        cornData.put("growing_season", "Late spring to early fall");
        cornData.put("description", "Corn (maize) is a cereal grain domesticated by indigenous peoples in southern Mexico. It's now one of the most widely grown crops worldwide.");
        cropData.put("corn", cornData);
        
        // Carrots data
        Map<String, Object> carrotsData = new HashMap<>();
        carrotsData.put(CROP_NAME, "Carrots");
        carrotsData.put(CROP_TYPE, "Vegetable");
        carrotsData.put(CROP_IMAGE, R.drawable.placeholder_image);
        carrotsData.put("temperature", "15-20°C");
        carrotsData.put("humidity", "50-70%");
        carrotsData.put("sunlight", "Full sun to partial shade");
        carrotsData.put("water", "Consistent moisture, especially during germination");
        carrotsData.put("soil_type", "Loose, sandy, well-draining soil free of rocks");
        carrotsData.put("soil_ph", "6.0-6.8");
        carrotsData.put("soil_nutrients", "Low nitrogen, moderate potassium and phosphorus");
        carrotsData.put("soil_depth", "At least 12 inches for root development");
        carrotsData.put("growing_season", "Spring and fall, cool seasons");
        carrotsData.put("description", "Carrots are root vegetables with a crisp texture and are rich in beta-carotene. They grow best in loose, sandy soil and cooler temperatures.");
        cropData.put("carrots", carrotsData);

        // Bananas data
        Map<String, Object> bananasData = new HashMap<>();
        bananasData.put(CROP_NAME, "Bananas");
        bananasData.put(CROP_TYPE, "Fruit");
        bananasData.put(CROP_IMAGE, R.drawable.placeholder_image);
        bananasData.put("temperature", "26-30°C");
        bananasData.put("humidity", "70-90%");
        bananasData.put("sunlight", "Full sun to partial shade");
        bananasData.put("water", "Regular, consistent watering");
        bananasData.put("soil_type", "Deep, well-draining, loamy soil");
        bananasData.put("soil_ph", "5.5-7.0");
        bananasData.put("soil_nutrients", "High in potassium and phosphorus");
        bananasData.put("soil_depth", "At least 24 inches");
        bananasData.put("growing_season", "Year-round in tropical regions, growth slows in winter in subtropical areas");
        bananasData.put("description", "Bananas are tropical fruits growing in clusters on tall plants. They're rich in potassium and are one of the world's most popular fruits.");
        cropData.put("bananas", bananasData);
        
        // Oranges data
        Map<String, Object> orangesData = new HashMap<>();
        orangesData.put(CROP_NAME, "Oranges");
        orangesData.put(CROP_TYPE, "Fruit");
        orangesData.put(CROP_IMAGE, R.drawable.orange);
        orangesData.put("temperature", "15-29°C");
        orangesData.put("humidity", "50-80%");
        orangesData.put("sunlight", "Full sun exposure");
        orangesData.put("water", "Regular watering, especially during fruit development");
        orangesData.put("soil_type", "Well-draining, slightly acidic soil");
        orangesData.put("soil_ph", "5.5-6.5");
        orangesData.put("soil_nutrients", "Nitrogen, phosphorus, potassium, and micronutrients");
        orangesData.put("soil_depth", "Deep soil for extensive root system");
        orangesData.put("growing_season", "Spring blossoming, winter harvest in most regions");
        orangesData.put("description", "Oranges are citrus fruits known for their vitamin C content. They need warm climates and well-draining soil to thrive.");
        cropData.put("oranges", orangesData);
        
        // Grapes data
        Map<String, Object> grapesData = new HashMap<>();
        grapesData.put(CROP_NAME, "Grapes");
        grapesData.put(CROP_TYPE, "Fruit");
        grapesData.put(CROP_IMAGE, R.drawable.grapes);
        grapesData.put("temperature", "15-25°C");
        grapesData.put("humidity", "60-70%");
        grapesData.put("sunlight", "Full sun exposure");
        grapesData.put("water", "Moderate watering, reduced during ripening");
        grapesData.put("soil_type", "Well-draining, slightly alkaline soil");
        grapesData.put("soil_ph", "6.0-7.0");
        grapesData.put("soil_nutrients", "Balanced nutrients with emphasis on potassium");
        grapesData.put("soil_depth", "Deep soil for extensive root system");
        grapesData.put("growing_season", "Spring budding, fall harvest");
        grapesData.put("description", "Grapes are grown on vines and are used for fresh consumption, wine production, and dried as raisins. They need proper trellising and pruning.");
        cropData.put("grapes", grapesData);
        
        // Watermelon data
        Map<String, Object> watermelonData = new HashMap<>();
        watermelonData.put(CROP_NAME, "Watermelon");
        watermelonData.put(CROP_TYPE, "Fruit");
        watermelonData.put(CROP_IMAGE, R.drawable.watermelon);
        watermelonData.put("temperature", "21-29°C");
        watermelonData.put("humidity", "60-80%");
        watermelonData.put("sunlight", "Full sun exposure");
        watermelonData.put("water", "Regular watering, critical during fruit development");
        watermelonData.put("soil_type", "Sandy loam, well-draining soil");
        watermelonData.put("soil_ph", "6.0-7.0");
        watermelonData.put("soil_nutrients", "Rich in nitrogen early, potassium during fruiting");
        watermelonData.put("soil_depth", "At least 12 inches");
        watermelonData.put("growing_season", "Summer (warm season crop)");
        watermelonData.put("description", "Watermelon is a large fruit with juicy, sweet flesh and high water content. It's grown on vining plants that spread across the ground.");
        cropData.put("watermelon", watermelonData);
        
        // Strawberry data
        Map<String, Object> strawberryData = new HashMap<>();
        strawberryData.put(CROP_NAME, "Strawberry");
        strawberryData.put(CROP_TYPE, "Fruit");
        strawberryData.put(CROP_IMAGE, R.drawable.strawberry);
        strawberryData.put("temperature", "15-26°C");
        strawberryData.put("humidity", "60-75%");
        strawberryData.put("sunlight", "Full sun to partial shade");
        strawberryData.put("water", "Regular watering, keeping soil moist");
        strawberryData.put("soil_type", "Well-draining, sandy loam rich in organic matter");
        strawberryData.put("soil_ph", "5.5-6.5");
        strawberryData.put("soil_nutrients", "Balanced NPK with calcium");
        strawberryData.put("soil_depth", "6-12 inches");
        strawberryData.put("growing_season", "Spring to early summer for June-bearing; ever-bearing varieties produce throughout growing season");
        strawberryData.put("description", "Strawberries are sweet, red fruits that grow on low-growing perennial plants. They're often grown in raised beds or containers.");
        cropData.put("strawberry", strawberryData);
        
        // Pineapple data
        Map<String, Object> pineappleData = new HashMap<>();
        pineappleData.put(CROP_NAME, "Pineapple");
        pineappleData.put(CROP_TYPE, "Fruit");
        pineappleData.put(CROP_IMAGE, R.drawable.pineapple);
        pineappleData.put("temperature", "22-32°C");
        pineappleData.put("humidity", "70-80%");
        pineappleData.put("sunlight", "Full sun exposure");
        pineappleData.put("water", "Moderate watering, allowing soil to dry between waterings");
        pineappleData.put("soil_type", "Well-draining, sandy or loamy soil");
        pineappleData.put("soil_ph", "4.5-6.5");
        pineappleData.put("soil_nutrients", "Balanced nutrients with higher potassium");
        pineappleData.put("soil_depth", "At least 12 inches");
        pineappleData.put("growing_season", "Takes 18-24 months from planting to harvest");
        pineappleData.put("description", "Pineapples are tropical fruits that grow from the center of a rosette of spiky leaves. Each plant typically produces one fruit at a time.");
        cropData.put("pineapple", pineappleData);
        
        // Cherry data
        Map<String, Object> cherryData = new HashMap<>();
        cherryData.put(CROP_NAME, "Cherry");
        cherryData.put(CROP_TYPE, "Fruit");
        cherryData.put(CROP_IMAGE, R.drawable.placeholder_image);
        cherryData.put("temperature", "15-25°C");
        cherryData.put("humidity", "50-60%");
        cherryData.put("sunlight", "Full sun exposure");
        cherryData.put("water", "Regular watering, especially during fruit development");
        cherryData.put("soil_type", "Well-draining, deep loamy soil");
        cherryData.put("soil_ph", "6.0-7.0");
        cherryData.put("soil_nutrients", "Balanced, avoid excessive nitrogen");
        cherryData.put("soil_depth", "Deep soil for extensive root system");
        cherryData.put("growing_season", "Spring flowering, early summer harvest");
        cherryData.put("description", "Cherries are small stone fruits that grow on trees. They require a period of winter chill for proper fruiting and are sensitive to late spring frosts.");
        cropData.put("cherry", cherryData);
        
        // Kiwi data
        Map<String, Object> kiwiData = new HashMap<>();
        kiwiData.put(CROP_NAME, "Kiwi");
        kiwiData.put(CROP_TYPE, "Fruit");
        kiwiData.put(CROP_IMAGE, R.drawable.kiwi);
        kiwiData.put("temperature", "15-25°C");
        kiwiData.put("humidity", "60-80%");
        kiwiData.put("sunlight", "Full sun to partial shade");
        kiwiData.put("water", "Regular watering, keeping soil moist");
        kiwiData.put("soil_type", "Well-draining, fertile loam");
        kiwiData.put("soil_ph", "5.5-7.0");
        kiwiData.put("soil_nutrients", "Rich in organic matter");
        kiwiData.put("soil_depth", "Deep soil for extensive root system");
        kiwiData.put("growing_season", "Takes 3-5 years after planting to bear fruit; harvest in fall");
        kiwiData.put("description", "Kiwi fruits grow on woody vines that require support structures. Most varieties need both male and female plants for pollination and fruit production.");
        cropData.put("kiwi", kiwiData);
        
        // Papaya data
        Map<String, Object> papayaData = new HashMap<>();
        papayaData.put(CROP_NAME, "Papaya");
        papayaData.put(CROP_TYPE, "Fruit");
        papayaData.put(CROP_IMAGE, R.drawable.papaya);
        papayaData.put("temperature", "22-30°C");
        papayaData.put("humidity", "70-80%");
        papayaData.put("sunlight", "Full sun exposure");
        papayaData.put("water", "Regular watering, avoiding waterlogging");
        papayaData.put("soil_type", "Well-draining, rich in organic matter");
        papayaData.put("soil_ph", "5.5-7.0");
        papayaData.put("soil_nutrients", "Rich in nitrogen and potassium");
        papayaData.put("soil_depth", "At least 15 inches");
        papayaData.put("growing_season", "Year-round in tropical regions; fruits 9-12 months after planting");
        papayaData.put("description", "Papayas are tropical fruits with soft, orange flesh and black seeds. They grow on short-lived trees that produce fruit year-round in suitable climates.");
        cropData.put("papaya", papayaData);
        
        // Guava data
        Map<String, Object> guavaData = new HashMap<>();
        guavaData.put(CROP_NAME, "Guava");
        guavaData.put(CROP_TYPE, "Fruit");
        guavaData.put(CROP_IMAGE, R.drawable.guava);
        guavaData.put("temperature", "23-28°C");
        guavaData.put("humidity", "70-80%");
        guavaData.put("sunlight", "Full sun exposure");
        guavaData.put("water", "Regular watering, reduced during fruiting");
        guavaData.put("soil_type", "Well-draining, wide range of soil types");
        guavaData.put("soil_ph", "4.5-7.0");
        guavaData.put("soil_nutrients", "Moderate fertility requirements");
        guavaData.put("soil_depth", "At least 18 inches");
        guavaData.put("growing_season", "Main harvest in summer-fall, may produce year-round in some regions");
        guavaData.put("description", "Guavas are tropical fruits with aromatic flesh that can be white to pink or red. They're adapted to a wide range of soils and are relatively drought-tolerant once established.");
        cropData.put("guava", guavaData);
        
        // Onions data
        Map<String, Object> onionsData = new HashMap<>();
        onionsData.put(CROP_NAME, "Onions");
        onionsData.put(CROP_TYPE, "Vegetable");
        onionsData.put(CROP_IMAGE, R.drawable.onion);
        onionsData.put("temperature", "13-24°C");
        onionsData.put("humidity", "50-70%");
        onionsData.put("sunlight", "Full sun exposure");
        onionsData.put("water", "Regular watering, reduced as bulbs mature");
        onionsData.put("soil_type", "Well-draining, sandy loam");
        onionsData.put("soil_ph", "6.0-7.0");
        onionsData.put("soil_nutrients", "Moderate nitrogen, higher phosphorus and potassium");
        onionsData.put("soil_depth", "At least 6 inches");
        onionsData.put("growing_season", "Spring through summer, or fall through spring in warmer regions");
        onionsData.put("description", "Onions are bulb vegetables grown for their distinctive flavor. Day length affects bulb formation, so variety selection should match your region.");
        cropData.put("onions", onionsData);
        
        // Cucumber data
        Map<String, Object> cucumberData = new HashMap<>();
        cucumberData.put(CROP_NAME, "Cucumber");
        cucumberData.put(CROP_TYPE, "Vegetable");
        cucumberData.put(CROP_IMAGE, R.drawable.cucumber);
        cucumberData.put("temperature", "18-24°C");
        cucumberData.put("humidity", "70-90%");
        cucumberData.put("sunlight", "Full sun exposure");
        cucumberData.put("water", "Regular, consistent watering");
        cucumberData.put("soil_type", "Well-draining, fertile loam");
        cucumberData.put("soil_ph", "6.0-7.0");
        cucumberData.put("soil_nutrients", "Rich in organic matter");
        cucumberData.put("soil_depth", "At least 12 inches");
        cucumberData.put("growing_season", "Late spring through summer");
        cucumberData.put("description", "Cucumbers are vining plants producing crisp, refreshing fruits. They can be grown on trellises to save space and produce straighter fruits.");
        cropData.put("cucumber", cucumberData);
        
        // Broccoli data
        Map<String, Object> broccoliData = new HashMap<>();
        broccoliData.put(CROP_NAME, "Broccoli");
        broccoliData.put(CROP_TYPE, "Vegetable");
        broccoliData.put(CROP_IMAGE, R.drawable.placeholder_image);
        broccoliData.put("temperature", "15-21°C");
        broccoliData.put("humidity", "60-70%");
        broccoliData.put("sunlight", "Full sun exposure");
        broccoliData.put("water", "Regular watering, keeping soil consistently moist");
        broccoliData.put("soil_type", "Well-draining, fertile soil");
        broccoliData.put("soil_ph", "6.0-7.0");
        broccoliData.put("soil_nutrients", "Rich in nitrogen and organic matter");
        broccoliData.put("soil_depth", "At least 18 inches");
        broccoliData.put("growing_season", "Spring or fall, prefers cool temperatures");
        broccoliData.put("description", "Broccoli is a cool-season crop that forms edible flower heads. It's sensitive to heat, which can cause premature flowering (bolting).");
        cropData.put("broccoli", broccoliData);
        
        // Eggplant data
        Map<String, Object> eggplantData = new HashMap<>();
        eggplantData.put(CROP_NAME, "Eggplant");
        eggplantData.put(CROP_TYPE, "Vegetable");
        eggplantData.put(CROP_IMAGE, R.drawable.eggplant);
        eggplantData.put("temperature", "21-29°C");
        eggplantData.put("humidity", "60-70%");
        eggplantData.put("sunlight", "Full sun exposure");
        eggplantData.put("water", "Regular watering, keeping soil evenly moist");
        eggplantData.put("soil_type", "Well-draining, sandy loam rich in organic matter");
        eggplantData.put("soil_ph", "5.5-6.5");
        eggplantData.put("soil_nutrients", "Rich in nitrogen, phosphorus, and potassium");
        eggplantData.put("soil_depth", "At least 12 inches");
        eggplantData.put("growing_season", "Warm season, summer crop");
        eggplantData.put("description", "Eggplants are heat-loving vegetables with glossy fruits. They require warm temperatures for germination and growth, and are sensitive to cold.");
        cropData.put("eggplant", eggplantData);
        
        // Bell Pepper data
        Map<String, Object> bellPepperData = new HashMap<>();
        bellPepperData.put(CROP_NAME, "Bell Pepper");
        bellPepperData.put(CROP_TYPE, "Vegetable");
        bellPepperData.put(CROP_IMAGE, R.drawable.bellpepper);
        bellPepperData.put("temperature", "18-26°C");
        bellPepperData.put("humidity", "60-70%");
        bellPepperData.put("sunlight", "Full sun exposure");
        bellPepperData.put("water", "Regular watering, consistent moisture");
        bellPepperData.put("soil_type", "Well-draining, fertile soil");
        bellPepperData.put("soil_ph", "6.0-6.8");
        bellPepperData.put("soil_nutrients", "Rich in phosphorus and calcium");
        bellPepperData.put("soil_depth", "At least 18 inches");
        bellPepperData.put("growing_season", "Warm season, late spring through summer");
        bellPepperData.put("description", "Bell peppers are sweet, crisp vegetables that ripen from green to yellow, orange, or red. They're sensitive to temperature fluctuations and benefit from consistent care.");
        cropData.put("bellPepper", bellPepperData);

        // Spinach data
        Map<String, Object> spinachData = new HashMap<>();
        spinachData.put(CROP_NAME, "Spinach");
        spinachData.put(CROP_TYPE, "Vegetable");
        spinachData.put(CROP_IMAGE, R.drawable.spinach);
        spinachData.put("temperature", "10-21°C");
        spinachData.put("humidity", "50-70%");
        spinachData.put("sunlight", "Partial to full sun");
        spinachData.put("water", "Regular watering, keeping soil moist");
        spinachData.put("soil_type", "Well-draining, fertile loam");
        spinachData.put("soil_ph", "6.0-7.0");
        spinachData.put("soil_nutrients", "High in nitrogen and organic matter");
        spinachData.put("soil_depth", "At least 6 inches");
        spinachData.put("growing_season", "Cool seasons, spring and fall");
        spinachData.put("description", "Spinach is a leafy green vegetable rich in nutrients. It grows quickly in cool weather but bolts (flowers) when days get long and temperatures rise.");
        cropData.put("spinach", spinachData);
        
        // Lettuce data
        Map<String, Object> lettuceData = new HashMap<>();
        lettuceData.put(CROP_NAME, "Lettuce");
        lettuceData.put(CROP_TYPE, "Vegetable");
        lettuceData.put(CROP_IMAGE, R.drawable.lettuce);
        lettuceData.put("temperature", "10-22°C");
        lettuceData.put("humidity", "50-70%");
        lettuceData.put("sunlight", "Partial to full sun");
        lettuceData.put("water", "Regular watering, keeping soil consistently moist");
        lettuceData.put("soil_type", "Well-draining, fertile soil");
        lettuceData.put("soil_ph", "6.0-7.0");
        lettuceData.put("soil_nutrients", "Moderate nitrogen");
        lettuceData.put("soil_depth", "At least 6 inches");
        lettuceData.put("growing_season", "Cool seasons, spring and fall");
        lettuceData.put("description", "Lettuce is a cool-season crop with many varieties. It prefers cool temperatures and will bolt (flower) in heat, making the leaves bitter.");
        cropData.put("lettuce", lettuceData);
        
        // Cauliflower data
        Map<String, Object> cauliflowerData = new HashMap<>();
        cauliflowerData.put(CROP_NAME, "Cauliflower");
        cauliflowerData.put(CROP_TYPE, "Vegetable");
        cauliflowerData.put(CROP_IMAGE, R.drawable.cauliflower);
        cauliflowerData.put("temperature", "15-21°C");
        cauliflowerData.put("humidity", "60-70%");
        cauliflowerData.put("sunlight", "Full sun exposure");
        cauliflowerData.put("water", "Regular watering, consistent moisture");
        cauliflowerData.put("soil_type", "Well-draining, fertile soil");
        cauliflowerData.put("soil_ph", "6.0-7.0");
        cauliflowerData.put("soil_nutrients", "Rich in nitrogen, phosphorus, and potassium");
        cauliflowerData.put("soil_depth", "At least 18 inches");
        cauliflowerData.put("growing_season", "Cool seasons, spring and fall");
        cauliflowerData.put("description", "Cauliflower forms white heads that need to be protected from sunlight (blanched). It's a cool-season crop that's sensitive to temperature fluctuations.");
        cropData.put("cauliflower", cauliflowerData);
        
        // Peas data
        Map<String, Object> peasData = new HashMap<>();
        peasData.put(CROP_NAME, "Peas");
        peasData.put(CROP_TYPE, "Vegetable");
        peasData.put(CROP_IMAGE, R.drawable.peas);
        peasData.put("temperature", "13-20°C");
        peasData.put("humidity", "50-70%");
        peasData.put("sunlight", "Full sun to partial shade");
        peasData.put("water", "Regular watering, moderate needs");
        peasData.put("soil_type", "Well-draining, light soil");
        peasData.put("soil_ph", "6.0-7.0");
        peasData.put("soil_nutrients", "Low to moderate nitrogen (fixes own nitrogen)");
        peasData.put("soil_depth", "At least 12 inches");
        peasData.put("growing_season", "Cool seasons, early spring and fall");
        peasData.put("description", "Peas are cool-season legumes that improve soil by fixing nitrogen. They grow best on trellises and produce edible pods or seeds depending on variety.");
        cropData.put("peas", peasData);
        
        // Garlic data
        Map<String, Object> garlicData = new HashMap<>();
        garlicData.put(CROP_NAME, "Garlic");
        garlicData.put(CROP_TYPE, "Vegetable");
        garlicData.put(CROP_IMAGE, R.drawable.garlic);
        garlicData.put("temperature", "13-24°C");
        garlicData.put("humidity", "40-60%");
        garlicData.put("sunlight", "Full sun exposure");
        garlicData.put("water", "Regular watering, reduced as bulbs mature");
        garlicData.put("soil_type", "Well-draining, fertile soil");
        garlicData.put("soil_ph", "6.0-7.0");
        garlicData.put("soil_nutrients", "Rich in organic matter, moderate nitrogen");
        garlicData.put("soil_depth", "At least 6 inches");
        garlicData.put("growing_season", "Plant in fall for summer harvest in most regions");
        garlicData.put("description", "Garlic is planted as individual cloves that develop into full bulbs. It needs a period of cold dormancy (vernalization) to form proper bulbs.");
        cropData.put("garlic", garlicData);
        
        // Barley data
        Map<String, Object> barleyData = new HashMap<>();
        barleyData.put(CROP_NAME, "Barley (Jau)");
        barleyData.put(CROP_TYPE, "Grain");
        barleyData.put(CROP_IMAGE, R.drawable.barley);
        barleyData.put("temperature", "15-20°C");
        barleyData.put("humidity", "40-60%");
        barleyData.put("sunlight", "Full sun exposure");
        barleyData.put("water", "Moderate watering, drought tolerant once established");
        barleyData.put("soil_type", "Well-draining, loamy soil");
        barleyData.put("soil_ph", "6.0-7.0");
        barleyData.put("soil_nutrients", "Moderate nitrogen and phosphorus");
        barleyData.put("soil_depth", "At least 12 inches");
        barleyData.put("growing_season", "Spring or fall planting depending on variety");
        barleyData.put("description", "Barley is a versatile grain used for animal feed, human food, and beer production. It's more salt and drought tolerant than wheat.");
        cropData.put("barley", barleyData);
        
        // Oats data
        Map<String, Object> oatsData = new HashMap<>();
        oatsData.put(CROP_NAME, "Oats (Jai)");
        oatsData.put(CROP_TYPE, "Grain");
        oatsData.put(CROP_IMAGE, R.drawable.oats);
        oatsData.put("temperature", "15-21°C");
        oatsData.put("humidity", "60-70%");
        oatsData.put("sunlight", "Full sun exposure");
        oatsData.put("water", "Moderate watering, more than other grains");
        oatsData.put("soil_type", "Well-draining, loamy soil");
        oatsData.put("soil_ph", "5.0-6.5");
        oatsData.put("soil_nutrients", "Moderate fertility");
        oatsData.put("soil_depth", "At least 12 inches");
        oatsData.put("growing_season", "Cool season crop, spring or fall planting");
        oatsData.put("description", "Oats are cereal grains known for their nutritional value. They tolerate acidic soils better than other grains and are often used as a cover crop.");
        cropData.put("oats", oatsData);
        
        // Millet data
        Map<String, Object> milletData = new HashMap<>();
        milletData.put(CROP_NAME, "Pearl Millet (Bajra)");
        milletData.put(CROP_TYPE, "Grain");
        milletData.put(CROP_IMAGE, R.drawable.bajra);
        milletData.put("temperature", "20-30°C");
        milletData.put("humidity", "40-60%");
        milletData.put("sunlight", "Full sun exposure");
        milletData.put("water", "Drought resistant, low water requirements");
        milletData.put("soil_type", "Well-draining, sandy loam");
        milletData.put("soil_ph", "5.5-7.0");
        milletData.put("soil_nutrients", "Low fertility requirements");
        milletData.put("soil_depth", "At least 12 inches");
        milletData.put("growing_season", "Warm season crop, late spring planting");
        milletData.put("description", "Millet is a drought-resistant grain crop that matures quickly, often in 60-90 days. It's an important staple food in many parts of Africa and Asia.");
        cropData.put("millet", milletData);
        
        // Quinoa data
        Map<String, Object> quinoaData = new HashMap<>();
        quinoaData.put(CROP_NAME, "Quinoa");
        quinoaData.put(CROP_TYPE, "Grain");
        quinoaData.put(CROP_IMAGE, R.drawable.quinoa);
        quinoaData.put("temperature", "15-25°C");
        quinoaData.put("humidity", "40-60%");
        quinoaData.put("sunlight", "Full sun exposure");
        quinoaData.put("water", "Moderate initially, drought tolerant after establishment");
        quinoaData.put("soil_type", "Well-draining, sandy loam");
        quinoaData.put("soil_ph", "6.0-8.5");
        quinoaData.put("soil_nutrients", "Low fertility requirements");
        quinoaData.put("soil_depth", "At least 12 inches");
        quinoaData.put("growing_season", "Cool season crop in warm regions, warm season in cooler regions");
        quinoaData.put("description", "Quinoa is a pseudo-cereal known for its high protein content and complete amino acid profile. It's highly adaptable to different climates and tolerates salt, drought, and poor soils.");
        cropData.put("quinoa", quinoaData);
        
        // Sorghum data
        Map<String, Object> sorghumData = new HashMap<>();
        sorghumData.put(CROP_NAME, "Sorghum (Jowar)");
        sorghumData.put(CROP_TYPE, "Grain");
        sorghumData.put(CROP_IMAGE, R.drawable.sorghum);
        sorghumData.put("temperature", "20-30°C");
        sorghumData.put("humidity", "40-60%");
        sorghumData.put("sunlight", "Full sun exposure");
        sorghumData.put("water", "Drought resistant, efficient water use");
        sorghumData.put("soil_type", "Well-draining, tolerates wide range");
        sorghumData.put("soil_ph", "5.5-8.5");
        sorghumData.put("soil_nutrients", "Moderate fertility requirements");
        sorghumData.put("soil_depth", "Deep soil for extensive root system");
        sorghumData.put("growing_season", "Warm season crop, late spring planting");
        sorghumData.put("description", "Sorghum is a heat and drought-tolerant grain crop used for food, animal feed, and biofuel. It's the fifth most important cereal crop globally and highly adaptable to harsh conditions.");
        cropData.put("sorghum", sorghumData);
    }

    private void setupClickListeners() {
        tomatoesCard.setOnClickListener(v -> navigateToCropDetail("tomatoes"));
        wheatCard.setOnClickListener(v -> navigateToCropDetail("wheat"));
        applesCard.setOnClickListener(v -> navigateToCropDetail("apples"));
        riceCard.setOnClickListener(v -> navigateToCropDetail("rice"));
        potatoesCard.setOnClickListener(v -> navigateToCropDetail("potatoes"));
        mangoesCard.setOnClickListener(v -> navigateToCropDetail("mangoes"));
        cornCard.setOnClickListener(v -> navigateToCropDetail("corn"));
        carrotsCard.setOnClickListener(v -> navigateToCropDetail("carrots"));
        
        // New fruit click listeners
        bananasCard.setOnClickListener(v -> navigateToCropDetail("bananas"));
        orangesCard.setOnClickListener(v -> navigateToCropDetail("oranges"));
        grapesCard.setOnClickListener(v -> navigateToCropDetail("grapes"));
        watermelonCard.setOnClickListener(v -> navigateToCropDetail("watermelon"));
        strawberryCard.setOnClickListener(v -> navigateToCropDetail("strawberry"));
        pineappleCard.setOnClickListener(v -> navigateToCropDetail("pineapple"));
        cherryCard.setOnClickListener(v -> navigateToCropDetail("cherry"));
        kiwiCard.setOnClickListener(v -> navigateToCropDetail("kiwi"));
        papayaCard.setOnClickListener(v -> navigateToCropDetail("papaya"));
        guavaCard.setOnClickListener(v -> navigateToCropDetail("guava"));
        
        // New vegetable click listeners
        onionsCard.setOnClickListener(v -> navigateToCropDetail("onions"));
        cucumberCard.setOnClickListener(v -> navigateToCropDetail("cucumber"));
        broccoliCard.setOnClickListener(v -> navigateToCropDetail("broccoli"));
        eggplantCard.setOnClickListener(v -> navigateToCropDetail("eggplant"));
        bellPepperCard.setOnClickListener(v -> navigateToCropDetail("bellPepper"));
        spinachCard.setOnClickListener(v -> navigateToCropDetail("spinach"));
        lettuceCard.setOnClickListener(v -> navigateToCropDetail("lettuce"));
        cauliflowerCard.setOnClickListener(v -> navigateToCropDetail("cauliflower"));
        peasCard.setOnClickListener(v -> navigateToCropDetail("peas"));
        garlicCard.setOnClickListener(v -> navigateToCropDetail("garlic"));
        
        // New grain click listeners
        barleyCard.setOnClickListener(v -> navigateToCropDetail("barley"));
        oatsCard.setOnClickListener(v -> navigateToCropDetail("oats"));
        milletCard.setOnClickListener(v -> navigateToCropDetail("millet"));
        quinoaCard.setOnClickListener(v -> navigateToCropDetail("quinoa"));
        sorghumCard.setOnClickListener(v -> navigateToCropDetail("sorghum"));
    }

    private void navigateToCropDetail(String cropKey) {
        Map<String, Object> cropInfo = cropData.get(cropKey);
        if (cropInfo != null) {
            CropDetailFragment fragment = new CropDetailFragment();
            Bundle args = new Bundle();
            
            // Pass data to the detail fragment
            for (Map.Entry<String, Object> entry : cropInfo.entrySet()) {
                if (entry.getValue() instanceof String) {
                    args.putString(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Integer) {
                    args.putInt(entry.getKey(), (Integer) entry.getValue());
                }
            }
            
            fragment.setArguments(args);
            
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
} 