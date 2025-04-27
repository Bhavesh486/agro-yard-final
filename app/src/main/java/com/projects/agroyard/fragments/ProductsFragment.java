package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.projects.agroyard.R;
import com.projects.agroyard.adapters.ProductAdapter;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.models.Product;
import com.projects.agroyard.utils.FirestoreHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProductsFragment extends Fragment {
    private static final String TAG = "ProductsFragment";
    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Product> productList = new ArrayList<>();
    private static final String API_URL = Constants.DB_URL_BASE + "get_products.php";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_products, container, false);

        try {
            // Initialize views
            recyclerView = view.findViewById(R.id.products_recycler_view);
            swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

            // Setup RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            productAdapter = new ProductAdapter(getContext(), productList, this::onProductClick);
            recyclerView.setAdapter(productAdapter);

            // Setup SwipeRefreshLayout
            swipeRefreshLayout.setOnRefreshListener(this::fetchProducts);

            // Initial fetch
            fetchProducts();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Products: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error initializing Products: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return view;
    }

    private void fetchProducts() {
        swipeRefreshLayout.setRefreshing(true);

        FirestoreHelper.getAllProducts(new FirestoreHelper.ProductsCallback() {
            @Override
            public void onProductsLoaded(List<Map<String, Object>> products) {
                List<Product> newProducts = new ArrayList<>();
                
                for (Map<String, Object> productData : products) {
                    newProducts.add(new Product(productData));
                }
                
                Log.d(TAG, "Found " + newProducts.size() + " products in Firestore");
                
                requireActivity().runOnUiThread(() -> {
                    productList.clear();
                    productList.addAll(newProducts);
                    productAdapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);

                    if (newProducts.isEmpty()) {
                        Toast.makeText(getContext(), "No products available",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "Updated UI with " + newProducts.size() + " products");
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching products from Firestore: " + e.getMessage(), e);
                requireActivity().runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), 
                        "Error fetching products: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void onProductClick(Product product) {
        // Navigate to product details fragment
        ProductDetailFragment detailFragment = ProductDetailFragment.newInstance(product);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }
} 