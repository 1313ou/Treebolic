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
import java.util.Collection;
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
	static private Collection<Service> services = null;

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
	 * @param context context
	 * @param filter  positive filter
	 * @return collection of services
	 */
	@NonNull
	static private Collection<Service> collectServices(@NonNull final Context context, @Nullable @SuppressWarnings("SameParameterValue") final String filter)
	{
		final Collection<Service> services2 = new ArrayList<>();
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
						final Service service2 = new Service();
						service2.put(Service.NAME, service.name);
						service2.put(Service.PACKAGE, pkg.packageName);
						service2.put(Service.PROCESS, service.processName);
						service2.put(Service.ENABLED, service.enabled);
						service2.put(Service.EXPORTED, service.exported);
						service2.put(Service.FLAGS, Integer.toHexString(service.flags));
						service2.put(Service.PERMISSION, service.permission);
						service2.put(Service.LABEL, Services.loadText(packageManager, pkg.packageName, service.labelRes));
						service2.put(Service.DESCRIPTION, Services.loadText(packageManager, pkg.packageName, service.descriptionRes));
						service2.put(Service.LOGO, service.logo);
						service2.put(Service.ICON, service.icon);
						service2.put(Service.DRAWABLE, pkg.packageName + '#' + service.icon);

						services2.add(service2);
					}
					else
					{
						Log.d(TAG, "Dropped " + service);
					}
				}
			}
		}
		return services2;
	}

	/**
	 * Make adapter
	 *
	 * @param context       context
	 * @param itemLayoutRes item layout
	 * @param from          from key
	 * @param to            to res id
	 * @return base adapter
	 */
	@Nullable
	static public SimpleAdapter makeAdapter(@NonNull final Context context, @SuppressWarnings("SameParameterValue") @LayoutRes final int itemLayoutRes, final String[] from, final int[] to)
	{
		// data
		final Collection<Service> services = Services.getServices(context);
		if (services == null)
		{
			return null;
		}
		if (services.size() == 0)
		{
			Toast.makeText(context, R.string.error_no_services, Toast.LENGTH_SHORT).show();
		}

		// fill in the grid_item layout
		return new SimpleAdapter(context, new ArrayList<>(services), itemLayoutRes, from, to)
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
	 * @param context context
	 * @return list of services
	 */
	@Nullable
	static public Collection<Service> getServices(@NonNull final Context context)
	{
		if (services != null)
		{
			return services;
		}
		services = buildServices(context);
		return services;
	}

	/**
	 * Build collection of services
	 *
	 * @param context context
	 * @return collection of services
	 */
	@Nullable
	static public Collection<Service> buildServices(@NonNull final Context context)
	{
		try
		{
			return Services.collectServices(context, "org.treebolic\\..*");
		}
		catch (@NonNull final Exception e)
		{
			Log.e(TAG, "Error when scanning for services", e);
			return null;
		}
	}
}
