package com.kronos.download

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import jaygoo.library.m3u8downloader.M3U8Downloader
import jaygoo.library.m3u8downloader.M3U8DownloaderConfig
import jaygoo.library.m3u8downloader.OnM3U8DownloadListener
import jaygoo.library.m3u8downloader.bean.M3U8Task
import jaygoo.library.m3u8downloader.bean.M3U8TaskState
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*
import java.net.URLDecoder
import java.util.*
import java.util.regex.Pattern

/**
 * Created by Leif Zhang on 16/9/29.
 * Email leifzhanggithub@gmail.com
 */
object DownloadManager {

    private var models: HashMap<String, DownloadModel>? = null
    private var config: DownloadConfig? = null
    private var m3u8Config: M3U8DownloaderConfig? = null
    private var m3u8Downloader: M3U8Downloader? = null
    private val okHttpClient: OkHttpClient?
        get() = config!!.okHttpClient


    init {
        models = HashMap()
    }

    fun setConfig(downloadConfig: DownloadConfig) {
        this.config = downloadConfig
        models = config!!.downloadDb!!.getFromDB(downloadConfig)
    }


    private fun isReady() {
        if (config == null) {
            throw NullPointerException()
        }
    }

    fun pauseAll() {
        for ((_, value) in models!!) {
            value.state = DownloadConstants.DOWNLOAD_PAUSE
            if (checkIsM3u8(value.downloadUrl))
                m3u8Downloader?.cancel(value.downloadUrl)
        }
    }

    fun pause(url: String) {
        if (models!!.containsKey(url)) {
            models!![url]?.state = DownloadConstants.DOWNLOAD_PAUSE
        }

        if (checkIsM3u8(url))
            m3u8Downloader?.cancel(url)
    }

    fun startAll(context: Context) {
        initM3u8Config(context)
        if (config!!.settingConfig!!.isAutoDownload() || FileUtils.isConnectWIFI(context)) {
            for ((key, model) in models!!) {
                model.state = DownloadConstants.DOWNLOADING
                val intent = Intent(context, DownloadService::class.java)
                intent.putExtra("url", key)
                context.startService(intent)
            }
        }
    }

    fun start(context: Context, url: String) {
        initM3u8Config(context)
        if (models!!.containsKey(url)) {
            val intent = Intent(context, DownloadService::class.java)
            intent.putExtra("url", url)
            context.startService(intent)
        }
    }

    private fun putModel(url: String, model: DownloadModel) {
        models!![url] = model
        saveAll()
    }

    fun getModel(url: String): DownloadModel? {
        return if (models!!.containsKey(url)) {
            models!![url]
        } else {
            null
        }
    }


    @Throws(IOException::class)
    fun startRequest(url: String) {
        val downloadModel = getModel(url) ?: return
        if (checkIsM3u8(url)) {
            downloadM3u8(url, downloadModel)
            return
        }

        if (downloadModel.check()) {
            downloadModel.state = DownloadConstants.DOWNLOAD_FINISH
            return
        }

        if (downloadModel.downloadLength > downloadModel.totalLength) {
            val file = File(downloadModel.sdCardFile)
            file.delete()
            downloadModel.downloadLength = 0
            downloadModel.progress = 0
            downloadModel.totalLength = 0
        }
        val range = "bytes=" + downloadModel.downloadLength + "-"
        val request = Request.Builder().url(downloadModel.downloadUrl)
                .addHeader("RANGE", range).build()
        val response = okHttpClient!!.newCall(request).execute()
        val responseBody = response.body()
        if (downloadModel.state == 0) {
            downloadModel.state = DownloadConstants.DOWNLOADING
        }
        if (downloadModel.totalLength == 0L) {
            downloadModel.totalLength = responseBody!!.contentLength()
        }
        writeFile(downloadModel, responseBody!!.byteStream())
        if (downloadModel.check()) {
            downloadModel.state = DownloadConstants.DOWNLOAD_FINISH
        }
    }

    private fun downloadM3u8(url: String, model: DownloadModel?) {
        model?.state = 0
        m3u8Downloader?.setOnM3U8DownloadListener(object : OnM3U8DownloadListener() {
            override fun onDownloadError(task: M3U8Task?, errorMsg: Throwable?) {
                super.onDownloadError(task, errorMsg)
                updateDownloadState(model, task)
            }

            override fun onDownloadItem(task: M3U8Task?, itemFileSize: Long, totalTs: Int, curTs: Int) {
                super.onDownloadItem(task, itemFileSize, totalTs, curTs)
                updateDownloadState(model, task)
            }

            override fun onDownloadPause(task: M3U8Task?) {
                super.onDownloadPause(task)
                updateDownloadState(model, task)
            }

            override fun onDownloadPending(task: M3U8Task?) {
                super.onDownloadPending(task)
                updateDownloadState(model, task)
            }

            override fun onDownloadPrepare(task: M3U8Task?) {
                super.onDownloadPrepare(task)
                updateDownloadState(model, task)
            }

            override fun onDownloadProgress(task: M3U8Task?) {
                super.onDownloadProgress(task)
                updateDownloadState(model, task)
                model?.updateProgress(task?.progress?.toInt() ?: 0)
            }

            override fun onDownloadSuccess(task: M3U8Task?) {
                super.onDownloadSuccess(task)
                updateDownloadState(model, task)
                model?.updateProgress(100)
                model?.totalLength = task?.m3U8?.fileSize ?: 0
                model?.downloadLength = task?.m3U8?.fileSize ?: 0
            }
        })
        m3u8Downloader?.download(url)
    }

