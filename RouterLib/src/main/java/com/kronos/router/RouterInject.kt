package com.kronos.router

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * Created by zhangyang on 16/7/18.
 */
object RouterInject {
    fun inject(activity: FragmentActivity, intent: Intent) {
        val bundle = intent.extras
        val target = bundle?.getString("target")
        val inject = getActivityInject(activity) ?: return
        val fragmentManager = activity.supportFragmentManager
        val fragments = fragmentManager.fragments
        var fragment : Fragment
        for (i in 0 until fragments.size) {
            fragment = fragments[i]
            if (fragment == null) {
                continue
            }
            val fragmentInject = activityInject(fragment, target)
            if (fragmentInject != null) {
                fragmentInject.inject(bundle)
                break
            }
        }
        inject.inject(bundle!!)
    }

    fun inject(fragment: Fragment, bundle: Bundle?) {
        val target = bundle?.getString("target")
        val inject = deal(fragment, target)
        inject?.inject(bundle!!)
    }

    private fun activityInject(fragment: Fragment, target: String?): IFragmentInject? {
        return deal(fragment, target)
    }

    private fun deal(any: Any, target: String?): IFragmentInject? {
        return if (TextUtils.equals(any.javaClass.name, target)) {
            getFragmentInject(any)
        } else null
    }

    private fun getActivityInject(any: Any): IActivityInject? {
        return if (any is IActivityInject) {
            any
        } else {
            null
        }
    }

    private fun getFragmentInject(any: Any): IFragmentInject? {
        return if (any is IFragmentInject) {
            any
        } else {
            null
        }
    }

}
