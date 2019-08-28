/*
 * Copyright (c) Treebolic 2019. Bernard Bou <1313ou@gmail.com>
 */

package org.treebolic;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.util.Properties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dalvik.system.DexClassLoader;
import treebolic.provider.IProvider;

/**
 * Treebolic plugin activity (builds model from plugin)
 *
 * @author Bernard Bou
 */
public class TreebolicPluginActivity extends TreebolicSourceActivity
{
	/**
	 * Log tag
	 */
	private static final String TAG = "TreebolicPluginA";

	/**
	 * Parameter : pluginProvider package
	 */
	@Nullable
	private String pluginPkg;

	/**
	 * Plugin class loader
	 */
	@Nullable
	private ClassLoader classLoader = null;

	/**
	 * Plugin provider
	 */
	@Nullable
	private IProvider provider = null;

	// C O N S T R U C T O R

	public TreebolicPluginActivity()
	{
		super(R.menu.treebolic_plugin);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if(this.classLoader != null)
		{
			this.classLoader = null;
		}
	}

	// M E N U

	@Override
	public boolean onOptionsItemSelected(@NonNull final MenuItem item)
	{
		if (item.getItemId() == R.id.action_query)
		{
			query();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// T R E E B O L I C C O N T E X T

	/**
	 * Make parameters from bundle
	 *
	 * @return properties
	 */
	@Override
	protected Properties makeParameters()
	{
		final Properties parameters = super.makeParameters();
		if (this.pluginPkg != null)
		{
			parameters.setProperty("plugin", this.pluginPkg);
		}
		return parameters;
	}

	// U N M A R S H A L

	@Override
	protected void unmarshalArgs(@NonNull final Intent intent)
	{
		final Bundle params = intent.getExtras();
		assert params != null;
		this.pluginPkg = params.getString(TreebolicIface.ARG_PLUGINPKG);

		// super
		super.unmarshalArgs(intent);
	}

	// Q U E R Y

	@SuppressWarnings("WeakerAccess")
	@Override
	protected void query()
	{
		queryModel();
	}

	@Override
	protected void requery(final String source0)
	{
		this.source = source0;
		queryModel();
	}

	/**
	 * Make model
	 */
	private void queryModel()
	{
		Log.d(TreebolicPluginActivity.TAG, "Requesting model from " + this.pluginPkg + " provider " + this.providerName + " and source " + this.source);
		try
		{
			// class loader
			if (TreebolicPluginActivity.this.classLoader == null)
			{
				TreebolicPluginActivity.this.classLoader = TreebolicPluginActivity.getPluginClassLoader(TreebolicPluginActivity.this, TreebolicPluginActivity.this.pluginPkg);
			}

			// provider
			if (this.provider == null)
			{
				// load class
				final Class<?> clazz = TreebolicPluginActivity.this.classLoader.loadClass(TreebolicPluginActivity.this.providerName);
				Log.d(TreebolicPluginActivity.TAG, "Class has been loaded " + clazz.toString());

				// ClassLoader.loadClass:
				// The class loader returned by Thread.getContextClassLoader() may fail for processes that host multiple applications.
				// You should explicitly specify a locatorContext class loader.
				// For example: Thread.setContextClassLoader(getClass().getClassLoader());
				//Thread.currentThread().setContextClassLoader(clazz.getClassLoader());

				// the class instance is cast to an interface through which the method is called directly
				this.provider = (IProvider) clazz.newInstance();
				Log.i(TreebolicPluginActivity.TAG, "Loaded provider " + this.provider);
			}

			// init widget with provider
			TreebolicPluginActivity.this.widget.init(this.provider, this.source);
		}
		catch (@NonNull final Exception e)
		{
			Toast.makeText(TreebolicPluginActivity.this, R.string.error_query, Toast.LENGTH_LONG).show();
			Log.e(TreebolicPluginActivity.TAG, "Exception while making provider", e);
		}
	}

	// C L A S S L O A D E R

	/**
	 * Get plugin class loader
	 *
	 * @param context   locatorContext
	 * @param pluginPkg plugin package
	 * @return pluginProvider dex class loader
	 * @throws NameNotFoundException name not found exception
	 */
	@NonNull
	private static ClassLoader getPluginClassLoader(@NonNull final Context context, final String pluginPkg) throws NameNotFoundException
	{
		File dexCache;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			dexCache = context.getCodeCacheDir();
		}
		else
		{
			// cache to store optimized classes
			dexCache = context.getDir("plugins", Context.MODE_PRIVATE);
			/*
			if (dexCache == null || !dexCache.exists())
			{
				// do not cache optimized classes on external storage. External storage does not provide access controls necessary to protect your application from code injection attacks
				// external storage cache
				Log.w(TreebolicPluginActivity.TAG, "app/data storage is not accessible, trying to use external storage");
				final File sd = Environment.getExternalStorageDirectory();
				if (sd == null)
				{
					return null; // nowhere to store the dex
				}
				dexCache = new File(sd, "temp");
			}
			*/

			if (!dexCache.exists())
			{
				//noinspection ResultOfMethodCallIgnored
				dexCache.mkdir();
			}
		}
		final String dexCachePath = dexCache.getAbsolutePath();

		// application class loader
		ClassLoader classLoader = context.getClass().getClassLoader();

		// get plugin apk
		final ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(pluginPkg, 0);
		final String apk = appInfo.sourceDir;
		Log.d(TreebolicPluginActivity.TAG, "Base plugin apk is " + apk);

		// dex class loader with base apk
		classLoader = new DexClassLoader(apk, dexCachePath, null, classLoader);

		// dex class loader with split apk
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			if (appInfo.splitPublicSourceDirs != null)
			{
				for (String splitApk : appInfo.splitPublicSourceDirs)
				{
					Log.d(TreebolicPluginActivity.TAG, "Split plugin apk is " + splitApk);
					classLoader = new DexClassLoader(splitApk, dexCachePath, null, classLoader);
				}
			}
		}
		return classLoader;
	}

	// I N T E N T

	/**
	 * Make Treebolic plugin activity intent
	 *
	 * @param context   locatorContext
	 * @param pluginPkg plugin package
	 * @param provider  provider name class
	 * @param urlScheme url scheme
	 * @param source    source
	 * @param base      base
	 * @param imageBase image base
	 * @param settings  settings
	 * @param style     style
	 * @return intent
	 */
	@NonNull
	static public Intent makeTreebolicIntent(final Context context, final String pluginPkg, final String provider, final String urlScheme, final String source, final String base, final String imageBase, final String settings, final String style)
	{
		final Intent intent = new Intent(context, TreebolicPluginActivity.class);
		intent.putExtra(TreebolicIface.ARG_PLUGINPKG, pluginPkg);
		intent.putExtra(TreebolicIface.ARG_PROVIDER, provider);
		intent.putExtra(TreebolicIface.ARG_URLSCHEME, urlScheme);
		intent.putExtra(TreebolicIface.ARG_SOURCE, source);
		intent.putExtra(TreebolicIface.ARG_BASE, base);
		intent.putExtra(TreebolicIface.ARG_IMAGEBASE, imageBase);
		intent.putExtra(TreebolicIface.ARG_SETTINGS, settings);
		intent.putExtra(TreebolicIface.ARG_STYLE, style);
		return intent;
	}
}
