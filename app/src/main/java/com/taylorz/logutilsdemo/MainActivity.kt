package com.taylorz.logutilsdemo

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.taylorz.logutils.LogUtils
import constant.UiType
import listener.OnBtnClickListener
import listener.UpdateDownloadListener
import model.UiConfig
import model.UpdateConfig
import update.UpdateAppUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.FileChannel


class MainActivity : AppCompatActivity() {
    val downloadUrl = "https://www.pgyer.com/app/installUpdate/8824dd549d717ea031647aa41098059a?sig=14%2B8Un572jK2Pa5UlUPCZ5dELVGdH2YMtlAJd7Z4cpTS1pR5%2BmuA69GkNEQKw0zN&forceHttps="
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        LogUtils.init(this)
        val config = LogUtils.config
        config.globalTag = "测试"
        LogUtils.d("开始")
//        val data = mapOf<String, String>(
//            "name" to "taylorz"
//        )
//        LogUtils.json("测试啊",data)
//        try {
//            val array = arrayOf(1, 2, 3)
//            val ss = array[4]
//        }catch (e: Exception){
//            LogUtils.e(e)
//        }

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
//        showUpdate()
    }

    private fun showUpdate() {
        val updateConfig = UpdateConfig().apply {
            force = true
            serverVersionCode = 99
            serverVersionName = "2.1.2.3"
        }
        UpdateAppUtils.getInstance()
            .apkUrl(downloadUrl)
            .updateTitle("发现新版本")
            .updateContent("测试的")
            .updateConfig(updateConfig)
            .uiConfig(UiConfig(uiType = UiType.PLENTIFUL))
            .setUpdateDownloadListener(object : UpdateDownloadListener {
                override fun onDownload(progress: Int) {
                    LogUtils.d("更新进度：$progress")
                }

                override fun onError(e: Throwable) {
                    LogUtils.e("更新出错：${e.localizedMessage}")
                }

                override fun onFinish() {
                    LogUtils.d("更新完成")
                }

                override fun onStart() {
                    LogUtils.d("更新开始")
                }
            })
            .setUpdateBtnClickListener(object : OnBtnClickListener {
                override fun onClick(): Boolean {

                    return false // 返回 false 表示事件未消费，允许继续操作
                }
            })
            .setCancelBtnClickListener(object : OnBtnClickListener {
                override fun onClick(): Boolean {
                    return false // 返回 false 表示事件未消费，允许继续操作
                }
            })
            .update()
    }


}