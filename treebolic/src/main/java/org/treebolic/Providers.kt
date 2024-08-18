/*
 * Copyright (c) 2023. Bernard Bou
 */
package org.treebolic

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import android.widget.SimpleAdapter
import androidx.annotation.LayoutRes
import org.treebolic.storage.Storage.getTreebolicStorage
import java.io.IOException
import java.util.Properties
import java.util.TreeMap

typealias Provider = HashMap<String, Any?>

object Providers {

    private const val TAG = "Providers"

    const val NAME: String = "name"
    const val PROVIDER: String = "provider"
    const val DESCRIPTION: String = "description"
    const val ICON: String = "icon"
    const val MIMETYPE: String = "mimetype"
    const val URLSCHEME: String = "schema"
    const val EXTENSIONS: String = "extensions"

    const val PROCESS: String = "process"
    const val PACKAGE: String = "package"

    const val SOURCE: String = "source"
    const val BASE: String = "base"
    const val IMAGEBASE: String = "imagebase"
    const val SETTINGS: String = "settings"

    const val STYLE: String = "style"

    /**
     * Make provider
     *
     * @param props properties
     * @param base base
     * @param imageBase image
     * @param process process
     * @return provider
     */
    private fun make(props: Properties, base: String?, imageBase: String?, process: String?): Provider {
        val p = Provider()

        // structural
        val name = props.getProperty(NAME)
        val provider = props.getProperty(PROVIDER)
        val description = props.getProperty(DESCRIPTION)
        val mimeType = props.getProperty(MIMETYPE)
        val icon = props.getProperty(ICON)
        val scheme = props.getProperty(URLSCHEME)
        val extensions = props.getProperty(EXTENSIONS)
        p[PROVIDER] = provider
        p[NAME] = name
        p[DESCRIPTION] = description
        p[ICON] = icon
        p[MIMETYPE] = mimeType
        p[EXTENSIONS] = extensions
        p[URLSCHEME] = scheme
        p[PROCESS] = process
        p[PACKAGE] = BuildConfig.APPLICATION_ID

        // data
        val source = props.getProperty(SOURCE)
        val settings = props.getProperty(SETTINGS)
        p[SOURCE] = source
        p[SETTINGS] = settings
        p[BASE] = base
        p[IMAGEBASE] = imageBase
        return p
    }

    /**
     * Data
     */
    private var providersByClass: Map<String, Provider>? = null

    /**
     * Get provider from class name key
     *
     * @param key class name key
     * @return provider
     */
    fun get(key: String): Provider? {
        checkNotNull(providersByClass)
        return providersByClass!![key]
    }

    /**
     * Get (possibly cached) map of providers
     *
     * @param context context
     * @return map of providers
     */
    private fun getProvidersByClass(context: Context): Map<String, Provider>? {
        if (providersByClass != null) {
            return providersByClass
        }
        try {
            providersByClass = buildProvidersFromManifests(context)
            return providersByClass
        } catch (e: Exception) {
            Log.d(TAG, "When scanning for providers: " + e.message)
            return null
        }
    }

    /**
     * Get (possibly cached) list of providers
     *
     * @param context context
     * @return list of providers
     */
    fun getProviders(context: Context): Collection<Provider>? {
        val providersMap = getProvidersByClass(context)
        return providersMap?.values
    }

    // F R O M   M A N I F E S T S

    private const val ASSET_DIR = "providers"

    private const val ASSET_IMAGE_DIR = "providers_images"

    private fun buildProvidersFromManifests(context: Context): Map<String, Provider>? {
        var result: MutableMap<String, Provider>? = null

        // base and image base in external storage
        val treebolicStorage = getTreebolicStorage(context)
        val base = Uri.fromFile(treebolicStorage).toString() + '/'

        try {
            val process = Utils.getProcessName(context)

            val assetManager = context.assets
            val manifests = checkNotNull(assetManager.list(ASSET_DIR))
            for (manifest in manifests) {
                Log.i(TAG, "Reading $manifest")
                try {
                    assetManager.open("$ASSET_DIR/$manifest").use { input ->
                        val props = Properties()
                        props.load(input)
                        val provider = make(props, base, base, process)

                        // record
                        if (result == null) {
                            result = TreeMap(Comparator.reverseOrder())
                        }
                        val key = checkNotNull(provider[PROVIDER])
                        result!!.put(key.toString(), provider)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error while reading $manifest", e)
                }
            }
            return result
        } catch (e: IOException) {
            Log.e(TAG, "Error while listing assets", e)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Error while getting process name", e)
        }
        return null
    }

    // D R A W A B L E

    fun readAssetDrawable(context: Context, imageFile: String): Drawable? {
        try {
            context.assets.open("$ASSET_IMAGE_DIR/$imageFile").use { input ->
                return Drawable.createFromResourceStream(context.resources, null, input, null)
            }
        } catch (ignored: IOException) {
        }
        return null
    }

    // A D A P T E R   F A C T O R Y

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
    fun makeAdapter(context: Context, @LayoutRes itemLayoutRes: Int, from: Array<String>, to: IntArray?): SimpleAdapter? {
        // data
        val providers = getProviders(context)

        // adapter
        return makeAdapter(context, providers, itemLayoutRes, from, to)
    }

    /**
     * Make adapter
     *
     * @param context   context
     * @param providers providers
     * @param itemRes   item layout
     * @param from      from key
     * @param to        to res id
     * @return base adapter
     */
    private fun makeAdapter(context: Context, providers: Collection<Provider>?, itemRes: Int, from: Array<String>, to: IntArray?): SimpleAdapter? {
        // data
        if (providers == null) {
            return null
        }

        // fill in the grid_item layout
        return object : SimpleAdapter(
            context, ArrayList(providers), itemRes, from, to
        ) {
            override fun setViewImage(imageView: ImageView, value: String) {
                try {
                    val drawable = readAssetDrawable(context, value)
                    imageView.setImageDrawable(drawable)
                } catch (ignored: Exception) {
                    //
                }
            }
        }
    }
}
