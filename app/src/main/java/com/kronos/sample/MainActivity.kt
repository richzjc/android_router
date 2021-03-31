package com.kronos.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import butterknife.ButterKnife
import butterknife.OnClick
import com.kronos.download.DownloadManager
import com.kronos.router.Router
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Router.sharedRouter().attachApplication(application)
        ButterKnife.bind(this)
    }

    @OnClick(R.id.routerTesting, R.id.routerBaidu)
    fun onClick(view: View) {
        thread {
            DownloadManager.pause("https://www.baidu.com")
        }
//        when (view.id) {
//            R.id.routerTesting -> Router.sharedRouter().open("https://www.baidu.com/test", this)
//            R.id.routerBaidu -> Router.sharedRouter().open("https://www.baidu.com/test/12345", this)
//        }
    }
}
