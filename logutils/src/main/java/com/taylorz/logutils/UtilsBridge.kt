package com.taylorz.logutils

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import java.io.File

object UtilsBridge {
    object MemoryConstants {
        const val BYTE: Int = 1
        const val KB: Int = 1024
        const val MB: Int = 1048576
        const val GB: Int = 1073741824
    }

    @SuppressLint("DefaultLocale")
    fun byte2FitMemorySize(byteSize: Long): String {
        return byte2FitMemorySize(byteSize, 3)
    }

    /**
     * Size of byte to fit size of memory.
     *
     * to three decimal places
     *
     * @param byteSize  Size of byte.
     * @param precision The precision
     * @return fit size of memory
     */
    @SuppressLint("DefaultLocale")
    fun byte2FitMemorySize(byteSize: Long, precision: Int): String {
        require(precision >= 0) { "precision shouldn't be less than zero!" }
        require(byteSize >= 0) { "byteSize shouldn't be less than zero!" }
        return if (byteSize < MemoryConstants.KB) {
            String.format("%." + precision + "fB", byteSize.toDouble())
        } else if (byteSize < MemoryConstants.MB) {
            String.format(
                "%." + precision + "fKB",
                byteSize.toDouble() / MemoryConstants.KB
            )
        } else if (byteSize < MemoryConstants.GB) {
            String.format(
                "%." + precision + "fMB",
                byteSize.toDouble() / MemoryConstants.MB
            )
        } else {
            String.format(
                "%." + precision + "fGB",
                byteSize.toDouble() / MemoryConstants.GB
            )
        }
    }

    fun writeFileFromString(filePath: String?, content: String?, append: Boolean): Boolean {
        return FileIOUtils.writeFileFromString(filePath, content, append)
    }

    fun isSpace(s: String?): Boolean {
        if (s == null) return true
        var i = 0
        val len = s.length
        while (i < len) {
            if (!Character.isWhitespace(s[i])) {
                return false
            }
            ++i
        }
        return true
    }

    /**
     * Create a directory if it doesn't exist, otherwise do nothing.
     *
     * @param file The file.
     * @return `true`: exists or creates successfully<br></br>`false`: otherwise
     */
    fun createOrExistsDir(file: File?): Boolean {
        return file != null && (if (file.exists()) file.isDirectory else file.mkdirs())
    }

    internal class FileHead(private val mName: String) {
        private val mFirst = LinkedHashMap<String, String>()
        private val mLast = LinkedHashMap<String, String>()

        fun addFirst(key: String, value: String) {
            append2Host(mFirst, key, value)
        }

        fun append(extra: Map<String, String>?) {
            append2Host(mLast, extra)
        }

        fun append(key: String, value: String) {
            append2Host(mLast, key, value)
        }

        private fun append2Host(host: MutableMap<String, String>, extra: Map<String, String>?) {
            if (extra == null || extra.isEmpty()) {
                return
            }
            for ((key, value) in extra) {
                append2Host(host, key, value)
            }
        }

        private fun append2Host(host: MutableMap<String, String>, key: String, value: String) {
            var result = key
            if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
                return
            }
            val delta = 19 - key.length // 19 is length of "Device Manufacturer"
            if (delta > 0) {
                result = key + "                   ".substring(0, delta)
            }
            host[result] = value
        }

        val appended: String
            get() {
                val sb = StringBuilder()
                for ((key, value) in mLast) {
                    sb.append(key).append(": ").append(value).append("\n")
                }
                return sb.toString()
            }

        override fun toString(): String {
            val border = "************* $mName Head ****************\n"
            val systemInfo = """
        |设备厂商               : ${Build.MANUFACTURER}
        |设备型号               : ${Build.MODEL}
        |系统版本               : 安卓${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})
    """.trimMargin()

            val (version, versionCode) = getAppVersionInfo()
            val appInfo = """
        |版本名称               : $version
        |构建版本               : $versionCode
    """.trimMargin()

            val firstInfo =
                mFirst.entries.joinToString(separator = "\n") { "${it.key}: ${it.value}" }
            return buildString {
                append(border)
                append(firstInfo).append("\n")
                append(systemInfo).append("\n")
                append(appInfo).append("\n")
                append(appended).append("\n")
                append(border)
            }
        }

        @SuppressLint("NewApi")
        fun getAppVersionInfo(): Pair<String, Int> {
            return try {
                val packageInfo: PackageInfo = LogHelpr.application.packageManager.getPackageInfo(
                    LogHelpr.application.packageName,
                    0
                )
                val versionName = packageInfo.versionName
                val versionCode =
                    packageInfo.longVersionCode.toInt() // For API 28+ use longVersionCode
                Pair(versionName, versionCode)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                Pair("Unknown", -1)
            }
        }
    }
}