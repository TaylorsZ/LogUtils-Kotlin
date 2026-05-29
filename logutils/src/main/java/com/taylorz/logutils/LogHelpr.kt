package com.taylorz.logutils

import android.app.Application

open class LogHelpr {
    open fun init(app: Application) {
        UtilsBridge.init(app)
    }
}
