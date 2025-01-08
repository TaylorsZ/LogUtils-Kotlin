package com.taylorz.logutils

import android.app.Application

open class LogHelpr {
    companion object {
        lateinit var application: Application
            private set
    }
    open fun init(app: Application) {
        application = app
    }
}