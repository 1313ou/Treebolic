package org.treebolic;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import org.treebolic.filechooser.EntryChooser;
import org.treebolic.filechooser.FileChooserActivity;
import org.treebolic.guide.AboutActivity;
import org.treebolic.guide.HelpActivity;
import org.treebolic.guide.Tip;
import org.treebolic.storage.Storage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Treebolic main activity (home)
 *
 * @author Bernard Bou
 */
public class MainActivity extends Activity implements OnClickListener
{
	/**
	 * Log tag
	 */
	private static final String TAG = "Treebolic MainActivity"; //$NON-NLS-1$

	/**
	 * State
	 */
	private static final String STATE_SELECTED_PROVIDER_ITEM = "org.treebolic.provider.selected"; //$NON-NLS-1$

	/**
	 * File request code
	 */
	private static final int REQUEST_FILE_CODE = 1;

	/**
	 * Bundle request code
	 */
	private static final int REQUEST_BUNDLE_CODE = 2;

	/**
	 * Serialized model request code
	 */
	private static final int REQUEST_SERIALIZED_CODE = 3;

	/**
	 * Download request
	 */
	private static final int REQUEST_DOWNLOAD_CODE = 10;

	/**
	 * Selected pluginProvider
	 */
	private HashMap<String, Object> pluginProvider;

	/**
	 * Provider spinner
	 */
	private Spinner spinner;

	/**
	 * Provider adapter
	 */
	private SimpleAdapter adapter;

	// L I F E C Y C L E O V E R R I D E S

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@SuppressLint("InflateParams")
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		// init
		initialize();

		// set up the action bar to show a custom layout
		final ActionBar actionBar = getActionBar();
		final View actionBarView = getLayoutInflater().inflate(R.layout.actionbar_custom, null);
		if (actionBar != null)
		{
			actionBar.setCustomView(actionBarView);
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
			actionBar.setDisplayShowTitleEnabled(false);
		}

		// spinner
		this.spinner = (Spinner) actionBarView.findViewById(R.id.spinner);

		// spinner adapter: create the key-id mapping
		final String[] from = new String[]{Providers.ICON, Providers.NAME};
		final int[] to = new int[]{R.id.icon, R.id.provider};

