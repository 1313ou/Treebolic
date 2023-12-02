/*
 * Copyright (c) 2023. Bernard Bou
 */

package org.treebolic;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;

import androidx.annotation.NonNull;

@SuppressWarnings("WeakerAccess")
public class Utils
{
	/**
	 * Get process name
	 *
	 * @param context context
	 * @return process name
	 * @throws NameNotFoundException name not found
	 */
	static String getProcessName(@NonNull final Context context) throws NameNotFoundException
	{
		@NonNull final String pkgName = BuildConfig.APPLICATION_ID;
		final PackageManager pm = context.getPackageManager();
		final ApplicationInfo info = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU ? //
				pm.getApplicationInfo(pkgName, PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA)) : //
				pm.getApplicationInfo(pkgName, PackageManager.GET_META_DATA);
		return info.processName;
	}
}
