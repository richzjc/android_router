package com.kronos.router

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.FrameLayout
import android.widget.TabHost
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager
import com.kronos.router.fragment.FragmentRouter
import com.kronos.router.fragment.IFragmentRouter
import com.kronos.router.fragment.SubFragmentRouters
import com.kronos.router.fragment.SubFragmentType
import com.kronos.router.utils.Const
import com.kronos.router.utils.ReflectUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

fun inject(activity: FragmentActivity?, intent: Intent?) {
    activity ?: return

    MainScope().launch {
        val isReady = checkIsReady(activity)
        if (isReady)
            realInjectActivity(activity, intent)
        else {
            var flagIndex = 0
            do {
                delay(100)
                flagIndex += 1
            } while (!(checkIsReady(activity) || flagIndex > 100))

            if (checkIsReady(activity))
                realInjectActivity(activity, intent)
        }
    }
}

private suspend fun realInjectActivity(activity: FragmentActivity, intent: Intent?) {
    val bundle = intent?.extras
    getActivityInject(activity, bundle)
    val index = bundle?.getInt(Const.FRAGMENT_INDEX, -1) ?: -1
    if (index >= 0) {
        setCurrentItem(activity, bundle ?: Bundle())
    }
}

private fun setCurrentItem(obj: Any, bundle: Bundle) {
    val index = bundle.getInt(Const.FRAGMENT_INDEX, -1)
    val fragmentRouters = obj.javaClass.getAnnotation(SubFragmentRouters::class.java)
    var view: View? = null
    if (fragmentRouters != null) {
        if (obj is Activity)
            view = obj.findViewById(ReflectUtil.getId(obj as Context, "id", fragmentRouters.widgetIdName))
        else if (obj is Fragment)
            view = obj.view!!.findViewById(ReflectUtil.getId(obj.context, "id", fragmentRouters.widgetIdName))
    }
    if (view is TabHost) {
        view.currentTab = index
        getTabHostFragment(view, bundle)
    } else if (view is ViewPager) {
        view.currentItem = index
        getViewPagerFragment(view, fragmentRouters!!.filedName, bundle)
    }
}

