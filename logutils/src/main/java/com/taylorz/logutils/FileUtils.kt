package com.taylorz.logutils

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.text.TextUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Collections
import javax.net.ssl.HttpsURLConnection

/**
 * <pre>
 * author: Blankj
 * blog  : http://blankj.com
 * time  : 2016/05/03
 * desc  : utils about file
</pre> *
 */
object FileUtils {

    interface OnReplaceListener {
        fun onReplace(srcFile: File?, destFile: File?): Boolean
    }
    private val LINE_SEP: String? = System.lineSeparator()

    /**
     * Return the file by path.
     *
     * @param filePath The path of file.
     * @return the file
     */
    fun getFileByPath(filePath: String?): File? {
        return if (UtilsBridge.isSpace(filePath)) null else filePath?.let { File(it) }
    }

    /**
     * Return whether the file exists.
     *
     * @param file The file.
     * @return `true`: yes<br></br>`false`: no
     */
    fun isFileExists(file: File?): Boolean {
        if (file == null) return false
        if (file.exists()) {
            return true
        }
        return isFileExists(file.absolutePath)
    }

    /**
     * Return whether the file exists.
     *
     * @param filePath The path of file.
     * @return `true`: yes<br></br>`false`: no
     */
    fun isFileExists(filePath: String?): Boolean {
        val file =
            getFileByPath(filePath) ?: return false
        if (file.exists()) {
            return true
        }
        return isFileExistsApi29(filePath)
    }

