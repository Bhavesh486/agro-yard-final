package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.projects.agroyard.R;
import com.projects.agroyard.adapters.BidProductAdapter;
import com.projects.agroyard.models.BidModel;
import com.projects.agroyard.utils.RealTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class BiddingFragment extends Fragment {

    private RecyclerView bidContainer;
    private List<BidModel> combinedList;
    private BidProductAdapter bidProductAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bidding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
//        loadBids();

        DatabaseReference biddersRef = FirebaseDatabase.getInstance("https://agro-yard-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("bids");
        biddersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loadBids(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //Nothing
            }
        });

    }

    private void initializeViews(View view) {
        bidContainer = view.findViewById(R.id.product_cards_container_rec);
        combinedList = new ArrayList<>();
        bidProductAdapter = new BidProductAdapter(combinedList);

        // Set LayoutManager for RecyclerView
        bidContainer.setLayoutManager(new LinearLayoutManager(getContext()));
        bidContainer.setAdapter(bidProductAdapter);
    }

    private void loadBids(DataSnapshot snapshot) {
        RealTimeUtils.getBids(snapshot, (success, bidModel) -> {
            if (success) {
                // Notify adapter that the data has been loaded and changed
                combinedList.clear();
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                List<BidModel> filteredList = new java.util.ArrayList<>();
                for (BidModel model : bidModel) {
                    if ((model.getBidders() != null && model.getBidders().containsKey(currentUserId)) || "Starting".equalsIgnoreCase(model.getStatus())) {
                        filteredList.add(model);
                    }
                }

                combinedList.addAll(filteredList);
                bidProductAdapter.notifyDataSetChanged();
            } else {
                // Handle failure case when bids could not be fetched
                Log.d("BiddingFragment", "Failed to load bids");
            }
        });
    }
}
