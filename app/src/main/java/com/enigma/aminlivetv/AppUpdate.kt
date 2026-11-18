package com.enigma.aminlivetv

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

// Downloads a newer release APK, accepts it only if it is this app, newer, and signed with the
// same certificate, then hands it to Android's package installer, which asks the viewer to confirm.
internal class AppUpdate(private val context: Context, private val onFailed: () -> Unit) {
    private val folder = File(context.cacheDir, "update")

    // Blocking: call off the main thread. Returns the verified APK, or null.
    fun download(tag: String): File? = runCatching {
        folder.mkdirs()
        val target = File(folder, "Amin-Live-TV-$tag.apk")
        folder.listFiles()?.filter { it != target }?.forEach { it.delete() }
        if (target.isFile && verified(target)) return target
        val partial = File(folder, "${target.name}.part")
        val connection = URI(ReleaseCheck.apkUrl(tag)).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "Amin-Live-TV/${BuildConfig.VERSION_NAME}")
            check(connection.responseCode == HttpURLConnection.HTTP_OK)
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val size = input.read(buffer)
                        if (size < 0) break
                        total += size
                        check(total <= MAX_BYTES)
                        output.write(buffer, 0, size)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        check(partial.renameTo(target))
        if (verified(target)) target else null.also { target.delete() }
    }.getOrElse {
        Log.w("AminUpdate", "Update download failed", it)
        null
    }

    private fun verified(apk: File): Boolean {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
            else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        val candidate = pm.getPackageArchiveInfo(apk.path, flags) ?: return false
        val installed = pm.getPackageInfo(context.packageName, flags)
        val ok = candidate.packageName == context.packageName &&
            versionCode(candidate) > versionCode(installed) &&
            certificates(candidate).isNotEmpty() && certificates(candidate) == certificates(installed)
        if (!ok) Log.w("AminUpdate", "Rejected ${apk.name}: not a newer copy signed with this app's key")
        return ok
    }

    @Suppress("DEPRECATION")
    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun certificates(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners
        } else {
            info.signatures
        }
        return signatures.orEmpty().map { it.toCharsString() }.toSet()
    }

    // Android shows its own confirmation, and on first use asks to allow installs from this app.
    // The installer reports back through MainActivity. After a successful update Android 11 returns
    // to the TV's home screen; it blocks apps from reopening themselves from the background.
    fun install(apk: File) {
        runCatching {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                .apply { setAppPackageName(context.packageName) }
            val id = installer.createSession(params)
            installer.openSession(id).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("update.apk", 0, apk.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                val status = Intent(context, MainActivity::class.java)
                    .setAction(ACTION_STATUS)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val mutable = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
                val pending = PendingIntent.getActivity(context, id, status, PendingIntent.FLAG_UPDATE_CURRENT or mutable)
                session.commit(pending.intentSender)
            }
        }.onFailure {
            Log.w("AminUpdate", "Could not start the installer", it)
            onFailed()
        }
    }

    // Handles the installer's report. Returns false for any other intent.
    fun handle(intent: Intent?): Boolean {
        if (intent?.action != ACTION_STATUS) return false
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirm != null) context.startActivity(confirm) else onFailed()
            }
            // Cancel leaves the button so the viewer can try again.
            PackageInstaller.STATUS_SUCCESS, PackageInstaller.STATUS_FAILURE_ABORTED -> Unit
            else -> {
                Log.w("AminUpdate", "Install failed: $status ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
                onFailed()
            }
        }
        return true
    }

    private companion object {
        const val ACTION_STATUS = "com.enigma.aminlivetv.UPDATE_STATUS"
        const val MAX_BYTES = 64L * 1024 * 1024
    }
}
