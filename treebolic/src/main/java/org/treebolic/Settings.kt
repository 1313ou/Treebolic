/*
 * Copyright (c) 2023. Bernard Bou
 */
package org.treebolic

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.preference.PreferenceManager
import java.net.MalformedURLException
import java.net.URL

/**
 * Settings
 *
 * @author Bernard Bou
 */
object Settings {

    /**
     * Demo
     */
    const val DEMO: String = "demo.zip"

    /**
     * Initialized preference name
     */
    const val PREF_INITIALIZED: String = "pref_initialized_" + BuildConfig.VERSION_NAME

    /**
     * First preference name
     */
    const val PREF_FIRSTRUN: String = "pref_first_run"

    /**
     * Provider preference name
     */
    const val PREF_PROVIDER: String = "pref_provider"

    /**
     * Provider icon preference name
     */
    const val PREF_PROVIDER_ICON: String = "pref_provider_icon"

    /**
     * Active provider preference name
     */
    const val PREF_PROVIDER_NAME: String = "pref_provider_name"

    /**
     * Service preference name
     */
    const val PREF_SERVICE: String = "pref_service"

    /**
     * Service source preference
     */
    const val PREF_SERVICE_SOURCE: String = "pref_service_source"

    /**
     * Preference file prefix
     */
    const val PREF_FILE_PREFIX: String = "org.treebolic_preferences_"

    /**
     * Download base preference name
     */
    const val PREF_DOWNLOAD_BASE: String = "pref_download_base"

    /**
     * Download file preference name
     */
    const val PREF_DOWNLOAD_FILE: String = "pref_download_file"

    /**
     * Default CSS
     */
    const val STYLE_DEFAULT: String = ".content { }\n" +  //
            ".link {color: #FFA500;font-size: small;}\n" +  //
            ".linking {color: #FFA500; font-size: small; }" +  //
            ".mount {color: #CD5C5C; font-size: small;}" +  //
            ".mounting {color: #CD5C5C; font-size: small; }" +  //
            ".searching {color: #FF7F50; font-size: small; }"

    /**
     * Clear provider SharedPreferences
     *
     * @param context context
     */
    @SuppressLint("ApplySharedPref")
    fun clearProviderSettings(context: Context) {
        // providers
        val providers = Providers.getProviders(context)

        // clear prefs for providers
        if (providers != null) {
            for (provider in providers) {
                // provider shared preferences
                val providerSharedPrefs = context.getSharedPreferences(getSharedPreferencesName(provider), Context.MODE_PRIVATE)
                providerSharedPrefs.edit().clear().commit()
            }
        }
    }

    /**
     * Set providers default settings from provider data
     *
     * @param context context
     */
    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    fun setDefaults(context: Context) {
        // providers
        val providers = Providers.getProviders(context)

        // create prefs for built-in providers
        if (providers != null) {
            for (provider in providers) {
                // provider shared preferences
                val providerSharedPrefs = context.getSharedPreferences(getSharedPreferencesName(provider), Context.MODE_PRIVATE)

                // commit non existent values
                val editor = providerSharedPrefs.edit()
                editor.clear()

                val keys = arrayOf(TreebolicIface.PREF_SOURCE, TreebolicIface.PREF_BASE, TreebolicIface.PREF_IMAGEBASE, TreebolicIface.PREF_SETTINGS, PREF_PROVIDER)
                val providerKeys = arrayOf(Providers.SOURCE, Providers.BASE, Providers.IMAGEBASE, Providers.SETTINGS, Providers.PROVIDER)
                for (j in keys.indices) {
                    val key = keys[j]
                    val value = provider[providerKeys[j]]
                    editor.putString(key, value.toString())
                }

                editor.commit()
            }
        }
    }

    /**
     * Set active provider settings (copied into default preferences)
     *
     * @param context  context
     * @param provider active provider
     */
    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    fun setActivePrefs(context: Context, provider: Provider) {
        val defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = defaultSharedPrefs.edit()
        val providerSharedPrefs = context.getSharedPreferences(getSharedPreferencesName(provider), Context.MODE_PRIVATE)
        val providerClass = providerSharedPrefs.getString(PREF_PROVIDER, null)
        editor.putString(PREF_PROVIDER, providerClass)

        val keys = arrayOf(TreebolicIface.PREF_SOURCE, TreebolicIface.PREF_BASE, TreebolicIface.PREF_IMAGEBASE, TreebolicIface.PREF_SETTINGS)
        for (key in keys) {
            val value = providerSharedPrefs.getString(key, null)
            editor.putString(key, value)
        }
        editor.commit()
    }

    /**
     * Put string preference
     *
     * @param context context
     * @param key     key
     * @param value   value
     */
    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    fun putStringPref(context: Context, key: String?, value: String?) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPref.edit().putString(key, value).commit()
    }

    /**
     * Put integer preference
     *
     * @param context context
     * @param key     key
     * @param value   value
     */
    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    fun putIntPref(context: Context, key: String?, value: Int) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPref.edit().putInt(key, value).commit()
    }

    /**
     * Get string preference
     *
     * @param context context
     * @param key     key
     * @return value
     */
    @JvmStatic
    fun getStringPref(context: Context, key: String?): String? {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPref.getString(key, null)
    }

    /**
     * Get int preference
     *
     * @param context context
     * @param key     key
     * @return value
     */
    fun getIntPref(context: Context, key: String?): Int {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPref.getInt(key, 0)
    }

    /**
     * Get preference value as url
     *
     * @param context context
     * @param key     key
     * @return preference value as
     */
    @JvmStatic
    fun getURLPref(context: Context, key: String?): URL? {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val result = sharedPref.getString(key, null)
        return makeURL(result)
    }

    // U T I L S

    /**
     * Make URL from string
     *
     * @param url url string
     * @return url
     */
    private fun makeURL(url: String?): URL? {
        return try {
            URL(url)
        } catch (ignored: MalformedURLException) {
            null
        }
    }

    /**
     * Application settings
     *
     * @param context context
     * @param pkgName package name
     */
    fun applicationSettings(context: Context, pkgName: String) {
        val apiLevel = Build.VERSION.SDK_INT
        val intent = Intent()

        if (apiLevel >= 9) {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.setData(Uri.parse("package:$pkgName"))
        } else {
            val appPkgName = if (apiLevel == 8) "pkg" else "com.android.settings.ApplicationPkgName"

            intent.setAction(Intent.ACTION_VIEW)
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails")
            intent.putExtra(appPkgName, pkgName)
        }

        // start activity
        context.startActivity(intent)
    }

    fun getSharedPreferencesName(provider: Provider): String {
        return PREF_FILE_PREFIX + provider[Providers.PROVIDER]
    }
}
