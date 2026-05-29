package com.taylorz.logutils
import java.io.File

object EmptyFolderCleaner {

    /**
     * 删除指定路径下所有空文件夹
     *
     * @param path 根目录路径
     * @return 删除的空文件夹数量
     */
    fun deleteEmptyFolders(path: String): Int {
        val root = File(path)
        if (!root.exists() || !root.isDirectory) {
            return 0
        }
        return deleteRecursively(root)
    }

    /**
     * 递归删除空文件夹
     */
    private fun deleteRecursively(dir: File): Int {
        var deletedCount = 0

        val files = dir.listFiles() ?: return 0

        // 先递归处理子目录
        for (file in files) {
            if (file.isDirectory) {
                deletedCount += deleteRecursively(file)
            }
        }

        // 重新获取一次，防止子目录已被删除
        val currentFiles = dir.listFiles()

        // 如果当前目录为空，则删除
        if (currentFiles != null && currentFiles.isEmpty()) {
            if (dir.delete()) {
                deletedCount++
                println("已删除空文件夹: ${dir.absolutePath}")
            }
        }

        return deletedCount
    }
}