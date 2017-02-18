package org.treebolic;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.treebolic.storage.Storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SimpleAdapter;

public class Providers
{
	/**
	 * Log tag
	 */
	private static final String TAG = "Providers"; //$NON-NLS-1$

	/**
	 * Data
	 */
	static private List<HashMap<String, Object>> data = null;

	/**
	 * Keys
	 */
	static public final String NAME = "name"; //$NON-NLS-1$
	static public final String PROCESS = "process"; //$NON-NLS-1$
	static public final String PACKAGE = "package"; //$NON-NLS-1$
	static public final String ISPLUGIN = "isplugin"; //$NON-NLS-1$
	static public final String PROVIDER = "provider"; //$NON-NLS-1$
	static public final String MIMETYPE = "mimetype"; //$NON-NLS-1$
	static public final String EXTENSIONS = "extensions"; //$NON-NLS-1$
	static public final String URLSCHEME = "urlscheme"; //$NON-NLS-1$
	static public final String STYLE = "style"; //$NON-NLS-1$
	static public final String ICON = "icon"; //$NON-NLS-1$
	static public final String SOURCE = "source"; //$NON-NLS-1$
	static public final String BASE = "base"; //$NON-NLS-1$
	static public final String IMAGEBASE = "imagebase"; //$NON-NLS-1$
	static public final String SETTINGS = "settings"; //$NON-NLS-1$

