package com.dark.darknama.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/** Shares only the downloaded APK through FileProvider; no file:// or storage permission. */
object UpdateInstaller {
    enum class Result { STARTED, NEEDS_PERMISSION, MISSING_FILE, FAILED }

    fun install(context: Context, apk: File): Result {
        if (!apk.isFile || apk.length() == 0L) return Result.MISSING_FILE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()) return Result.NEEDS_PERMISSION
        return try {
            val uri = FileProvider.getUriForFile(context,
                "${context.packageName}.fileprovider", apk)
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            Result.STARTED
        } catch (_: Exception) {
            Result.FAILED
        }
    }

    fun permissionSettings(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"))
}
