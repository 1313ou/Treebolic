/*
 * Copyright (c) 2023. Bernard Bou
 */

package org.treebolic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * List of Services
 *
 * @author Bernard Bou
 */
@SuppressWarnings("WeakerAccess")
public class Services
{
	/**
	 * Log tag
	 */
	private static final String TAG = "Services";

	/**
	 * Data
	 */
	@Nullable
	static private List<HashMap<String, Object>> data = null;

	/**
	 * Keys
	 */
	static public final String NAME = "name";
	static public final String PROCESS = "process";
	static public final String PACKAGE = "package";
	static public final String FLAGS = "flags";
	static public final String EXPORTED = "exported";
	static public final String ENABLED = "enabled";
	static public final String PERMISSION = "permission";
	static public final String LABEL = "label";
	static public final String DESCRIPTION = "description";
	static public final String ICON = "icon";
	static public final String LOGO = "logo";
	static public final String DRAWABLE = "drawable";


	/**
	 * Load icon
	 *
	 * @param packageManager package manager
	 * @param packageName    package name
	 * @param iconRes        icon id
	 * @return drawable
	 */
	static public Drawable loadIcon(@NonNull final PackageManager packageManager, final String packageName, @DrawableRes final int iconRes)
	{
		if (iconRes != 0)
		{
			return packageManager.getDrawable(packageName, iconRes, null);
		}
		return packageManager.getDefaultActivityIcon();
	}

	/**
	 * Load label
	 *
	 * @param packageName package name
	 * @param labelRes    label id
	 * @return label
	 */
	@NonNull
	static private String loadText(@NonNull final PackageManager packageManager, final String packageName, @StringRes final int labelRes)
	{
		if (labelRes != 0)
		{
			final CharSequence label = packageManager.getText(packageName, labelRes, null);
			return label == null ? "null" : label.toString();
		}
		return "?";
	}

	/**
	 * List services
	 *
	 * @param context locatorContext
	 * @param filter  positive filter
	 */
	@SuppressWarnings("boxing")
	static private void makeServices(@NonNull final Context context, @Nullable @SuppressWarnings("SameParameterValue") final String filter)
	{
		final PackageManager packageManager = context.getPackageManager();

		@SuppressLint("QueryPermissionsNeeded") final List<PackageInfo> pkgs = packageManager.getInstalledPackages(PackageManager.GET_SERVICES);
		for (final PackageInfo pkg : pkgs)
		{
			final ServiceInfo[] services = pkg.services;
			if (services != null && (filter == null || pkg.packageName.matches(filter)))
			{
				for (final ServiceInfo service : services)
				{
					if (filter == null || service.name.matches(filter))
					{
						final HashMap<String, Object> map = new HashMap<>();
						map.put(Services.NAME, service.name);
						map.put(Services.PACKAGE, pkg.packageName);
						map.put(Services.PROCESS, service.processName);
						map.put(Services.ENABLED, service.enabled);
						map.put(Services.EXPORTED, service.exported);
						map.put(Services.FLAGS, Integer.toHexString(service.flags));
						map.put(Services.PERMISSION, service.permission);
						map.put(Services.LABEL, Services.loadText(packageManager, pkg.packageName, service.labelRes));
						map.put(Services.DESCRIPTION, Services.loadText(packageManager, pkg.packageName, service.descriptionRes));
						map.put(Services.LOGO, service.logo);
						map.put(Services.ICON, service.icon);
						map.put(Services.DRAWABLE, pkg.packageName + '#' + service.icon);

						assert data != null;
						data.add(map);
					}
					else
					{
						Log.d(TAG, "Dropped " + service);
					}
				}
			}
		}
	}

	/**
	 * Make adapter
	 *
	 * @param context       locatorContext
	 * @param itemLayoutRes item layout
	 * @param from          from key
	 * @param to            to res id
	 * @return base adapter
	 */
	@Nullable
	static public SimpleAdapter makeAdapter(@NonNull final Context context, @SuppressWarnings("SameParameterValue") @LayoutRes final int itemLayoutRes, final String[] from, final int[] to, @SuppressWarnings("SameParameterValue") final boolean rescan)
	{
		// data
		final List<HashMap<String, Object>> services = Services.getServices(context, rescan);
		if (services == null)
		{
			return null;
		}
		if (services.size() == 0)
		{
			Toast.makeText(context, R.string.error_no_services, Toast.LENGTH_SHORT).show();
		}

		// fill in the grid_item layout
		return new SimpleAdapter(context, services, itemLayoutRes, from, to)
		{
			@Override
			public void setViewImage(@NonNull final ImageView v, @NonNull final String value)
			{
				try
				{
					final String[] fields = value.split("#");
					final int index = Integer.parseInt(fields[1]);
					final Drawable drawable = Services.loadIcon(context.getPackageManager(), fields[0], index);
					v.setImageDrawable(drawable);
				}
				catch (@NonNull final Exception ignored)
				{
					//
				}
			}
		};
	}

	/**
	 * Get (possibly cached) list of services
	 *
	 * @param context locatorContext
	 * @param rescan  rescan, do not use cache
	 * @return list of services
	 */
	@Nullable
	static public List<HashMap<String, Object>> getServices(@NonNull final Context context, final boolean rescan)
	{
		boolean scan = rescan;
		if (data == null)
		{
			data = new ArrayList<>();
			scan = true;
		}
		if (scan)
		{
			data.clear();
			try
			{
				Services.makeServices(context, "org.treebolic\\..*");
			}
			catch (@NonNull final Exception e)
			{
				Log.e(Services.TAG, "Error when scanning for services", e);
				return null;
			}
		}
		return data;
	}
}
