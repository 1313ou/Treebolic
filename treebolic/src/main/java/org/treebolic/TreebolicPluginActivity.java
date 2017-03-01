package org.treebolic;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.util.Properties;

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
	private String pluginPkg;

	/**
	 * Plugin class loader
	 */
	private ClassLoader classLoader = null;

	/**
	 * Plugin provider
	 */
	private IProvider provider = null;

	/**
	 * Restoring
	 */
	private boolean restoring;

	// C O N S T R U C T O R

	public TreebolicPluginActivity()
	{
		super(R.menu.treebolic_plugin);
	}

	// M E N U

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.action_query:
				query();
				return true;

			default:
				break;
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
		final Properties theseParameters = super.makeParameters();
		if (this.pluginPkg != null)
		{
			theseParameters.setProperty("plugin", this.pluginPkg);
		}
		return theseParameters;
	}

	/*
	@Override
	public URL getBase()
	{
		return super.getBase();

		try
		{
			final Context pluginContext = createPackageContext(this.pluginPkg, Context.CONTEXT_IGNORE_SECURITY);
			final File thisDir = pluginContext.getFilesDir();
			return thisDir.toURI().toURL();

		}
		catch (final NameNotFoundException e)
		{
			Log.d(TreebolicPluginActivity.TAG, "Plugin context", e);
		}
		catch (MalformedURLException e)
		{
			Log.d(TreebolicPluginActivity.TAG, "Plugin context", e);
		}
		return null;
	}
	*/

	// U N M A R S H A L

	@Override
	protected void unmarshalArgs(final Intent intent)
	{
		final Bundle params = intent.getExtras();
		this.pluginPkg = params.getString(TreebolicIface.ARG_PLUGINPKG);

		// super
		super.unmarshalArgs(intent);
	}

	// Q U E R Y

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
				if (TreebolicPluginActivity.this.classLoader == null)
				{
					return;
				}
			}

			// provider
			if (this.provider == null)
			{
				// load class
				final Class<?> clazz = TreebolicPluginActivity.this.classLoader.loadClass(TreebolicPluginActivity.this.providerName);
				Log.d(TreebolicPluginActivity.TAG, "Class has been loaded " + clazz.toString());

				// ClassLoader.loadClass:
				// The class loader returned by Thread.getContextClassLoader() may fail for processes that host multiple applications.
				// You should explicitly specify a context class loader.
				// For example: Thread.setContextClassLoader(getClass().getClassLoader());
				Thread.currentThread().setContextClassLoader(clazz.getClassLoader());

				// the class instance is cast to an interface through which the method is called directly
				this.provider = (IProvider) clazz.newInstance();
				Log.i(TreebolicPluginActivity.TAG, "Loaded provider " + this.provider);
			}

			// init widget with provider
			TreebolicPluginActivity.this.widget.init(this.provider, this.source);
		}
		catch (final Exception e)
		{
			Toast.makeText(TreebolicPluginActivity.this, R.string.error_query, Toast.LENGTH_LONG).show();
			Log.e(TreebolicPluginActivity.TAG, "Exception while making provider", e);
		}
	}

	// C L A S S L O A D E R

	/**
	 * Get plugin class loader
	 *
	 * @param context   context
	 * @param pluginPkg plugin package
	 * @return pluginProvider dex class loader
	 * @throws NameNotFoundException
	 */
	static ClassLoader getPluginClassLoader(final Context context, final String pluginPkg) throws NameNotFoundException
	{
		// cache to store optimized classes
		File dexCache = context.getDir("plugins", Context.MODE_PRIVATE);
		if (dexCache == null || !dexCache.exists())
		{
			// external storage cache
			Log.w(TreebolicPluginActivity.TAG, "app/data storage is not accessible, trying to use external storage");
			final File sd = Environment.getExternalStorageDirectory();
			if (sd == null)
			{
				return null; // nowhere to store the dex
			}
			dexCache = new File(sd, "temp");
			if (!dexCache.exists())
			{
				dexCache.mkdir();
			}
		}
		// get plugin apk
		final ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(pluginPkg, 0);
		final String apk = appInfo.sourceDir;
		Log.d(TreebolicPluginActivity.TAG, "Plugin apk is " + apk);

		// application class loader
		final ClassLoader appClassLoader = context.getClass().getClassLoader();

		// plugin dex class loader
		return new DexClassLoader(apk, dexCache.getAbsolutePath(), null, appClassLoader);
	}

	// I N T E N T

	/**
	 * Make Treebolic plugin activity intent
	 *
	 * @param context   context
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
