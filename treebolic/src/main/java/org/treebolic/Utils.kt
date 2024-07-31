/*
 * Copyright (c) 2023. Bernard Bou
 */
package org.treebolic

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object Utils {

    /**
     * Get process name
     *
     * @param context context
     * @return process name
     */
    @Throws(PackageManager.NameNotFoundException::class)
    fun getProcessName(context: Context): String {
        val pkgName = BuildConfig.APPLICATION_ID
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) //
            pm.getApplicationInfo(pkgName, PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())) else  //
            pm.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
        return info.processName
    }
}
