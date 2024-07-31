/*
 * Copyright (c) 2023. Bernard Bou
 */
package org.treebolic

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes

typealias Service = HashMap<String, Any?>

object ServiceKeys {

    const val NAME: String = "name"
    const val PROCESS: String = "process"
    const val PACKAGE: String = "package"
    const val FLAGS: String = "flags"
    const val EXPORTED: String = "exported"
    const val ENABLED: String = "enabled"
    const val PERMISSION: String = "permission"
    const val LABEL: String = "label"
    const val DESCRIPTION: String = "description"
    const val ICON: String = "icon"
    const val LOGO: String = "logo"
    const val DRAWABLE: String = "drawable"
}

/**
 * List of Services
 *
 * @author Bernard Bou
 */
object Services {

    /**
     * Log tag
     */
    private const val TAG = "Services"

    /**
     * Data
     */
    private var services: Collection<Service>? = null

    /**
     * Load icon
     *
     * @param packageManager package manager
     * @param packageName    package name
     * @param iconRes        icon id
     * @return drawable
     */
    @JvmStatic
    fun loadIcon(packageManager: PackageManager, packageName: String?, @DrawableRes iconRes: Int): Drawable? {
        if (iconRes != 0) {
            return packageManager.getDrawable(packageName!!, iconRes, null)
        }
        return packageManager.defaultActivityIcon
    }

    /**
     * Load label
     *
     * @param packageName package name
     * @param labelRes    label id
     * @return label
     */
    private fun loadText(packageManager: PackageManager, packageName: String, @StringRes labelRes: Int): String {
        if (labelRes != 0) {
            val label = packageManager.getText(packageName, labelRes, null)
            return label?.toString() ?: "null"
        }
        return "?"
    }

    /**
     * List services
     *
     * @param context context
     * @param filter  positive filter
     * @return collection of services
     */
    private fun collectServices(context: Context, filter: String?): Collection<Service> {
        val services2: MutableCollection<Service> = ArrayList()
        val packageManager = context.packageManager

        @SuppressLint("QueryPermissionsNeeded") val pkgs = packageManager.getInstalledPackages(PackageManager.GET_SERVICES)
        for (pkg in pkgs) {
            val services = pkg.services
            if (services != null && (filter == null || pkg.packageName.matches(filter.toRegex()))) {
                for (service in services) {
                    if (filter == null || service.name.matches(filter.toRegex())) {
                        val service2 = Service()
                        service2[ServiceKeys.NAME] = service.name
                        service2[ServiceKeys.PACKAGE] = pkg.packageName
                        service2[ServiceKeys.PROCESS] = service.processName
                        service2[ServiceKeys.ENABLED] = service.enabled
                        service2[ServiceKeys.EXPORTED] = service.exported
                        service2[ServiceKeys.FLAGS] = Integer.toHexString(service.flags)
                        service2[ServiceKeys.PERMISSION] = service.permission
                        service2[ServiceKeys.LABEL] = loadText(packageManager, pkg.packageName, service.labelRes)
                        service2[ServiceKeys.DESCRIPTION] = loadText(packageManager, pkg.packageName, service.descriptionRes)
                        service2[ServiceKeys.LOGO] = service.logo
                        service2[ServiceKeys.ICON] = service.icon
                        service2[ServiceKeys.DRAWABLE] = pkg.packageName + '#' + service.icon

                        services2.add(service2)
                    } else {
                        Log.d(TAG, "Dropped $service")
                    }
                }
            }
        }
        return services2
    }

    /**
     * Make adapter
     *
     * @param context       context
     * @param itemLayoutRes item layout
     * @param from          from key
     * @param to            to res id
     * @return base adapter
     */
    @JvmStatic
    fun makeAdapter(context: Context, @LayoutRes itemLayoutRes: Int, from: Array<String>, to: IntArray): SimpleAdapter? {
        // data
        val services = getServices(context) ?: return null
        if (services.isEmpty()) {
            Toast.makeText(context, R.string.error_no_services, Toast.LENGTH_SHORT).show()
        }

        // fill in the grid_item layout
        return object : SimpleAdapter(
            context,
            ArrayList(services),
            itemLayoutRes,
            from, to
        ) {
            override fun setViewImage(v: ImageView, value: String) {
                try {
                    val fields = value.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val index = fields[1].toInt()
                    val drawable = loadIcon(context.packageManager, fields[0], index)
                    v.setImageDrawable(drawable)
                } catch (ignored: Exception) {
                    //
                }
            }
        }
    }

    /**
     * Get (possibly cached) list of services
     *
     * @param context context
     * @return list of services
     */
    @JvmStatic
    fun getServices(context: Context): Collection<Service>? {
        if (services != null) {
            return services
        }
        services = buildServices(context)
        return services
    }

    /**
     * Build collection of services
     *
     * @param context context
     * @return collection of services
     */
    private fun buildServices(context: Context): Collection<Service>? {
        try {
            return collectServices(context, "org.treebolic\\..*")
        } catch (e: Exception) {
            Log.e(TAG, "Error when scanning for services", e)
            return null
        }
    }
}
