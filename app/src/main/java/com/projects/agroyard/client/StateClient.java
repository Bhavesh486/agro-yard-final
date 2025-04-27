package com.projects.agroyard.client;

import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.model.StatesModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface StateClient {
    @GET(Constants.STATE_DIST_URL)
    Call<StatesModel> getStateDistrict();
}
