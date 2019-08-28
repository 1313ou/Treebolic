/*
 * Copyright (c) Treebolic 2019. Bernard Bou <1313ou@gmail.com>
 */

package org.treebolic;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public class Utils
{
	/**
	 * Get package class loader
	 *
	 * @param context current locatorContext
	 * @param pkgName package name
	 * @return package class loader
	 * @throws NameNotFoundException name not found exception
	 */
	static ClassLoader getClassLoader(@NonNull final Context context, final String pkgName) throws NameNotFoundException
	{
		final Context providerContext = context.createPackageContext(pkgName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
		return providerContext.getClassLoader();
	}

	/**
	 * Get package resources
	 *
	 * @param context current locatorContext
	 * @param pkgName package name
	 * @return package resources
	 * @throws NameNotFoundException name not found exception
	 */
	static Resources getResources(@NonNull final Context context, final String pkgName) throws NameNotFoundException
	{
		final Context providerContext = context.createPackageContext(pkgName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
		return providerContext.getResources();
	}

	/**
	 * Get plugin default shared preferences
	 *
	 * @param context current locatorContext
	 * @param pkg     package name
	 * @return default shared preferences
	 */
	@Nullable
	static SharedPreferences getPluginDefaultSharedPreferences(@NonNull final Context context, final String pkg)
	{
		try
		{
			final Context pluginContext = context.createPackageContext(pkg, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
			return PreferenceManager.getDefaultSharedPreferences(pluginContext);
		}
		catch (@NonNull final NameNotFoundException ignored)
		{
			//
		}
		return null;
	}
}
