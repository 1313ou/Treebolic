/*
 * Copyright (c) 2023. Bernard Bou
 */
package org.treebolic

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Permission helper
 *
 * @author [Bernard Bou](mailto:1313ou@gmail.com)
 */
internal object Permissions {

    private const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1313

    fun check(activity: Activity): Boolean {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
            return false
        }
        return true
    }
}
