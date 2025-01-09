package com.taylorz.logutilsdemo

import android.app.Activity
import android.os.Bundle
import com.taylorz.logutils.CrashHandler
import com.taylorz.logutils.LogUtils


class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        LogUtils.init(this)
        val config = LogUtils.config
        config.globalTag = "测试"
        LogUtils.d("开始")
        val data = mapOf<String, String>(
            "name" to "taylorz"
        )
        LogUtils.json("测试啊",data)
        try {
            val array = arrayOf(1, 2, 3)
            val ss = array[4]
        }catch (e: Exception){
            LogUtils.e(e)
        }
    }

}