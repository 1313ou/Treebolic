package org.treebolic;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.treebolic.filechooser.EntryChooser;
import org.treebolic.filechooser.FileChooserActivity;
import org.treebolic.guide.AboutActivity;
import org.treebolic.guide.HelpActivity;
import org.treebolic.guide.Tip;
import org.treebolic.storage.Storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Treebolic main activity (home)
 *
 * @author Bernard Bou
 */
public class MainActivity extends AppCompatActivity implements OnClickListener
{
	/**
	 * Log tag
	 */
	private static final String TAG = "TreebolicMainA";

	/**
	 * State
	 */
	private static final String STATE_SELECTED_PROVIDER_ITEM = "org.treebolic.provider.selected";

	/**
	 * Rescan key
	 */
	private static final String RESCAN_KEY = "rescan";

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

	// Adapter Key - Res id mapping
	static private final String[] from = new String[]{Providers.ICON, Providers.NAME};
	static private final int[] to = new int[]{R.id.icon, R.id.provider};

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

	@SuppressLint("InflateParams")
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// layout
		setContentView(R.layout.activity_main);

		// init preferences
		initialize();

		// toolbar
		final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// action bar
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			// custom layout
			final View actionBarView = getLayoutInflater().inflate(R.layout.actionbar_custom, null);

