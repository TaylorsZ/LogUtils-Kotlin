package com.taylorz.logutilsdemo

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

object StoragePermissionUtils {

    const val REQUEST_CODE_STORAGE = 1001
    const val REQUEST_CODE_MANAGE_STORAGE = 1002

    /**
     * 是否已有存储权限
     */
    fun hasStoragePermission(context: Context): Boolean {

        return when {

            // Android 11+
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }

            // Android 6 - Android 10
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                ContextCompat.checkSelfPermission(
                    context,
                    WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }

            else -> true
        }
    }

    /**
     * 请求存储权限
     */
    fun requestStoragePermission(activity: Activity) {

        when {

            // Android 11+
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {

                try {

                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                    ).apply {
                        data = "package:${activity.packageName}".toUri()
                    }

                    activity.startActivityForResult(
                        intent,
                        REQUEST_CODE_MANAGE_STORAGE
                    )

                } catch (e: Exception) {

                    val intent = Intent(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    )

                    activity.startActivityForResult(
                        intent,
                        REQUEST_CODE_MANAGE_STORAGE
                    )
                }
            }

            // Android 6 - Android 10
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {

                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        READ_EXTERNAL_STORAGE,
                        WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_CODE_STORAGE
                )
            }
        }
    }
}