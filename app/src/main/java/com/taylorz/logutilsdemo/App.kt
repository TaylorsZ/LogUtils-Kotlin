package com.taylorz.logutilsdemo

import android.app.Application
import android.os.Environment
import android.util.Log
import com.taylorz.logutils.LogUtils
import update.UpdateAppUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        val config = LogUtils.config
        config.globalTag = "测试"
        config.processName = "APP常规日志"
        //设置 log 文件开关
        config.isLogSwitch = true
        config.saveDays = 2

        config.fileExtension = ".txt"
        config.isLog2FileSwitch = true
        val logPath = Environment.getExternalStorageDirectory().absolutePath + "/测试日志"

        val dateFormat = SimpleDateFormat("HH_mm_ss", Locale.CHINA)
        val date = Date()
        config.filePrefix = dateFormat.format(date)

        val dateFormat2 = SimpleDateFormat("yyyy_MM_dd", Locale.CHINA)
        val datelogPath = logPath.plus("/").plus(dateFormat2.format(date))
        config.dir = datelogPath
        Log.d("测试", "测试:${config.dir}")
        UpdateAppUtils.init(this)
    }
}
