package org.treebolic;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.SearchView;
import android.widget.Toast;

import org.treebolic.guide.HelpActivity;
import org.treebolic.guide.Tip;
import org.treebolic.search.SearchSettings;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import treebolic.IContext;
import treebolic.Widget;
import treebolic.view.View;

/**
 * Treebolic basic activity
 *
 * @author Bernard Bou
 */
abstract public class TreebolicBasicActivity extends Activity implements IContext
{
	/**
	 * Log tag
	 */
	private static final String TAG = "TreebolicBasicA"; //$NON-NLS-1$

	/**
	 * Parameter : Document base
	 */
	protected String base;

	/**
	 * Parameter : Image base
	 */
	protected String imageBase;

	/**
	 * Parameter : Settings
	 */
	protected String settings;

	/**
	 * Parameter : CSS style for WebViews
	 */
	protected String style;

	/**
	 * Parameter : Returned URL urlScheme that is handled
	 */
	protected String urlScheme;

	/**
	 * Parameter : parameters
	 */
	protected Properties parameters;

	// components

	/**
	 * Treebolic widget
	 */
	protected Widget widget;

	/**
	 * Search view on action bar
	 */
	protected SearchView searchView;

	/**
	 * Input
	 */
	protected final String input;

	// parent

	/**
	 * Parent (client) activity
	 */
	protected Intent parentActivity;

	// menu

	/**
	 * Menu id
	 */
	private final int menuId;

	// C O N S T R U C T O R

	protected TreebolicBasicActivity(final int menuId0)
	{
		this.menuId = menuId0;
		this.input = null;
	}

