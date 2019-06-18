/*
 * Copyright (c) Treebolic 2019. Bernard Bou <1313ou@gmail.com>
 */

package org.treebolic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;

import org.treebolic.preference.OpenEditTextPreference;

import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.NavUtils;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.legacy.contrib.Header;

/**
 * A PreferenceActivity that presents a set of application settings. On handset devices, settings are presented as a single list. On tablets, settings
 * are split by category, with category headers shown to the left of the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html"> Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings API Guide</a> for more information on developing a Settings UI.
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

	// E V E N T S

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		// super
		super.onCreate(savedInstanceState);

		// toolbar
		setupToolbar(R.layout.toolbar, R.id.toolbar);

		// set up the action bar
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayOptions(ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
		}

		// read args
		final String action = getIntent().getAction();
		if (action == null)
		{
			SettingsActivity.provider = (HashMap<String, Object>) getIntent().getSerializableExtra(SettingsActivity.ARG_PROVIDER_SELECTED);
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull final MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBuildHeaders(@NonNull final List<Header> target)
	{
		loadHeadersFromResource(R.xml.pref_headers, target);
	}

	// S E T U P

	@Override
	public boolean isValidFragment(final String fragmentName)
	{
		return ActivePreferenceFragment.class.getName().equals(fragmentName) || //
				XmlPreferenceFragment.class.getName().equals(fragmentName) || //
				TextIndentPreferenceFragment.class.getName().equals(fragmentName) || //
				TextIndentTrePreferenceFragment.class.getName().equals(fragmentName) || //
				TextPairPreferenceFragment.class.getName().equals(fragmentName) || //
				DownloadPreferenceFragment.class.getName().equals(fragmentName) || //
				ServicePreferenceFragment.class.getName().equals(fragmentName);
	}

	// D E T E C T I O N

	@Override
	public boolean onIsMultiPane()
	{
		return SettingsActivity.isLargeTablet(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For example, 10" tablets are extra-large.
	 */
	private static boolean isLargeTablet(@NonNull final Context context)
	{
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

	// S U M M A R Y

	private static final Preference.SummaryProvider<Preference> STRING_SUMMARY_PROVIDER = (preference) -> {

		final SharedPreferences sharedPrefs = preference.getSharedPreferences();
		final String value = sharedPrefs.getString(preference.getKey(), null);
		return value == null ? "" : value;
	};

	// F R A G M E N T S

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
					prefManager.setSharedPreferencesName("org.treebolic_preferences_" + SettingsActivity.provider.get(Providers.NAME));
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
							final Drawable drawable = activity.getPackageManager().getApplicationIcon((String) SettingsActivity.provider.get(Providers.PACKAGE));
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
					assert preference != null;
					preference.setSummaryProvider(STRING_SUMMARY_PROVIDER);
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

	public static class XmlPreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return getName(0);
		}
	}

	public static class TextIndentPreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return getName(1);
		}
	}

	public static class TextIndentTrePreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return getName(2);
		}
	}

	public static class TextPairPreferenceFragment extends ProviderPreferenceFragment
	{
		@Nullable
		@Override
		protected String getName()
		{
			return getName(3);
		}
	}

	abstract public static class ProviderPreferenceFragment extends PreferenceFragmentCompat
	{
		@Nullable
		abstract protected String getName();

		@Override
		public void onCreatePreferences(@SuppressWarnings("unused") final Bundle savedInstanceState, @SuppressWarnings("unused") final String rootKey)
		{
			// non-default preference manager
			final PreferenceManager prefManager = getPreferenceManager();
			prefManager.setSharedPreferencesName("org.treebolic_preferences_" + getName());
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
