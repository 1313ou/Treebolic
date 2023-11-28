/*
 * Copyright (c) 2023. Bernard Bou
 */

package org.treebolic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Build;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

/**
 * Settings
 *
 * @author Bernard Bou
 */
@SuppressWarnings("WeakerAccess")
public class Settings
{
	/**
	 * Demo
	 */
	public static final String DEMO = "demo.zip";

	/**
	 * Initialized preference name
	 */
	public static final String PREF_INITIALIZED = "pref_initialized";

	/**
	 * First preference name
	 */
	public static final String PREF_FIRSTRUN = "pref_first_run";

	/**
	 * Provider preference name
	 */
	public static final String PREF_PROVIDER = "pref_provider";

	/**
	 * Provider icon preference name
	 */
	public static final String PREF_PROVIDER_ICON = "pref_provider_icon";

	/**
	 * Active provider preference name
	 */
	public static final String PREF_PROVIDER_NAME = "pref_provider_name";

	/**
	 * Service preference name
	 */
	public static final String PREF_SERVICE = "pref_service";

	/**
	 * Preference file prefix
	 */
	public static final String PREF_FILE_PREFIX = "org.treebolic_preferences_";

	/**
	 * Download base preference name
	 */
	public static final String PREF_DOWNLOAD_BASE = "pref_download_base";

	/**
	 * Download file preference name
	 */
	public static final String PREF_DOWNLOAD_FILE = "pref_download_file";

	/**
	 * Default CSS
	 */
	public static final String STYLE_DEFAULT = ".content { }\n" + //
			".link {color: #FFA500;font-size: small;}\n" + //
			".linking {color: #FFA500; font-size: small; }" + //
			".mount {color: #CD5C5C; font-size: small;}" + //
			".mounting {color: #CD5C5C; font-size: small; }" + //
			".searching {color: #FF7F50; font-size: small; }";

	/**
	 * Set providers default settings from provider data
	 *
	 * @param context locatorContext
	 */
	@SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
	static public void setDefaults(@NonNull final Context context)
	{
		// create providers
		final List<HashMap<String, Object>> providers = Providers.getProviders(context, false);

		// create prefs for built-in providers
		if (providers != null)
		{
			for (int i = 0; i < providers.size(); i++)
			{
				final HashMap<String, Object> provider = providers.get(i);

				// provider shared preferences
				final SharedPreferences providerSharedPrefs = context.getSharedPreferences(Settings.PREF_FILE_PREFIX + provider.get(Providers.NAME), Context.MODE_PRIVATE);

				// commit non existent values
				final Editor providerEditor = providerSharedPrefs.edit();
				final String[] keys = new String[]{TreebolicIface.PREF_SOURCE, TreebolicIface.PREF_BASE, TreebolicIface.PREF_IMAGEBASE, TreebolicIface.PREF_SETTINGS, Settings.PREF_PROVIDER};
				final String[] providerKeys = new String[]{Providers.SOURCE, Providers.BASE, Providers.IMAGEBASE, Providers.SETTINGS, Providers.PROVIDER};
				for (int j = 0; j < keys.length; j++)
				{
					final String key = keys[j];
					if (!providerSharedPrefs.contains(key))
					{
						final String value = (String) provider.get(providerKeys[j]);
						providerEditor.putString(key, value).commit();
					}
				}
			}
		}
	}

	/**
	 * Set active provider settings (copied into default preferences)
	 *
	 * @param context  locatorContext
	 * @param provider active provider
	 */
	@SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
	static public void setActivePrefs(@NonNull final Context context, @NonNull final HashMap<String, Object> provider)
	{
		final SharedPreferences defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		final Editor defaultEditor = defaultSharedPrefs.edit();
		SharedPreferences providerSharedPrefs = context.getSharedPreferences(Settings.PREF_FILE_PREFIX + provider.get(Providers.NAME), Context.MODE_PRIVATE);
		final String providerClass = providerSharedPrefs.getString(Settings.PREF_PROVIDER, null);
		defaultEditor.putString(Settings.PREF_PROVIDER, providerClass);

		final String[] keys = new String[]{TreebolicIface.PREF_SOURCE, TreebolicIface.PREF_BASE, TreebolicIface.PREF_IMAGEBASE, TreebolicIface.PREF_SETTINGS};
		for (final String key : keys)
		{
			final String value = providerSharedPrefs.getString(key, null);
			defaultEditor.putString(key, value);
		}
		defaultEditor.commit();
	}

	/**
	 * Put string preference
	 *
	 * @param context locatorContext
	 * @param key     key
	 * @param value   value
	 */
	@SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
	static public void putStringPref(@NonNull final Context context, @SuppressWarnings("SameParameterValue") final String key, final String value)
	{
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		sharedPref.edit().putString(key, value).commit();
	}

	/**
	 * Put integer preference
	 *
	 * @param context locatorContext
	 * @param key     key
	 * @param value   value
	 */
	@SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
	static public void putIntPref(@NonNull final Context context, final String key, final int value)
	{
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		sharedPref.edit().putInt(key, value).commit();
	}

	/**
	 * Get string preference
	 *
	 * @param context locatorContext
	 * @param key     key
	 * @return value
	 */
	@Nullable
	static public String getStringPref(@NonNull final Context context, final String key)
	{
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPref.getString(key, null);
	}

	/**
	 * Get int preference
	 *
	 * @param context locatorContext
	 * @param key     key
	 * @return value
	 */
	static public int getIntPref(@NonNull final Context context, final String key)
	{
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPref.getInt(key, 0);
	}

	/**
	 * Get preference value as url
	 *
	 * @param context locatorContext
	 * @param key     key
	 * @return preference value as
	 */
	@Nullable
	static public URL getURLPref(@NonNull final Context context, final String key)
	{
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		final String result = sharedPref.getString(key, null);
		return Settings.makeURL(result);
	}

	// U T I L S

	/**
	 * Make URL from string
	 *
	 * @param url url string
	 * @return url
	 */
	@Nullable
	static public URL makeURL(final String url)
	{
		try
		{
			return new URL(url);
		}
		catch (@NonNull final MalformedURLException ignored)
		{
			return null;
		}
	}

	/**
	 * Application settings
	 *
	 * @param context locatorContext
	 * @param pkgName package name
	 */
	static public void applicationSettings(@NonNull final Context context, @SuppressWarnings("SameParameterValue") final String pkgName)
	{
		final int apiLevel = Build.VERSION.SDK_INT;
		final Intent intent = new Intent();

		if (apiLevel >= 9)
		{
			intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			intent.setData(Uri.parse("package:" + pkgName));
		}
		else
		{
			final String appPkgName = apiLevel == 8 ? "pkg" : "com.android.settings.ApplicationPkgName";

			intent.setAction(Intent.ACTION_VIEW);
			intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
			intent.putExtra(appPkgName, pkgName);
		}

		// start activity
		context.startActivity(intent);
	}
}
