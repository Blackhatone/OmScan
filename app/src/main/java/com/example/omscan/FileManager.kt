package com.example.omscan

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileManager {
    private const val SCANS_DIR = "scans"

    fun getScansDirectory(context: Context): File {
        val dir = File(context.filesDir, SCANS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun generateFileName(extension: String): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "SCAN_$timeStamp.$extension"
    }

    fun getAllScans(context: Context): List<File> {
        return getScansDirectory(context).listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteFile(file: File): Boolean {
        return if (file.exists()) {
            file.delete()
        } else false
    }

    fun renameFile(file: File, newName: String): File? {
        val extension = file.extension
        val nameWithoutExtension = if (newName.endsWith(".$extension", ignoreCase = true)) {
            newName.substring(0, newName.length - extension.length - 1)
        } else newName
        
        val newFile = File(file.parentFile, "$nameWithoutExtension.$extension")
        return if (file.renameTo(newFile)) {
            newFile
        } else null
    }
}