		// spinner adapter
		this.adapter = Providers.makeAdapter(this, R.layout.actionbar_item_providers, from, to, false);
		if (this.adapter != null)
		{
			// prepare the list providers
			this.adapter.setDropDownViewResource(R.layout.actionbar_item_providers);

			// set up the dropdown list navigation in the action bar.
			this.spinner.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				@SuppressWarnings("synthetic-access")
				@Override
				public void onItemSelected(final AdapterView<?> parentView, final View selectedItemView, final int position, final long id)
				{
					MainActivity.this.pluginProvider = (HashMap<String, Object>) MainActivity.this.adapter.getItem(position);
					final String name = (String) MainActivity.this.pluginProvider.get(Providers.NAME);
					Settings.putStringPref(MainActivity.this, Settings.PREF_PROVIDER_NAME, name);
					Settings.setActivePrefs(MainActivity.this, MainActivity.this.pluginProvider);
					updateButton();
					Log.d(MainActivity.TAG, (String) MainActivity.this.pluginProvider.get(Providers.PROVIDER));
				}

				@Override
				public void onNothingSelected(final AdapterView<?> parentView)
				{
					//
				}
			});

			// set spinner adapter
			this.spinner.setAdapter(this.adapter);

			// saved name
			final String name = Settings.getStringPref(MainActivity.this, Settings.PREF_PROVIDER_NAME);

			// position
			if (name != null)
			{
				for (int position = 0; position < this.adapter.getCount(); position++)
				{
					final HashMap<String, Object> provider = (HashMap<String, Object>) this.adapter.getItem(position);
					if (provider.get(Providers.NAME).equals(name))
					{
						this.spinner.setSelection(position);
					}
				}
			}
		}

		// fragment
		if (savedInstanceState == null)
		{
			getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume()
	{
		super.onResume();
		updateButton();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	public void onSaveInstanceState(final Bundle savedInstanceState)
	{
		// serialize the current dropdown position
		final int position = this.spinner.getSelectedItemPosition();
		savedInstanceState.putInt(MainActivity.STATE_SELECTED_PROVIDER_ITEM, position);

		// always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	public void onRestoreInstanceState(final Bundle savedInstanceState)
	{
		// always call the superclass so it can restore the view hierarchy
		super.onRestoreInstanceState(savedInstanceState);

		// restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(MainActivity.STATE_SELECTED_PROVIDER_ITEM))
		{
			this.spinner.setSelection(savedInstanceState.getInt(MainActivity.STATE_SELECTED_PROVIDER_ITEM));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		// inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		// search view
		final SearchView searchView = (SearchView) menu.findItem(R.id.searchView).getActionView();
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
		{
			@SuppressWarnings("synthetic-access")
			@Override
			public boolean onQueryTextSubmit(final String query)
			{
				searchView.clearFocus();
				searchView.setQuery("", false); //$NON-NLS-1$
				tryStartTreebolic(query);
				return true;
			}

			@Override
			public boolean onQueryTextChange(final String newText)
			{
				return false;
			}
		});

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(final View arg0)
	{
		tryStartTreebolic((String) null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId())
		{
			case R.id.action_treebolic:
				tryStartTreebolic((String) null);
				return true;

			case R.id.action_treebolic_client:
				tryStartTreebolicClient();
				return true;

			case R.id.action_treebolic_source:
				requestTreebolicSource();
				return true;

			case R.id.action_treebolic_bundle:
				requestTreebolicBundle();
				return true;

			case R.id.action_treebolic_serialized:
				requestTreebolicSerialized();
				return true;

			case R.id.action_demo:
				final Uri archiveUri = Storage.copyAssetFile(this, Settings.DEMO);
				tryStartTreebolicBundle(archiveUri);
				return true;

			case R.id.action_download:
			{
				final Intent intent = new Intent(this, DownloadActivity.class);
				intent.putExtra(org.treebolic.download.DownloadActivity.ARG_ALLOW_EXPAND_ARCHIVE, true);
				startActivityForResult(intent, MainActivity.REQUEST_DOWNLOAD_CODE);
				return true;
			}

			case R.id.action_settings:
				tryStartTreebolicSettings();
				return true;

			case R.id.action_tips:
				Tip.show(getFragmentManager());
				return true;

			case R.id.action_help:
				HelpActivity.start(this);
				return true;

			case R.id.action_about:
				AboutActivity.start(this);
				return true;

			case R.id.action_services:
				startActivity(new Intent(this, ServicesActivity.class));
				return true;

			case R.id.action_providers:
				startActivity(new Intent(this, ProvidersActivity.class));
				return true;

			case R.id.action_rescan:
				Providers.getProviders(this, true);
				this.adapter.notifyDataSetChanged();
				return true;

			case R.id.action_finish:
				finish();
				return true;

			case R.id.action_kill:
				Process.killProcess(Process.myPid());
				return true;

			case R.id.action_app_settings:
				Settings.applicationSettings(this, "org.treebolic"); //$NON-NLS-1$
				return true;

			// case R.id.action_test:
			// test();
			// return true;

			default:
				break;
		}
		return false;
	}

	/**
	 * Initialize
	 */
	@SuppressLint("CommitPrefEdits")
	private void initialize()
	{
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

		// test if initialized
		final boolean result = sharedPref.getBoolean(Settings.PREF_INITIALIZED, false);
		if (result)
		{
			return;
		}

		// default settings
		Settings.setDefaults(this);

		// deploy
		Storage.expandZipAssetFile(this, "tests.zip"); //$NON-NLS-1$

		// flag as initialized
		sharedPref.edit().putBoolean(Settings.PREF_INITIALIZED, true).commit();
	}

	// S P E C I F I C R E T U R N S

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent returnIntent)
	{
		// handle selection of input by other activity which returns selected input
		switch (requestCode)
		{
			case REQUEST_FILE_CODE:
			case REQUEST_BUNDLE_CODE:
			case REQUEST_SERIALIZED_CODE:
				if (resultCode == Activity.RESULT_OK)
				{
					final Uri fileUri = returnIntent.getData();
					if (fileUri == null)
					{
						break;
					}

					Toast.makeText(this, fileUri.toString(), Toast.LENGTH_SHORT).show();
					switch (requestCode)
					{
						case REQUEST_FILE_CODE:
							setFolder(fileUri);
							tryStartTreebolic(fileUri);
							break;
						case REQUEST_BUNDLE_CODE:
							setFolder(fileUri);
							tryStartTreebolicBundle(fileUri);
							break;
						case REQUEST_SERIALIZED_CODE:
							setFolder(fileUri);
							tryStartTreebolicSerialized(fileUri);
							break;
						default:
							break;
					}
				}
				break;
			case REQUEST_DOWNLOAD_CODE:
				break;
			default:
				break;
		}
		super.onActivityResult(requestCode, resultCode, returnIntent);
	}

	// F R A G M E N T

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment
	{
		/**
		 * Constructor
		 */
		public PlaceholderFragment()
		{
			//
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
		 */
		@Override
		public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
		{
			return inflater.inflate(R.layout.fragment_main, container, false);
		}
	}

	// T E S T

	// F O L D E R P R E F E R E N C E

	static final String PREF_CURRENTFOLDER = "org.treebolic.folder"; //$NON-NLS-1$

	/**
	 * Get initial folder
	 *
	 * @return initial folder
	 */
	private String getFolder()
	{
		final File thisFolder = FileChooserActivity.getFolder(this, MainActivity.PREF_CURRENTFOLDER);
		if (thisFolder != null)
		{
			return thisFolder.getPath();
		}
		return Storage.getTreebolicStorage(this).getAbsolutePath();
	}

	/**
	 * Set folder to parent of given uri
	 *
	 * @param fileUri uri
	 */
	private void setFolder(final Uri fileUri)
	{
		final String path = new File(fileUri.getPath()).getParent();
		FileChooserActivity.setFolder(this, MainActivity.PREF_CURRENTFOLDER, path);
	}

	/**
	 * Update button visibility
	 */
	protected void updateButton()
	{
		final Button button = (Button) findViewById(R.id.treebolicButton);
		button.setVisibility(sourceSet() ? View.VISIBLE : View.INVISIBLE);
	}

	/**
	 * Whether source is set
	 *
	 * @return true if source is set
	 */
	private boolean sourceSet()
	{
		final String source = Settings.getStringPref(this, TreebolicIface.PREF_SOURCE);
		final String base = Settings.getStringPref(this, TreebolicIface.PREF_BASE);
		if (source != null && !source.isEmpty())
		{
			final File baseFile = base == null ? null : new File(Uri.parse(base).getPath());
			final File file = new File(baseFile, source);
			Log.d(MainActivity.TAG, "file=" + file); //$NON-NLS-1$
			return file.exists();
		}
		return false;
	}

	// R E Q U E S T S ( S T A R T A C T I V I T Y F O R R E S U L T )

	/**
	 * Request Treebolic source
	 */
	private void requestTreebolicSource()
	{
		final String extensions = (String) this.pluginProvider.get(Providers.EXTENSIONS);

		final Intent intent = new Intent(this, org.treebolic.filechooser.FileChooserActivity.class);
		intent.setType((String) this.pluginProvider.get(Providers.MIMETYPE));
		intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_INITIAL_DIR, (String) this.pluginProvider.get(Providers.BASE));
		intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_EXTENSION_FILTER, extensions == null ? null : extensions.split(",")); //$NON-NLS-1$
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, MainActivity.REQUEST_FILE_CODE);
	}

	/**
	 * Request Treebolic bundle
	 */
	private void requestTreebolicBundle()
	{
		final Intent intent = new Intent(this, org.treebolic.filechooser.FileChooserActivity.class);
		intent.setType("application/zip"); //$NON-NLS-1$
		intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_INITIAL_DIR, (String) this.pluginProvider.get(Providers.BASE));
		intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_EXTENSION_FILTER, new String[]{"zip", "jar"}); //$NON-NLS-1$ //$NON-NLS-2$
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, MainActivity.REQUEST_BUNDLE_CODE);
	}

	/**
	 * Request Treebolic activity
	 */
	private void requestTreebolicSerialized()
	{
		final Intent intent = new Intent(this, org.treebolic.filechooser.FileChooserActivity.class);
		intent.setType("application/x-java-serialized-object"); //$NON-NLS-1$
		intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_INITIAL_DIR, getFolder());
		intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_EXTENSION_FILTER, new String[]{"ser"}); //$NON-NLS-1$
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, MainActivity.REQUEST_SERIALIZED_CODE);
	}

	// R E Q U E S T S ( S T A R T A C T I V I T Y )

	/**
	 * Try to start Treebolic activity from source
	 *
	 * @param source0 source
	 */
	@SuppressWarnings("boxing")
	private void tryStartTreebolic(final String source0)
	{
		if (this.pluginProvider == null)
		{
			return;
		}
		final Boolean isPlugin = (Boolean) this.pluginProvider.get(Providers.ISPLUGIN);
		if (isPlugin)
		{
			tryStartTreebolicPlugin(source0);
			return;
		}
		tryStartTreebolicBuiltin(source0);
	}

	/**
	 * Try to start Treebolic builtin provider activity
	 *
	 * @param source0 source
	 */
	private void tryStartTreebolicBuiltin(final String source0)
	{
		final String provider = (String) this.pluginProvider.get(Providers.PROVIDER);
		if (provider == null || provider.isEmpty())
		{
			Toast.makeText(this, R.string.error_null_provider, Toast.LENGTH_SHORT).show();
			return;
		}
		final String source = source0 != null ? source0 : Settings.getStringPref(this, TreebolicIface.PREF_SOURCE);
		if (source == null || source.isEmpty())
		{
			Toast.makeText(this, R.string.error_null_source, Toast.LENGTH_SHORT).show();
			return;
		}
		final String base = Settings.getStringPref(this, TreebolicIface.PREF_BASE);
		final String imageBase = Settings.getStringPref(this, TreebolicIface.PREF_IMAGEBASE);
		final String settings = Settings.getStringPref(this, TreebolicIface.PREF_SETTINGS);

		final Intent intent = TreebolicActivity.makeTreebolicIntent(this, provider, source, base, imageBase, settings, null);
		Log.d(MainActivity.TAG, "Start treebolic from provider:" + provider + " source:" + source); //$NON-NLS-1$ //$NON-NLS-2$
		startActivity(intent);
	}

	/**
	 * Try to start Treebolic plugin provider activity
	 *
	 * @param source0 source
	 */
	private void tryStartTreebolicPlugin(final String source0)
	{
		if (this.pluginProvider == null)
		{
			return;
		}
		final String pluginPkg = (String) this.pluginProvider.get(Providers.PACKAGE);
		if (pluginPkg == null || pluginPkg.isEmpty())
		{
			Toast.makeText(this, R.string.error_null_plugin, Toast.LENGTH_SHORT).show();
			return;
		}
		final String provider = (String) this.pluginProvider.get(Providers.PROVIDER);
		if (provider == null || provider.isEmpty())
		{
			Toast.makeText(this, R.string.error_null_provider, Toast.LENGTH_SHORT).show();
			return;
		}
		final String urlScheme = (String) this.pluginProvider.get(Providers.URLSCHEME);
		final String source = source0 != null ? source0 : (String) this.pluginProvider.get(Providers.SOURCE);
		if (source == null || source.isEmpty())
		{
			Toast.makeText(this, R.string.error_null_source, Toast.LENGTH_SHORT).show();
			return;
		}
		final String base = (String) this.pluginProvider.get(Providers.BASE);
		final String imageBase = (String) this.pluginProvider.get(Providers.IMAGEBASE);
		final String settings = (String) this.pluginProvider.get(Providers.SETTINGS);
		final String style = (String) this.pluginProvider.get(Providers.STYLE);

		final Intent intent = TreebolicPluginActivity.makeTreebolicIntent(this, pluginPkg, provider, urlScheme, source, base, imageBase, settings, style);
		Log.d(MainActivity.TAG, "Start treebolic from pluginProvider " + pluginPkg + " provider:" + provider + " source:" + source); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		startActivity(intent);
	}

	/**
	 * Try to start Treebolic activity from XML source file
	 *
	 * @param fileUri XML file uri
	 */
	@SuppressWarnings("boxing")
	private void tryStartTreebolic(final Uri fileUri)
	{
		if (this.pluginProvider == null)
		{
			return;
		}
		final String source = fileUri.toString();
		if (source == null || source.isEmpty())
		{
			Toast.makeText(this, R.string.error_null_source, Toast.LENGTH_SHORT).show();
			return;
		}
		final String provider = (String) this.pluginProvider.get(Providers.PROVIDER);
		if (provider == null || provider.isEmpty())
		{
			Toast.makeText(this, R.string.error_null_provider, Toast.LENGTH_SHORT).show();
			return;
		}
		final String base = Settings.getStringPref(this, TreebolicIface.PREF_BASE);
		final String imageBase = Settings.getStringPref(this, TreebolicIface.PREF_IMAGEBASE);
		final String settings = Settings.getStringPref(this, TreebolicIface.PREF_SETTINGS);

		final String pkg = (String) this.pluginProvider.get(Providers.PACKAGE);
		final Boolean isPlugin = (Boolean) this.pluginProvider.get(Providers.ISPLUGIN);
		final String style = (String) this.pluginProvider.get(Providers.STYLE);
		final String urlScheme = (String) this.pluginProvider.get(Providers.URLSCHEME);

		final Intent intent = isPlugin ? //
				TreebolicPluginActivity.makeTreebolicIntent(this, pkg, provider, urlScheme, source, base, imageBase, settings, style) : TreebolicActivity.makeTreebolicIntent(this, provider, source, base, imageBase, settings, style);
		Log.d(MainActivity.TAG, "Start treebolic from uri " + fileUri); //$NON-NLS-1$
		startActivity(intent);
	}

	/**
	 * Try to start Treebolic activity from zipped bundle file
	 *
	 * @param archiveUri archive uri
	 */
	private void tryStartTreebolicBundle(final Uri archiveUri)
	{
		try
		{
			// choose bundle entry
			EntryChooser.choose(this, new File(archiveUri.getPath()), new EntryChooser.Callback()
			{
				@SuppressWarnings("synthetic-access")
				@Override
				public void onSelect(final String zipEntry)
				{
					tryStartTreebolicBundle(archiveUri, zipEntry);
				}
			});
		}
		catch (final IOException e)
		{
			Log.d(MainActivity.TAG, "Failed to start treebolic from bundle uri " + archiveUri, e); //$NON-NLS-1$
		}
	}

	/**
	 * Try to start Treebolic activity from zip file
	 *
	 * @param archiveUri archive file uri
	 * @param zipEntry   archive entry
	 */
	@SuppressWarnings({"boxing", "UnnecessaryLocalVariable"})
	private void tryStartTreebolicBundle(final Uri archiveUri, final String zipEntry)
	{
		Log.d(MainActivity.TAG, "Start treebolic from bundle uri " + archiveUri + " and zipentry " + zipEntry); //$NON-NLS-1$ //$NON-NLS-2$
		final String source = zipEntry; // alternatively: "jar:" + fileUri.toString() + "!/" + zipEntry;
		if (source == null)
		{
			Toast.makeText(this, R.string.error_null_source, Toast.LENGTH_SHORT).show();
			return;
		}
		final String provider = (String) this.pluginProvider.get(Providers.PROVIDER);
		if (provider == null || provider.isEmpty())
		{
			Toast.makeText(this, R.string.error_null_provider, Toast.LENGTH_SHORT).show();
			return;
		}

		final String base = "jar:" + archiveUri.toString() + "!/"; //$NON-NLS-1$ //$NON-NLS-2$
		final String imageBase = base;
		final String settings = Settings.getStringPref(this, TreebolicIface.PREF_SETTINGS);

		final String pkg = (String) this.pluginProvider.get(Providers.PACKAGE);
		final Boolean isPlugin = (Boolean) this.pluginProvider.get(Providers.ISPLUGIN);
		final String style = (String) this.pluginProvider.get(Providers.STYLE);
		final String urlScheme = (String) this.pluginProvider.get(Providers.URLSCHEME);

		final Intent intent = isPlugin ? //
				TreebolicPluginActivity.makeTreebolicIntent(this, pkg, provider, urlScheme, source, base, imageBase, settings, style) : TreebolicActivity.makeTreebolicIntent(this, provider, source, base, imageBase, settings, style);
		Log.d(MainActivity.TAG, "Start treebolic from bundle uri " + archiveUri); //$NON-NLS-1$
		startActivity(intent);
	}

	/**
	 * Try to start Treebolic activity from zipped serialized model file
	 *
	 * @param archiveUri zipped serialized model file
	 */
	private void tryStartTreebolicSerialized(final Uri archiveUri)
	{
		if (archiveUri == null)
		{
			Toast.makeText(this, R.string.error_null_source, Toast.LENGTH_SHORT).show();
			return;
		}
		final Intent intent = TreebolicModelActivity.makeTreebolicSerializedIntent(this, archiveUri);
		Log.d(MainActivity.TAG, "Start treebolic from serialized uri " + archiveUri); //$NON-NLS-1$
		startActivity(intent);
	}

	/**
	 * Try to start Treebolic settings activity
	 */
	private void tryStartTreebolicSettings()
	{
		final Intent intent = new Intent(this, SettingsActivity.class);
		if (this.pluginProvider != null)
		{
			intent.putExtra(SettingsActivity.ARG_PROVIDER_SELECTED, this.pluginProvider);
		}
		startActivity(intent);
	}

	/**
	 * Try to start Treebolic client activity
	 */
	private void tryStartTreebolicClient()
	{
		final Intent intent = new Intent();
		intent.setClass(this, org.treebolic.TreebolicClientActivity.class);
		Log.d(MainActivity.TAG, "Start treebolic client"); //$NON-NLS-1$
		startActivity(intent);
	}
}