    private fun updateDownloadState(model: DownloadModel?, task: M3U8Task?) {
        task ?: return
        val state = task.state
        when (state) {
            M3U8TaskState.DOWNLOADING -> model?.state = DownloadConstants.DOWNLOADING
            M3U8TaskState.PREPARE -> model?.state = DownloadConstants.DOWNLOADING
            M3U8TaskState.DEFAULT -> model?.state = 0
            M3U8TaskState.PENDING -> model?.state = 0
            M3U8TaskState.SUCCESS -> model?.state = DownloadConstants.DOWNLOAD_FINISH
            M3U8TaskState.ERROR -> model?.state = DownloadConstants.DOWNLOAD_PAUSE
            M3U8TaskState.PAUSE -> model?.state = DownloadConstants.DOWNLOAD_PAUSE
            M3U8TaskState.ENOSPC -> model?.state = DownloadConstants.DOWNLOAD_PAUSE
        }
    }

    @Throws(IOException::class)
    private fun writeFile(model: DownloadModel, input: InputStream) {
        val destFilePath = model.sdCardFile
        if (!FileUtils.createFile(destFilePath)) {
            return
        }
        val randomAccessFile = RandomAccessFile(destFilePath, "rw")
        randomAccessFile.seek(model.downloadLength)
        var readCount: Int
        val len = 1024
        val buffer = ByteArray(len)
        val bufferedInputStream = BufferedInputStream(input, len)
        do {
            readCount = bufferedInputStream.read(buffer)
            if (readCount != -1) {
                if (model.state == DownloadConstants.DOWNLOADING) {
                    randomAccessFile.write(buffer, 0, readCount)
                    model.addDownloadLength(readCount)
                } else {
                    break
                }
            }
        } while (true)
        randomAccessFile.close()
        bufferedInputStream.close()
    }

    fun saveOne(url: String) {
        try {
            config!!.downloadDb!!.saveToDb(models!![url])
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun checkIsM3u8(url: String?): Boolean {
        url ?: return false
        var url = url
        try {
            url = URLDecoder.decode(url, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        val suffixes = "avi|mpeg|3gp|mp3|mp4|wav|jpeg|gif|jpg|png|apk|exe|txt|html|zip|java|doc|m3u8|M3U8"
        val pat = Pattern.compile("[\\w]+[\\.]($suffixes)")//正则判断
        val mc = pat.matcher(url)//条件匹配
        var suffix = ""
        while (mc.find()) {
            suffix = mc.group()//截取文件名后缀名
        }
        return !TextUtils.isEmpty(suffix) && (suffix.endsWith("m3u8") || suffix.endsWith("M3U8"))
    }

    fun saveAll() {
        try {
            config!!.downloadDb!!.saveToDb(models)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun remove(url: String) {
        if (models!!.containsKey(url)) {
            models!!.remove(url)
        }
    }

    fun setDownloadModel(url: String, context: Context) {
        setDownloadModel(url, context, null)
    }

    fun setDownloadModel(url: String, context: Context, fileName: String? = null) {
        initM3u8Config(context)
        isReady()
        var downloadModel = getModel(url)
        if (downloadModel == null) {
            downloadModel = DownloadModel()
            downloadModel.fileName = fileName
            downloadModel.downloadUrl = url
            downloadModel.setSuffixName(config!!.settingConfig!!.fileSuffix)
            downloadModel.setDownloadFolder(config!!.downloadFolder)
            putModel(url, downloadModel)
        }
        val intent = Intent(context, DownloadService::class.java)
        intent.putExtra("url", url)
        context.startService(intent)
    }

    private fun initM3u8Config(context: Context) {
        if (m3u8Config == null || m3u8Downloader == null) {
            var folder = config!!.downloadFolder
            if (TextUtils.isEmpty(folder))
                folder = Environment.getExternalStorageDirectory().path + "/wallstreetcn/"
            if (Looper.getMainLooper().thread != Thread.currentThread()) {
                val handler = Handler(Looper.getMainLooper()) {
                    m3u8Config = M3U8DownloaderConfig.build(context)
                            .setThreadCount(1)
                            .setSaveDir(folder)
                            .setConnTimeout(10000)
                            .setReadTimeout(10000)
                            .setDebugMode(false)

                    m3u8Downloader = M3U8Downloader.getInstance()
                    true
                }
                handler.sendEmptyMessage(0)
            } else {
                m3u8Config = M3U8DownloaderConfig.build(context)
                        .setThreadCount(1)
                        .setSaveDir(config!!.downloadFolder)
                        .setConnTimeout(10000)
                        .setReadTimeout(10000)
                        .setDebugMode(false)

                m3u8Downloader = M3U8Downloader.getInstance()
            }
        }
    }
}
