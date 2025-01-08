package com.taylorz.logutils

import android.util.Log
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * <pre>
 * author: Blankj
 * blog  : http://blankj.com
 * time  : 2017/06/22
 * desc  : utils about file io
</pre> *
 */
object FileIOUtils  {

    interface OnProgressUpdateListener {
        fun onProgressUpdate(progress: Double)
    }

    private const val sBufferSize = 524288

    ///////////////////////////////////////////////////////////////////////////
    // writeFileFromIS without progress
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Write file from input stream.
     *
     * @param filePath The path of file.
     * @param is       The input stream.
     * @return `true`: success<br></br>`false`: fail
     */
    fun writeFileFromIS(filePath: String?, `is`: InputStream?): Boolean {
        return writeFileFromIS(FileUtils.getFileByPath(filePath), `is`, false, null)
    }


    /**
     * Write file from input stream.
     *
     * @param file     The file.
     * @param is       The input stream.
     * @param append   True to append, false otherwise.
     * @param listener The progress update listener.
     * @return `true`: success<br></br>`false`: fail
     */
    fun writeFileFromIS(
        file: File?,
        `is`: InputStream?,
        append: Boolean,
        listener: OnProgressUpdateListener?
    ): Boolean {
        if (`is` == null || !FileUtils.createOrExistsFile(file)) {
            Log.e("FileIOUtils", "create file <$file> failed.")
            return false
        }
        var os: OutputStream? = null
        try {
            os = BufferedOutputStream(FileOutputStream(file, append), sBufferSize)
            if (listener == null) {
                val data = ByteArray(sBufferSize)
                var len: Int
                while ((`is`.read(data).also { len = it }) != -1) {
                    os.write(data, 0, len)
                }
            } else {
                val totalSize = `is`.available().toDouble()
                var curSize = 0
                listener.onProgressUpdate(0.0)
                val data = ByteArray(sBufferSize)
                var len: Int
                while ((`is`.read(data).also { len = it }) != -1) {
                    os.write(data, 0, len)
                    curSize += len
                    listener.onProgressUpdate(curSize / totalSize)
                }
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } finally {
            try {
                `is`.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                os?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Write file from string.
     *
     * @param filePath The path of file.
     * @param content  The string of content.
     * @param append   True to append, false otherwise.
     * @return `true`: success<br></br>`false`: fail
     */
    @JvmStatic
    fun writeFileFromString(
        filePath: String?,
        content: String?,
        append: Boolean
    ): Boolean {
        return writeFileFromString(FileUtils.getFileByPath(filePath), content, append)
    }

    /**
     * Write file from string.
     *
     * @param file    The file.
     * @param content The string of content.
     * @param append  True to append, false otherwise.
     * @return `true`: success<br></br>`false`: fail
     */
    fun writeFileFromString(
        file: File?,
        content: String?,
        append: Boolean
    ): Boolean {
        if (file == null || content == null) return false
        if (!FileUtils.createOrExistsFile(file)) {
            Log.e("FileIOUtils", "create file <$file> failed.")
            return false
        }
        var bw: BufferedWriter? = null
        try {
            bw = BufferedWriter(FileWriter(file, append))
            bw.write(content)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } finally {
            try {
                bw?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