	// L I F E C Y C L E

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// action bar
		final ActionBar actionBar = getActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// widget
		this.widget = new Widget(this, this);
		setContentView(this.widget);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(final Intent intent)
	{
		// an activity will always be paused before receiving a new intent, so you can count on onResume() being called after this method
		super.onNewIntent(intent);

		// getIntent() still returns the original Intent, use setIntent(Intent) to update it to this new Intent.
		setIntent(intent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume()
	{
		Log.d(TreebolicBasicActivity.TAG, "Activity resumed"); //$NON-NLS-1$

		// super
		super.onResume();

		// retrieve arguments
		unmarshalArgs(getIntent());

		// make parameters
		this.parameters = makeParameters();

		// query
		query();

		// first run
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean hasRun = prefs.getBoolean(Settings.PREF_FIRSTRUN, false);
		if (!hasRun)
		{
			final SharedPreferences.Editor edit = prefs.edit();

			// flag as 'has run'
			edit.putBoolean(Settings.PREF_FIRSTRUN, true).apply();

			// tips
			Tip.show(getFragmentManager());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause()
	{
		Log.d(TreebolicBasicActivity.TAG, "Activity paused, terminating surface drawing thread"); //$NON-NLS-1$

		// terminate thread
		final View view = this.widget.getView();
		if (view != null)
		{
			view.getThread().terminate();
		}

		// super
		super.onPause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		// inflate
		getMenuInflater().inflate(this.menuId, menu);

		// search view listener
		final MenuItem searchMenuItem = menu.findItem(R.id.searchView);
		searchMenuItem.expandActionView();
		this.searchView = (SearchView) searchMenuItem.getActionView();
		int width = this.getResources().getInteger(R.integer.search_view_max_width);
		if (width != -1)
		{
			this.searchView.setMaxWidth(width);
		}
		this.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
		{
			/*
			 * (non-Javadoc)
			 * 
			 * @see android.widget.SearchView.OnQueryTextListener#onQueryTextSubmit(java.lang.String)
			 */
			@Override
			public boolean onQueryTextSubmit(final String query)
			{
				handleQueryChanged(query, true);
				return true;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.widget.SearchView.OnQueryTextListener#onQueryTextChange(java.lang.String)
			 */
			@Override
			public boolean onQueryTextChange(final String query)
			{
				handleQueryChanged(query, false);
				return true;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.action_settings:
				final Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				return true;

			case R.id.action_finish:
				finish();
				return true;

			case R.id.action_kill:
				Process.killProcess(Process.myPid());
				return true;

			case R.id.action_tips:
				Tip.show(getFragmentManager());
				return true;

			case R.id.action_help:
				HelpActivity.start(this);
				return true;

			case R.id.action_search_run:
				handleSearchRun();
				return true;

			case R.id.action_search_reset:
				handleSearchReset();
				return true;

			case R.id.action_search_settings:
				SearchSettings.show(getFragmentManager());
				return true;

			default:
				break;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#getParentActivityIntent()
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public Intent getParentActivityIntent()
	{
		if (this.parentActivity != null)
		{
			return this.parentActivity;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
		{
			return super.getParentActivityIntent();
		}
		return null;
	}

	// T R E E B O L I C M O D E L

	/**
	 * Unmarshal model and parameters from intent
	 *
	 * @param intent intent
	 */
	protected void unmarshalArgs(final Intent intent)
	{
		// retrieve arguments
		final Bundle params = intent.getExtras();
		params.setClassLoader(getClassLoader());

		// retrieve arguments
		this.base = params.getString(TreebolicIface.ARG_BASE);
		this.imageBase = params.getString(TreebolicIface.ARG_IMAGEBASE);
		this.settings = params.getString(TreebolicIface.ARG_SETTINGS);
		this.style = params.getString(TreebolicIface.ARG_STYLE);
		this.urlScheme = params.getString(TreebolicIface.ARG_URLSCHEME);
		this.parentActivity = params.getParcelable(TreebolicIface.ARG_PARENTACTIVITY);
	}

	// T R E E B O L I C C O N T E X T

	/*
	 * (non-Javadoc)
	 * 
	 * @see treebolic.IContext#getBase()
	 */
	@Override
	public URL getBase()
	{
		if (this.base != null)
		{
			try
			{
				return new URL(this.base);
			}
			catch (final MalformedURLException e)
			{
				//
			}
		}
		return Settings.getURLPref(this, TreebolicIface.PREF_BASE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treebolic.IContext#getImagesBase()
	 */
	@Override
	public URL getImagesBase()
	{
		if (this.imageBase != null)
		{
			try
			{
				return new URL(this.imageBase);
			}
			catch (final MalformedURLException e)
			{
				//
			}
		}
		return Settings.getURLPref(this, TreebolicIface.PREF_IMAGEBASE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treebolic.IContext#getParameters()
	 */
	@Override
	public Properties getParameters()
	{
		return this.parameters;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treebolic.IContext#getStyle()
	 */
	@Override
	public String getStyle()
	{
		return this.style != null ? this.style : //
				Settings.STYLE_DEFAULT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treebolic.IContext#linkTo(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean linkTo(final String url, final String target)
	{
		// if url is handled by client, return query to client, which will handle it by initiating another query
		if (this.urlScheme != null && url.startsWith(this.urlScheme))
		{
			final String source2 = url.substring(this.urlScheme.length());
			requery(source2);
			return true;
		}

		// standard handling
		try
		{
			final Intent intent = new Intent(Intent.ACTION_VIEW);
			final Uri uri = Uri.parse(url);
			final String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
			final String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			if (mimetype == null)
			{
				intent.setData(uri);
			}
			else
			{
				intent.setDataAndType(uri, mimetype);
			}
			startActivity(intent);
			return true;
		}
		catch (final Exception e)
		{
			Toast.makeText(this, R.string.error_link, Toast.LENGTH_LONG).show();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treebolic.IContext#getInput()
	 */
	@Override
	public String getInput()
	{
		return this.input;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treebolic.IContext#warn(java.lang.String)
	 */
	@Override
	public void warn(final String message)
	{
		toast(message, Toast.LENGTH_LONG);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treebolic.IContext#status(java.lang.String)
	 */
	@Override
	public void status(final String message)
	{
		toast(message, Toast.LENGTH_SHORT);
	}

	// Q U E R Y

	/**
	 * Initial query
	 */
	abstract protected void query();

	/**
	 * Requery (linkTo, or searchView)
	 *
	 * @param source source
	 */
	abstract protected void requery(final String source);

	// S E A R C H

	static private final String CMD_SEARCH = "SEARCH"; //$NON-NLS-1$

	static private final String CMD_RESET = "RESET"; //$NON-NLS-1$

	static private final String CMD_CONTINUE = "CONTINUE"; //$NON-NLS-1$

	static private final String SCOPE_SOURCE = "SOURCE"; //$NON-NLS-1$

	static private final String SCOPE_LABEL = "LABEL"; //$NON-NLS-1$

	static private final String MODE_STARTSWITH = "STARTSWITH"; //$NON-NLS-1$

	static private final int SEARCH_TRIGGER_LEVEL = Integer.MAX_VALUE;

	/**
	 * Search pending flag
	 */
	private boolean searchPending = false;

	/**
	 * SearchView query change listener
	 *
	 * @param query  new query
	 * @param submit whether submit was changed
	 */
	protected void handleQueryChanged(final String query, boolean submit)
	{
		// clear keyboard out of the way
		if (submit)
		{
			closeKeyboard();
		}

		// reset current search if any
		resetSearch();

		if (submit || query.length() > SEARCH_TRIGGER_LEVEL)
		{
			// query applies to source: search is a requery
			final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			final String scope = sharedPrefs.getString(SearchSettings.PREF_SEARCH_SCOPE, SCOPE_LABEL); // label, content, link, id
			if (SCOPE_SOURCE.equals(scope))
			{
				Log.d(TAG, "Source" + ' ' + '"' + query + '"'); //$NON-NLS-1$
				if (submit)
				{
					requery(query);
				}
				return;
			}

			// query applies to non-source scope (label, content, ..): tree search
			final String mode = sharedPrefs.getString(SearchSettings.PREF_SEARCH_MODE, MODE_STARTSWITH); // equals, startswith, includes
			runSearch(scope, mode, query);
		}
	}

	/**
	 * Tree search handler
	 */
	protected void handleSearchRun()
	{
		// clear keyboard out of the way
		closeKeyboard();

		// new or continued search
		if (!this.searchPending)
		{
			final String query = this.searchView.getQuery().toString();
			final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			final String scope = sharedPrefs.getString(SearchSettings.PREF_SEARCH_SCOPE, SCOPE_LABEL); // label, content, link, id
			if (SCOPE_SOURCE.equals(scope))
			{
				Log.d(TAG, "Source" + ' ' + '"' + query + '"'); //$NON-NLS-1$
				requery(query);
				return;
			}

			final String mode = sharedPrefs.getString(SearchSettings.PREF_SEARCH_MODE, MODE_STARTSWITH); // equals, startswith, includes
			runSearch(scope, mode, query);
		}
		else
		{
			continueSearch();
		}
	}

	/**
	 * Tree search reset handler
	 */
	protected void handleSearchReset()
	{
		// clear keyboard out of the way
		closeKeyboard();

		// get query
		String query = TreebolicBasicActivity.this.searchView.getQuery().toString();

		// clear current query
		TreebolicBasicActivity.this.searchView.setQuery("", false); //$NON-NLS-1$

		// query was already empty
		if ("".equals(query)) //$NON-NLS-1$
		{
			resetSearch();
		}

		// home
		this.widget.focus(null);
	}

	private void closeKeyboard()
	{
		final android.view.View view = this.getCurrentFocus();
		if (view != null)
		{
			final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	// SEARCH INTERFACE

	protected void runSearch(String scope, String mode, String target)
	{
		if (target == null || target.isEmpty())
		{
			return;
		}

		Log.d(TAG, "Search run" + scope + ' ' + mode + ' ' + target); //$NON-NLS-1$
		this.searchPending = true;
		this.widget.search(CMD_SEARCH, scope, mode, target);
	}

	protected void continueSearch()
	{
		Log.d(TAG, "Search continue"); //$NON-NLS-1$
		this.widget.search(CMD_CONTINUE);
	}

	protected void resetSearch()
	{
		Log.d(TAG, "Search reset"); //$NON-NLS-1$
		this.searchPending = false;
		this.widget.search(CMD_RESET);
	}

	// H E L P E R S

	/**
	 * Make parameters from bundle
	 *
	 * @return properties
	 */
	protected Properties makeParameters()
	{
		final Properties theseParameters = new Properties();
		if (this.base != null)
		{
			theseParameters.setProperty("base", this.base); //$NON-NLS-1$
		}
		if (this.imageBase != null)
		{
			theseParameters.setProperty("imagebase", this.imageBase); //$NON-NLS-1$
		}
		if (this.settings != null)
		{
			theseParameters.setProperty("settings", this.settings); //$NON-NLS-1$
		}
		theseParameters.setProperty("debug", Boolean.toString(BuildConfig.DEBUG)); //$NON-NLS-1$
		return theseParameters;
	}

	/**
	 * Put toast on UI thread
	 *
	 * @param message  message
	 * @param duration duration
	 */
	private void toast(final String message, final int duration)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				Toast.makeText(TreebolicBasicActivity.this, message, duration).show();
			}
		});
	}
}
