package com.kronos.router;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TabHost;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.kronos.router.fragment.FragmentRouter;
import com.kronos.router.fragment.IFragmentRouter;
import com.kronos.router.fragment.SubFragmentRouters;
import com.kronos.router.fragment.SubFragmentType;
import com.kronos.router.utils.Const;
import com.kronos.router.utils.ReflectUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Predicate;

public class RouterInject {

    public static void inject(FragmentActivity activity, Intent intent) {
        Bundle bundle = intent.getExtras();
        getActivityInject(activity, bundle);
        int index = bundle.getInt(Const.FRAGMENT_INDEX, -1);
        if (index >= 0) {
            setCurrentItem(activity, bundle);
        }
    }

    private static void getActivityInject(FragmentActivity activity, Bundle bundle) {
        if (activity != null && bundle != null) {
            String path = bundle.getString(Const.FRGMENT_ROUTER, "");
            if (!TextUtils.isEmpty(path)) {
                int index = -1;
                String nextFragmentUrl = "";
                String[] paths = path.split(",");
                if (paths.length > 0) {
                    String subRouterUrl = paths[0];
                    List<String> routers = getListRouters(activity);
                    for (int i = 0; i < routers.size(); i++) {
                        if (TextUtils.equals(subRouterUrl, routers.get(i))) {
                            index = i;
                            nextFragmentUrl = getNextFragmentRouter(paths);
                            break;
                        }
                    }
                }
                bundle.putInt(Const.FRAGMENT_INDEX, index);
                bundle.putString(Const.FRGMENT_ROUTER, nextFragmentUrl);
            }
        }
    }

