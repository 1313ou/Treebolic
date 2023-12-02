/*
 * Copyright (c) Treebolic 2023. Bernard Bou <1313ou@gmail.com>
 */

package org.treebolic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Properties;

import androidx.annotation.NonNull;

public class Provider extends HashMap<String, String> implements Serializable
{
	public static final String NAME = "name";
	static public final String PROVIDER = "provider";
	public static final String DESCRIPTION = "description";
	public static final String ICON = "icon";
	public static final String MIMETYPE = "mimetype";
	public static final String URLSCHEME = "schema";
	public static final String EXTENSIONS = "extensions";

	static public final String PROCESS = "process";
	static public final String PACKAGE = "package";

	static public final String SOURCE = "source";
	static public final String BASE = "base";
	static public final String IMAGEBASE = "imagebase";
	static public final String SETTINGS = "settings";
	static public final String STYLE = "style";

	public Provider(@NonNull final Properties props, final String base, final String imgbase, final String process)
	{
		super();

		// structural
		final String name = props.getProperty(Provider.NAME);
		final String provider = props.getProperty(Provider.PROVIDER);
		final String description = props.getProperty(Provider.DESCRIPTION);
		final String mimeType = props.getProperty(Provider.MIMETYPE);
		final String icon = props.getProperty(Provider.ICON);
		final String scheme = props.getProperty(Provider.URLSCHEME);
		final String extensions = props.getProperty(Provider.EXTENSIONS);
		put(Provider.PROVIDER, provider);
		put(Provider.NAME, name);
		put(Provider.DESCRIPTION, description);
		put(Provider.ICON, icon);
		put(Provider.MIMETYPE, mimeType);
		put(Provider.EXTENSIONS, extensions);
		put(Provider.URLSCHEME, scheme);
		put(Provider.PROCESS, process);
		put(Provider.PACKAGE, BuildConfig.APPLICATION_ID);

		// data
		final String source = props.getProperty(Provider.SOURCE);
		final String settings = props.getProperty(Provider.SETTINGS);
		put(Provider.SOURCE, source);
		put(Provider.SETTINGS, settings);
		put(Provider.BASE, base);
		put(Provider.IMAGEBASE, imgbase);
	}

	public String getSharedPreferencesName()
	{
		return Settings.PREF_FILE_PREFIX + get(Provider.PROVIDER);
	}
}
