package org.treebolic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.contrib.AppCompatPreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.util.HashMap;
import java.util.List;

/**
 * A PreferenceActivity that presents a set of application settings. On handset devices, settings are presented as a single list. On tablets, settings
 * are split by category, with category headers shown to the left of the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html"> Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity
{
	/**
	 * Determines whether to always show the simplified settings UI, where settings are presented in a single list. When false, settings are shown as a
	 * master/detail two-pane view on tablets. When true, a single pane is shown on tablets.
	 */
	private static final boolean ALWAYS_SIMPLE_PREFS = false;

	/**
	 * Selected provider argument
	 */
	public static final String ARG_PROVIDER_SELECTED = "org.treebolic.selected";

	/**
	 * Subactions
	 */
	public static final String ACTION_GENERAL = "org.treebolic.prefs.GENERAL";
	public static final String ACTION_XML = "org.treebolic.prefs.XML";
	public static final String ACTION_TXT = "org.treebolic.prefs.TXT";
	public static final String ACTION_TRE = "org.treebolic.prefs.TRE";
	public static final String ACTION_TXT2 = "org.treebolic.prefs.TXT2";
	public static final String ACTION_DOWNLOAD = "org.treebolic.prefs.DOWNLOAD";
	public static final String ACTION_SERVICE = "org.treebolic.prefs.SERVICE";

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
		final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

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
	protected void onPostCreate(final Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);

		if (SettingsActivity.isSimplePreferences(this))
		{
			setupSimplePreferencesScreen();
		}
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(this);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBuildHeaders(final List<Header> target)
	{
		if (!SettingsActivity.isSimplePreferences(this))
		{
			loadHeadersFromResource(R.xml.pref_headers, target);
		}
	}

	// S E T U P

	@Override
	protected boolean isValidFragment(final String fragmentName)
	{
		return ActivePreferenceFragment.class.getName().equals(fragmentName) || //
				XmlPreferenceFragment.class.getName().equals(fragmentName) || //
				TextIndentPreferenceFragment.class.getName().equals(fragmentName) || //
				TextIndentTrePreferenceFragment.class.getName().equals(fragmentName) || //
				TextPairPreferenceFragment.class.getName().equals(fragmentName) || //
				DownloadPreferenceFragment.class.getName().equals(fragmentName) || //
				ServicePreferenceFragment.class.getName().equals(fragmentName);
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the device configuration dictates that a simplified, single-pane UI should be shown.
	 */
	@SuppressWarnings({"deprecation", "boxing"})
	private void setupSimplePreferencesScreen()
	{
		// In the simplified UI, fragments are not used at all and we instead use the older PreferenceActivity APIs.
		final String action = getIntent().getAction();
		if (action != null)
		{
			switch (action)
			{
				case SettingsActivity.ACTION_GENERAL:
				{
					final Boolean isPlugin = (Boolean) SettingsActivity.provider.get(Providers.ISPLUGIN);

					// shared preferences
					final PreferenceManager prefManager = getPreferenceManager();
					if (!isPlugin)
					{
						prefManager.setSharedPreferencesName("org.treebolic_preferences_" + SettingsActivity.provider.get(Providers.NAME));
						prefManager.setSharedPreferencesMode(Context.MODE_PRIVATE);
					}
					final SharedPreferences sharedPrefs = prefManager.getSharedPreferences();

					// layout
					addPreferencesFromResource(isPlugin ? R.xml.pref_active_plugin : R.xml.pref_active_builtin);

					// active name
					final Preference namePref = findPreference(Settings.PREF_PROVIDER_NAME);
					if (namePref != null)
					{
						final String key = namePref.getKey();
						namePref.setSummary(Settings.getStringPref(this, key));
					}

					// active icon
					final Preference iconPref = findPreference(Settings.PREF_PROVIDER_ICON);
					if (iconPref != null)
					{
						try
						{
							if ((Boolean) SettingsActivity.provider.get(Providers.ISPLUGIN))
							{
								final Drawable drawable = getPackageManager().getApplicationIcon((String) SettingsActivity.provider.get(Providers.PACKAGE));
								iconPref.setIcon(drawable);
							}
							else
							{
								final int resId = (Integer) SettingsActivity.provider.get(Providers.ICON);
								iconPref.setIcon(resId);
							}
						}
						catch (final NameNotFoundException e)
						{
							iconPref.setIcon(R.drawable.ic_treebolic);
						}
					}

					// active preferences
					for (final String prefKey : new String[]{TreebolicIface.PREF_SOURCE, TreebolicIface.PREF_BASE, TreebolicIface.PREF_IMAGEBASE, TreebolicIface.PREF_SETTINGS, Settings.PREF_PROVIDER})
					{
						final Preference pref = findPreference(prefKey);
						if (pref != null)
						{
							final String key = pref.getKey();
							pref.setSummary(sharedPrefs.getString(key, null));
						}
					}

					// forward button to plugin provider settings activity
					if (isPlugin)
					{
						final Preference button = findPreference("button_provider_settings");
						button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
						{
							@SuppressWarnings("synthetic-access")
							@Override
							public boolean onPreferenceClick(final Preference arg0)
							{
								final String pkg = (String) SettingsActivity.provider.get(Providers.PACKAGE);
								final String activityName = pkg + ".SettingsActivity";
								final Intent intent = new Intent();
								intent.setComponent(new ComponentName(pkg, activityName));
								startActivity(intent);
								return true;
							}
						});
					}
					break;
				}
				case SettingsActivity.ACTION_DOWNLOAD:
				{
					addPreferencesFromResource(R.xml.pref_download);
					final Preference pref = findPreference(Settings.PREF_DOWNLOAD);
					final String key = pref.getKey();
					pref.setSummary(Settings.getStringPref(this, key));
					break;
				}
				case SettingsActivity.ACTION_SERVICE:
					addPreferencesFromResource(R.xml.pref_service);
					final ListPreference listPreference = (ListPreference) findPreference(Settings.PREF_SERVICE);
					fillWithServiceData(listPreference);
					break;
				default:
				{
					String key = null;
					switch (action)
					{
						case SettingsActivity.ACTION_XML:
							key = "xml";

							break;
						case SettingsActivity.ACTION_TXT:
							key = "text (indented)";

							break;
						case SettingsActivity.ACTION_TRE:
							key = "text (indented, tre)";

							break;
						case SettingsActivity.ACTION_TXT2:
							key = "text (pairs)";

							break;
					}

					// non-default preference manager
					final PreferenceManager prefManager = getPreferenceManager();
					prefManager.setSharedPreferencesName("org.treebolic_preferences_" + key);

					prefManager.setSharedPreferencesMode(Context.MODE_PRIVATE);
					addPreferencesFromResource(R.xml.pref_general);
					final SharedPreferences sharedPrefs = prefManager.getSharedPreferences();

					// provider
					final Preference providerPref = findPreference(Settings.PREF_PROVIDER);
					final String providerKey = providerPref.getKey();
					final String providerValue = sharedPrefs.getString(providerKey, null);
					providerPref.setSummary(providerValue);
					break;
				}
			}
		}
		else
		{
			// Load the legacy preferences headers
			addPreferencesFromResource(R.xml.pref_headers_legacy);
		}
	}

	// D E T E C T I O N

	@Override
	public boolean onIsMultiPane()
	{
		return SettingsActivity.isLargeTablet(this) && !SettingsActivity.isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For example, 10" tablets are extra-large.
	 */
	private static boolean isLargeTablet(final Context context)
	{
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device doesn't
	 * have newer APIs like {@link PreferenceFragment}, or the device doesn't have an extra-large screen. In these cases, a single-pane "simplified" settings UI
	 * should be shown.
	 */
	private static boolean isSimplePreferences(final Context context)
	{
		//noinspection ConstantConditions
		return SettingsActivity.ALWAYS_SIMPLE_PREFS || !SettingsActivity.isLargeTablet(context);
	}

	// L I S T E N E R

	/**
	 * A preference value change listener that updates the preference's summary to reflect its new value.
	 */
	private final Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener()
	{
		@Override
		public boolean onPreferenceChange(final Preference preference, final Object value)
		{
			// set the summary to the value's simple string representation.
			final String stringValue = value == null ? "" : value.toString();
			preference.setSummary(stringValue);
			return true;
		}
	};

	// B I N D S U M M A R Y

	/**
	 * Binds a preference's summary to its value. More specifically, when the preference's value is changed, its summary (line of text below the preference
	 * title) is updated to reflect the value. The summary is also immediately updated upon calling this method. The exact display format is dependent on the
	 * type of preference.
	 *
	 * @see #listener
	 */
	private void bind(final Preference preference, final String value0, final OnPreferenceChangeListener listener0)
	{
		// set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(listener0);

		// trigger the listener immediately with the preference's current value.
		this.listener.onPreferenceChange(preference, value0);
	}

	// F R A G M E N T S

	public static class ActivePreferenceFragment extends PreferenceFragment
	{
		@SuppressWarnings({"synthetic-access", "boxing"})
		@Override
		public void onCreate(final Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);

			if (SettingsActivity.provider == null)
			{
				return;
			}

			// activity
			final SettingsActivity activity = (SettingsActivity) getActivity();

			// shared preferences
			final PreferenceManager prefManager = getPreferenceManager();
			final Boolean isPlugin = (Boolean) SettingsActivity.provider.get(Providers.ISPLUGIN);
			if (!isPlugin)
			{
				prefManager.setSharedPreferencesName("org.treebolic_preferences_" + SettingsActivity.provider.get(Providers.NAME));
				prefManager.setSharedPreferencesMode(Context.MODE_PRIVATE);
			}
			final SharedPreferences sharedPrefs = prefManager.getSharedPreferences();

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
					if ((Boolean) SettingsActivity.provider.get(Providers.ISPLUGIN))
					{
						final Drawable drawable = getActivity().getPackageManager().getApplicationIcon((String) SettingsActivity.provider.get(Providers.PACKAGE));
						iconPref.setIcon(drawable);
					}
					else
					{
						final int resId = (Integer) SettingsActivity.provider.get(Providers.ICON);
						iconPref.setIcon(resId);
					}
				}
				catch (final NameNotFoundException e)
				{
					iconPref.setIcon(R.drawable.ic_treebolic);
				}
			}

			// active preferences
			for (final String prefKey : new String[]{TreebolicIface.PREF_SOURCE, TreebolicIface.PREF_BASE, TreebolicIface.PREF_IMAGEBASE, TreebolicIface.PREF_SETTINGS, Settings.PREF_PROVIDER})
			{
				final Preference pref = findPreference(prefKey);
				if (pref != null)
				{
					final String key = pref.getKey();
					activity.bind(pref, sharedPrefs.getString(key, null), activity.listener);
				}
			}

			// forward button to plugin provider settings activity
			if (isPlugin)
			{
				final Preference button = findPreference("button_provider_settings");
				button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
				{
					@Override
					public boolean onPreferenceClick(final Preference arg0)
					{
						final String pkg = (String) SettingsActivity.provider.get(Providers.PACKAGE);
						final String activityName = pkg + ".SettingsActivity";
						final Intent intent = new Intent();
						intent.setComponent(new ComponentName(pkg, activityName));
						startActivity(intent);
						return true;
					}
				});
			}
		}
	}

	public static class XmlPreferenceFragment extends ProviderPreferenceFragment
	{
		@Override
		protected String getName()
		{
			return getName(0);
		}
	}

	public static class TextIndentPreferenceFragment extends ProviderPreferenceFragment
	{
		@Override
		protected String getName()
		{
			return getName(1);
		}
	}

	public static class TextIndentTrePreferenceFragment extends ProviderPreferenceFragment
	{
		@Override
		protected String getName()
		{
			return getName(2);
		}
	}

	public static class TextPairPreferenceFragment extends ProviderPreferenceFragment
	{
		@Override
		protected String getName()
		{
			return getName(3);
		}
	}

	abstract public static class ProviderPreferenceFragment extends PreferenceFragment
	{
		abstract protected String getName();

		@SuppressWarnings({"synthetic-access"})
		@Override
		public void onCreate(final Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);

			// non-default preference manager
			final PreferenceManager prefManager = getPreferenceManager();
			prefManager.setSharedPreferencesName("org.treebolic_preferences_" + getName());
			prefManager.setSharedPreferencesMode(Context.MODE_PRIVATE);
			final SharedPreferences sharedPrefs = prefManager.getSharedPreferences();

			// inflate
			addPreferencesFromResource(R.xml.pref_general);

			// activity
			final SettingsActivity activity = (SettingsActivity) getActivity();

			// bind
			final Preference sourcePreference = findPreference(TreebolicIface.PREF_SOURCE);
			if (sourcePreference != null)
			{
				final String key = sourcePreference.getKey();
				activity.bind(sourcePreference, sharedPrefs.getString(key, null), activity.listener);
			}

			final Preference basePreference = findPreference(TreebolicIface.PREF_BASE);
			if (basePreference != null)
			{
				final String key = basePreference.getKey();
				activity.bind(basePreference, sharedPrefs.getString(key, null), activity.listener);
			}

			final Preference imageBasePreference = findPreference(TreebolicIface.PREF_IMAGEBASE);
			if (imageBasePreference != null)
			{
				final String key = imageBasePreference.getKey();
				activity.bind(imageBasePreference, sharedPrefs.getString(key, null), activity.listener);
			}

			final Preference settingsPreference = findPreference(TreebolicIface.PREF_SETTINGS);
			if (settingsPreference != null)
			{
				final String key = settingsPreference.getKey();
				activity.bind(settingsPreference, sharedPrefs.getString(key, null), activity.listener);
			}

			final Preference providerPreference = findPreference(Settings.PREF_PROVIDER);
			if (providerPreference != null)
			{
				final String key = providerPreference.getKey();
				activity.bind(providerPreference, sharedPrefs.getString(key, null), activity.listener);
			}
		}

		protected String getName(final int thisIndex)
		{
			final List<HashMap<String, Object>> providers = Providers.getProviders(getActivity(), false);
			if (providers != null)
			{
				final HashMap<String, Object> provider = providers.get(thisIndex);
				if (provider != null)
				{
					return (String) provider.get(Providers.NAME);
				}
			}
			return null;
		}
	}

	public static class DownloadPreferenceFragment extends PreferenceFragment
	{
		@SuppressWarnings("synthetic-access")
		@Override
		public void onCreate(final Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);

			// inflate
			addPreferencesFromResource(R.xml.pref_download);

			// bind
			final SettingsActivity activity = (SettingsActivity) getActivity();
			final Preference preference = findPreference(Settings.PREF_DOWNLOAD);
			final String value = Settings.getStringPref(activity, preference.getKey());
			activity.bind(preference, value, activity.listener);
		}
	}

	public static class ServicePreferenceFragment extends PreferenceFragment
	{
		@SuppressWarnings("synthetic-access")
		@Override
		public void onCreate(final Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);

			// inflate
			addPreferencesFromResource(R.xml.pref_service);

			// preference
			final ListPreference listPreference = (ListPreference) findPreference(Settings.PREF_SERVICE);

			// activity
			final SettingsActivity activity = (SettingsActivity) getActivity();

			// connect to data
			activity.fillWithServiceData(listPreference);

			// bind
			activity.bind(listPreference, Settings.getStringPref(activity, listPreference.getKey()), activity.listener);
		}
	}

	/**
	 * Connect list preference to service data
	 *
	 * @param listPreference list preference
	 */
	private void fillWithServiceData(final ListPreference listPreference)
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
