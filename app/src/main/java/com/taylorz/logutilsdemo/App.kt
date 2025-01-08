package com.taylorz.logutilsdemo

import android.app.Application
import com.taylorz.logutils.LogHelpr
import com.taylorz.logutils.LogUtils

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        LogHelpr().init(this)
    }
}