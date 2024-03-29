/*
 * Copyright (c) 2023. Bernard Bou
 */

package org.treebolic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.treebolic.clients.TreebolicAIDLBoundClient;
import org.treebolic.clients.TreebolicBoundClient;
import org.treebolic.clients.TreebolicBroadcastClient;
import org.treebolic.clients.TreebolicClientActivityStub;
import org.treebolic.clients.TreebolicMessengerClient;
import org.treebolic.clients.iface.ITreebolicClient;
import org.treebolic.guide.AboutActivity;
import org.treebolic.guide.HelpActivity;
import org.treebolic.guide.Tip;
import org.treebolic.search.SearchSettings;
import org.treebolic.services.iface.ITreebolicService;

import java.net.URL;
import java.util.Properties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import treebolic.IContext;
import treebolic.Widget;
import treebolic.glue.component.TreebolicThread;
import treebolic.model.Model;
import treebolic.model.ModelDump;
import treebolic.view.View;

/**
 * Treebolic client activity (requests model from server) and dispatches returned model to display.
 * May instruct server to forward model directly to rendering activity.
 *
 * @author Bernard Bou
 */
public class TreebolicClientActivity extends TreebolicClientActivityStub implements IContext
{
	/**
	 * Log tag
	 */
	private static final String TAG = "TreebolicClientA";

	/**
	 * Client
	 */
	@Nullable
	private String argService;

	/**
	 * Parameters
	 */
	private Properties parameters;

	/**
	 * Treebolic widget
	 */
	private Widget widget;

	/**
	 * Search view on action bar
	 */
	private SearchView searchView;

	/**
	 * Client status indicator
	 */
	private MenuItem clientStatusMenuItem;

	// L I F E C Y C L E

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// widget
		this.widget = new Widget(this, this);

		// content view
		setContentView(R.layout.activity_treebolic_client);
		final ViewGroup container = findViewById(R.id.container);
		container.addView(this.widget);

		// toolbar
		final Toolbar toolbar = findViewById(R.id.toolbar);

		// action bar
		setSupportActionBar(toolbar);
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setElevation(0);
			actionBar.setDisplayOptions(ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
		}

		// floating action button
		final FloatingActionButton fab = findViewById(R.id.fab);
		fab.setVisibility(View.GONE);
		fab.setOnClickListener((v) -> {
			fab.setVisibility(View.GONE);
			handleQuery();
		});