	/**
	 * List providers
	 *
	 * @param context
	 *            context
	 * @param parentPackageName
	 *            package name
	 * @return list of providers (hashmap of features)
	 * @throws NameNotFoundException
	 */
	@SuppressWarnings("boxing")
	static private void makeProviders(final Context context, final String parentPackageName) throws NameNotFoundException
	{
		final PackageManager packageManager = context.getPackageManager();

		// process name
		final ApplicationInfo info = packageManager.getApplicationInfo(parentPackageName, PackageManager.GET_META_DATA);
		final String processName = info.processName;

		// process uid
		final int uid = android.os.Process.myUid();

		// special case of parent package
		Providers.addBuiltInProviders(context, parentPackageName, processName);

		// scan processes with same uid
		final String[] pkgs = packageManager.getPackagesForUid(uid);
		for (final String pkg : pkgs)
		{
			// special case of parent package (already processed)
			if (parentPackageName.equals(pkg))
			{
				continue;
			}

			// special case of app package
			if (pkg.endsWith(".app") || pkg.endsWith(".service")) //$NON-NLS-1$ //$NON-NLS-2$
			{
				continue;
			}

			// plugin name
			final String[] components = pkg.split("\\."); //$NON-NLS-1$
			final String name = components[components.length - 1];

			// plugin context
			Context pluginContext;
			try
			{
				pluginContext = context.createPackageContext(pkg, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
			}
			catch (final NameNotFoundException e1)
			{
				Log.d(Providers.TAG, "Error while creating package context for " + pkg + ' ' + e1.getMessage()); //$NON-NLS-1$
				continue;
			}

			// preferences from plugin context
			final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(pluginContext);
			final String source = sharedPref.getString(TreebolicIface.PREF_SOURCE, null);
			final String base = sharedPref.getString(TreebolicIface.PREF_BASE, null);
			final String imagebase = sharedPref.getString(TreebolicIface.PREF_IMAGEBASE, null);
			final String settings = sharedPref.getString(TreebolicIface.PREF_SETTINGS, null);

			// plugin class loader
			final ClassLoader pluginClassLoader = pluginContext.getClassLoader();

			try
			{
				// plugin description
				final Class<?> pluginProviderDataClass = pluginClassLoader.loadClass(pkg + ".ProviderData"); //$NON-NLS-1$
				Log.d(Providers.TAG, "Plugin access class has been loaded " + pluginProviderDataClass.toString()); //$NON-NLS-1$

				// plugin classes
				final Method getClassesMethod = pluginProviderDataClass.getMethod("getProviderClasses", (Class<?>[]) null); //$NON-NLS-1$
				if (getClassesMethod == null)
				{
					continue;
				}
				final String[] providerNames = (String[]) getClassesMethod.invoke(null, (Object[]) null);

				// plugin mimetype
				String mimetype = null;
				try
				{
					final Method getMimetypeMethod = pluginProviderDataClass.getMethod("getMimetype", (Class<?>[]) null); //$NON-NLS-1$
					mimetype = (String) getMimetypeMethod.invoke(null, (Object[]) null);
				}
				catch (final NoSuchMethodException e)
				{
					//
				}

				// plugin mimetype
				String extensions = null;
				try
				{
					final Method getExtensionsMethod = pluginProviderDataClass.getMethod("getExtensions", (Class<?>[]) null); //$NON-NLS-1$
					extensions = (String) getExtensionsMethod.invoke(null, (Object[]) null);
				}
				catch (final NoSuchMethodException e)
				{
					//
				}

				// plugin urlScheme
				String urlScheme = "treebolic:"; //$NON-NLS-1$
				try
				{
					final Method getSchemeMethod = pluginProviderDataClass.getMethod("getUrlScheme", (Class<?>[]) null); //$NON-NLS-1$
					urlScheme = (String) getSchemeMethod.invoke(null, (Object[]) null);
				}
				catch (final NoSuchMethodException e)
				{
					//
				}

				// plugin style
				String style = null;
				try
				{
					final Method getStyleMethod = pluginProviderDataClass.getMethod("getStyle", (Class<?>[]) null); //$NON-NLS-1$
					style = (String) getStyleMethod.invoke(null, (Object[]) null);
				}
				catch (final NoSuchMethodException e)
				{
					//
				}

				// enter
				int ith = 0;
				for (final String providerName : providerNames)
				{
					String uniqueName;
					if (providerNames.length == 1)
					{
						uniqueName = name;
					}
					else
					{
						final String[] fields = providerName.split("\\."); //$NON-NLS-1$
						uniqueName = name + (fields.length < 2 ? ++ith : '-' + fields[fields.length - 2]);
					}

					final HashMap<String, Object> provider = new HashMap<String, Object>();
					// structural
					provider.put(Providers.PROVIDER, providerName);
					provider.put(Providers.NAME, uniqueName);
					provider.put(Providers.PACKAGE, pkg);
					provider.put(Providers.PROCESS, processName);
					provider.put(Providers.MIMETYPE, mimetype);
					provider.put(Providers.EXTENSIONS, extensions);
					provider.put(Providers.URLSCHEME, urlScheme);
					provider.put(Providers.ISPLUGIN, true);
					provider.put(Providers.ICON, pkg);
					provider.put(Providers.STYLE, style);
					// settings
					provider.put(Providers.SOURCE, source);
					provider.put(Providers.BASE, base);
					provider.put(Providers.IMAGEBASE, imagebase);
					provider.put(Providers.SETTINGS, settings);

					Providers.data.add(provider);
				}
			}
			catch (final Exception e)
			{
				Log.d(Providers.TAG, "Error while scanning for provider " + pkg + ' ' + e.getMessage()); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Add builtin providers to list
	 *
	 * @param context
	 *            context
	 * @param parentPackage
	 *            parent package
	 * @param processName
	 *            process name
	 * @return number of built-in providers
	 */
	@SuppressWarnings("boxing")
	static private int addBuiltInProviders(final Context context, final String parentPackage, final String processName)
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
		final TypedArray icons = resources.obtainTypedArray(R.array.pref_providers_list_icons);

		// lowest array length
		int lowest = Integer.MAX_VALUE;
		for (final int l : new int[] { titles.length, values.length, mimetypes.length, urlSchemes.length, settings.length, icons.length() })
			if (lowest > l)
			{
				lowest = l;
			}

		// add array data
		for (int i = 0; i < lowest; i++)
		{
			final HashMap<String, Object> provider = new HashMap<String, Object>();
			// structural
			provider.put(Providers.PROVIDER, values[i]);
			provider.put(Providers.NAME, titles[i]);
			provider.put(Providers.PACKAGE, parentPackage);
			provider.put(Providers.PROCESS, processName);
			provider.put(Providers.ISPLUGIN, false);
			provider.put(Providers.ICON, icons.getResourceId(i, -1));
			provider.put(Providers.MIMETYPE, mimetypes[i]);
			provider.put(Providers.EXTENSIONS, extensions[i]);
			provider.put(Providers.URLSCHEME, urlSchemes[i]);
			// settings
			provider.put(Providers.SOURCE, sources[i]);
			provider.put(Providers.BASE, base);
			provider.put(Providers.IMAGEBASE, imagebase);
			provider.put(Providers.SETTINGS, settings[i]);

			Providers.data.add(provider);
		}
		icons.recycle();
		return lowest;
	}

	/**
	 * Make adapter
	 *
	 * @param context
	 *            context
	 * @param itemRes
	 *            item layout
	 * @param from
	 *            from key
	 * @param to
	 *            to res id
	 * @return base adapter
	 */
	static public SimpleAdapter makeAdapter(final Context context, final int itemRes, final String[] from, final int[] to, final boolean rescan)
	{
		// data
		final List<HashMap<String, Object>> providers = Providers.getProviders(context, rescan);
		if (providers == null)
			return null;

		// fill in the grid_item layout
		final SimpleAdapter adapter = new SimpleAdapter(context, providers, itemRes, from, to)
		{
			@Override
			public void setViewImage(final ImageView v, final String pkg)
			{
				try
				{
					// icon
					final Drawable drawable = context.getPackageManager().getApplicationIcon(pkg);
					v.setImageDrawable(drawable);
				}
				catch (final Exception re)
				{
					//
				}
			}
		};
		return adapter;
	}

	/**
	 * Get (possibly cached) list of providers
	 *
	 * @param context
	 *            context
	 * @param rescan
	 *            rescan, do not use cache
	 * @return list of providers (including builtin + plugins)
	 */
	static public List<HashMap<String, Object>> getProviders(final Context context, final boolean rescan)
	{
		boolean scan = rescan;
		if (Providers.data == null)
		{
			Providers.data = new ArrayList<HashMap<String, Object>>();
			scan = true;
		}
		if (scan)
		{
			Providers.data.clear();
			try
			{
				Providers.makeProviders(context, "org.treebolic"); //$NON-NLS-1$
			}
			catch (final Exception e)
			{
				Log.d(Providers.TAG, "When scanning for providers: " + e.getMessage()); //$NON-NLS-1$
				return null;
			}
		}
		return Providers.data;
	}
}
