package com.kronos.router.fragment;

import android.text.TextUtils;
import androidx.fragment.app.Fragment;
import java.util.HashMap;
import java.util.Map;

public class FragmentRouterManager {

    private HashMap<String, Fragment> map;
    private static volatile FragmentRouterManager instance;
    public static FragmentRouterManager getInstance(){
        if(instance == null){
            synchronized (FragmentRouterManager.class){
                if(instance == null)
                    instance = new FragmentRouterManager();
            }
        }
        return instance;
    }

    private FragmentRouterManager(){
        map = new HashMap<>();
    }

    public void registerFragmentRouter(Fragment fragment){
        if(fragment != null){
           FragmentRouter router = fragment.getClass().getAnnotation(FragmentRouter.class);
           if(router != null && !TextUtils.isEmpty(router.url())){
               if(!map.containsKey(router.url())){
                   map.put(router.url(), fragment);
               }
           }else if(fragment instanceof IFragmentRouter){
               String url = ((IFragmentRouter) fragment).getFragmentRouter();
               if(!TextUtils.isEmpty(url)){
                   map.put(url, fragment);
               }
           }
        }
    }

    public Fragment getFragmentByRouter(String fragmentRouter){
        return map.get(fragmentRouter);
    }

    public void unRegisterFragmentRouter(Fragment fragment){
        for(Map.Entry entry : map.entrySet()){
            if(entry.getValue() == fragment){
                map.remove(entry.getKey());
                break;
            }
        }
    }
}
