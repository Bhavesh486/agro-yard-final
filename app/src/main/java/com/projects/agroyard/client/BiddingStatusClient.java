package com.projects.agroyard.client;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BiddingStatusClient {
private static final String BASE_URL = "http://192.168.180.130:3000/"; // Replace YOUR_SERVER_IP
private static Retrofit retrofit = null;

public static Retrofit getClient() {
    if (retrofit == null) {
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
    return retrofit;
}
}
