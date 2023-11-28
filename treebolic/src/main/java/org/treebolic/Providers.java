/*
 * Copyright (c) 2023. Bernard Bou
 */

package org.treebolic;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SimpleAdapter;

import org.treebolic.storage.Storage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public class Providers
{
	/**
	 * Log tag
	 */
	private static final String TAG = "Providers";

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
	static public final String PROVIDER = "provider";
	static public final String MIMETYPE = "mimetype";
	static public final String EXTENSIONS = "extensions";
	static public final String URLSCHEME = "urlscheme";
	static public final String STYLE = "style";
	static public final String ICON = "icon";
	static public final String SOURCE = "source";
	static public final String BASE = "base";
	static public final String IMAGEBASE = "imagebase";
	static public final String SETTINGS = "settings";

	/**
	 * List providers
	 *
	 * @param context           locatorContext
	 * @param parentPackageName package name
	 * @throws NameNotFoundException name not found exception
	 */
	@SuppressWarnings({"boxing"})
	static private void makeProviders(@NonNull final Context context, @NonNull @SuppressWarnings("SameParameterValue") final String parentPackageName) throws NameNotFoundException
	{
		final String processName = Utils.getProcessName(context, parentPackageName);
		Providers.addBuiltInProviders(context, parentPackageName, processName);
	}

	/**
	 * Add builtin providers to list
	 *
	 * @param context       locatorContext
	 * @param parentPackage parent package
	 * @param processName   process name
	 * @return number of built-in providers
	 */
	@SuppressWarnings({"boxing", "UnnecessaryLocalVariable", "UnusedReturnValue"})
	static private int addBuiltInProviders(@NonNull final Context context, final String parentPackage, final String processName)
	{
		// base and image base in external storage
		final File treebolicStorage = Storage.getTreebolicStorage(context);
		final String base = Uri.fromFile(treebolicStorage).toString() + '/';
		final String imagebase = base;

		// resources
		final Resources resources = context.getResources();
		final String[] titles = resources.getStringArray(R.array.pref_providers_list_titles);
		final String[] values = resources.getStringArray(R.array.pref_providers_list_values);
		final String[] mimetypes = resources.getStringArray(R.array.pref_providers_list_mimetypes);
		final String[] extensions = resources.getStringArray(R.array.pref_providers_list_extensions);
		final String[] urlSchemes = resources.getStringArray(R.array.pref_providers_list_schemes);
		final String[] sources = resources.getStringArray(R.array.pref_providers_list_sources);
		final String[] settings = resources.getStringArray(R.array.pref_providers_list_settings);
		TypedArray icons = null;
		try
		{
			icons = resources.obtainTypedArray(R.array.pref_providers_list_icons);

			// lowest array length
			int lowest = Integer.MAX_VALUE;
			for (final int l : new int[]{titles.length, values.length, mimetypes.length, urlSchemes.length, settings.length, icons.length()})
			{
				if (lowest > l)
				{
					lowest = l;
				}
			}

			// add array data
			for (int i = 0; i < lowest; i++)
			{
				final HashMap<String, Object> provider = new HashMap<>();

				// structural
				provider.put(Providers.PROVIDER, values[i]);
				provider.put(Providers.NAME, titles[i]);
				provider.put(Providers.PACKAGE, parentPackage);
				provider.put(Providers.PROCESS, processName);
				provider.put(Providers.ICON, icons.getResourceId(i, -1));
				provider.put(Providers.MIMETYPE, mimetypes[i]);
				provider.put(Providers.EXTENSIONS, extensions[i]);
				provider.put(Providers.URLSCHEME, urlSchemes[i]);

				// settings
				provider.put(Providers.SOURCE, sources[i]);
				provider.put(Providers.BASE, base);
				provider.put(Providers.IMAGEBASE, imagebase);
				provider.put(Providers.SETTINGS, settings[i]);

				assert data != null;
				data.add(provider);
			}
			return lowest;
		}
		finally
		{
			if (icons != null)
			{
				icons.recycle();
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
	 * @param rescan        rescan list
	 * @return base adapter
	 */
	@Nullable
	static public SimpleAdapter makeAdapter(@NonNull final Context context, @SuppressWarnings("SameParameterValue") @LayoutRes final int itemLayoutRes, @SuppressWarnings("SameParameterValue") final String[] from, @SuppressWarnings("SameParameterValue") final int[] to, @SuppressWarnings("SameParameterValue") final boolean rescan)
	{
		// data
		final List<HashMap<String, Object>> providers = Providers.getProviders(context, rescan);

		// adapter
		return makeAdapter(context, providers, itemLayoutRes, from, to);
	}

	/**
	 * Make adapter
	 *
	 * @param context   locatorContext
	 * @param providers providers
	 * @param itemRes   item layout
	 * @param from      from key
	 * @param to        to res id
	 * @return base adapter
	 */
	@Nullable
	static public SimpleAdapter makeAdapter(@NonNull final Context context, @Nullable final List<HashMap<String, Object>> providers, final int itemRes, final String[] from, final int[] to)
	{
		// data
		if (providers == null)
		{
			return null;
		}

		// fill in the grid_item layout
		return new SimpleAdapter(context, providers, itemRes, from, to)
		{
			@Override
			public void setViewImage(@NonNull final ImageView imageView, final String pkg)
			{
				try
				{
					// icon
					final Drawable drawable = context.getPackageManager().getApplicationIcon(pkg);
					imageView.setImageDrawable(drawable);
				}
				catch (@NonNull final Exception ignored)
				{
					//
				}
			}
		};
	}

	/**
	 * Get (possibly cached) list of providers
	 *
	 * @param context locatorContext
	 * @param rescan  rescan, do not use cache
	 * @return list of providers
	 */
	@Nullable
	static public List<HashMap<String, Object>> getProviders(@NonNull final Context context, final boolean rescan)
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
				Providers.makeProviders(context, "org.treebolic");
			}
			catch (@NonNull final Exception e)
			{
				Log.d(Providers.TAG, "When scanning for providers: " + e.getMessage());
				return null;
			}
		}
		return data;
	}
}
