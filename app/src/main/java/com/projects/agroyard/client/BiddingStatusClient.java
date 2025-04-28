package com.projects.agroyard.client;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BiddingStatusClient {
private static final String BASE_URL = "https://agro-yard-final.onrender.com/"; // Replace YOUR_SERVER_IP
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