    private static String getNextFragmentRouter(String[] paths) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < paths.length; i++) {
            builder.append(paths[i]).append(",");
        }
        String value = builder.toString();
        if (value.endsWith(",")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static List<String> getListRouters(Activity obj) {
        SubFragmentRouters fragmentRouters = obj.getClass().getAnnotation(SubFragmentRouters.class);
        List<String> routers = new ArrayList<>();
        if (fragmentRouters != null) {
            int type = fragmentRouters.fragmentType();
            if (type == SubFragmentType.TABHOST_FRRAGMENTS) {
                View view = obj.findViewById(ReflectUtil.getId(obj, "id", fragmentRouters.widgetIdName()));
                if (!(view instanceof TabHost)) {
                    throw new IllegalArgumentException("该控件不属于TabHost");
                } else {
                    getTabHostRouters((TabHost) view, routers);
                }
            } else if (type == SubFragmentType.VIEWPAGER_FRAGMENTS) {
                View view = obj.findViewById(ReflectUtil.getId(obj, "id", fragmentRouters.widgetIdName()));
                if (!(view instanceof ViewPager)) {
                    throw new IllegalArgumentException("该控件不属于ViewPager");
                } else {
                    getViewPagerRouters((ViewPager) view, fragmentRouters.filedName(), routers);
                }
            } else {
                throw new IllegalArgumentException("传入的fragmentType 目前不支持");
            }
        }
        return routers;
    }

    private static void getTabHostRouters(TabHost host, List<String> routers) {
        try {
            Method method = host.getClass().getDeclaredMethod("getTabs");
            if (method != null) {
                method.setAccessible(true);
                Object obj = method.invoke(host);
                if (obj != null && obj instanceof List) {
                    for (Object tabInfoObj : (List) obj) {
                        Field clsField = tabInfoObj.getClass().getDeclaredField("clss");
                        clsField.setAccessible(true);
                        Class cls = (Class) clsField.get(tabInfoObj);
                        if (cls != null) {
                            FragmentRouter fragmentRouter = (FragmentRouter) cls.getAnnotation(FragmentRouter.class);
                            if (fragmentRouter != null) {
                                routers.add(fragmentRouter.url());
                            } else {
                                routers.add("");
                            }
                        } else {
                            routers.add("");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getViewPagerRouters(ViewPager viewPager, String fieldName, List<String> routers) {
        PagerAdapter pagerAdapter = viewPager.getAdapter();
        if (pagerAdapter != null) {
            try {
                Field field = pagerAdapter.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                List list = (List) field.get(pagerAdapter);
                for (Object obj : list) {
                    if (obj instanceof IFragmentRouter) {
                        String url = ((IFragmentRouter) obj).getFragmentRouter();
                        if (!TextUtils.isEmpty(url)) {
                            routers.add(url);
                        } else {
                            parseAnnotation(obj, routers);
                        }
                    } else {
                        parseAnnotation(obj, routers);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void parseAnnotation(Object obj, List<String> routers) {
        FragmentRouter fragmentRouter = obj.getClass().getAnnotation(FragmentRouter.class);
        if (fragmentRouter != null) {
            String url = fragmentRouter.url();
            if (!TextUtils.isEmpty(url)) {
                routers.add(url);
            } else {
                routers.add("");
            }
        } else {
            routers.add("");
        }
    }


    public static void inject(Fragment fragment, Bundle bundle) {
        getFragmentInject(fragment, bundle);
        int index = bundle.getInt(Const.FRAGMENT_INDEX, -1);
        if (index >= 0) {
            setCurrentItem(fragment, bundle);
        }
    }

    private static void getFragmentInject(Fragment fragment, Bundle bundle) {
        if (fragment != null && bundle != null) {
            String path = bundle.getString(Const.FRGMENT_ROUTER, "");
            if (!TextUtils.isEmpty(path)) {
                int index = -1;
                String nextFragmentUrl = "";
                String[] paths = path.split(",");
                if (paths.length > 0) {
                    String subRouterUrl = paths[0];
                    List<String> routers = getListRouters(fragment);
                    for (int i = 0; i < routers.size(); i++) {
                        if (TextUtils.equals(subRouterUrl, routers.get(i))) {
                            index = i;
                            nextFragmentUrl = getNextFragmentRouter(paths);
                            break;
                        }
                    }
                }
                bundle.putInt(Const.FRAGMENT_INDEX, index);
                bundle.putString(Const.FRGMENT_ROUTER, nextFragmentUrl);
            }
        }
    }

    private static List<String> getListRouters(Fragment obj) {
        SubFragmentRouters fragmentRouters = obj.getClass().getAnnotation(SubFragmentRouters.class);
        List<String> routers = new ArrayList<>();
        if (fragmentRouters != null) {
            int type = fragmentRouters.fragmentType();
            if (type == SubFragmentType.TABHOST_FRRAGMENTS) {
                View view = obj.getView().findViewById(ReflectUtil.getId(obj.getContext(), "id", fragmentRouters.widgetIdName()));
                if (!(view instanceof TabHost)) {
                    throw new IllegalArgumentException("该控件不属于TabHost");
                } else {
                    getTabHostRouters((TabHost) view, routers);
                }
            } else if (type == SubFragmentType.VIEWPAGER_FRAGMENTS) {
                View view = obj.getView().findViewById(ReflectUtil.getId(obj.getContext(), "id", fragmentRouters.widgetIdName()));
                if (!(view instanceof ViewPager)) {
                    throw new IllegalArgumentException("该控件不属于ViewPager");
                } else {
                    getViewPagerRouters((ViewPager) view, fragmentRouters.filedName(), routers);
                }
            } else {
                throw new IllegalArgumentException("传入的fragmentType 目前不支持");
            }
        }
        return routers;
    }


    private static void setCurrentItem(Object obj, Bundle bundle) {
        int index = bundle.getInt(Const.FRAGMENT_INDEX, -1);
        SubFragmentRouters fragmentRouters = obj.getClass().getAnnotation(SubFragmentRouters.class);
        View view = null;
        if (fragmentRouters != null) {
            if (obj instanceof Activity)
                view = ((Activity) obj).findViewById(ReflectUtil.getId((Context) obj, "id", fragmentRouters.widgetIdName()));
            else if (obj instanceof Fragment)
                view = ((Fragment) obj).getView().findViewById(ReflectUtil.getId(((Fragment) obj).getContext(), "id", fragmentRouters.widgetIdName()));
        }

        if (view instanceof TabHost) {
            ((TabHost) view).setCurrentTab(index);
            getTabHostFragment((TabHost) view, bundle);
        } else if (view instanceof ViewPager) {
            ((ViewPager) view).setCurrentItem(index);
            getViewPagerFragment((ViewPager) view, fragmentRouters.filedName(), bundle);
        }
    }

    private static void getTabHostFragment(TabHost host, final Bundle bundle) {
        try {
            int index = bundle.getInt(Const.FRAGMENT_INDEX, -1);
            bundle.remove(Const.FRAGMENT_INDEX);
            Method method = host.getClass().getDeclaredMethod("getTabs");
            if (method != null) {
                method.setAccessible(true);
                Object obj = method.invoke(host);
                if (obj != null && obj instanceof List) {
                    final Object tabInfo = ((List) obj).get(index);
                    final Field fragmentField = tabInfo.getClass().getDeclaredField("fragment");
                    fragmentField.setAccessible(true);
                    Object fragment = fragmentField.get(tabInfo);
                    if (fragment != null) {
                        inject((Fragment) fragment, bundle);
                    }else{
                        Observable.interval(100, TimeUnit.MILLISECONDS)
                                .takeUntil(new Predicate<Long>() {
                                    @Override
                                    public boolean test(Long aLong) throws Exception {
                                        return fragmentField.get(tabInfo) != null || aLong > 100;
                                    }
                                })
                                .doFinally(new Action() {
                                    @Override
                                    public void run() throws Exception {
                                        Object obj = fragmentField.get(tabInfo);
                                        if(obj != null)
                                            inject((Fragment) obj, bundle);
                                    }
                                })
                                .subscribe();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getViewPagerFragment(ViewPager viewPager, String fieldName, Bundle bundle) {
        final PagerAdapter pagerAdapter = viewPager.getAdapter();
        if (pagerAdapter != null) {
            try {
                final Field field = pagerAdapter.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                List list = (List) field.get(pagerAdapter);
                int index = bundle.getInt(Const.FRAGMENT_INDEX, -1);
                bundle.remove(Const.FRAGMENT_INDEX);
                final Object fragment = list.get(index);
                if (fragment != null) {
                    inject((Fragment) fragment, bundle);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
