package com.projects.agroyard.client.callback;

import java.util.List;
import java.util.Map;

public interface StateDataCallback {
    void onStateDataFetched(Map<String, List<String>> statesDistMap);
}