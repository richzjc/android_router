package com.kronos.router;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.text.TextUtils;
import android.util.Log;

import com.kronos.router.exception.ContextNotProvided;
import com.kronos.router.interceptor.RealCall;
import com.kronos.router.model.FragmentRouterModel;
import com.kronos.router.model.HostParams;
import com.kronos.router.model.RouterOptions;
import com.kronos.router.model.RouterParams;
import com.kronos.router.utils.Const;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static com.kronos.router.RouterInjectKt.inject;

public class Router {

    private static Router _router;

    public static Router sharedRouter() {  // 第一次检查
        if (_router == null) {
            synchronized (Router.class) {
                // 第二次检查
                if (_router == null) {
                    _router = new Router();
                }
            }
        }
        return _router;
    }


    private Application _context;
    private final Map<String, HostParams> hosts = new HashMap<>();
    private RouterLoader loader;
    private RealCall realCall;

    private Router() {
        loader = new RouterLoader();
        realCall = new RealCall(hosts);
    }

    public void attachApplication(Application context) {
        this._context = context;
        loader.attach(context);
        context.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Bundle bundle = activity.getIntent().getExtras();
                if(bundle != null){
                    String fragmentRouter =  bundle.getString(Const.FRGMENT_ROUTER, "");
                    if(!TextUtils.isEmpty(fragmentRouter) && activity instanceof FragmentActivity){
                        inject((FragmentActivity) activity, activity.getIntent());
                    }
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    private Application getContext() {
        return this._context;
    }

    public static void map(String url, RouterCallback callback) {
        RouterOptions options = new RouterOptions();
        options.setCallback(callback);
        map(url, null, options);
    }

    public static void map(String url, Class<? extends Activity> mClass) {
        map(url, mClass, new RouterOptions());
    }

    public static void map(String url, Class<? extends Activity> mClass, @Nullable Class<? extends Fragment> targetFragment) {
        map(url, mClass, targetFragment, null);
    }

    public static void map(String url, Class<? extends Activity> mClass, FragmentRouterModel... fragmentRouterModels){
        map(url, mClass);
        RouterOptions routerOptions;
        Bundle bundle;
        for(FragmentRouterModel routerModel : fragmentRouterModels){
            routerOptions = new RouterOptions();
            bundle = new Bundle();
            bundle.putBoolean(Const.IS_FRAGMENT_ROUTER, true);
            bundle.putString(Const.FRAGMENT_ROUTER_PATH, routerModel.path);
            routerOptions.setDefaultParams(bundle);
            map(routerModel.fragmentRouterUrl, mClass, routerOptions);
        }
    }

    public static void map(String url, Class<? extends Activity> mClass, @Nullable Class<? extends Fragment> targetFragment,
                           Bundle bundle) {
        RouterOptions options = new RouterOptions(bundle);
        assert targetFragment != null;
        options.putParams("target", targetFragment.getName());
        map(url, mClass, options);
    }


    public static void map(String url, Class<? extends Activity> mClass, RouterOptions options) {
        if (options == null) {
            options = new RouterOptions();
        }
        Uri uri = Uri.parse(url);
        options.setOpenClass(mClass);
        HostParams hostParams;
        if (sharedRouter().hosts.containsKey(uri.getHost())) {
            hostParams = sharedRouter().hosts.get(uri.getHost());
        } else {
            hostParams = new HostParams(uri.getHost());
            sharedRouter().hosts.put(hostParams.getHost(), hostParams);
        }
        hostParams.setRoute(uri.getPath(), options);
    }


    public void openExternal(String url) {
        this.openExternal(url, this._context);
    }


    public void openExternal(String url, Context context) {
        this.openExternal(url, null, context);
    }

    public void openExternal(String url, Bundle extras) {
        this.openExternal(url, extras, this._context);
    }


    public void openExternal(String url, Bundle extras, Context context) {
        if (context == null) {
            throw new ContextNotProvided("You need to supply a context for Router "
                    + this.toString());
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        this.addFlagsToIntent(intent, context);
        if (extras != null) {
            intent.putExtras(extras);
        }
        context.startActivity(intent);
    }

    public void open(String url) {
        this.open(url, this._context);
    }

    public void open(String url, Bundle extras) {
        this.open(url, extras, this._context);
    }

    public void open(String url, Context context) {
        this.open(url, null, context);
    }

    public void open(String url, Bundle extras, Context context) {
        if (context == null) {
            throw new ContextNotProvided("You need to supply a context for Router " + this.toString());
        }
        RouterParams params = realCall.open(url);
        RouterOptions options = params.getRouterOptions();
        if (options.getCallback() != null) {
            RouterContext routeContext = new RouterContext(params.getOpenParams(), extras, context);
            options.getCallback().run(routeContext);
            return;
        }

        if(options.getDefaultParams() != null){
            boolean IS_FRAGMENT_ROUTER = options.getDefaultParams().getBoolean(Const.IS_FRAGMENT_ROUTER, false);
            if(IS_FRAGMENT_ROUTER){
                Log.i("fragmentUrl", url);
                String FRAGMENT_ROUTER_PATH = options.getDefaultParams().getString(Const.FRAGMENT_ROUTER_PATH, "");
                String[] paths = FRAGMENT_ROUTER_PATH.split(",");
                if(paths.length > 0){
                    String lastPath = paths[paths.length - 1];
                    Uri parsedUri = Uri.parse(lastPath);
                    String urlPath = TextUtils.isEmpty(parsedUri.getPath()) ? "" : parsedUri.getPath().substring(1);
                    parsedUri = Uri.parse(url);
                    String host = parsedUri.getHost();
                    int hostIndex = url.indexOf(host);
                    String realPath = url.substring(hostIndex + host.length() + 1);
                    int index = lastPath.lastIndexOf(urlPath);
                    lastPath = lastPath.substring(0, index) + realPath;
                    StringBuilder builder = new StringBuilder();
                    for(int i = 0; i < paths.length - 1; i++){
                        builder.append(paths[i]).append(",");
                    }
                    builder.append(lastPath);
                    FRAGMENT_ROUTER_PATH = builder.toString();
                }
                if(extras == null)
                    extras = new Bundle();
                extras.putString(Const.FRGMENT_ROUTER, FRAGMENT_ROUTER_PATH);
            }
        }

        Intent intent = this.intentFor(context, params);
        if (intent == null) {
            // Means the options weren't opening a new activity
            return;
        }
        if (extras != null) {
            intent.putExtras(extras);
        } else {
            Bundle bundle = new Bundle();
            intent.putExtras(bundle);
        }
        if (!(context instanceof Activity)) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    private void addFlagsToIntent(Intent intent, Context context) {
        if (context == this._context) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
    }

    private Intent intentFor(RouterParams params) {
        RouterOptions options = params.getRouterOptions();
        Intent intent = new Intent();
        assert options != null;
        if (options.getDefaultParams() != null) {
            intent.putExtras(options.getDefaultParams());
        }
        for (Entry<String, String> entry : params.getOpenParams().entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        return intent;
    }


    public boolean isCallbackUrl(String url) {
        RouterParams params = realCall.open(url);
        RouterOptions options = params.getRouterOptions();
        return options.getCallback() != null;
    }


    public Intent intentFor(Context context, String url) {
        RouterParams params = realCall.open(url);
        return intentFor(context, params);
    }

    private Intent intentFor(Context context, RouterParams params) {
        RouterOptions options = params.getRouterOptions();
        if (options.getCallback() != null) {
            return null;
        }

        Intent intent = intentFor(params);
        intent.setClass(context, options.getOpenClass());
        this.addFlagsToIntent(intent, context);
        return intent;
    }


    public boolean isLoadingFinish() {
        return loader.isLoadingFinish();
    }

}