    private fun isFileExistsApi29(filePath: String?): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                val uri = Uri.parse(filePath)
                val cr = LogHelpr.application.contentResolver
                val afd = cr?.openAssetFileDescriptor(uri, "r") ?: return false
                try {
                    afd.close()
                } catch (ignore: IOException) {
                }
            } catch (e: FileNotFoundException) {
                return false
            }
            return true
        }
        return false
    }

    /**
     * Rename the file.
     *
     * @param filePath The path of file.
     * @param newName  The new name of file.
     * @return `true`: success<br></br>`false`: fail
     */
    fun rename(filePath: String?, newName: String): Boolean {
        return rename(getFileByPath(filePath), newName)
    }

    /**
     * Rename the file.
     *
     * @param file    The file.
     * @param newName The new name of file.
     * @return `true`: success<br></br>`false`: fail
     */
    fun rename(file: File?, newName: String): Boolean {
        // Check if file is null, doesn't exist, or new name is empty/space
        if (file == null || !file.exists() || UtilsBridge.isSpace(newName)) return false
        // Check if the new name is the same as the current one
        if (newName == file.name) return true

        val newFile = file.parentFile?.resolve(newName) // Use resolve for cleaner path construction
        if (newFile == null) return false
        // If the new file already exists, return false
        return !newFile.exists() && file.renameTo(newFile)
    }

    /**
     * Return whether it is a directory.
     *
     * @param dirPath The path of directory.
     * @return `true`: yes<br></br>`false`: no
     */
    fun isDir(dirPath: String?): Boolean {
        return isDir(getFileByPath(dirPath))
    }

    /**
     * Return whether it is a directory.
     *
     * @param file The file.
     * @return `true`: yes<br></br>`false`: no
     */
    fun isDir(file: File?): Boolean {
        return file != null && file.exists() && file.isDirectory
    }

    /**
     * Return whether it is a file.
     *
     * @param filePath The path of file.
     * @return `true`: yes<br></br>`false`: no
     */
    fun isFile(filePath: String?): Boolean {
        return isFile(getFileByPath(filePath))
    }

    /**
     * Return whether it is a file.
     *
     * @param file The file.
     * @return `true`: yes<br></br>`false`: no
     */
    fun isFile(file: File?): Boolean {
        return file != null && file.exists() && file.isFile
    }

    /**
     * Create a directory if it doesn't exist, otherwise do nothing.
     *
     * @param dirPath The path of directory.
     * @return `true`: exists or creates successfully<br></br>`false`: otherwise
     */
    fun createOrExistsDir(dirPath: String?): Boolean {
        return createOrExistsDir(getFileByPath(dirPath))
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

    /**
     * Create a file if it doesn't exist, otherwise do nothing.
     *
     * @param filePath The path of file.
     * @return `true`: exists or creates successfully<br></br>`false`: otherwise
     */
    fun createOrExistsFile(filePath: String?): Boolean {
        return createOrExistsFile(getFileByPath(filePath))
    }

    /**
     * Create a file if it doesn't exist, otherwise do nothing.
     *
     * @param file The file.
     * @return `true`: exists or creates successfully<br></br>`false`: otherwise
     */
    fun createOrExistsFile(file: File?): Boolean {
        if (file == null) return false
        if (file.exists()) return file.isFile
        if (!createOrExistsDir(file.parentFile)) return false
        try {
            return file.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Create a file if it doesn't exist, otherwise delete old file before creating.
     *
     * @param filePath The path of file.
     * @return `true`: success<br></br>`false`: fail
     */
    fun createFileByDeleteOldFile(filePath: String?): Boolean {
        return createFileByDeleteOldFile(getFileByPath(filePath))
    }

    /**
     * Create a file if it doesn't exist, otherwise delete old file before creating.
     *
     * @param file The file.
     * @return `true`: success<br></br>`false`: fail
     */
    fun createFileByDeleteOldFile(file: File?): Boolean {
        if (file == null) return false
        // file exists and unsuccessfully delete then return false
        if (file.exists() && !file.delete()) return false
        if (!createOrExistsDir(file.parentFile)) return false
        try {
            return file.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Copy the directory or file.
     *
     * @param srcPath  The path of source.
     * @param destPath The path of destination.
     * @return `true`: success<br></br>`false`: fail
     */
    fun copy(
        srcPath: String?,
        destPath: String?
    ): Boolean {
        return copy(getFileByPath(srcPath), getFileByPath(destPath), null)
    }

    /**
     * Copy the directory or file.
     *
     * @param srcPath  The path of source.
     * @param destPath The path of destination.
     * @param listener The replace listener.
     * @return `true`: success<br></br>`false`: fail
     */
    fun copy(
        srcPath: String?,
        destPath: String?,
        listener: OnReplaceListener?
    ): Boolean {
        return copy(getFileByPath(srcPath), getFileByPath(destPath), listener)
    }

    /**
     * Copy the directory or file.
     *
     * @param src      The source.
     * @param dest     The destination.
     * @param listener The replace listener.
     * @return `true`: success<br></br>`false`: fail
     */
    /**
     * Copy the directory or file.
     *
     * @param src  The source.
     * @param dest The destination.
     * @return `true`: success<br></br>`false`: fail
     */
    @JvmOverloads
    fun copy(
        src: File?,
        dest: File?,
        listener: OnReplaceListener? = null
    ): Boolean {
        if (src == null) return false
        if (src.isDirectory) {
            return copyDir(src, dest, listener)
        }
        return copyFile(src, dest, listener)
    }

    /**
     * Copy the directory.
     *
     * @param srcDir   The source directory.
     * @param destDir  The destination directory.
     * @param listener The replace listener.
     * @return `true`: success<br></br>`false`: fail
     */
    private fun copyDir(
        srcDir: File,
        destDir: File?,
        listener: OnReplaceListener?
    ): Boolean {
        return copyOrMoveDir(srcDir, destDir, listener, false)
    }

    /**
     * Copy the file.
     *
     * @param srcFile  The source file.
     * @param destFile The destination file.
     * @param listener The replace listener.
     * @return `true`: success<br></br>`false`: fail
     */
    private fun copyFile(
        srcFile: File,
        destFile: File?,
        listener: OnReplaceListener?
    ): Boolean {
        return copyOrMoveFile(srcFile, destFile, listener, false)
    }

    /**
     * Move the directory or file.
     *
     * @param srcPath  The path of source.
     * @param destPath The path of destination.
     * @return `true`: success<br></br>`false`: fail
     */
    fun move(
        srcPath: String?,
        destPath: String?
    ): Boolean {
        return move(getFileByPath(srcPath), getFileByPath(destPath), null)
    }

    /**
     * Move the directory or file.
     *
     * @param srcPath  The path of source.
     * @param destPath The path of destination.
     * @param listener The replace listener.
     * @return `true`: success<br></br>`false`: fail
     */
    fun move(
        srcPath: String?,
        destPath: String?,
        listener: OnReplaceListener?
    ): Boolean {
        return move(getFileByPath(srcPath), getFileByPath(destPath), listener)
    }

    /**
     * Move the directory or file.
     *
     * @param src      The source.
     * @param dest     The destination.
     * @param listener The replace listener.
     * @return `true`: success<br></br>`false`: fail
     */
    /**
     * Move the directory or file.
     *
     * @param src  The source.
     * @param dest The destination.
     * @return `true`: success<br></br>`false`: fail
     */
    @JvmOverloads
    fun move(
        src: File?,
        dest: File?,
        listener: OnReplaceListener? = null
    ): Boolean {
        if (src == null) return false
        if (src.isDirectory) {
            return moveDir(src, dest, listener)
        }
        return moveFile(src, dest, listener)
    }

    /**
     * Move the directory.
     *
     * @param srcDir   The source directory.
     * @param destDir  The destination directory.
     * @param listener The replace listener.
     * @return `true`: success<br></br>`false`: fail
     */
    fun moveDir(
        srcDir: File?,
        destDir: File?,
        listener: OnReplaceListener?
    ): Boolean {
        return copyOrMoveDir(srcDir, destDir, listener, true)
    }

    /**
     * Move the file.
     *
     * @param srcFile  The source file.
     * @param destFile The destination file.
     * @param listener The replace listener.
     * @return `true`: success<br></br>`false`: fail
     */
    fun moveFile(
        srcFile: File?,
        destFile: File?,
        listener: OnReplaceListener?
    ): Boolean {
        return copyOrMoveFile(srcFile, destFile, listener, true)
    }

    private fun copyOrMoveDir(
        srcDir: File?,
        destDir: File?,
        listener: OnReplaceListener?,
        isMove: Boolean
    ): Boolean {
        if (srcDir == null || destDir == null) return false
        // destDir's path locate in srcDir's path then return false
        val srcPath = srcDir.path + File.separator
        val destPath = destDir.path + File.separator
        if (destPath.contains(srcPath)) return false
        if (!srcDir.exists() || !srcDir.isDirectory) return false
        if (!createOrExistsDir(destDir)) return false
        val files = srcDir.listFiles()
        if (files != null && files.size > 0) {
            for (file in files) {
                val oneDestFile = File(destPath + file.name)
                if (file.isFile) {
                    if (!copyOrMoveFile(file, oneDestFile, listener, isMove)) return false
                } else if (file.isDirectory) {
                    if (!copyOrMoveDir(file, oneDestFile, listener, isMove)) return false
                }
            }
        }
        return !isMove || deleteDir(srcDir)
    }

    private fun copyOrMoveFile(
        srcFile: File?,
        destFile: File?,
        listener: OnReplaceListener?,
        isMove: Boolean
    ): Boolean {
        if (srcFile == null || destFile == null) return false
        // srcFile equals destFile then return false
        if (srcFile == destFile) return false
        // srcFile doesn't exist or isn't a file then return false
        if (!srcFile.exists() || !srcFile.isFile) return false
        if (destFile.exists()) {
            if (listener == null || listener.onReplace(
                    srcFile,
                    destFile
                )
            ) { // require delete the old file
                if (!destFile.delete()) { // unsuccessfully delete then return false
                    return false
                }
            } else {
                return true
            }
        }
        if (!createOrExistsDir(destFile.parentFile)) return false
        try {
            return FileIOUtils.writeFileFromIS(destFile.absolutePath, FileInputStream(srcFile))
                    && !(isMove && !deleteFile(srcFile))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Delete the directory.
     *
     * @param filePath The path of file.
     * @return `true`: success<br></br>`false`: fail
     */
    fun delete(filePath: String?): Boolean {
        return delete(getFileByPath(filePath))
    }

    /**
     * Delete the directory.
     *
     * @param file The file.
     * @return `true`: success<br></br>`false`: fail
     */
    fun delete(file: File?): Boolean {
        if (file == null) return false
        if (file.isDirectory) {
            return deleteDir(file)
        }
        return deleteFile(file)
    }

    /**
     * Delete the directory.
     *
     * @param dir The directory.
     * @return `true`: success<br></br>`false`: fail
     */
    private fun deleteDir(dir: File?): Boolean {
        if (dir == null) return false
        // dir doesn't exist then return true
        if (!dir.exists()) return true
        // dir isn't a directory then return false
        if (!dir.isDirectory) return false
        val files = dir.listFiles()
        if (files != null && files.size > 0) {
            for (file in files) {
                if (file.isFile) {
                    if (!file.delete()) return false
                } else if (file.isDirectory) {
                    if (!deleteDir(file)) return false
                }
            }
        }
        return dir.delete()
    }

    /**
     * Delete the file.
     *
     * @param file The file.
     * @return `true`: success<br></br>`false`: fail
     */
    private fun deleteFile(file: File?): Boolean {
        return file != null && (!file.exists() || file.isFile && file.delete())
    }

    /**
     * Delete the all in directory.
     *
     * @param dirPath The path of directory.
     * @return `true`: success<br></br>`false`: fail
     */
    fun deleteAllInDir(dirPath: String?): Boolean {
        return deleteAllInDir(getFileByPath(dirPath))
    }

    /**
     * Delete the all in directory.
     *
     * @param dir The directory.
     * @return `true`: success<br></br>`false`: fail
     */
    fun deleteAllInDir(dir: File?): Boolean {
        return deleteFilesInDirWithFilter(
            dir
        ) { true }
    }

    /**
     * Delete all files in directory.
     *
     * @param dirPath The path of directory.
     * @return `true`: success<br></br>`false`: fail
     */
    fun deleteFilesInDir(dirPath: String?): Boolean {
        return deleteFilesInDir(getFileByPath(dirPath))
    }

    /**
     * Delete all files in directory.
     *
     * @param dir The directory.
     * @return `true`: success<br></br>`false`: fail
     */
    fun deleteFilesInDir(dir: File?): Boolean {
        return deleteFilesInDirWithFilter(
            dir
        ) { pathname -> pathname.isFile }
    }

    /**
     * Delete all files that satisfy the filter in directory.
     *
     * @param dirPath The path of directory.
     * @param filter  The filter.
     * @return `true`: success<br></br>`false`: fail
     */
    fun deleteFilesInDirWithFilter(
        dirPath: String?,
        filter: FileFilter?
    ): Boolean {
        return deleteFilesInDirWithFilter(getFileByPath(dirPath), filter)
    }

    /**
     * Delete all files that satisfy the filter in directory.
     *
     * @param dir    The directory.
     * @param filter The filter.
     * @return `true`: success<br></br>`false`: fail
     */
    fun deleteFilesInDirWithFilter(dir: File?, filter: FileFilter?): Boolean {
        if (dir == null || filter == null) return false
        // dir doesn't exist then return true
        if (!dir.exists()) return true
        // dir isn't a directory then return false
        if (!dir.isDirectory) return false
        val files = dir.listFiles()
        if (files != null && files.size != 0) {
            for (file in files) {
                if (filter.accept(file)) {
                    if (file.isFile) {
                        if (!file.delete()) return false
                    } else if (file.isDirectory) {
                        if (!deleteDir(file)) return false
                    }
                }
            }
        }
        return true
    }

    /**
     * Return the files in directory.
     *
     * Doesn't traverse subdirectories
     *
     * @param dirPath    The path of directory.
     * @param comparator The comparator to determine the order of the list.
     * @return the files in directory
     */
    /**
     * Return the files in directory.
     *
     * Doesn't traverse subdirectories
     *
     * @param dirPath The path of directory.
     * @return the files in directory
     */
    @JvmOverloads
    fun listFilesInDir(dirPath: String?, comparator: Comparator<File?>? = null): List<File?> {
        return listFilesInDir(getFileByPath(dirPath), false, comparator)
    }

    /**
     * Return the files in directory.
     *
     * Doesn't traverse subdirectories
     *
     * @param dir        The directory.
     * @param comparator The comparator to determine the order of the list.
     * @return the files in directory
     */
    /**
     * Return the files in directory.
     *
     * Doesn't traverse subdirectories
     *
     * @param dir The directory.
     * @return the files in directory
     */
    @JvmOverloads
    fun listFilesInDir(dir: File?, comparator: Comparator<File?>? = null): List<File?> {
        return listFilesInDir(dir, false, comparator)
    }

    /**
     * Return the files in directory.
     *
     * @param dirPath     The path of directory.
     * @param isRecursive True to traverse subdirectories, false otherwise.
     * @return the files in directory
     */
    fun listFilesInDir(dirPath: String?, isRecursive: Boolean): List<File?> {
        return listFilesInDir(getFileByPath(dirPath), isRecursive)
    }

    /**
     * Return the files in directory.
     *
     * @param dirPath     The path of directory.
     * @param isRecursive True to traverse subdirectories, false otherwise.
     * @param comparator  The comparator to determine the order of the list.
     * @return the files in directory
     */
    fun listFilesInDir(
        dirPath: String?,
        isRecursive: Boolean,
        comparator: Comparator<File?>?
    ): List<File?> {
        return listFilesInDir(getFileByPath(dirPath), isRecursive, comparator)
    }

    /**
     * Return the files in directory.
     *
     * @param dir         The directory.
     * @param isRecursive True to traverse subdirectories, false otherwise.
     * @param comparator  The comparator to determine the order of the list.
     * @return the files in directory
     */
    /**
     * Return the files in directory.
     *
     * @param dir         The directory.
     * @param isRecursive True to traverse subdirectories, false otherwise.
     * @return the files in directory
     */
    @JvmOverloads
    fun listFilesInDir(
        dir: File?,
        isRecursive: Boolean,
        comparator: Comparator<File?>? = null
    ): List<File?> {
        return listFilesInDirWithFilter(
            dir,
            { true }, isRecursive, comparator
        )
    }

    /**
     * Return the files that satisfy the filter in directory.
     *
     * Doesn't traverse subdirectories
     *
     * @param dirPath The path of directory.
     * @param filter  The filter.
     * @return the files that satisfy the filter in directory
     */
    fun listFilesInDirWithFilter(
        dirPath: String?,
        filter: FileFilter
    ): List<File?> {
        return listFilesInDirWithFilter(getFileByPath(dirPath), filter)
    }

    /**
     * Return the files that satisfy the filter in directory.
     *
     * Doesn't traverse subdirectories
     *
     * @param dirPath    The path of directory.
     * @param filter     The filter.
     * @param comparator The comparator to determine the order of the list.
     * @return the files that satisfy the filter in directory
     */
    fun listFilesInDirWithFilter(
        dirPath: String?,
        filter: FileFilter,
        comparator: Comparator<File?>?
    ): List<File?> {
        return listFilesInDirWithFilter(getFileByPath(dirPath), filter, comparator)
    }

    /**
     * Return the files that satisfy the filter in directory.
     *
     * Doesn't traverse subdirectories
     *
     * @param dir        The directory.
     * @param filter     The filter.
     * @param comparator The comparator to determine the order of the list.
     * @return the files that satisfy the filter in directory
     */
    fun listFilesInDirWithFilter(
        dir: File?,
        filter: FileFilter,
        comparator: Comparator<File?>?
    ): List<File?> {
        return listFilesInDirWithFilter(dir, filter, false, comparator)
    }

    /**
     * Return the files that satisfy the filter in directory.
     *
     * @param dirPath     The path of directory.
     * @param filter      The filter.
     * @param isRecursive True to traverse subdirectories, false otherwise.
     * @return the files that satisfy the filter in directory
     */
    fun listFilesInDirWithFilter(
        dirPath: String?,
        filter: FileFilter,
        isRecursive: Boolean
    ): List<File?> {
        return listFilesInDirWithFilter(getFileByPath(dirPath), filter, isRecursive)
    }

    /**
     * Return the files that satisfy the filter in directory.
     *
     * @param dirPath     The path of directory.
     * @param filter      The filter.
     * @param isRecursive True to traverse subdirectories, false otherwise.
     * @param comparator  The comparator to determine the order of the list.
     * @return the files that satisfy the filter in directory
     */
    fun listFilesInDirWithFilter(
        dirPath: String?,
        filter: FileFilter,
        isRecursive: Boolean,
        comparator: Comparator<File?>?
    ): List<File?> {
        return listFilesInDirWithFilter(getFileByPath(dirPath), filter, isRecursive, comparator)
    }

    /**
     * Return the files that satisfy the filter in directory.
     *
     * @param dir         The directory.
     * @param filter      The filter.
     * @param isRecursive True to traverse subdirectories, false otherwise.
     * @param comparator  The comparator to determine the order of the list.
     * @return the files that satisfy the filter in directory
     */
    /**
     * Return the files that satisfy the filter in directory.
     *
     * Doesn't traverse subdirectories
     *
     * @param dir    The directory.
     * @param filter The filter.
     * @return the files that satisfy the filter in directory
     */
    /**
     * Return the files that satisfy the filter in directory.
     *
     * @param dir         The directory.
     * @param filter      The filter.
     * @param isRecursive True to traverse subdirectories, false otherwise.
     * @return the files that satisfy the filter in directory
     */
    @JvmOverloads
    fun listFilesInDirWithFilter(
        dir: File?,
        filter: FileFilter,
        isRecursive: Boolean = false,
        comparator: Comparator<File?>? = null
    ): List<File?> {
        val files = listFilesInDirWithFilterInner(dir, filter, isRecursive)
        if (comparator != null) {
            Collections.sort(files, comparator)
        }
        return files
    }


    private fun listFilesInDirWithFilterInner(
        dir: File?,
        filter: FileFilter,
        isRecursive: Boolean
    ): List<File?> {
        val list: MutableList<File?> = ArrayList()
        if (!isDir(dir)) return list
        val files = dir!!.listFiles()
        if (files != null && files.size > 0) {
            for (file in files) {
                if (filter.accept(file)) {
                    list.add(file)
                }
                if (isRecursive && file.isDirectory) {
                    list.addAll(listFilesInDirWithFilterInner(file, filter, true))
                }
            }
        }
        return list
    }

    /**
     * Return the time that the file was last modified.
     *
     * @param filePath The path of file.
     * @return the time that the file was last modified
     */
    fun getFileLastModified(filePath: String?): Long {
        return getFileLastModified(getFileByPath(filePath))
    }

    /**
     * Return the time that the file was last modified.
     *
     * @param file The file.
     * @return the time that the file was last modified
     */
    fun getFileLastModified(file: File?): Long {
        if (file == null) return -1
        return file.lastModified()
    }

    /**
     * Return the charset of file simply.
     *
     * @param filePath The path of file.
     * @return the charset of file simply
     */
    fun getFileCharsetSimple(filePath: String?): String {
        return getFileCharsetSimple(getFileByPath(filePath))
    }

    /**
     * Return the charset of file simply.
     *
     * @param file The file.
     * @return the charset of file simply
     */
    fun getFileCharsetSimple(file: File?): String {
        if (file == null) return ""
        if (isUtf8(file)) return "UTF-8"
        var p = 0
        var `is`: InputStream? = null
        try {
            `is` = BufferedInputStream(FileInputStream(file))
            p = (`is`.read() shl 8) + `is`.read()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                `is`?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return when (p) {
            0xfffe -> "Unicode"
            0xfeff -> "UTF-16BE"
            else -> "GBK"
        }
    }

    /**
     * Return whether the charset of file is utf8.
     *
     * @param filePath The path of file.
     * @return `true`: yes<br></br>`false`: no
     */
    fun isUtf8(filePath: String?): Boolean {
        return isUtf8(getFileByPath(filePath))
    }

    /**
     * Return whether the charset of file is utf8.
     *
     * @param file The file.
     * @return `true`: yes<br></br>`false`: no
     */
    fun isUtf8(file: File?): Boolean {
        if (file == null) return false
        var `is`: InputStream? = null
        try {
            val bytes = ByteArray(24)
            `is` = BufferedInputStream(FileInputStream(file))
            val read = `is`.read(bytes)
            if (read != -1) {
                val readArr = ByteArray(read)
                System.arraycopy(bytes, 0, readArr, 0, read)
                return isUtf8(readArr) == 100
            } else {
                return false
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                `is`?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * UTF-8编码方式
     * ----------------------------------------------
     * 0xxxxxxx
     * 110xxxxx 10xxxxxx
     * 1110xxxx 10xxxxxx 10xxxxxx
     * 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
     */
    private fun isUtf8(raw: ByteArray): Int {
        var utf8 = 0
        var ascii = 0
        if (raw.size > 3) {
            if ((raw[0] == 0xEF.toByte()) && (raw[1] == 0xBB.toByte()) && (raw[2] == 0xBF.toByte())) {
                return 100
            }
        }
        val len = raw.size
        var child = 0
        var i = 0
        while (i < len) {
            // UTF-8 byte shouldn't be FF and FE
            if ((raw[i].toInt() and 0xFF.toByte().toInt()) == 0xFF.toByte()
                    .toInt() || (raw[i].toInt() and 0xFE.toByte().toInt()) == 0xFE.toByte()
                    .toInt()
            ) {
                return 0
            }
            if (child == 0) {
                // ASCII format is 0x0*******
                if ((raw[i].toInt() and 0x7F.toByte()
                        .toInt()) == raw[i].toInt() && raw[i].toInt() != 0
                ) {
                    ascii++
                } else if ((raw[i].toInt() and 0xC0.toByte().toInt()) == 0xC0.toByte()
                        .toInt()
                ) {
                    // 0x11****** maybe is UTF-8
                    for (bit in 0..7) {
                        if ((((0x80 shr bit).toByte()).toInt() and raw[i].toInt()) == ((0x80 shr bit).toByte()).toInt()) {
                            child = bit
                        } else {
                            break
                        }
                    }
                    utf8++
                }
                i++
            } else {
                child = if ((raw.size - i > child)) child else (raw.size - i)
                var currentNotUtf8 = false
                for (children in 0 until child) {
                    // format must is 0x10******
                    if ((raw[i + children].toInt() and (0x80.toByte()).toInt()) != (0x80.toByte()).toInt()) {
                        if ((raw[i + children].toInt() and 0x7F.toByte()
                                .toInt()) == raw[i + children].toInt() && raw[i].toInt() != 0
                        ) {
                            // ASCII format is 0x0*******
                            ascii++
                        }
                        currentNotUtf8 = true
                    }
                }
                if (currentNotUtf8) {
                    utf8--
                    i++
                } else {
                    utf8 += child
                    i += child
                }
                child = 0
            }
        }
        // UTF-8 contains ASCII
        if (ascii == len) {
            return 100
        }
        return (100 * ((utf8 + ascii).toFloat() / len.toFloat())).toInt()
    }

    /**
     * Return the number of lines of file.
     *
     * @param filePath The path of file.
     * @return the number of lines of file
     */
    fun getFileLines(filePath: String?): Int {
        return getFileLines(getFileByPath(filePath))
    }

    /**
     * Return the number of lines of file.
     *
     * @param file The file.
     * @return the number of lines of file
     */
    fun getFileLines(file: File?): Int {
        var count = 1
        var `is`: InputStream? = null
        try {
            `is` = BufferedInputStream(FileInputStream(file))
            val buffer = ByteArray(1024)
            var readChars: Int
            if (LINE_SEP!!.endsWith("\n")) {
                while ((`is`.read(buffer, 0, 1024).also { readChars = it }) != -1) {
                    for (i in 0 until readChars) {
                        if (buffer[i] == '\n'.code.toByte()) ++count
                    }
                }
            } else {
                while ((`is`.read(buffer, 0, 1024).also { readChars = it }) != -1) {
                    for (i in 0 until readChars) {
                        if (buffer[i] == '\r'.code.toByte()) ++count
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                `is`?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return count
    }

    /**
     * Return the size.
     *
     * @param filePath The path of file.
     * @return the size
     */
    fun getSize(filePath: String?): String {
        return getSize(getFileByPath(filePath))
    }

    /**
     * Return the size.
     *
     * @param file The directory.
     * @return the size
     */
    fun getSize(file: File?): String {
        if (file == null) return ""
        if (file.isDirectory) {
            return getDirSize(file)
        }
        return getFileSize(file)
    }

    /**
     * Return the size of directory.
     *
     * @param dir The directory.
     * @return the size of directory
     */
    private fun getDirSize(dir: File): String {
        val len = getDirLength(dir)
        return if (len == -1L) "" else UtilsBridge.byte2FitMemorySize(len)
    }

    /**
     * Return the size of file.
     *
     * @param file The file.
     * @return the length of file
     */
    private fun getFileSize(file: File): String {
        val len = getFileLength(file)
        return if (len == -1L) "" else UtilsBridge.byte2FitMemorySize(len)
    }

    /**
     * Return the length.
     *
     * @param filePath The path of file.
     * @return the length
     */
    fun getLength(filePath: String?): Long {
        return getLength(getFileByPath(filePath))
    }

    /**
     * Return the length.
     *
     * @param file The file.
     * @return the length
     */
    fun getLength(file: File?): Long {
        if (file == null) return 0
        if (file.isDirectory) {
            return getDirLength(file)
        }
        return getFileLength(file)
    }

    /**
     * Return the length of directory.
     *
     * @param dir The directory.
     * @return the length of directory
     */
    private fun getDirLength(dir: File): Long {
        if (!isDir(dir)) return 0
        var len: Long = 0
        val files = dir.listFiles()
        if (files != null && files.size > 0) {
            for (file in files) {
                len += if (file.isDirectory) {
                    getDirLength(file)
                } else {
                    file.length()
                }
            }
        }
        return len
    }

    /**
     * Return the length of file.
     *
     * @param filePath The path of file.
     * @return the length of file
     */
    fun getFileLength(filePath: String): Long {
        val isURL = filePath.matches("[a-zA-z]+://[^\\s]*".toRegex())
        if (isURL) {
            try {
                val conn = URL(filePath).openConnection() as HttpsURLConnection
                conn.setRequestProperty("Accept-Encoding", "identity")
                conn.connect()
                if (conn.responseCode == 200) {
                    return conn.contentLength.toLong()
                }
                return -1
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return getFileLength(getFileByPath(filePath))
    }

    /**
     * Return the length of file.
     *
     * @param file The file.
     * @return the length of file
     */
    private fun getFileLength(file: File?): Long {
        if (!isFile(file)) return -1
        return file!!.length()
    }

    /**
     * Return the MD5 of file.
     *
     * @param filePath The path of file.
     * @return the md5 of file
     */
    fun getFileMD5ToString(filePath: String?): String {
        val file = if (UtilsBridge.isSpace(filePath)) null else File(filePath!!)
        return getFileMD5ToString(file)
    }
    /**
     * Return the MD5 of file.
     *
     * @param file The file.
     * @return the md5 of file
     */
    fun getFileMD5ToString(file: File?): String {
        return ConvertUtils.bytes2HexString(getFileMD5(file))
    }

    /**
     * Return the MD5 of file.
     *
     * @param filePath The path of file.
     * @return the md5 of file
     */
    fun getFileMD5(filePath: String?): ByteArray? {
        return getFileMD5(getFileByPath(filePath))
    }

    /**
     * Return the MD5 of file.
     *
     * @param file The file.
     * @return the md5 of file
     */
    fun getFileMD5(file: File?): ByteArray? {
        if (file == null) return null
        var dis: DigestInputStream? = null
        try {
            val fis = FileInputStream(file)
            var md = MessageDigest.getInstance("MD5")
            dis = DigestInputStream(fis, md)
            val buffer = ByteArray(1024 * 256)
            while (true) {
                if (dis.read(buffer) <= 0) break
            }
            md = dis.messageDigest
            return md.digest()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                dis?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    /**
     * Return the file's path of directory.
     *
     * @param file The file.
     * @return the file's path of directory
     */
    fun getDirName(file: File?): String {
        if (file == null) return ""
        return getDirName(file.absolutePath)
    }

    /**
     * Return the file's path of directory.
     *
     * @param filePath The path of file.
     * @return the file's path of directory
     */
    fun getDirName(filePath: String): String {
        if (UtilsBridge.isSpace(filePath)) return ""
        val lastSep = filePath.lastIndexOf(File.separator)
        return if (lastSep == -1) "" else filePath.substring(0, lastSep + 1)
    }

    /**
     * Return the name of file.
     *
     * @param file The file.
     * @return the name of file
     */
    fun getFileName(file: File?): String {
        if (file == null) return ""
        return getFileName(file.absolutePath)
    }

    /**
     * Return the name of file.
     *
     * @param filePath The path of file.
     * @return the name of file
     */
    fun getFileName(filePath: String): String {
        if (UtilsBridge.isSpace(filePath)) return ""
        val lastSep = filePath.lastIndexOf(File.separator)
        return if (lastSep == -1) filePath else filePath.substring(lastSep + 1)
    }

    /**
     * Return the name of file without extension.
     *
     * @param file The file.
     * @return the name of file without extension
     */
    fun getFileNameNoExtension(file: File?): String {
        if (file == null) return ""
        return getFileNameNoExtension(file.path)
    }

    /**
     * Return the name of file without extension.
     *
     * @param filePath The path of file.
     * @return the name of file without extension
     */
    fun getFileNameNoExtension(filePath: String): String {
        if (UtilsBridge.isSpace(filePath)) return ""
        val lastPoi = filePath.lastIndexOf('.')
        val lastSep = filePath.lastIndexOf(File.separator)
        if (lastSep == -1) {
            return (if (lastPoi == -1) filePath else filePath.substring(0, lastPoi))
        }
        if (lastPoi == -1 || lastSep > lastPoi) {
            return filePath.substring(lastSep + 1)
        }
        return filePath.substring(lastSep + 1, lastPoi)
    }

    /**
     * Return the extension of file.
     *
     * @param file The file.
     * @return the extension of file
     */
    fun getFileExtension(file: File?): String {
        if (file == null) return ""
        return getFileExtension(file.path)
    }

    /**
     * Return the extension of file.
     *
     * @param filePath The path of file.
     * @return the extension of file
     */
    fun getFileExtension(filePath: String): String {
        if (UtilsBridge.isSpace(filePath)) return ""
        val lastPoi = filePath.lastIndexOf('.')
        val lastSep = filePath.lastIndexOf(File.separator)
        if (lastPoi == -1 || lastSep >= lastPoi) return ""
        return filePath.substring(lastPoi + 1)
    }

    /**
     * Notify system to scan the file.
     *
     * @param filePath The path of file.
     */
    fun notifySystemToScan(filePath: String?) {
        notifySystemToScan(getFileByPath(filePath))
    }

    /**
     * Notify system to scan the file.
     *
     * @param file The file.
     */
    fun notifySystemToScan(file: File?) {
        if (file == null || !file.exists()) return
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intent.setData(Uri.parse("file://" + file.absolutePath))
        LogHelpr.application.sendBroadcast(intent)
    }

    /**
     * Return the total size of file system.
     *
     * @param anyPathInFs Any path in file system.
     * @return the total size of file system
     */
    @SuppressLint("ObsoleteSdkInt")
    fun getFsTotalSize(anyPathInFs: String?): Long {
        if (TextUtils.isEmpty(anyPathInFs)) return 0
        val statFs = StatFs(anyPathInFs)
        val blockSize: Long
        val totalSize: Long
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = statFs.blockSizeLong
            totalSize = statFs.blockCountLong
        } else {
            blockSize = statFs.blockSize.toLong()
            totalSize = statFs.blockCount.toLong()
        }
        return blockSize * totalSize
    }

    /**
     * Return the available size of file system.
     *
     * @param anyPathInFs Any path in file system.
     * @return the available size of file system
     */
    @SuppressLint("ObsoleteSdkInt")
    fun getFsAvailableSize(anyPathInFs: String?): Long {
        if (TextUtils.isEmpty(anyPathInFs)) return 0
        val statFs = StatFs(anyPathInFs)
        val blockSize: Long
        val availableSize: Long
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = statFs.blockSizeLong
            availableSize = statFs.availableBlocksLong
        } else {
            blockSize = statFs.blockSize.toLong()
            availableSize = statFs.availableBlocks.toLong()
        }
        return blockSize * availableSize
    }
}

