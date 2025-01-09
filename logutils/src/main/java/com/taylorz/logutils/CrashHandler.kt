package com.taylorz.logutils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("StaticFieldLeak")
object CrashHandler : Thread.UncaughtExceptionHandler {

    private lateinit var context: Context
    private lateinit var crashDirectory: String
    private lateinit var cb:((String, Throwable)->Unit)

    fun init(context: Context, crashDirectory: String,callback:((String,Throwable)->Unit)) {
        this.context = context
        this.crashDirectory = crashDirectory
        Thread.setDefaultUncaughtExceptionHandler(this)
        this.cb = callback
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val crashReport = generateCrashReport(thread, throwable)
        val filePath = saveCrashReportToFile(crashReport)
        throwable.printStackTrace()
        this.cb.invoke(filePath,throwable)
    }

    private fun generateCrashReport(thread: Thread, throwable: Throwable): String {
        val sdf = SimpleDateFormat("yyyy_MM_dd HH:mm:ss", Locale.getDefault())
        val currentTime = sdf.format(Date())

        val packageInfo = getPackageInfo(context)
        val appVersionName = packageInfo?.versionName ?: "N/A"
        val appVersionCode = packageInfo?.versionCode ?: "N/A"

        val report = StringBuilder()
        report.append("************* Crash Head ****************\n")
        report.append("崩溃时间: ").append(currentTime).append("\n")
        report.append("版本名称: ").append(appVersionName).append("\n")
        report.append("构建版本: ").append(appVersionCode).append("\n")
        report.append(getRomInfo()).append("\n")
        report.append("************* Crash Head ****************\n\n")

        report.append("************* 崩溃内容 开始 ****************\n\n")
        report.append("Exception: ").append(throwable::class.java.name).append("\n")
        report.append("Message: ").append(throwable.message).append("\n")
        report.append("Stack Trace:\n")
        throwable.stackTrace.forEach { element ->
            report.append("\tat ").append(element.toString()).append("\n")
        }
        report.append("\n\n************* 崩溃内容 结束 ****************\n\n")
        return report.toString()
    }
    private fun getRomInfo(): String {
        val manufacturer = getSystemProperty("ro.product.manufacturer") ?: "未知"
        val brand = getSystemProperty("ro.product.brand") ?: "未知"
        val model = getSystemProperty("ro.product.model") ?: "未知"
        val version = getSystemProperty("ro.build.version.release") ?: "未知"
        val fingerprint = getSystemProperty("ro.build.fingerprint") ?: "未知"
        val miuiVersion = getSystemProperty("ro.miui.ui.version.name")
        val emuiVersion = getSystemProperty("ro.build.version.emui")
        val flymeVersion = getSystemProperty("ro.build.display.id")?.takeIf { it.contains("Flyme") }

        val customRom = when {
            miuiVersion != null -> "MIUI $miuiVersion"
            emuiVersion != null -> "EMUI $emuiVersion"
            flymeVersion != null -> flymeVersion
            else -> "未知"
        }

        return """
        设备厂商: $manufacturer
        设备品牌: $brand
        设备型号: $model
        系统版本: $version
        定制ROM: $customRom
        系统指纹: $fingerprint
    """.trimIndent()
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            process.inputStream.bufferedReader().use { it.readLine() }
        } catch (e: Exception) {
            null
        }
    }
    private fun saveCrashReportToFile(crashReport: String):String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        val crashFileName = "crash_$currentTime.txt"
        val crashFile = File(crashDirectory, crashFileName)

        try {
            FileWriter(crashFile).use { writer ->
                writer.write(crashReport)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return crashFile.toString()
    }

    private fun getPackageInfo(context: Context): PackageInfo? {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }
}