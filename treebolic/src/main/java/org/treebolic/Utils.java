/*
 * Copyright (c) 2023. Bernard Bou
 */

package org.treebolic;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

@SuppressWarnings("WeakerAccess")
public class Utils
{
	/**
	 * Get process name
	 *
	 * @param context context
	 * @param pkgName package name
	 * @return process name
	 * @throws NameNotFoundException
	 */
	static String getProcessName(@NonNull final Context context, @NonNull final String pkgName) throws NameNotFoundException
	{
		final PackageManager pm = context.getPackageManager();
		final ApplicationInfo info = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU ? //
				pm.getApplicationInfo(pkgName, PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA)) : //
				pm.getApplicationInfo(pkgName, PackageManager.GET_META_DATA);
		return info.processName;
	}

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
}