			// set up action bar
			actionBar.setCustomView(actionBarView);
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_USE_LOGO);

			// spinner
			this.spinner = (Spinner) actionBarView.findViewById(R.id.spinner);

			// set up the dropdown list navigation in the action bar.
			this.spinner.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				@SuppressWarnings("synthetic-access")
				@Override
				public void onItemSelected(final AdapterView<?> parentView, final View selectedItemView, final int position, final long id)
				{
					final HashMap<String, Object> provider = (HashMap<String, Object>) MainActivity.this.adapter.getItem(position);
					if (provider.containsKey(RESCAN_KEY))
					{
						setAdapter();
						return;
					}

					MainActivity.this.pluginProvider = provider;

					final String name = (String) MainActivity.this.pluginProvider.get(Providers.NAME);
					Settings.putStringPref(MainActivity.this, Settings.PREF_PROVIDER_NAME, name);
					Settings.setActivePrefs(MainActivity.this, MainActivity.this.pluginProvider);
					Log.d(MainActivity.TAG, (String) MainActivity.this.pluginProvider.get(Providers.PROVIDER));

					updateButton();
				}

				@Override
				public void onNothingSelected(final AdapterView<?> parentView)
				{
					//
				}
			});

			// adapter
			setAdapter();
		}

		// fragment
		if (savedInstanceState == null)
		{
			getSupportFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		updateButton();
	}

	@Override
	public void onSaveInstanceState(final Bundle savedInstanceState)
	{
		// serialize the current dropdown position
		final int position = this.spinner.getSelectedItemPosition();
		savedInstanceState.putInt(MainActivity.STATE_SELECTED_PROVIDER_ITEM, position);

		// always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

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

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		// inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		// search view
		final MenuItem searchMenuItem = menu.findItem(R.id.action_search);
		final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
		{
			@SuppressWarnings("synthetic-access")
			@Override
			public boolean onQueryTextSubmit(final String query)
			{
				searchView.clearFocus();
				searchView.setQuery("", false);
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

	@Override
	public void onClick(final View arg0)
	{
		tryStartTreebolic((String) null);
	}

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
				tryStartOneOfTreebolicClients();
				return true;

			case R.id.action_treebolic_default_client:
				TreebolicClientActivity.initializeSearchPrefs(this);
				tryStartTreebolicDefaultClient();
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
				this.spinner.setSelection(0);
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
				Tip.show(getSupportFragmentManager());
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

			case R.id.action_finish:
				finish();
				return true;

			case R.id.action_kill:
				Process.killProcess(Process.myPid());
				return true;

			case R.id.action_app_settings:
				Settings.applicationSettings(this, "org.treebolic");
				return true;

			// case R.id.action_test:
			// test();
			// return true;

			default:
				break;
		}
		return false;
	}

	// S E L E C T I O N   A C T I V I T Y   R E T U R N S (source, bundle, serialized)

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent returnIntent)
	{
		// handle selection of input by other activity which returns selected input
		switch (requestCode)
		{
			case REQUEST_FILE_CODE:
			case REQUEST_BUNDLE_CODE:
			case REQUEST_SERIALIZED_CODE:
				if (resultCode == AppCompatActivity.RESULT_OK)
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
		@Override
		public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
		{
			return inflater.inflate(R.layout.fragment_main, container, false);
		}
	}

	// P R E F E R E N C E S

	/**
	 * Initialize
	 */
	@SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
	private void initialize()
	{
		// permissions
		Permissions.check(this);

		// test if initialized
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean initialized = sharedPref.getBoolean(Settings.PREF_INITIALIZED, false);
		if (!initialized)
		{
			// default settings
			Settings.setDefaults(this);

			// flag as initialized
			sharedPref.edit().putBoolean(Settings.PREF_INITIALIZED, true).commit();
		}

		// deploy
		final File dir = Storage.getTreebolicStorage(this);
		if (dir.isDirectory())
		{
			if (dir.list().length == 0)
			{
				// deploy
				Storage.expandZipAssetFile(this, "tests.zip");
				// Storage.expandZipAssetFile(this, "serialized.zip");
			}
		}
	}

	// folder preference

	static final String PREF_CURRENTFOLDER = "org.treebolic.folder";

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

	// S P I N N E R

	/**
	 * Set spinner adapter
	 */
	private void setAdapter()
	{
		// spinner adapter
		this.adapter = makeAdapter(R.layout.spinner_item_providers, from, to);
		if (this.adapter != null)
		{
			// set spinner adapter
			this.spinner.setAdapter(this.adapter);

			// saved name
			final String name = Settings.getStringPref(MainActivity.this, Settings.PREF_PROVIDER_NAME);

			// position
			if (name != null)
			{
				for (int position = 0; position < this.adapter.getCount(); position++)
				{
					@SuppressWarnings("unchecked") final HashMap<String, Object> provider = (HashMap<String, Object>) this.adapter.getItem(position);
					if (provider.get(Providers.NAME).equals(name))
					{
						this.spinner.setSelection(position);
					}
				}
			}
		}
	}

	/**
	 * Make adapter
	 *
	 * @param itemRes item layout
	 * @param from    from key
	 * @param to      to res id
	 * @return base adapter
	 */
	private SimpleAdapter makeAdapter(final int itemRes, final String[] from, final int[] to)
	{
		// data
		List<HashMap<String, Object>> providers0 = Providers.getProviders(this, false);
		if (providers0 == null)
		{
			providers0 = new ArrayList<>();
		}
		@SuppressWarnings("unchecked") final List<HashMap<String, Object>> providers = new ArrayList(providers0);
		providers.add(makeRescanDummy());

		// adapter
		final SimpleAdapter adapter = new SimpleAdapter(this, providers, itemRes, from, to)
		{
			@Override
			public void setViewImage(final ImageView imageView, final String pkg)
			{
				try
				{
					// icon
					final Drawable drawable = getPackageManager().getApplicationIcon(pkg);
					imageView.setImageDrawable(drawable);
				}
				catch (final Exception re)
				{
					//
				}
			}
		};

		// drop down
		adapter.setDropDownViewResource(R.layout.spinner_item_providers_dropdown);

		return adapter;
	}

	/**
	 * Dummy provider for rescan action
	 *
	 * @return dummy provider
	 */
	private HashMap<String, Object> makeRescanDummy()
	{
		final HashMap<String, Object> result = new HashMap<>();
		final String name = getString(R.string.title_rescan);
		result.put(Providers.NAME, name);
		result.put(Providers.ICON, R.drawable.ic_refresh);
		result.put(Providers.PACKAGE, null);
		result.put(RESCAN_KEY, true);
		return result;
	}

	// A C T I O N   B U T T O N

	/**
	 * Update button visibility
	 */
	private void updateButton()
	{
		final ImageButton button = (ImageButton) findViewById(R.id.treebolicButton);
		final TextView sourceText = (TextView) findViewById(R.id.treebolicSource);
		final String source = Settings.getStringPref(this, TreebolicIface.PREF_SOURCE);
		final boolean qualifies = sourceQualifies(source);
		button.setVisibility(qualifies ? View.VISIBLE : View.INVISIBLE);
		sourceText.setVisibility(qualifies ? View.VISIBLE : View.INVISIBLE);
		if (qualifies)
		{
			sourceText.setText(source);
		}
	}

	/**
	 * Whether source qualifies
	 *
	 * @return true if source qualifies
	 */
	private boolean sourceQualifies(final String source)
	{
		if (source != null && !source.isEmpty())
		{
			/*
			final String base = Settings.getStringPref(this, TreebolicIface.PREF_BASE);
			final File baseFile = base == null ? null : new File(Uri.parse(base).getPath());
			final File file = new File(baseFile, source);
			Log.d(MainActivity.TAG, "file=" + file);
			return file.exists();
			 */

			/*
			final File file = new File(source);
			Log.d(MainActivity.TAG, "file=" + file);
			return file.exists() && file.isDirectory();
			 */
			return true;
		}
		return false;
	}

	/**
	 * Choose service
	 */
	private void tryStartOneOfTreebolicClients()
	{
		final List<HashMap<String, Object>> services = Services.getServices(this, true);

		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.title_services);
		alert.setMessage(R.string.title_choose_service);

		final RadioGroup input = new RadioGroup(this);
		if (services != null)
		{
			for (HashMap<String, Object> service : services)
			{
				final RadioButton radioButton = new RadioButton(this);
				radioButton.setText((String) service.get(Services.LABEL));
				final String drawableRef = (String) service.get(Services.DRAWABLE);
				final String[] fields = drawableRef.split("#");
				final int index = Integer.parseInt(fields[1]);
				final Drawable drawable = Services.loadIcon(getPackageManager(), fields[0], index);
				radioButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
				radioButton.setCompoundDrawablePadding(10);
				radioButton.setTag(service);
				input.addView(radioButton);
			}
		}
		alert.setView(input);
		alert.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(final DialogInterface dialog, final int whichButton)
			{
				// canceled.
			}
		});

		final AlertDialog dialog = alert.create();
		input.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId)
			{
				dialog.dismiss();

				int childCount = input.getChildCount();
				for (int i = 0; i < childCount; i++)
				{
					final RadioButton radioButton = (RadioButton) input.getChildAt(i);
					if (radioButton.getId() == input.getCheckedRadioButtonId())
					{
						@SuppressWarnings("unchecked") final HashMap<String, Object> service = (HashMap<String, Object>) radioButton.getTag();
						tryStartTreebolicClient(service);
					}
				}
			}
		});
		dialog.show();
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
		intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_EXTENSION_FILTER, extensions == null ? null : extensions.split(","));
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, MainActivity.REQUEST_FILE_CODE);
	}

	/**
	 * Request Treebolic bundle
	 */
	private void requestTreebolicBundle()
	{
		final Intent intent = new Intent(this, org.treebolic.filechooser.FileChooserActivity.class);
		intent.setType("application/zip");
		intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_INITIAL_DIR, (String) this.pluginProvider.get(Providers.BASE));
		intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_EXTENSION_FILTER, new String[]{"zip", "jar"});
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, MainActivity.REQUEST_BUNDLE_CODE);
	}

	/**
	 * Request Treebolic activity
	 */
	private void requestTreebolicSerialized()
	{
		final Intent intent = new Intent(this, org.treebolic.filechooser.FileChooserActivity.class);
		intent.setType("application/x-java-serialized-object");
		intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_INITIAL_DIR, getFolder());
		intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_EXTENSION_FILTER, new String[]{"ser"});
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
		Log.d(MainActivity.TAG, "Start treebolic from provider:" + provider + " source:" + source);
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
		Log.d(MainActivity.TAG, "Start treebolic from pluginProvider " + pluginPkg + " provider:" + provider + " source:" + source);
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
		Log.d(MainActivity.TAG, "Start treebolic from uri " + fileUri);
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
			Log.d(MainActivity.TAG, "Failed to start treebolic from bundle uri " + archiveUri, e);
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
		Log.d(MainActivity.TAG, "Start treebolic from bundle uri " + archiveUri + " and zipentry " + zipEntry);
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

		final String base = "jar:" + archiveUri.toString() + "!/";
		final String imageBase = base;
		final String settings = Settings.getStringPref(this, TreebolicIface.PREF_SETTINGS);

		final String pkg = (String) this.pluginProvider.get(Providers.PACKAGE);
		final Boolean isPlugin = (Boolean) this.pluginProvider.get(Providers.ISPLUGIN);
		final String style = (String) this.pluginProvider.get(Providers.STYLE);
		final String urlScheme = (String) this.pluginProvider.get(Providers.URLSCHEME);

		final Intent intent = isPlugin ? //
				TreebolicPluginActivity.makeTreebolicIntent(this, pkg, provider, urlScheme, source, base, imageBase, settings, style) : TreebolicActivity.makeTreebolicIntent(this, provider, source, base, imageBase, settings, style);
		Log.d(MainActivity.TAG, "Start treebolic from bundle uri " + archiveUri);
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
		Log.d(MainActivity.TAG, "Start treebolic from serialized uri " + archiveUri);
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
	 * Try to start Treebolic default client activity
	 */
	private void tryStartTreebolicDefaultClient()
	{
		final Intent intent = new Intent();
		intent.setClass(this, org.treebolic.TreebolicClientActivity.class);
		Log.d(MainActivity.TAG, "Start  treebolic default client");
		startActivity(intent);
	}

	/**
	 * Try to start Treebolic client activity
	 */
	private void tryStartTreebolicClient(final HashMap<String, Object> service)
	{
		final String argService = (String) service.get(Services.PACKAGE) + '/' + service.get(Services.NAME);

		final Intent intent = new Intent();
		intent.setClass(this, org.treebolic.TreebolicClientActivity.class);
		intent.putExtra(TreebolicIface.ARG_SERVICE, argService);
		Log.d(MainActivity.TAG, "Start treebolic client for " + argService);
		startActivity(intent);
	}
}