private fun getViewPagerFragment(viewPager: ViewPager, fieldName: String, bundle: Bundle) {
    val pagerAdapter = viewPager.adapter
    if (pagerAdapter != null) {
        try {
            val field = pagerAdapter.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            val list = field[pagerAdapter] as List<*>
            val index = bundle.getInt(Const.FRAGMENT_INDEX, -1)
            bundle.remove(Const.FRAGMENT_INDEX)
            val fragment = list[index]
            if (fragment != null) {
                inject(fragment as? Fragment, bundle)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}

private fun getTabHostFragment(host: TabHost, bundle: Bundle) {
    try {
        val index = bundle.getInt(Const.FRAGMENT_INDEX, -1)
        bundle.remove(Const.FRAGMENT_INDEX)
        val method = host.javaClass.getDeclaredMethod("getTabs")
        if (method != null) {
            method.isAccessible = true
            val obj = method.invoke(host)
            if (obj != null && obj is List<*>) {
                val tabInfo = obj[index]!!
                val fragmentField = tabInfo.javaClass.getDeclaredField("fragment")
                fragmentField.isAccessible = true
                val fragment = fragmentField[tabInfo]
                if (fragment != null) {
                    inject(fragment as Fragment, bundle)
                }
            }
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
}

fun inject(fragment: Fragment?, bundle: Bundle?) {
    fragment ?: return
    MainScope().launch {
        val isReady = checkIsReady(fragment)
        if (isReady) {
            realInjectFragment(fragment, bundle ?: Bundle())
        } else {
            var flagIndex = 0
            do {
                delay(100)
                flagIndex += 1
            } while (!(checkIsReady(fragment) || flagIndex > 100))

            if (checkIsReady(fragment))
                realInjectFragment(fragment, bundle ?: Bundle())
        }
    }
}

private suspend fun realInjectFragment(fragment: Fragment, bundle: Bundle) {
    getFragmentInject(fragment, bundle)
    val index = bundle.getInt(Const.FRAGMENT_INDEX, -1)
    if (index >= 0) {
        setCurrentItem(fragment, bundle)
    }
}

private suspend fun getFragmentInject(fragment: Fragment?, bundle: Bundle?) {
    if (fragment != null && bundle != null) {
        val path = bundle.getString(Const.FRGMENT_ROUTER, "")
        if (!TextUtils.isEmpty(path)) {
            var index = -1
            var nextFragmentUrl: String? = ""
            val paths = path.split(",").toTypedArray()
            if (paths.isNotEmpty()) {
                val subRouterUrl = paths[0]
                val routers = getListRouters(fragment)
                for (i in routers!!.indices) {
                    if (TextUtils.equals(subRouterUrl, routers[i])) {
                        index = i
                        nextFragmentUrl = getNextFragmentRouter(paths)
                        break
                    }
                }
            }
            bundle.putInt(Const.FRAGMENT_INDEX, index)
            bundle.putString(Const.FRGMENT_ROUTER, nextFragmentUrl)
        }
    }
}

private suspend fun getListRouters(obj: Fragment): List<String>? {
    val routers = mutableListOf<String>()
    val fragmentRouters = obj.javaClass.getAnnotation(SubFragmentRouters::class.java)
    if (fragmentRouters != null) {
        val type: Int = fragmentRouters.fragmentType
        if (type == SubFragmentType.TABHOST_FRRAGMENTS) {
            val view = obj.view!!.findViewById<View>(ReflectUtil.getId(obj.context, "id", fragmentRouters.widgetIdName))
            require(view is TabHost) { "该控件不属于TabHost" }
            getTabHostRouters(view, routers)
        } else if (type == SubFragmentType.VIEWPAGER_FRAGMENTS) {
            val view = obj.view!!.findViewById<View>(ReflectUtil.getId(obj.context, "id", fragmentRouters.widgetIdName))
            require(view is ViewPager) { "该控件不属于ViewPager" }
            getViewPagerRouters(view, fragmentRouters.filedName, routers)
        } else {
            throw java.lang.IllegalArgumentException("传入的fragmentType 目前不支持")
        }
    }
    return routers
}

private fun checkIsReady(fragment: Fragment?): Boolean {
    return if (fragment == null || fragment.view == null) false else {
        val fragmentRouters = fragment.javaClass.getAnnotation(SubFragmentRouters::class.java)
        if (fragmentRouters == null) true else {
            when (fragmentRouters.fragmentType) {
                SubFragmentType.TABHOST_FRRAGMENTS -> {
                    isReadyTabHost(fragment, fragmentRouters)
                }
                SubFragmentType.VIEWPAGER_FRAGMENTS -> {
                    isReadyViewPager(fragment, fragmentRouters)
                }
                else -> {
                    true
                }
            }
        }
    }
}

private fun isReadyViewPager(fragment: Fragment, fragmentRouters: SubFragmentRouters): Boolean {
    val view = fragment.view!!.findViewById<View>(ReflectUtil.getId(fragment.context, "id", fragmentRouters.widgetIdName))
    return if (view !is ViewPager) {
        true
    } else {
        val pagerAdapter = view.adapter
        if (pagerAdapter != null) {
            try {
                val field = pagerAdapter.javaClass.getDeclaredField(fragmentRouters.filedName)
                field.isAccessible = true
                val list = field[pagerAdapter] as List<*>
                list != null && list.isNotEmpty()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                true
            }
        } else {
            false
        }
    }
}

private fun isReadyTabHost(fragment: Fragment, fragmentRouters: SubFragmentRouters): Boolean {
    val view = fragment.view!!.findViewById<View>(ReflectUtil.getId(fragment.context, "id", fragmentRouters.widgetIdName))
    return if (view !is TabHost) {
        true
    } else {
        try {
            val method = view.javaClass.getDeclaredMethod("getTabs")
            if (method != null) {
                method.isAccessible = true
                val obj = method.invoke(view)
                obj is List<*> && obj.size > 0
            } else {
                true
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            true
        }
    }
}

private suspend fun getActivityInject(activity: FragmentActivity?, bundle: Bundle?) {
    if (activity != null && bundle != null) {
        val path = bundle.getString(Const.FRGMENT_ROUTER, "")
        if (!TextUtils.isEmpty(path)) {
            var index = -1
            var nextFragmentUrl: String? = ""
            val paths = path.split(",").toTypedArray()
            if (paths.isNotEmpty()) {
                val subRouterUrl = paths[0]
                val routers = getListRouters(activity)
                for (i in routers!!.indices) {
                    if (TextUtils.equals(subRouterUrl, routers[i])) {
                        index = i
                        nextFragmentUrl = getNextFragmentRouter(paths)
                        break
                    }
                }
            }
            bundle.putInt(Const.FRAGMENT_INDEX, index)
            bundle.putString(Const.FRGMENT_ROUTER, nextFragmentUrl)
        }
    }
}

private fun getNextFragmentRouter(paths: Array<String>): String? {
    val builder = StringBuilder()
    for (i in 1 until paths.size) {
        builder.append(paths[i]).append(",")
    }
    val value = builder.toString()
    return if (value.endsWith(",")) {
        value.substring(0, value.length - 1)
    } else value
}

private suspend fun getListRouters(obj: Activity): List<String>? {
    val routers = mutableListOf<String>()
    val fragmentRouters = obj.javaClass.getAnnotation(SubFragmentRouters::class.java)
    if (fragmentRouters != null) {
        val type: Int = fragmentRouters.fragmentType
        if (type == SubFragmentType.TABHOST_FRRAGMENTS) {
            val view = obj.findViewById<View>(ReflectUtil.getId(obj, "id", fragmentRouters.widgetIdName))
            require(view is TabHost) { "该控件不属于TabHost" }
            getTabHostRouters(view, routers)
        } else if (type == SubFragmentType.VIEWPAGER_FRAGMENTS) {
            val view = obj.findViewById<View>(ReflectUtil.getId(obj, "id", fragmentRouters.widgetIdName))
            require(view is ViewPager) { "该控件不属于ViewPager" }
            getViewPagerRouters(view, fragmentRouters.filedName, routers)
        } else {
            throw IllegalArgumentException("传入的fragmentType 目前不支持")
        }
    }
    return routers
}

private fun getViewPagerRouters(viewPager: ViewPager, fieldName: String, routers: MutableList<String>) {
    val pagerAdapter = viewPager.adapter
    if (pagerAdapter != null) {
        try {
            val field = pagerAdapter.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            val list = field[pagerAdapter] as List<*>
            for (obj in list) {
                if (obj is IFragmentRouter) {
                    val url = obj.getFragmentRouter()
                    if (!TextUtils.isEmpty(url)) {
                        routers.add(url!!)
                    } else {
                        parseAnnotation(obj, routers)
                    }
                } else {
                    parseAnnotation(obj!!, routers)
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}

private fun parseAnnotation(obj: Any, routers: MutableList<String>) {
    val fragmentRouter = obj.javaClass.getAnnotation(FragmentRouter::class.java)
    if (fragmentRouter != null) {
        val url: String = fragmentRouter.url
        if (!TextUtils.isEmpty(url)) {
            routers.add(url)
        } else {
            routers.add("")
        }
    } else {
        routers.add("")
    }
}

private suspend fun getTabHostRouters(host: TabHost, routers: MutableList<String>) {
    try {
        val method = host.javaClass.getDeclaredMethod("getTabs")
        if (method != null) {
            method.isAccessible = true
            val obj = method.invoke(host)
            if (obj != null && obj is List<*>) {
                for (tabInfoObj in obj) {
                    waitFragmentRouter(tabInfoObj!!, routers)
                }
            }
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
}

private suspend fun waitFragmentRouter(tabInfoObj: Any, routers: MutableList<String>) {
    val fragmentField = tabInfoObj!!::class.java.getDeclaredField("fragment")
    fragmentField.isAccessible = true
    val fragment = fragmentField[tabInfoObj]
    if (fragment != null) {
        if (fragment is IFragmentRouter)
            routers.add(fragment.getFragmentRouter() ?: "")
        else
            parseAnnotation(fragment, routers)
    } else {
        var flagIndex = 0
        do {
            delay(100)
            flagIndex += 1
        } while (!(fragmentField[tabInfoObj] != null || flagIndex > 100))

        val fragment = fragmentField[tabInfoObj]
        if (fragment != null) {
            if (fragment is IFragmentRouter)
                routers.add(fragment.getFragmentRouter() ?: "")
            else
                parseAnnotation(fragment, routers)
        } else
            routers.add("")
    }
}

private fun checkIsReady(activity: Activity): Boolean {
    val content = activity.findViewById<FrameLayout>(android.R.id.content)
    return if (content.childCount <= 0) false
    else {
        val fragmentRouters = activity.javaClass.getAnnotation(SubFragmentRouters::class.java)
        if (fragmentRouters == null) true else {
            when (fragmentRouters.fragmentType) {
                SubFragmentType.TABHOST_FRRAGMENTS -> {
                    isReadyTabHost(activity, fragmentRouters)
                }
                SubFragmentType.VIEWPAGER_FRAGMENTS -> {
                    isReadyViewPager(activity, fragmentRouters)
                }
                else -> {
                    true
                }
            }
        }
    }
}

private fun isReadyTabHost(activity: Activity, fragmentRouters: SubFragmentRouters): Boolean {
    val view = activity.findViewById<View>(ReflectUtil.getId(activity, "id", fragmentRouters.widgetIdName))
    return if (view !is TabHost) {
        true
    } else {
        try {
            val method = view.javaClass.getDeclaredMethod("getTabs")
            if (method != null) {
                method.isAccessible = true
                val obj = method.invoke(view)
                obj is List<*> && obj.size > 0
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }
}

private fun isReadyViewPager(activity: Activity, fragmentRouters: SubFragmentRouters): Boolean {
    val view = activity.findViewById<View>(ReflectUtil.getId(activity, "id", fragmentRouters.widgetIdName))
    return if (view !is ViewPager) {
        true
    } else {
        val pagerAdapter = view.adapter
        if (pagerAdapter != null) {
            try {
                val field = pagerAdapter.javaClass.getDeclaredField(fragmentRouters.filedName)
                field.isAccessible = true
                val list = field[pagerAdapter] as List<*>
                list != null && list.isNotEmpty()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                true
            }
        } else {
            false
        }
    }
}