/*
 * Copyright (c) Treebolic 2023. Bernard Bou <1313ou@gmail.com>
 */

package org.treebolic;

import java.util.HashMap;

public class Service extends HashMap<String, Object>
{
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

	public Service()
	{
		super();
	}
}
