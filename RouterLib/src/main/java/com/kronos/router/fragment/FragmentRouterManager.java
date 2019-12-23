package com.kronos.router.fragment;

import com.kronos.router.model.FragmentRouterModel;
import java.util.HashMap;
import java.util.Map;

public class FragmentRouterManager {

    private HashMap<String, Map<String, FragmentRouterModel>> map;
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
            Map<String, FragmentRouterModel> subMap = map.get(url);
            for (int i = 0; i < routerModels.length; i++) {
                if(!subMap.containsKey(routerModels[i].fragmentRouterUrl)){
                   subMap.put(routerModels[i].fragmentRouterUrl, routerModels[i]);
                }
            }
        } else {
            Map<String, FragmentRouterModel> subMap = new HashMap<>();
            for (int i = 0; i < routerModels.length; i++) {
                subMap.put(routerModels[i].fragmentRouterUrl, routerModels[i]);
            }
            map.put(url, subMap);
        }
    }
}
