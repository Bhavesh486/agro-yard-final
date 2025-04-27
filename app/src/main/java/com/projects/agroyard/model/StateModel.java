package com.projects.agroyard.model;

import java.util.List;

public class StateModel {
    private String state;
    private List<String> districts;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<String> getDistricts() {
        return districts;
    }

    public void setDistricts(List<String> districts) {
        this.districts = districts;
    }
}
