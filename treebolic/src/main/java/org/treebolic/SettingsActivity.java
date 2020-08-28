/*
 * Copyright (c) Treebolic 2019. Bernard Bou <1313ou@gmail.com>
 */

package org.treebolic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import org.treebolic.preference.OpenEditTextPreference;

import java.util.HashMap;
import java.util.List;

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
	private static HashMap<String, Object> provider;

	// L I F E C Y C L E

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		// super
		super.onCreate(savedInstanceState);

		// read args
		final String action = getIntent().getAction();
		if (action == null)
		{
			SettingsActivity.provider = (HashMap<String, Object>) getIntent().getSerializableExtra(SettingsActivity.ARG_PROVIDER_SELECTED);
		}
	}

	// S U M M A R Y

	private static final Preference.SummaryProvider<Preference> STRING_SUMMARY_PROVIDER = (preference) -> {

		final SharedPreferences sharedPrefs = preference.getSharedPreferences();
		final String value = sharedPrefs.getString(preference.getKey(), null);
		return value == null ? "" : value;
	};

	// F R A G M E N T S

	@SuppressWarnings("WeakerAccess")
	public static class ActivePreferenceFragment extends PreferenceFragmentCompat
	{
		@Override
		public void onCreatePreferences(@SuppressWarnings("unused") final Bundle savedInstanceState, @SuppressWarnings("unused") final String rootKey)
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
				final Boolean isPluginBool = (Boolean) SettingsActivity.provider.get(Providers.ISPLUGIN);
				final boolean isPlugin = isPluginBool == null ? false : isPluginBool;
				if (!isPlugin)
				{
					prefManager.setSharedPreferencesName(Settings.PREF_FILE_PREFIX + SettingsActivity.provider.get(Providers.NAME));
					prefManager.setSharedPreferencesMode(Context.MODE_PRIVATE);
				}

				// resource
				addPreferencesFromResource(isPlugin ? R.xml.pref_active_plugin : R.xml.pref_active_builtin);

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
					try
					{
						if (isPlugin)
						{
							final String pack = (String) SettingsActivity.provider.get(Providers.PACKAGE);
							final Drawable drawable = pack == null ? null : activity.getPackageManager().getApplicationIcon(pack);
							iconPref.setIcon(drawable);
						}
						else
						{
							final Integer resIdInt = (Integer) SettingsActivity.provider.get(Providers.ICON);
							final int resId = resIdInt == null ? 0 : resIdInt;
							iconPref.setIcon(resId);
						}
					}
					catch (@NonNull final NameNotFoundException ignored)
					{
						iconPref.setIcon(R.drawable.ic_treebolic);
					}
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

				// forward button to plugin provider settings activity
				if (isPlugin)
				{
					final Preference button = findPreference("button_provider_settings");
					assert button != null;
					button.setOnPreferenceClickListener(arg0 -> {
						final String pkg = (String) SettingsActivity.provider.get(Providers.PACKAGE);
						assert pkg != null;
						final String activityName = pkg + ".SettingsActivity";
						final Intent intent = new Intent();
						intent.setComponent(new ComponentName(pkg, activityName));
						startActivity(intent);
						return true;
					});
				}
			}
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class XmlPreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return getName(0);
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class TextIndentPreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return getName(1);
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class TextIndentTrePreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return getName(2);
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class TextPairPreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return getName(3);
		}
	}

	@SuppressWarnings("WeakerAccess")
	abstract public static class ProviderPreferenceFragment extends PreferenceFragmentCompat
	{
		@Nullable
		abstract protected String getName();

		@Override
		public void onCreatePreferences(@SuppressWarnings("unused") final Bundle savedInstanceState, @SuppressWarnings("unused") final String rootKey)
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

		@SuppressWarnings("WeakerAccess")
		@Nullable
		protected String getName(final int index)
		{
			final List<HashMap<String, Object>> providers = Providers.getProviders(requireContext(), false);
			if (providers != null)
			{
				final HashMap<String, Object> provider = providers.get(index);
				if (provider != null)
				{
					return (String) provider.get(Providers.NAME);
				}
			}
			return null;
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class DownloadPreferenceFragment extends PreferenceFragmentCompat
	{
		@Override
		public void onCreatePreferences(@SuppressWarnings("unused") final Bundle savedInstanceState, @SuppressWarnings("unused") final String rootKey)
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
		public void onDisplayPreferenceDialog(final Preference preference)
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
		public void onCreatePreferences(@SuppressWarnings("unused") final Bundle savedInstanceState, @SuppressWarnings("unused") final String rootKey)
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
		final List<HashMap<String, Object>> services = Services.getServices(this, true);
		if (services != null)
		{
			final int n = services.size();
			final String[] entries = new String[n];
			final String[] values = new String[n];
			for (int i = 0; i < n; i++)
			{
				final HashMap<String, Object> service = services.get(i);
				entries[i] = (String) service.get(Services.LABEL);
				values[i] = (String) service.get(Services.PACKAGE) + '/' + service.get(Services.NAME);
			}
			listPreference.setEntries(entries);
			listPreference.setEntryValues(values);
		}
	}
}