		// init widget with model is asynchronous
	}

	@Override
	protected void onPause()
	{
		Log.d(TAG, "Activity paused, terminating surface drawing thread");

		// terminate thread
		final View view = this.widget.getView();
		if (view != null)
		{
			final TreebolicThread thread = view.getThread();
			if (thread != null)
			{
				thread.terminate();
			}
		}

		// super
		super.onPause();
	}

	// M E N U

	@Override
	public boolean onCreateOptionsMenu(@NonNull final Menu menu)
	{
		// menu
		getMenuInflater().inflate(R.menu.treebolic_client, menu);

		// client status
		this.clientStatusMenuItem = menu.findItem(R.id.action_client_status);
		this.clientStatusMenuItem.setOnMenuItemClickListener(item -> {
			Toast.makeText(TreebolicClientActivity.this, this.clientStatus ? R.string.client_up : R.string.client_down, Toast.LENGTH_SHORT).show();
			return true;
		});
		updateClientStatus(this.clientStatus);

		// search view
		final MenuItem searchMenuItem = menu.findItem(R.id.action_search);
		searchMenuItem.expandActionView();
		this.searchView = (SearchView) searchMenuItem.getActionView();

		// search view width
		int screenWidth = treebolic.glue.component.Utils.screenWidth(this);
		this.searchView.setMaxWidth(screenWidth / 2);

		// search view listener
		this.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
		{
			@Override
			public boolean onQueryTextSubmit(@NonNull final String query)
			{
				TreebolicClientActivity.this.searchView.clearFocus();
				TreebolicClientActivity.this.searchView.setQuery("", false);
				handleQueryChanged(query, true);
				return true;
			}

			@Override
			public boolean onQueryTextChange(@NonNull final String query)
			{
				handleQueryChanged(query, false);
				return true;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull final MenuItem item)
	{
		int id = item.getItemId();
		if (R.id.action_treebolic_client_toggle == id)
		{
			if (this.clientStatus)
			{
				stop();
			}
			else
			{
				start();
			}
			return true;
		}
		else if (R.id.action_settings == id)
		{
			final Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}
		else if (R.id.action_settings_service == id)
		{
			Intent intent = new Intent(this, SettingsActivity.class);
			intent.putExtra(SettingsActivity.INITIAL_ARG, SettingsActivity.ServicePreferenceFragment.class.getName());
			startActivity(intent);
			return true;
		}
		else if (R.id.action_finish == id)
		{
			finish();
			return true;
		}
		else if (R.id.action_kill == id)
		{
			Process.killProcess(Process.myPid());
			return true;
		}
		else if (R.id.action_tips == id)
		{
			Tip.show(getSupportFragmentManager());
			return true;
		}
		else if (R.id.action_help == id)
		{
			startActivity(new Intent(this, HelpActivity.class));
			return true;
		}
		else if (R.id.action_about == id)
		{
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		}
		else if (R.id.action_search_run == id)
		{
			handleSearchRun();
			return true;
		}
		else if (R.id.action_search_reset == id)
		{
			handleSearchReset();
			return true;
		}
		else if (R.id.action_search_settings == id)
		{
			SearchSettings.show(getSupportFragmentManager());
			return true;
		}
		else
		{
			return false;
		}
	}

	// T R E E B O L I C C O N T E X T

	@Nullable
	@Override
	public URL getBase()
	{
		return Settings.getURLPref(this, TreebolicIface.PREF_BASE);
	}

	@Nullable
	@Override
	public URL getImagesBase()
	{
		return Settings.getURLPref(this, TreebolicIface.PREF_IMAGEBASE);
	}

	@NonNull
	@Override
	public Properties getParameters()
	{
		if (this.parameters == null)
		{
			this.parameters = makeParameters();
		}
		return this.parameters;
	}

	@NonNull
	@SuppressWarnings("SameReturnValue")
	@Override
	public String getStyle()
	{
		return Settings.STYLE_DEFAULT;
	}

	@Override
	public boolean linkTo(@NonNull final String url, final String target)
	{
		// if we handle url, initiate another query/response cycle
		if (this.urlScheme != null && url.startsWith(this.urlScheme))
		{
			final String source2 = url.substring(this.urlScheme.length());
			query(source2);
			return true;
		}

		// standard handling
		try
		{
			final Intent intent = new Intent(Intent.ACTION_VIEW);
			final Uri uri = Uri.parse(url);
			final String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
			final String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			intent.setDataAndType(uri, mimetype);
			startActivity(intent);
			return true;
		}
		catch (@NonNull final Exception e)
		{
			Toast.makeText(this, R.string.error_link, Toast.LENGTH_LONG).show();
			Log.w(TAG, "Error following link '" + url + "' " + e.getMessage());
		}
		return false;
	}

	@NonNull
	@Override
	public String getInput()
	{
		return this.searchView.getQuery().toString();
	}

	@Override
	public void warn(@NonNull final String message)
	{
		// toast(message, Toast.LENGTH_LONG);
		snackbar(message, Snackbar.LENGTH_LONG);
	}

	@Override
	public void status(@NonNull final String message)
	{
		// toast(message, Toast.LENGTH_SHORT);
		snackbar(message, Snackbar.LENGTH_SHORT);
	}

	/**
	 * Make parameters from bundle
	 *
	 * @return properties
	 */
	@NonNull
	private Properties makeParameters()
	{
		final Properties parameters = new Properties();
		parameters.setProperty("base", Settings.getStringPref(this, TreebolicIface.PREF_BASE));
		parameters.setProperty("imagebase", Settings.getStringPref(this, TreebolicIface.PREF_IMAGEBASE));
		parameters.setProperty("settings", Settings.getStringPref(this, TreebolicIface.PREF_SETTINGS));
		return parameters;
	}

	// U N M A R S H A L

	@SuppressWarnings("WeakerAccess")
	protected void unmarshalArgs(@NonNull final Intent intent)
	{
		this.argService = intent.getStringExtra(TreebolicIface.ARG_SERVICE);
	}

	// C L I E N T

	@Nullable
	@Override
	protected ITreebolicClient makeClient()
	{
		if (this.argService == null || this.argService.isEmpty())
		{
			// default
			this.argService = Settings.getStringPref(this, Settings.PREF_SERVICE);
		}

		return service2Client(this.argService);
	}

	/**
	 * Make client from service name
	 *
	 * @param service name
	 * @return client to service
	 */
	@Nullable
	@SuppressWarnings("WeakerAccess")
	protected ITreebolicClient service2Client(@Nullable final String service)
	{
		if (service != null && !service.isEmpty())
		{
			if (service.contains(ITreebolicService.TYPE_BROADCAST))
			{
				Log.d(TAG, "Making treebolic client to broadcast service" + service);
				return new TreebolicBroadcastClient(this, service, this, this);
			}
			else if (service.contains(ITreebolicService.TYPE_AIDL_BOUND))
			{
				Log.d(TAG, "Making treebolic client to AIDL bound service " + service);
				return new TreebolicAIDLBoundClient(this, service, this, this);
			}
			else if (service.contains(ITreebolicService.TYPE_BOUND))
			{
				Log.d(TAG, "Making treebolic client to bound service " + service);
				return new TreebolicBoundClient(this, service, this, this);
			}
			else if (service.contains(ITreebolicService.TYPE_MESSENGER))
			{
				Log.d(TAG, "Making treebolic client to messenger service " + service);
				return new TreebolicMessengerClient(this, service, this, this);
			}
		}
		Log.d(TAG, "Null service");
		return null;
	}

	// M O D E L

	/**
	 * Query model from source
	 *
	 * @param source source
	 */
	private void query(@Nullable final String source)
	{
		Log.d(TAG, "Query " + source);
		if (this.client == null)
		{
			Log.d(TAG, "Null client");
			return;
		}
		/* TODO allow null source
		if (source == null || source.isEmpty())
		{
			Log.d(TAG, "Null source");
			return;
		}
		*/
		Log.d(TAG, "Requesting model for source " + source);
		/*
		final String base = Settings.getStringPref(this, TreebolicIface.PREF_BASE);
		final String imageBase = Settings.getStringPref(this, TreebolicIface.PREF_IMAGEBASE);
		final String settings = Settings.getStringPref(this, TreebolicIface.PREF_SETTINGS);
		*/
		this.client.requestModel(source, null, null, null, null);
	}

	@Override
	public void onModel(@Nullable final Model model, final String urlScheme0)
	{
		Log.d(TAG, "Receiving model" + (BuildConfig.DEBUG ? '\n' + ModelDump.toString(model) : ' ' + (model == null ? "null" : model.toString())));

		// abort
		if (model == null)
		{
			Toast.makeText(this, R.string.error_null_model, Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		// init widget with model
		this.urlScheme = urlScheme0;
		TreebolicClientActivity.this.widget.init(model);
	}

	// S E A R C H

	static private final String CMD_SEARCH = "SEARCH";

	static private final String CMD_RESET = "RESET";

	static private final String CMD_CONTINUE = "CONTINUE";

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
	@SuppressWarnings("WeakerAccess")
	protected void handleQueryChanged(@NonNull final String query, boolean submit)
	{
		// clear keyboard out of the way
		if (submit)
		{
			closeKeyboard();
		}

		// reset current search if any
		this.widget.search(CMD_RESET);

		if (submit /*|| query.length() > SEARCH_TRIGGER_LEVEL*/)
		{
			// query applies to source: search is a requery
			final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			final String scope = sharedPrefs.getString(SearchSettings.PREF_SEARCH_SCOPE, SearchSettings.SCOPE_LABEL); // label, content, link, id
			if (SearchSettings.SCOPE_SOURCE.equals(scope))
			{
				Log.d(TAG, "Source" + ' ' + '"' + query + '"');
				query(query);
				return;
			}

			// query applies to non-source scope (label, content, ..): tree search
			final String mode = sharedPrefs.getString(SearchSettings.PREF_SEARCH_MODE, SearchSettings.MODE_STARTSWITH); // equals, startswith, includes
			runSearch(scope, mode, query);
		}
	}

	/**
	 * Tree search handler
	 */
	@SuppressWarnings("WeakerAccess")
	protected void handleSearchRun()
	{
		// clear keyboard out of the way
		closeKeyboard();

		// new or continued search
		if (!this.searchPending)
		{
			final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			final String query = this.searchView.getQuery().toString();
			final String scope = sharedPrefs.getString(SearchSettings.PREF_SEARCH_SCOPE, SearchSettings.SCOPE_LABEL); // label, content, link, id
			if (SearchSettings.SCOPE_SOURCE.equals(scope))
			{
				Log.d(TAG, "Source" + ' ' + '"' + query + '"');
				query(query);
				return;
			}

			final String mode = sharedPrefs.getString(SearchSettings.PREF_SEARCH_MODE, SearchSettings.MODE_STARTSWITH); // equals, startswith, includes
			runSearch(scope, mode, query);
		}
		else
		{
			continueSearch();
		}
	}

	/**
	 * Tree query handler
	 */
	@SuppressWarnings("WeakerAccess")
	protected void handleQuery()
	{
		// clear keyboard out of the way
		closeKeyboard();

		// new or continued search
		if (!this.searchPending)
		{
			final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			final String query = sharedPrefs.getString(Settings.PREF_SERVICE_SOURCE, "dummy");
			Log.d(TAG, "Source" + ' ' + '"' + query + '"');
			query(query);
		}
	}

	/**
	 * Tree search reset handler
	 */
	@SuppressWarnings("WeakerAccess")
	protected void handleSearchReset()
	{
		// clear keyboard out of the way
		closeKeyboard();

		// clear current query
		TreebolicClientActivity.this.searchView.setQuery("", false);

		resetSearch();
	}

	private void closeKeyboard()
	{
		final android.view.View view = this.getCurrentFocus();
		if (view != null)
		{
			final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			assert imm != null;
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	// S E A R C H   I N T E R F A C E

	@SuppressWarnings("WeakerAccess")
	protected void runSearch(String scope, String mode, String target)
	{
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

	@SuppressWarnings("WeakerAccess")
	protected void resetSearch()
	{
		Log.d(TAG, "Search reset");
		this.searchPending = false;
		this.widget.search(CMD_RESET);
	}

	// C O N N E C T I O N L I S T E N E R

	@Override
	public void onConnected(final boolean flag)
	{
		updateClientStatus(flag);
		super.onConnected(flag);
	}

	// S T A T U S

	private void updateClientStatus(final boolean flag)
	{
		// snackbar
		final String[] fields = this.argService == null ? null : this.argService.split("/");
		final String message = getString(flag ? R.string.status_client_connected : R.string.error_client_not_connected) + ' ' + (fields != null ? fields[1] : "");
		if (flag)
		{
			snackbar(message, Snackbar.LENGTH_LONG);
		}
		else
		{
			stickySnackbar(message);
		}

		// status icon
		if (this.clientStatusMenuItem != null)
		{
			this.clientStatusMenuItem.setIcon(flag ? R.drawable.ic_status_up : R.drawable.ic_status_down);
		}

		// fab
		if (flag)
		{
			final FloatingActionButton fab = findViewById(R.id.fab);
			fab.setVisibility(View.VISIBLE);
		}
	}

	// H E L P E R S

	/**
	 * Initialize search preferences
	 *
	 * @param context context
	 */
	static public void initializeSearchPrefs(@NonNull final Context context)
	{
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		final SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(SearchSettings.PREF_SEARCH_SCOPE, SearchSettings.SCOPE_SOURCE);
		editor.putString(SearchSettings.PREF_SEARCH_MODE, SearchSettings.MODE_IS);
		editor.apply();
	}

	/**
	 * Put toast on UI thread
	 *
	 * @param message  message
	 * @param duration duration
	 */
	private void toast(final String message, final int duration)
	{
		runOnUiThread(() -> Toast.makeText(TreebolicClientActivity.this, message, duration).show());
	}

	/**
	 * Put snackbar on UI thread
	 *
	 * @param message  message
	 * @param duration duration
	 */
	private void snackbar(@NonNull final String message, final int duration)
	{
		runOnUiThread(() -> {
			final Snackbar snack = Snackbar.make(TreebolicClientActivity.this.widget, message, duration);
			final android.view.View view = snack.getView();
			view.setBackgroundColor(ContextCompat.getColor(TreebolicClientActivity.this, R.color.snackbar_color));
			snack.show();
		});
	}

	/**
	 * Put sticky snackbar on UI thread
	 *
	 * @param message message
	 */
	private void stickySnackbar(@NonNull final String message)
	{
		runOnUiThread(() -> {
			final Snackbar snack = Snackbar.make(TreebolicClientActivity.this.widget, message, Snackbar.LENGTH_INDEFINITE);
			snack.setAction(android.R.string.ok, (v) -> snack.dismiss());
			snack.show();
		});
	}
}
