package com.projects.agroyard.client;

import com.projects.agroyard.models.ProductIdRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiCaller {

    public static void updateBidStatus(String productId) {
        BidStatusClient apiService = BiddingStatusClient.getClient().create(BidStatusClient.class);

        BidStatusRequest request = new BidStatusRequest(productId);

        Call<Void> call = apiService.updateBidStatus(request);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Success!
                    System.out.println("Bid Status Updated Successfully");
                } else {
                    System.err.println("Failed with code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public static void generateReciept(String productId) {
        BidStatusClient apiService = BiddingStatusClient.getClient().create(BidStatusClient.class);

        ProductIdRequest request = new ProductIdRequest(productId);

        apiService.saveReceipt(request).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    System.out.println("Receipt saved successfully!");
                } else {
                    System.out.println("Failed to save receipt: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                System.out.println("Error calling receipt API: " + t.getMessage());
            }
        });

    }
}
