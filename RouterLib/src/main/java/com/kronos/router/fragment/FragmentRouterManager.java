package com.kronos.router.fragment;

import com.kronos.router.model.FragmentRouterModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FragmentRouterManager {

    private HashMap<String, List<FragmentRouterModel>> map;
    private static volatile FragmentRouterManager instance;

    public static FragmentRouterManager getInstance() {
        if (instance == null) {
            synchronized (FragmentRouterManager.class) {
                if (instance == null)
                    instance = new FragmentRouterManager();
            }
        }
        return instance;
    }

    private FragmentRouterManager() {
        map = new HashMap<>();
    }

    public void put(String url, FragmentRouterModel[] routerModels) {
        if (map.containsKey(url)) {
            for (int i = 0; i < routerModels.length; i++) {
                map.get(url).add(routerModels[i]);
            }
        } else {
            List<FragmentRouterModel> list = new ArrayList<>();
            for (int i = 0; i < routerModels.length; i++) {
                map.get(url).add(routerModels[i]);
            }
            map.put(url, list);
        }
    }
}
