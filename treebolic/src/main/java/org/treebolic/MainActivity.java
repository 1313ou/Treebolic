/*
 * Copyright (c) 2023. Bernard Bou
 */

package org.treebolic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
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

import com.bbou.donate.DonateActivity;
import com.bbou.others.OthersActivity;
import com.bbou.rate.AppRate;

import org.treebolic.filechooser.EntryChooser;
import org.treebolic.filechooser.FileChooserActivity;
import org.treebolic.guide.AboutActivity;
import org.treebolic.guide.HelpActivity;
import org.treebolic.guide.Tip;
import org.treebolic.storage.Deployer;
import org.treebolic.storage.Storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

/**
 * Treebolic main activity (home)
 *
 * @author Bernard Bou
 */
public class MainActivity extends AppCompatCommonActivity implements OnClickListener
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
	@Nullable
	private SimpleAdapter adapter;

	/**
	 * Activity file result launcher
	 */
	protected ActivityResultLauncher<Intent> activityFileResultLauncher;

	/**
	 * Activity bundle result launcher
	 */
	protected ActivityResultLauncher<Intent> activityBundleResultLauncher;

	/**
	 * Activity serialized result launcher
	 */
	protected ActivityResultLauncher<Intent> activitySerializedResultLauncher;

	/**
	 * Activity download result launcher
	 */
	protected ActivityResultLauncher<Intent> activityDownloadResultLauncher;

	// L I F E C Y C L E O V E R R I D E S

	@SuppressLint("InflateParams")
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(@Nullable final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// rate
		AppRate.invoke(this);

		// layout
		setContentView(R.layout.activity_main);

		// init preferences
		initialize();

		// activity file result launcher
		this.activityFileResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

			boolean success = result.getResultCode() == Activity.RESULT_OK;
			if (success)
			{
				// handle selection of input by other activity which returns selected input (source, bundle, serialized)
				Intent returnIntent = result.getData();
				if (returnIntent != null)
				{
					final Uri fileUri = returnIntent.getData();
					if (fileUri != null)
					{
						Toast.makeText(this, fileUri.toString(), Toast.LENGTH_SHORT).show();
						setFolder(fileUri);
						tryStartTreebolic(fileUri);
					}
				}
			}
		});

		// activity bundle result launcher
		this.activityBundleResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

			boolean success = result.getResultCode() == Activity.RESULT_OK;
			if (success)
			{
				// handle selection of input by other activity which returns selected input (source, bundle, serialized)
				Intent returnIntent = result.getData();
				if (returnIntent != null)
				{
					final Uri fileUri = returnIntent.getData();
					if (fileUri != null)
					{
						Toast.makeText(this, fileUri.toString(), Toast.LENGTH_SHORT).show();
						setFolder(fileUri);
						tryStartTreebolicBundle(fileUri);
					}
				}
			}
		});

		// activity serialized  result launcher
		this.activitySerializedResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

			boolean success = result.getResultCode() == Activity.RESULT_OK;
			if (success)
			{
				// handle selection of input by other activity which returns selected input (source, bundle, serialized)
				Intent returnIntent = result.getData();
				if (returnIntent != null)
				{
					final Uri fileUri = returnIntent.getData();
					if (fileUri != null)
					{
						Toast.makeText(this, fileUri.toString(), Toast.LENGTH_SHORT).show();
						setFolder(fileUri);
						tryStartTreebolicSerialized(fileUri);
					}
				}
			}
		});

		// activity download result launcher
		this.activityDownloadResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

			//			boolean success = result.getResultCode() == Activity.RESULT_OK;
			//			if (success)
			//			{
			//				// handle selection of input by other activity which returns selected input (source, bundle, serialized)
			//				Intent returnIntent = result.getData();
			//			}
		});

		// toolbar
		final Toolbar toolbar = findViewById(R.id.toolbar);
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
			this.spinner = actionBarView.findViewById(R.id.spinner);

			// set up the dropdown list navigation in the action bar.
			this.spinner.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(final AdapterView<?> parentView, final View selectedItemView, final int position, final long id)
				{
					assert MainActivity.this.adapter != null;
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
					Log.d(MainActivity.TAG, name == null ? "null" : name);

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
	public void onSaveInstanceState(@NonNull final Bundle savedInstanceState)
	{
		// serialize the current dropdown position
		final int position = this.spinner.getSelectedItemPosition();
		savedInstanceState.putInt(MainActivity.STATE_SELECTED_PROVIDER_ITEM, position);

		// always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onRestoreInstanceState(@NonNull final Bundle savedInstanceState)
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
	public boolean onCreateOptionsMenu(@NonNull final Menu menu)
	{
		// inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		// search view
		final MenuItem searchMenuItem = menu.findItem(R.id.action_search);
		final SearchView searchView = (SearchView) searchMenuItem.getActionView();
		assert searchView != null;
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
		{
			@Override
			public boolean onQueryTextSubmit(@NonNull final String query)
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
	public boolean onOptionsItemSelected(@NonNull final MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (R.id.action_treebolic == id)
		{
			tryStartTreebolic((String) null);
			return true;
		}
		else if (R.id.action_treebolic_source == id)
		{
			requestTreebolicSource();
			return true;
		}
		else if (R.id.action_treebolic_bundle == id)
		{
			requestTreebolicBundle();
			return true;
		}
		else if (R.id.action_treebolic_serialized == id)
		{
			requestTreebolicSerialized();
			return true;
		}
		else if (R.id.action_treebolic_client == id)
		{
			TreebolicClientActivity.initializeSearchPrefs(this);
			tryStartOneOfTreebolicClients();
			return true;
		}
		else if (R.id.action_treebolic_default_client == id)
		{
			TreebolicClientActivity.initializeSearchPrefs(this);
			tryStartTreebolicDefaultClient();
			return true;
		}
		else if (R.id.action_demo == id)
		{
			final Uri archiveUri = Deployer.copyAssetFile(this, Settings.DEMO);
			this.spinner.setSelection(0);
			assert archiveUri != null;
			tryStartTreebolicBundle(archiveUri);
			return true;
		}
		else if (R.id.action_download == id)
		{
			final Intent intent = new Intent(this, DownloadActivity.class);
			intent.putExtra(org.treebolic.download.DownloadActivity.ARG_ALLOW_EXPAND_ARCHIVE, true);
			this.activityDownloadResultLauncher.launch(intent);
			return true;
		}
		else if (R.id.action_services == id)
		{
			startActivity(new Intent(this, ServicesActivity.class));
			return true;
		}
		else if (R.id.action_providers == id)
		{
			startActivity(new Intent(this, ProvidersActivity.class));
			return true;
		}
		else if (R.id.action_settings == id)
		{
			tryStartTreebolicSettings();
			return true;
		}
		else if (R.id.action_help == id)
		{
			startActivity(new Intent(this, HelpActivity.class));
			return true;
		}
		else if (R.id.action_tips == id)
		{
			Tip.show(getSupportFragmentManager());
			return true;
		}
		else if (R.id.action_about == id)
		{
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		}
		else if (R.id.action_others == id)
		{
			startActivity(new Intent(this, OthersActivity.class));
			return true;
		}
		else if (R.id.action_donate == id)
		{
			startActivity(new Intent(this, DonateActivity.class));
			return true;
		}
		else if (R.id.action_rate == id)
		{
			AppRate.rate(this);
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
		else if (R.id.action_app_settings == id)
		{
			Settings.applicationSettings(this, "org.treebolic");
			return true;
		}
		else
		{
			return false;
		}
	}

	// F R A G M E N T

	/**
	 * A placeholder fragment containing a simple view.
	 */
	@SuppressWarnings("WeakerAccess")
	public static class PlaceholderFragment extends Fragment
	{
		@Override
		public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
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
			final String[] dirContent = dir.list();
			if (dirContent == null || dirContent.length == 0)
			{
				// deploy
				Deployer.expandZipAssetFile(this, "tests.zip");
				// StorageUtils.expandZipAssetFile(this, "serialized.zip");
			}
		}
	}

	// folder preference

	private static final String PREF_CURRENTFOLDER = "org.treebolic.folder";

	/**
	 * Get initial folder
	 *
	 * @return initial folder
	 */
	@NonNull
	private String getFolder()
	{
		final File folder = FileChooserActivity.getFolder(this, MainActivity.PREF_CURRENTFOLDER);
		if (folder != null)
		{
			return folder.getPath();
		}
		return Storage.getTreebolicStorage(this).getAbsolutePath();
	}

	/**
	 * Set folder to parent of given uri
	 *
	 * @param fileUri uri
	 */
	private void setFolder(@NonNull final Uri fileUri)
	{
		final String path = fileUri.getPath();
		if (path != null)
		{
			final String parentPath = new File(path).getParent();
			FileChooserActivity.setFolder(this, MainActivity.PREF_CURRENTFOLDER, parentPath);
		}
	}

	// S P I N N E R

	/**
	 * Set spinner adapter
	 */
	private void setAdapter()
	{
		// spinner adapter
		this.adapter = makeAdapter(R.layout.spinner_item_providers, from, to);

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
				if (name.equals(provider.get(Providers.NAME)))
				{
					this.spinner.setSelection(position);
				}
			}
		}
	}

	/**
	 * Make adapter
	 *
	 * @param itemLayoutRes item layout
	 * @param from          from key
	 * @param to            to res id
	 * @return base adapter
	 */
	@NonNull
	private SimpleAdapter makeAdapter(@SuppressWarnings("SameParameterValue") @LayoutRes final int itemLayoutRes, @SuppressWarnings("SameParameterValue") final String[] from, @SuppressWarnings("SameParameterValue") final int[] to)
	{
		// data
		List<HashMap<String, Object>> providers0 = Providers.getProviders(this, true);
		if (providers0 == null)
		{
			providers0 = new ArrayList<>();
		}

		final List<HashMap<String, Object>> providers = new ArrayList<>(providers0);
		providers.add(makeRescanDummy());

		// adapter
		final SimpleAdapter adapter = new SimpleAdapter(this, providers, itemLayoutRes, from, to)
		{
			@Override
			public void setViewImage(@NonNull final ImageView imageView, final String pkg)
			{
				try
				{
					// icon
					final Drawable drawable = getPackageManager().getApplicationIcon(pkg);
					imageView.setImageDrawable(drawable);
				}
				catch (@NonNull final Exception ignored)
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
	@NonNull
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
		final ImageButton button = findViewById(R.id.treebolicButton);
		button.setOnClickListener(this);
		final TextView sourceText = findViewById(R.id.treebolicSource);
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
	private boolean sourceQualifies(@Nullable final String source)
	{
		//noinspection RedundantIfStatement
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
				if (drawableRef != null)
				{
					final String[] fields = drawableRef.split("#");
					final int index = Integer.parseInt(fields[1]);
					final Drawable drawable = Services.loadIcon(getPackageManager(), fields[0], index);
					radioButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
				}
				radioButton.setCompoundDrawablePadding(10);
				radioButton.setTag(service);
				input.addView(radioButton);
			}
		}
		alert.setView(input);
		alert.setNegativeButton(R.string.action_cancel, (dialog, whichButton) -> {
			// canceled.
		});

		final AlertDialog dialog = alert.create();
		input.setOnCheckedChangeListener((group, checkedId) -> {
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
		this.activityFileResultLauncher.launch(intent); // MainActivity.REQUEST_FILE_CODE
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
		this.activityBundleResultLauncher.launch(intent); // MainActivity.REQUEST_BUNDLE_CODE
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
		this.activitySerializedResultLauncher.launch(intent); // MainActivity.REQUEST_SERIALIZED_CODE);
	}

	// R E Q U E S T S ( S T A R T A C T I V I T Y )

	/**
	 * Try to start Treebolic activity from source
	 *
	 * @param source0 source
	 */
	private void tryStartTreebolic(final String source0)
	{
		if (this.pluginProvider == null)
		{
			return;
		}
		final Boolean isPluginBool = (Boolean) this.pluginProvider.get(Providers.ISPLUGIN);
		final boolean isPlugin = isPluginBool != null && isPluginBool;
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
	private void tryStartTreebolicBuiltin(@Nullable final String source0)
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
	private void tryStartTreebolicPlugin(@Nullable final String source0)
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
	 * Try to start Treebolic activity from source file
	 *
	 * @param fileUri XML file uri
	 */
	private void tryStartTreebolic(@NonNull final Uri fileUri)
	{
		if (this.pluginProvider == null)
		{
			return;
		}
		final String source = fileUri.toString();
		if (source.isEmpty())
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
		final String style = (String) this.pluginProvider.get(Providers.STYLE);
		final String urlScheme = (String) this.pluginProvider.get(Providers.URLSCHEME);
		final Boolean isPluginBool = (Boolean) this.pluginProvider.get(Providers.ISPLUGIN);
		final boolean isPlugin = isPluginBool != null && isPluginBool;

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
	private void tryStartTreebolicBundle(@NonNull final Uri archiveUri)
	{
		try
		{
			final String path = archiveUri.getPath();
			if (path != null)
			{
				// choose bundle entry
				EntryChooser.choose(this, new File(path), zipEntry -> tryStartTreebolicBundle(archiveUri, zipEntry));
			}
		}
		catch (@NonNull final IOException e)
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
	@SuppressWarnings({"UnnecessaryLocalVariable"})
	private void tryStartTreebolicBundle(@NonNull final Uri archiveUri, final String zipEntry)
	{
		Log.d(MainActivity.TAG, "Start treebolic from bundle uri " + archiveUri + " and zip entry " + zipEntry);
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

		final String base = "jar:" + archiveUri + "!/";
		final String imageBase = base;
		final String settings = Settings.getStringPref(this, TreebolicIface.PREF_SETTINGS);

		final String pkg = (String) this.pluginProvider.get(Providers.PACKAGE);
		final String style = (String) this.pluginProvider.get(Providers.STYLE);
		final String urlScheme = (String) this.pluginProvider.get(Providers.URLSCHEME);
		final Boolean isPluginBool = (Boolean) this.pluginProvider.get(Providers.ISPLUGIN);
		final boolean isPlugin = isPluginBool != null && isPluginBool;

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
	private void tryStartTreebolicSerialized(@Nullable final Uri archiveUri)
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
	private void tryStartTreebolicClient(@NonNull final HashMap<String, Object> service)
	{
		final String argService = (String) service.get(Services.PACKAGE) + '/' + service.get(Services.NAME);

		final Intent intent = new Intent();
		intent.setClass(this, org.treebolic.TreebolicClientActivity.class);
		intent.putExtra(TreebolicIface.ARG_SERVICE, argService);
		Log.d(MainActivity.TAG, "Start treebolic client for " + argService);
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
}
