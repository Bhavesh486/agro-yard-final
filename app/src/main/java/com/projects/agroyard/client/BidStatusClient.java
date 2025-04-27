package com.projects.agroyard.client;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface BidStatusClient {
    @Headers("Content-Type: application/json")
    @POST("/updateBidStatus")
    Call<Void> updateBidStatus(@Body BidStatusRequest request);
}
