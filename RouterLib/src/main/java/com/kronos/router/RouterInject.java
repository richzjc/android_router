package com.kronos.router;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TabHost;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import com.kronos.router.fragment.FragmentRouter;
import com.kronos.router.fragment.FragmentRouterManager;
import com.kronos.router.fragment.IFragmentRouter;
import com.kronos.router.fragment.SubFragmentRouters;
import com.kronos.router.fragment.SubFragmentType;
import com.kronos.router.utils.Const;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class RouterInject {

    public static void inject(FragmentActivity activity, Intent intent) {
        Bundle bundle = intent.getExtras();
        IActivityInject inject = getActivityInject(activity, bundle);
        if(inject != null){
            inject.inject(bundle);
        }


    }

    private static IActivityInject getActivityInject(FragmentActivity activity, Bundle bundle) {
        if(activity instanceof IActivityInject) {
            String fragmentUrl = bundle.getString(Const.FRGMENT_ROUTER, "");
            String activityUrl = bundle.getString(Const.ACTIVITY_ROUTER, "");
            String path = FragmentRouterManager.getInstance().getFragmentRouterPath(activityUrl, fragmentUrl);
            if(!TextUtils.isEmpty(path)){
                int index = 0;
                String nextFragmentUrl = "";
                String[] paths = path.split(",");
                if(paths.length > 0){
                    String subRouterUrl = paths[0];
                    List<String> routers = getListRouters(activity);
                    for(int i = 0; i < routers.size(); i++){
                        if(TextUtils.equals(subRouterUrl, routers.get(i))){
                            index = i;
                            nextFragmentUrl = getNextFragmentRouter(paths);
                            break;
                        }
                    }
                }
                bundle.putInt(Const.FRAGMENT_INDEX, index);
                bundle.putString(Const.FRGMENT_ROUTER, nextFragmentUrl);
                return (IActivityInject) activity;
            }
        }
        return null;
    }

    private static String getNextFragmentRouter(String[] paths){
        StringBuilder builder = new StringBuilder();
        for(int i = 1; i < paths.length; i++){
            builder.append(paths[i]).append(",");
        }
        String value = builder.toString();
        if(value.endsWith(",")){
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static List<String> getListRouters(Activity obj) {
        SubFragmentRouters fragmentRouters = obj.getClass().getAnnotation(SubFragmentRouters.class);
        List<String> routers = new ArrayList<>();
        if (fragmentRouters != null) {
            int type = fragmentRouters.fragmentType();
            if(type == SubFragmentType.TABHOST_FRRAGMENTS){
                View view = obj.findViewById(fragmentRouters.widgetId());
                if(!(view instanceof TabHost)){
                   throw new IllegalArgumentException("该控件不属于TabHost");
                }else{
                    getTabHostRouters((TabHost) view, routers);
                }
            }else if(type == SubFragmentType.VIEWPAGER_FRAGMENTS){
                View view = obj.findViewById(fragmentRouters.widgetId());
                if(!(view instanceof ViewPager)){
                    throw new IllegalArgumentException("该控件不属于ViewPager");
                }else{
                    getViewPagerRouters((ViewPager) view, fragmentRouters.filedName(), routers);
                }
            }else{
                throw new IllegalArgumentException("传入的fragmentType 目前不支持");
            }
        }
        return routers;
    }

    private static void getTabHostRouters(TabHost host, List<String> routers) {
        try {
            Method method = host.getClass().getDeclaredMethod("getTabs");
            if(method != null){
                method.setAccessible(true);
                Object obj = method.invoke(host);
                if(obj != null &&  obj instanceof List){
                    for(Object tabInfoObj : (List)obj){
                        Field clsField = tabInfoObj.getClass().getDeclaredField("clss");
                        clsField.setAccessible(true);
                        Class cls = (Class) clsField.get(tabInfoObj);
                        if(cls != null){
                            FragmentRouter fragmentRouter = (FragmentRouter) cls.getAnnotation(FragmentRouter.class);
                            if(fragmentRouter != null){
                                routers.add(fragmentRouter.url());
                            }else{
                                routers.add("");
                            }
                        }else{
                            routers.add("");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getViewPagerRouters(ViewPager viewPager, String fieldName, List<String> routers){

    }
}
