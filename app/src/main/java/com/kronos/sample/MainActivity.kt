package com.kronos.sample

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import butterknife.ButterKnife
import butterknife.OnClick
import com.kronos.download.DownloadConfig
import com.kronos.download.DownloadManager
import com.kronos.download.DownloadManager.setConfig
import com.kronos.download.DownloadSettingConfig
import com.kronos.router.Router
import jaygoo.library.m3u8downloader.M3U8DownloaderConfig

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Router.sharedRouter().attachApplication(application)
        ButterKnife.bind(this)


        val settingConfig = DownloadSettingConfig().setFileSuffix(".temp").setAutoDownload(false)
        val downloadConfig = DownloadConfig.Builder()
                .setSettingConfig(settingConfig)
                .builder()
        setConfig(downloadConfig)

        M3U8DownloaderConfig.build(this)
        if(M3u8C.downloader == null)
            Toast.makeText(this, "是空的", Toast.LENGTH_LONG).show()
    }

    @OnClick(R.id.routerTesting, R.id.routerBaidu)
    fun onClick(view: View) {
            DownloadManager.setDownloadModel("https://streaming.wallstcn.com/bd66ff2e-6f04-401e-bed7-75e1150740f7_hls.m3u8", this)
//        when (view.id) {
//            R.id.routerTesting -> Router.sharedRouter().open("https://www.baidu.com/test", this)
//            R.id.routerBaidu -> Router.sharedRouter().open("https://www.baidu.com/test/12345", this)
//        }
    }
}
