/*
 * Copyright (c) 2023. Bernard Bou
 */
package org.treebolic

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import org.treebolic.Services.getServices
import org.treebolic.Settings.getSharedPreferencesName
import org.treebolic.preference.OpenEditTextPreference
import org.treebolic.preference.OpenEditTextPreference.Companion.onDisplayPreferenceDialog

/**
 * Settings activity
 *
 * @author Bernard Bou
 */
class SettingsActivity : AppCompatCommonPreferenceActivity() {

    // L I F E C Y C L E

    override fun onCreate(savedInstanceState: Bundle?) {
        // super
        super.onCreate(savedInstanceState)

        // read args
        val action = intent.action
        if (action == null) {
            val intent = intent

            val key = intent.getStringExtra(ARG_PROVIDER_SELECTED)
            provider = if (key == null) null else Providers.get(key)
        }
    }

    // F R A G M E N T S

    class ActivePreferenceFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            if (provider == null) {
                return
            }

            // activity
            val activity = activity as SettingsActivity?
            if (activity != null) {
                // shared preferences
                val prefManager = preferenceManager
                prefManager.sharedPreferencesName = getSharedPreferencesName(provider!!)
                prefManager.sharedPreferencesMode = MODE_PRIVATE

                // resource
                addPreferencesFromResource(R.xml.pref_active_builtin)

                // bind
                // active name
                val namePref = findPreference<Preference>(Settings.PREF_PROVIDER_NAME)
                if (namePref != null) {
                    val key = namePref.key
                    namePref.summary = Settings.getStringPref(requireActivity(), key) // default prefs
                }

                // active icon
                val iconPref = findPreference<Preference>(Settings.PREF_PROVIDER_ICON)
                if (iconPref != null) {
                    val imageFile = provider!![Providers.ICON]
                    val context = requireContext()
                    val drawable = if (imageFile == null) null else Providers.readAssetDrawable(context, imageFile.toString())
                    iconPref.icon = drawable
                }

                // active preferences
                for (prefKey in arrayOf(TreebolicIface.PREF_SOURCE, TreebolicIface.PREF_BASE, TreebolicIface.PREF_IMAGEBASE, TreebolicIface.PREF_SETTINGS, Settings.PREF_PROVIDER)) {
                    val preference = findPreference<Preference>(prefKey)
                    if (preference != null) {
                        preference.summaryProvider = STRING_SUMMARY_PROVIDER
                    }
                }
            }
        }
    }

    class XmlSaxPreferenceFragment : ProviderPreferenceFragment() {

        override val name: String
            get() = "treebolic.provider.xml.sax.Provider"
    }

    class XmlDomPreferenceFragment : ProviderPreferenceFragment() {

        override val name: String
            get() = "treebolic.provider.xml.dom.Provider"
    }

    class TextIndentPreferenceFragment : ProviderPreferenceFragment() {

        override val name: String
            get() = "treebolic.provider.text.indent.Provider"
    }

    class TextIndentTrePreferenceFragment : ProviderPreferenceFragment() {

        override val name: String
            get() = "treebolic.provider.text.indent.tre.Provider"
    }

    class TextPairPreferenceFragment : ProviderPreferenceFragment() {

        override val name: String
            get() = "treebolic.provider.text.pair.Provider"
    }

    class DotPreferenceFragment : ProviderPreferenceFragment() {

        override val name: String
            get() = "treebolic.provider.graphviz.Provider"
    }

    abstract class ProviderPreferenceFragment : PreferenceFragmentCompat() {

        protected abstract val name: String?

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // non-default preference manager
            val prefManager = preferenceManager
            prefManager.sharedPreferencesName = Settings.PREF_FILE_PREFIX + name
            prefManager.sharedPreferencesMode = MODE_PRIVATE

            // inflate
            addPreferencesFromResource(R.xml.pref_general)

            // bind
            val sourcePreference = checkNotNull(findPreference(TreebolicIface.PREF_SOURCE))
            sourcePreference.summaryProvider = STRING_SUMMARY_PROVIDER

            val basePreference = checkNotNull(findPreference(TreebolicIface.PREF_BASE))
            basePreference.summaryProvider = STRING_SUMMARY_PROVIDER

            val imageBasePreference = checkNotNull(findPreference(TreebolicIface.PREF_IMAGEBASE))
            imageBasePreference.summaryProvider = STRING_SUMMARY_PROVIDER

            val settingsPreference = checkNotNull(findPreference(TreebolicIface.PREF_SETTINGS))
            settingsPreference.summaryProvider = STRING_SUMMARY_PROVIDER

            val providerPreference = checkNotNull(findPreference(Settings.PREF_PROVIDER))
            providerPreference.summaryProvider = STRING_SUMMARY_PROVIDER
        }
    }

    class DownloadPreferenceFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // inflate
            addPreferencesFromResource(R.xml.pref_download)

            // bind
            val basePreference = checkNotNull(findPreference(Settings.PREF_DOWNLOAD_BASE))
            basePreference.summaryProvider = OpenEditTextPreference.SUMMARY_PROVIDER

            val filePreference = checkNotNull(findPreference(Settings.PREF_DOWNLOAD_FILE))
            filePreference.summaryProvider = OpenEditTextPreference.SUMMARY_PROVIDER
        }

        override fun onDisplayPreferenceDialog(preference: Preference) {
            if (!onDisplayPreferenceDialog(this, preference)) {
                super.onDisplayPreferenceDialog(preference)
            }
        }
    }

    class ServicePreferenceFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // inflate
            addPreferencesFromResource(R.xml.pref_service)

            // preference
            val listPreference = checkNotNull(findPreference<ListPreference>(Settings.PREF_SERVICE))
            // activity
            val activity = activity as SettingsActivity?
            if (activity != null) {
                // connect to data
                activity.fillWithServiceData(listPreference)

                // bind
                listPreference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            }
        }
    }

    /**
     * Connect list preference to service data
     *
     * @param listPreference list preference
     */
    private fun fillWithServiceData(listPreference: ListPreference) {
        val services: Collection<Service>? = getServices(this)
        if (services != null) {
            val n = services.size
            val entries = arrayOfNulls<String>(n)
            val values = arrayOfNulls<String>(n)
            for ((i, service) in services.withIndex()) {
                entries[i] = service[ServiceKeys.LABEL] as String?
                values[i] = service[ServiceKeys.PACKAGE] as String? + '/' + service[ServiceKeys.NAME]
            }
            listPreference.entries = entries
            listPreference.entryValues = values
        }
    }

    companion object {

        /**
         * Selected provider argument
         */
        const val ARG_PROVIDER_SELECTED: String = "org.treebolic.selected"

        /**
         * Selected provider
         */
        private var provider: Provider? = null

        // S U M M A R Y

        private val STRING_SUMMARY_PROVIDER = SummaryProvider { preference: Preference ->
            val sharedPrefs = checkNotNull(preference.sharedPreferences)
            val value = sharedPrefs.getString(preference.key, null)
            value ?: ""
        }
    }
}
