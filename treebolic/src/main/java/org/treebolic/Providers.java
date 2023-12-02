/*
 * Copyright (c) 2023. Bernard Bou
 */

package org.treebolic;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SimpleAdapter;

import org.treebolic.storage.Storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

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
	static private Map<String, Provider> providerMap = null;

	private static final String ASSET_DIR = "providers";
	private static final String ASSET_IMAGE_DIR = "providers_images";

	public static @Nullable Map<String, Provider> buildProvidersFromManifests(final Context context)
	{
		Map<String, Provider> result = null;

		// base and image base in external storage
		final File treebolicStorage = Storage.getTreebolicStorage(context);
		final String base = Uri.fromFile(treebolicStorage).toString() + '/';

		try
		{
			final String process = Utils.getProcessName(context);

			final AssetManager assetManager = context.getAssets();
			String[] manifests = assetManager.list(ASSET_DIR);
			assert manifests != null;
			for (String manifest : manifests)
			{
				Log.i(TAG, "Reading " + manifest);
				try (InputStream is = assetManager.open(ASSET_DIR + '/' + manifest))
				{
					final Properties props = new Properties();
					props.load(is);
					final Provider provider = new Provider(props, base, base, process);

					// record
					if (result == null)
					{
						result = new TreeMap<>(Comparator.reverseOrder());
					}
					String key = provider.get(Provider.PROVIDER);
					assert key != null;
					result.put(key, provider);
				}
				catch (IOException e)
				{
					Log.e(TAG, "Error while reading " + manifest, e);
				}
			}
			return result;
		}
		catch (IOException e)
		{
			Log.e(TAG, "Error while listing assets", e);
		}
		catch (PackageManager.NameNotFoundException e)
		{
			Log.e(TAG, "Error while getting process name", e);
		}
		return null;
	}

	public static @Nullable Drawable readAssetDrawable(@NonNull final Context context, @NonNull final String imageFile)
	{
		try (InputStream is = context.getAssets().open(ASSET_IMAGE_DIR + '/' + imageFile))
		{
			//	DisplayMetrics dm = context.getResources().getDisplayMetrics();
			//	TypedValue value = new TypedValue();
			//	value.density = dm.densityDpi;

			return Drawable.createFromResourceStream(context.getResources(), null, is, null);
		}
		catch (IOException ignored)
		{
		}
		return null;
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
	static public SimpleAdapter makeAdapter(@NonNull final Context context, @LayoutRes final int itemLayoutRes, final String[] from, final int[] to)
	{
		// data
		final Map<String, Provider> providers = Providers.getProviders(context);

		// adapter
		return makeAdapter(context, providers == null ? null : providers.values(), itemLayoutRes, from, to);
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
	static public SimpleAdapter makeAdapter(@NonNull final Context context, @Nullable final Collection<Provider> providers, final int itemRes, final String[] from, final int[] to)
	{
		// data
		if (providers == null)
		{
			return null;
		}

		// fill in the grid_item layout
		return new SimpleAdapter(context, new ArrayList<>(providers), itemRes, from, to)
		{
			@Override
			public void setViewImage(@NonNull final ImageView imageView, final String value)
			{
				try
				{
					final Drawable drawable = readAssetDrawable(context, value);
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
	 * Get (possibly cached) map of providers
	 *
	 * @param context locatorContext
	 * @return map of providers
	 */
	@Nullable
	static public Map<String, Provider> getProviders(@NonNull final Context context)
	{
		if (providerMap != null)
		{
			return providerMap;
		}

		try
		{
			providerMap = buildProvidersFromManifests(context);
			return providerMap;
		}
		catch (@NonNull final Exception e)
		{
			Log.d(Providers.TAG, "When scanning for providers: " + e.getMessage());
			return null;
		}
	}

	public static Provider get(final String key)
	{
		return providerMap.get(key);
	}
}
