package org.treebolic;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import org.treebolic.search.SearchSettings;

import treebolic.IWidget;

@SuppressWarnings("WeakerAccess")
public class Searcher
{
	private static final String TAG = "Searcher";

	static private final String CMD_SEARCH = "SEARCH";

	static private final String CMD_RESET = "RESET";

	static private final String CMD_CONTINUE = "CONTINUE";

	/**
	 * Search pending flag
	 */
	private boolean searchPending;

	/**
	 * Context
	 */
	private final Context context;

	/**
	 * Widget
	 */
	private final IWidget widget;

	/**
	 * Constructor
	 *
	 * @param context0 locatorContext
	 * @param widget0  widget
	 */
	public Searcher(final Context context0, final IWidget widget0)
	{
		this.searchPending = false;
		this.context = context0;
		this.widget = widget0;
	}

	protected boolean search(final String target)
	{
		final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.context);

		final String scope = sharedPrefs.getString(SearchSettings.PREF_SEARCH_SCOPE, SearchSettings.SCOPE_LABEL); // label, content, link, id
		final String mode = sharedPrefs.getString(SearchSettings.PREF_SEARCH_MODE, SearchSettings.MODE_STARTSWITH); // equals, startswith, includes

		Log.d(TAG, "Search for " + scope + ' ' + mode + ' ' + '"' + target + '"');
		if ("source".equals(scope))
		{
			return false;
		}

		if (!this.searchPending)
		{
			runSearch(scope, mode, target);
		}
		else
		{
			continueSearch();
		}
		return true;
	}

	// SEARCH INTERFACE

	@SuppressWarnings("WeakerAccess")
	protected void runSearch(String scope, String mode, @Nullable String target)
	{
		if (target == null || target.isEmpty())
		{
			return;
		}

		Log.d(TAG, "Search run" + scope + ' ' + mode + ' ' + target);
		this.searchPending = true;
		this.widget.search(CMD_SEARCH, scope, mode, target);
	}

	@SuppressWarnings("WeakerAccess")
	protected void continueSearch()
	{
		Log.d(TAG, "Search continue");
		this.widget.search(CMD_CONTINUE);
	}

	protected void resetSearch()
	{
		Log.d(TAG, "Search reset");
		this.searchPending = false;
		this.widget.search(CMD_RESET);
	}
}
