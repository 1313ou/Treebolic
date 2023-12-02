/*
 * Copyright (c) 2023. Bernard Bou
 */

package org.treebolic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import org.treebolic.preference.OpenEditTextPreference;

import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

/**
 * Settings activity
 *
 * @author Bernard Bou
 */
public class SettingsActivity extends AppCompatCommonPreferenceActivity
{
	/**
	 * Selected provider argument
	 */
	public static final String ARG_PROVIDER_SELECTED = "org.treebolic.selected";

	/**
	 * Selected provider
	 */
	private static Provider provider;

	// L I F E C Y C L E

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		// super
		super.onCreate(savedInstanceState);

		// read args
		final String action = getIntent().getAction();
		if (action == null)
		{
			Intent intent = getIntent();
			String key = intent.getStringExtra(SettingsActivity.ARG_PROVIDER_SELECTED);
			SettingsActivity.provider = Providers.get(key);
		}
	}

	// S U M M A R Y

	private static final Preference.SummaryProvider<Preference> STRING_SUMMARY_PROVIDER = (preference) -> {

		final SharedPreferences sharedPrefs = preference.getSharedPreferences();
		assert sharedPrefs != null;
		final String value = sharedPrefs.getString(preference.getKey(), null);
		return value == null ? "" : value;
	};

	// F R A G M E N T S

	@SuppressWarnings("WeakerAccess")
	public static class ActivePreferenceFragment extends PreferenceFragmentCompat
	{
		@Override
		public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey)
		{
			if (SettingsActivity.provider == null)
			{
				return;
			}

			// activity
			final SettingsActivity activity = (SettingsActivity) getActivity();
			if (activity != null)
			{
				// shared preferences
				final PreferenceManager prefManager = getPreferenceManager();
				prefManager.setSharedPreferencesName(SettingsActivity.provider.getSharedPreferencesName());
				prefManager.setSharedPreferencesMode(Context.MODE_PRIVATE);

				// resource
				addPreferencesFromResource(R.xml.pref_active_builtin);

				// bind
				// active name
				final Preference namePref = findPreference(Settings.PREF_PROVIDER_NAME);
				if (namePref != null)
				{
					final String key = namePref.getKey();
					namePref.setSummary(Settings.getStringPref(getActivity(), key)); // default prefs
				}

				// active icon
				final Preference iconPref = findPreference(Settings.PREF_PROVIDER_ICON);
				if (iconPref != null)
				{
					final String imageFile = (String) SettingsActivity.provider.get(Provider.ICON);
					final Drawable drawable = imageFile == null ? null : Providers.readAssetDrawable(getContext(), imageFile);
					iconPref.setIcon(drawable);
				}

				// active preferences
				for (final String prefKey : new String[]{TreebolicIface.PREF_SOURCE, TreebolicIface.PREF_BASE, TreebolicIface.PREF_IMAGEBASE, TreebolicIface.PREF_SETTINGS, Settings.PREF_PROVIDER})
				{
					final Preference preference = findPreference(prefKey);
					if (preference != null)
					{
						preference.setSummaryProvider(STRING_SUMMARY_PROVIDER);
					}
				}
			}
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class XmlSaxPreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return "treebolic.provider.xml.sax.Provider";
		}
	}

	public static class XmlDomPreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return "treebolic.provider.xml.dom.Provider";
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class TextIndentPreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return "treebolic.provider.text.indent.Provider";
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class TextIndentTrePreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return "treebolic.provider.text.indent.tre.Provider";
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class TextPairPreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return "treebolic.provider.text.pair.Provider";
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class DotPreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return "treebolic.provider.graphviz.Provider";
		}
	}

	@SuppressWarnings("WeakerAccess")
	abstract public static class ProviderPreferenceFragment extends PreferenceFragmentCompat
	{
		@Nullable
		abstract protected String getName();

		@Override
		public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey)
		{
			// non-default preference manager
			final PreferenceManager prefManager = getPreferenceManager();
			prefManager.setSharedPreferencesName(Settings.PREF_FILE_PREFIX + getName());
			prefManager.setSharedPreferencesMode(Context.MODE_PRIVATE);

			// inflate
			addPreferencesFromResource(R.xml.pref_general);

			// bind
			final Preference sourcePreference = findPreference(TreebolicIface.PREF_SOURCE);
			assert sourcePreference != null;
			sourcePreference.setSummaryProvider(STRING_SUMMARY_PROVIDER);

			final Preference basePreference = findPreference(TreebolicIface.PREF_BASE);
			assert basePreference != null;
			basePreference.setSummaryProvider(STRING_SUMMARY_PROVIDER);

			final Preference imageBasePreference = findPreference(TreebolicIface.PREF_IMAGEBASE);
			assert imageBasePreference != null;
			imageBasePreference.setSummaryProvider(STRING_SUMMARY_PROVIDER);

			final Preference settingsPreference = findPreference(TreebolicIface.PREF_SETTINGS);
			assert settingsPreference != null;
			settingsPreference.setSummaryProvider(STRING_SUMMARY_PROVIDER);

			final Preference providerPreference = findPreference(Settings.PREF_PROVIDER);
			assert providerPreference != null;
			providerPreference.setSummaryProvider(STRING_SUMMARY_PROVIDER);
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class DownloadPreferenceFragment extends PreferenceFragmentCompat
	{
		@Override
		public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey)
		{
			// inflate
			addPreferencesFromResource(R.xml.pref_download);

			// bind
			final Preference basePreference = findPreference(Settings.PREF_DOWNLOAD_BASE);
			assert basePreference != null;
			basePreference.setSummaryProvider(OpenEditTextPreference.SUMMARY_PROVIDER);

			final Preference filePreference = findPreference(Settings.PREF_DOWNLOAD_FILE);
			assert filePreference != null;
			filePreference.setSummaryProvider(OpenEditTextPreference.SUMMARY_PROVIDER);
		}

		@Override
		public void onDisplayPreferenceDialog(@NonNull final Preference preference)
		{
			if (!OpenEditTextPreference.onDisplayPreferenceDialog(this, preference))
			{
				super.onDisplayPreferenceDialog(preference);
			}
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class ServicePreferenceFragment extends PreferenceFragmentCompat
	{
		@Override
		public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey)
		{
			// inflate
			addPreferencesFromResource(R.xml.pref_service);

			// preference
			final ListPreference listPreference = findPreference(Settings.PREF_SERVICE);
			assert listPreference != null;

			// activity
			final SettingsActivity activity = (SettingsActivity) getActivity();
			if (activity != null)
			{
				// connect to data
				activity.fillWithServiceData(listPreference);

				// bind
				listPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
			}
		}
	}

	/**
	 * Connect list preference to service data
	 *
	 * @param listPreference list preference
	 */
	private void fillWithServiceData(@NonNull final ListPreference listPreference)
	{
		final Collection<Service> services = Services.getServices(this);
		if (services != null)
		{
			final int n = services.size();
			final String[] entries = new String[n];
			final String[] values = new String[n];
			int i = 0;
			for (final Service service : services)
			{
				entries[i] = (String) service.get(Service.LABEL);
				values[i] = (String) service.get(Service.PACKAGE) + '/' + service.get(Service.NAME);
				i++;
			}
			listPreference.setEntries(entries);
			listPreference.setEntryValues(values);
		}
	}
}
