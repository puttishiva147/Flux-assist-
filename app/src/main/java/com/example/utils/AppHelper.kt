package com.example.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null
)

object AppHelper {

    private val appItemCache = ConcurrentHashMap<String, AppItem>()
    @Volatile
    private var cachedLauncherApps: List<AppItem>? = null
    @Volatile
    private var lastLauncherAppsScanTime = 0L

    private val backgroundExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "assistx-app-loader").apply {
            priority = Thread.MIN_PRIORITY
            isDaemon = true
        }
    }

    /**
     * Warms up the app cache in background to prevent UI-thread hitch when opening menus.
     */
    fun preloadAppsAsync(context: Context) {
        backgroundExecutor.execute {
            getInstalledLauncherApps(context)
        }
    }

    fun getInstalledLauncherApps(context: Context): List<AppItem> {
        val now = SystemClock.uptimeMillis()
        cachedLauncherApps?.let { cached ->
            if (now - lastLauncherAppsScanTime < 60_000L) {
                return cached
            }
        }

        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val list = mutableListOf<AppItem>()

            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) continue // Skip our own app

                val cachedItem = appItemCache[pkg]
                if (cachedItem != null) {
                    list.add(cachedItem)
                    continue
                }

                val name = info.loadLabel(pm).toString()
                val icon = try {
                    info.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }
                val item = AppItem(packageName = pkg, appName = name, icon = icon)
                appItemCache[pkg] = item
                list.add(item)
            }

            val sorted = list.sortedBy { it.appName.lowercase() }
            cachedLauncherApps = sorted
            lastLauncherAppsScanTime = now
            sorted
        } catch (e: Exception) {
            cachedLauncherApps ?: emptyList()
        }
    }

    fun getAppItem(context: Context, packageName: String): AppItem? {
        appItemCache[packageName]?.let { return it }

        val pm = context.packageManager
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val name = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            val item = AppItem(packageName, name, icon)
            appItemCache[packageName] = item
            item
        } catch (e: Exception) {
            null
        }
    }

    fun getAppItemAsync(context: Context, packageName: String, onLoaded: (AppItem) -> Unit) {
        appItemCache[packageName]?.let {
            onLoaded(it)
            return
        }

        backgroundExecutor.execute {
            val item = getAppItem(context, packageName)
            if (item != null) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onLoaded(item)
                }
            }
        }
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getDefaultQuickApps(context: Context): List<String> {
        val preferred = listOf(
            "com.android.chrome",
            "com.google.android.youtube",
            "com.whatsapp",
            "com.google.android.apps.maps",
            "com.google.android.apps.photos",
            "com.android.settings",
            "com.google.android.dialer"
        )
        val cached = cachedLauncherApps
        if (cached == null) {
            // Asynchronously warm up launcher apps cache without freezing UI thread
            preloadAppsAsync(context)
            return preferred.take(4)
        }
        val installed = cached.map { it.packageName }
        val selected = preferred.filter { installed.contains(it) }.take(4)
        return if (selected.isNotEmpty()) {
            selected
        } else {
            installed.take(4)
        }
    }
}

