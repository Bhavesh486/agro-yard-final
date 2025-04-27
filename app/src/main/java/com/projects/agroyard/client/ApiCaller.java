package com.projects.agroyard.client;

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
}
