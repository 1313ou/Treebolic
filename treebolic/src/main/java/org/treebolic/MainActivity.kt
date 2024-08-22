/*
 * Copyright (c) 2023. Bernard Bou
 */
package org.treebolic

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SimpleAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.bbou.donate.DonateActivity
import com.bbou.others.OthersActivity
import com.bbou.rate.AppRate.invoke
import com.bbou.rate.AppRate.rate
import org.treebolic.Services.getServices
import org.treebolic.Services.loadIcon
import org.treebolic.filechooser.EntryChooser.Companion.choose
import org.treebolic.filechooser.FileChooserActivity
import org.treebolic.filechooser.FileChooserActivity.Companion.getFolder
import org.treebolic.filechooser.FileChooserActivity.Companion.setFolder
import org.treebolic.guide.AboutActivity
import org.treebolic.guide.HelpActivity
import org.treebolic.guide.Tip.Companion.show
import org.treebolic.storage.Deployer.copyAssetFile
import org.treebolic.storage.Deployer.expandZipAssetFile
import org.treebolic.storage.Storage.getTreebolicStorage
import java.io.File
import java.io.IOException

/**
 * Treebolic main activity (home)
 *
 * @author Bernard Bou
 */
class MainActivity : AppCompatCommonActivity(), View.OnClickListener {

    /**
     * Selected provider
     */
    private var provider: Provider? = null

    /**
     * Provider spinner
     */
    private lateinit var spinner: Spinner

    /**
     * Provider adapter
     */
    private var adapter: SimpleAdapter? = null

    /**
     * Activity file result launcher
     */
    private var activityFileResultLauncher: ActivityResultLauncher<Intent>? = null

    /**
     * Activity bundle result launcher
     */
    private var activityBundleResultLauncher: ActivityResultLauncher<Intent>? = null

    /**
     * Activity serialized result launcher
     */
    private var activitySerializedResultLauncher: ActivityResultLauncher<Intent>? = null

    /**
     * Activity download result launcher
     */
    private var activityDownloadResultLauncher: ActivityResultLauncher<Intent>? = null

    // L I F E C Y C L E

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // rate
        invoke(this)

        // init preferences
        initialize()

        // layout
        setContentView(R.layout.activity_main)

        // activity file result launcher
        activityFileResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val success = result.resultCode == RESULT_OK
            if (success) {
                // handle selection of input by other activity which returns selected input (source, bundle, serialized)
                val returnIntent = result.data
                if (returnIntent != null) {
                    val fileUri = returnIntent.data
                    if (fileUri != null) {
                        Toast.makeText(this, fileUri.toString(), Toast.LENGTH_SHORT).show()
                        setFolder(fileUri)
                        tryStartTreebolic(fileUri)
                    }
                }
            }
        }

        // activity bundle result launcher
        activityBundleResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val success = result.resultCode == RESULT_OK
            if (success) {
                // handle selection of input by other activity which returns selected input (source, bundle, serialized)
                val returnIntent = result.data
                if (returnIntent != null) {
                    val fileUri = returnIntent.data
                    if (fileUri != null) {
                        Toast.makeText(this, fileUri.toString(), Toast.LENGTH_SHORT).show()
                        setFolder(fileUri)
                        tryStartTreebolicBundle(fileUri)
                    }
                }
            }
        }

        // activity serialized  result launcher
        activitySerializedResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val success = result.resultCode == RESULT_OK
            if (success) {
                // handle selection of input by other activity which returns selected input (source, bundle, serialized)
                val returnIntent = result.data
                if (returnIntent != null) {
                    val fileUri = returnIntent.data
                    if (fileUri != null) {
                        Toast.makeText(this, fileUri.toString(), Toast.LENGTH_SHORT).show()
                        setFolder(fileUri)
                        tryStartTreebolicSerialized(fileUri)
                    }
                }
            }
        }

        // activity download result launcher
        activityDownloadResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

        // toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // action bar
        val actionBar = supportActionBar
        if (actionBar != null) {

            // custom layout
            val actionBarView = layoutInflater.inflate(R.layout.actionbar_custom, null)

            // set up action bar
            actionBar.customView = actionBarView
            actionBar.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM or ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_USE_LOGO

            // spinner
            spinner = actionBarView.findViewById(R.id.spinner)

            // set up the dropdown list navigation in the action bar.
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                    checkNotNull(this@MainActivity.adapter)

                    @Suppress("UNCHECKED_CAST")
                    this@MainActivity.provider = adapter!!.getItem(position) as Provider

                    val name = provider!![Providers.NAME]
                    Settings.putStringPref(this@MainActivity, Settings.PREF_PROVIDER_NAME, name.toString())
                    Settings.setActivePrefs(this@MainActivity, provider!!)
                    Log.d(TAG, "Selected provider " + (name ?: "null"))

                    updateButton()
                }

                override fun onNothingSelected(parentView: AdapterView<*>?) {
                    //
                }
            }

            // adapter
            setAdapter()
        }

        // fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().add(R.id.container, PlaceholderFragment()).commit()
        }
    }

    override fun onResume() {
        super.onResume()
        updateButton()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        // serialize the current dropdown position
        val position = spinner.selectedItemPosition
        outState.putInt(STATE_SELECTED_PROVIDER_ITEM, position)

        // always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState)

        // restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_PROVIDER_ITEM)) {
            spinner.setSelection(savedInstanceState.getInt(STATE_SELECTED_PROVIDER_ITEM))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)

        // search view
        val searchMenuItem = menu.findItem(R.id.action_search)
        val searchView = checkNotNull(searchMenuItem.actionView as SearchView?)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchView.clearFocus()
                searchView.setQuery("", false)
                tryStartTreebolic(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })

        return true
    }

    override fun onClick(arg0: View) {
        tryStartTreebolic(null as String?)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (R.id.action_treebolic == id) {
            tryStartTreebolic(null as String?)
            return true
        } else if (R.id.action_treebolic_source == id) {
            requestTreebolicSource()
            return true
        } else if (R.id.action_treebolic_bundle == id) {
            requestTreebolicBundle()
            return true
        } else if (R.id.action_treebolic_serialized == id) {
            requestTreebolicSerialized()
            return true
        } else if (R.id.action_treebolic_client == id) {
            TreebolicClientActivity.initializeSearchPrefs(this)
            tryStartOneOfTreebolicClients()
            return true
        } else if (R.id.action_treebolic_default_client == id) {
            TreebolicClientActivity.initializeSearchPrefs(this)
            tryStartTreebolicDefaultClient()
            return true
        } else if (R.id.action_demo == id) {
            val archiveUri = copyAssetFile(this, Settings.DEMO)
            spinner.setSelection(0)
            checkNotNull(archiveUri)
            tryStartTreebolicBundle(archiveUri)
            return true
        } else if (R.id.action_download == id) {
            val intent = Intent(this, DownloadActivity::class.java)
            intent.putExtra(org.treebolic.download.DownloadActivity.ARG_ALLOW_EXPAND_ARCHIVE, true)
            activityDownloadResultLauncher!!.launch(intent)
            return true
        } else if (R.id.action_services == id) {
            startActivity(Intent(this, ServicesActivity::class.java))
            return true
        } else if (R.id.action_providers == id) {
            startActivity(Intent(this, ProvidersActivity::class.java))
            return true
        } else if (R.id.action_settings == id) {
            tryStartTreebolicSettings()
            return true
        } else if (R.id.action_settings_service == id) {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra(AppCompatCommonPreferenceActivity.INITIAL_ARG, SettingsActivity.ServicePreferenceFragment::class.java.name)
            startActivity(intent)
            return true
        } else if (R.id.action_help == id) {
            startActivity(Intent(this, HelpActivity::class.java))
            return true
        } else if (R.id.action_tips == id) {
            show(supportFragmentManager)
            return true
        } else if (R.id.action_about == id) {
            startActivity(Intent(this, AboutActivity::class.java))
            return true
        } else if (R.id.action_others == id) {
            startActivity(Intent(this, OthersActivity::class.java))
            return true
        } else if (R.id.action_donate == id) {
            startActivity(Intent(this, DonateActivity::class.java))
            return true
        } else if (R.id.action_rate == id) {
            rate(this)
            return true
        } else if (R.id.action_finish == id) {
            finish()
            return true
        } else if (R.id.action_kill == id) {
            Process.killProcess(Process.myPid())
            return true
        } else if (R.id.action_app_settings == id) {
            Settings.applicationSettings(this, BuildConfig.APPLICATION_ID)
            return true
        } else {
            return false
        }
    }

    // F R A G M E N T

    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_main, container, false)
        }
    }

    // P R E F E R E N C E S   A N D   D A T A

    /**
     * Initialize
     */
    private fun initialize() {
        // permissions
        Permissions.check(this)

        // test if initialized
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val initialized = sharedPref.getBoolean(Settings.PREF_INITIALIZED, false)
        if (!initialized) {
            // default settings
            Settings.setDefaults(this)

            // flag as initialized
            sharedPref.edit().putBoolean(Settings.PREF_INITIALIZED, true).commit()

            // deploy
            val dir = getTreebolicStorage(this)
            if (dir.isDirectory) {
                val dirContent = dir.list()
                if (dirContent == null || dirContent.isEmpty()) {
                    // deploy
                    expandZipAssetFile(this, "tests.zip")
                }
            }
        }
    }

    private val folder: String
        /**
         * Get initial folder
         *
         * @return initial folder
         */
        get() {
            val folder = getFolder(this, PREF_CURRENTFOLDER)
            if (folder != null) {
                return folder.path
            }
            return getTreebolicStorage(this).absolutePath
        }

    /**
     * Set folder to parent of given uri
     *
     * @param fileUri uri
     */
    private fun setFolder(fileUri: Uri) {
        val path = fileUri.path
        if (path != null) {
            val parentPath = File(path).parent
            setFolder(this, PREF_CURRENTFOLDER, parentPath)
        }
    }

    // S P I N N E R

    /**
     * Set spinner adapter
     */
    private fun setAdapter() {
        // spinner adapter
        adapter = makeAdapter(R.layout.spinner_item_providers, from, to)

        // set spinner adapter
        spinner.adapter = adapter

        // saved name
        val name = Settings.getStringPref(this@MainActivity, Settings.PREF_PROVIDER_NAME)

        // position
        if (name != null) {
            for (position in 0 until adapter!!.count) {
                @Suppress("UNCHECKED_CAST") val provider = adapter!!.getItem(position) as Provider
                if (name == provider[Providers.NAME]) {
                    spinner.setSelection(position)
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
    private fun makeAdapter(@Suppress("SameParameterValue") @LayoutRes itemLayoutRes: Int, @Suppress("SameParameterValue") from: Array<String>, @Suppress("SameParameterValue") to: IntArray): SimpleAdapter {
        // adapter
        val adapter = checkNotNull(Providers.makeAdapter(this, itemLayoutRes, from, to))

        adapter.setDropDownViewResource(R.layout.spinner_item_providers_dropdown)

        return adapter
    }

    // A C T I O N   B U T T O N

    /**
     * Update button visibility
     */
    private fun updateButton() {
        val button = findViewById<ImageButton>(R.id.treebolicButton)
        button.setOnClickListener(this)
        val sourceText = findViewById<TextView>(R.id.treebolicSource)
        val source = Settings.getStringPref(this, TreebolicIface.PREF_SOURCE)
        val qualifies = sourceQualifies(source)
        button.visibility = if (qualifies) View.VISIBLE else View.INVISIBLE
        sourceText.visibility = if (qualifies) View.VISIBLE else View.INVISIBLE
        if (qualifies) {
            sourceText.text = source
        }
    }

    /**
     * Whether source qualifies
     *
     * @return true if source qualifies
     */
    private fun sourceQualifies(source: String?): Boolean {
        return !source.isNullOrEmpty()
    }

    /**
     * Choose service
     */
    private fun tryStartOneOfTreebolicClients() {
        val services = getServices(this)

        val alert = AlertDialog.Builder(this)
        alert.setTitle(R.string.title_services)
        alert.setMessage(R.string.title_choose_service)

        val input = RadioGroup(this)
        if (services != null) {
            for (service in services) {
                val radioButton = RadioButton(this)
                radioButton.text = service[ServiceKeys.LABEL] as String?
                val drawableRef = service[ServiceKeys.DRAWABLE] as String?
                if (drawableRef != null) {
                    val fields = drawableRef.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val index = fields[1].toInt()
                    val drawable = loadIcon(packageManager, fields[0], index)
                    radioButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                }
                radioButton.compoundDrawablePadding = 10
                radioButton.tag = service
                input.addView(radioButton)
            }
        }
        alert.setView(input)
        alert.setNegativeButton(R.string.action_cancel) { _: DialogInterface?, _: Int -> }

        val dialog = alert.create()
        input.setOnCheckedChangeListener { _: RadioGroup?, _: Int ->
            dialog.dismiss()
            val childCount = input.childCount
            for (i in 0 until childCount) {
                val radioButton = input.getChildAt(i) as RadioButton
                if (radioButton.id == input.checkedRadioButtonId) {
                    @Suppress("UNCHECKED_CAST") val service = radioButton.tag as Service
                    tryStartTreebolicClient(service)
                }
            }
        }
        dialog.show()
    }

    // R E Q U E S T S ( S T A R T A C T I V I T Y F O R R E S U L T )

    /**
     * Request Treebolic source
     */
    private fun requestTreebolicSource() {
        val extensions = provider!![Providers.EXTENSIONS].toString()

        val intent = Intent(this, FileChooserActivity::class.java)
        intent.setType(provider!![Providers.MIMETYPE].toString())
        intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_INITIAL_DIR, provider!![Providers.BASE].toString())
        intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_EXTENSION_FILTER, extensions.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        activityFileResultLauncher!!.launch(intent) // MainActivity.REQUEST_FILE_CODE
    }

    /**
     * Request Treebolic bundle
     */
    private fun requestTreebolicBundle() {
        val intent = Intent(this, FileChooserActivity::class.java)
        intent.setType("application/zip")
        intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_INITIAL_DIR, provider!![Providers.BASE].toString())
        intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_EXTENSION_FILTER, arrayOf("zip", "jar"))
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        activityBundleResultLauncher!!.launch(intent) // MainActivity.REQUEST_BUNDLE_CODE
    }

    /**
     * Request Treebolic activity
     */
    private fun requestTreebolicSerialized() {
        val intent = Intent(this, FileChooserActivity::class.java)
        intent.setType("application/x-java-serialized-object")
        intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_INITIAL_DIR, folder)
        intent.putExtra(FileChooserActivity.ARG_FILECHOOSER_EXTENSION_FILTER, arrayOf("ser"))
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        activitySerializedResultLauncher!!.launch(intent)
    }

    // R E Q U E S T S ( S T A R T A C T I V I T Y )

    /**
     * Try to start Treebolic activity from source
     *
     * @param source0 source
     */
    private fun tryStartTreebolic(source0: String?) {
        tryStartTreebolicBuiltin(source0)
    }

    /**
     * Try to start Treebolic builtin provider activity
     *
     * @param source0 source
     */
    private fun tryStartTreebolicBuiltin(source0: String?) {
        val provider = provider!![Providers.PROVIDER].toString()
        if (provider.isEmpty()) {
            Toast.makeText(this, R.string.error_null_provider, Toast.LENGTH_SHORT).show()
            return
        }
        val source = source0 ?: Settings.getStringPref(this, TreebolicIface.PREF_SOURCE)
        if (source.isNullOrEmpty()) {
            Toast.makeText(this, R.string.error_null_source, Toast.LENGTH_SHORT).show()
            return
        }
        val base = Settings.getStringPref(this, TreebolicIface.PREF_BASE)
        val imageBase = Settings.getStringPref(this, TreebolicIface.PREF_IMAGEBASE)
        val settings = Settings.getStringPref(this, TreebolicIface.PREF_SETTINGS)

        val intent = TreebolicActivity.makeTreebolicIntent(this, provider, source, base, imageBase, settings, null)
        Log.d(TAG, "Start treebolic from provider:$provider source:$source")
        startActivity(intent)
    }

    /**
     * Try to start Treebolic activity from source file
     *
     * @param fileUri XML file uri
     */
    private fun tryStartTreebolic(fileUri: Uri) {
        if (provider == null) {
            return
        }
        val source = fileUri.toString()
        if (source.isEmpty()) {
            Toast.makeText(this, R.string.error_null_source, Toast.LENGTH_SHORT).show()
            return
        }
        val provider1 = provider!![Providers.PROVIDER].toString()
        if (provider1.isEmpty()) {
            Toast.makeText(this, R.string.error_null_provider, Toast.LENGTH_SHORT).show()
            return
        }
        val base = Settings.getStringPref(this, TreebolicIface.PREF_BASE)
        val imageBase = Settings.getStringPref(this, TreebolicIface.PREF_IMAGEBASE)
        val settings = Settings.getStringPref(this, TreebolicIface.PREF_SETTINGS)
        val style = provider!![Providers.STYLE].toString()

        val intent = TreebolicActivity.makeTreebolicIntent(this, provider1, source, base, imageBase, settings, style)
        Log.d(TAG, "Start treebolic from uri $fileUri")
        startActivity(intent)
    }

    /**
     * Try to start Treebolic activity from zipped bundle file
     *
     * @param archiveUri archive uri
     */
    private fun tryStartTreebolicBundle(archiveUri: Uri) {
        try {
            val path = archiveUri.path
            if (path != null) {
                // choose bundle entry
                choose(this, File(path)) { zipEntry -> tryStartTreebolicBundle(archiveUri, zipEntry) }
            }
        } catch (e: IOException) {
            Log.d(TAG, "Failed to start treebolic from bundle uri $archiveUri", e)
        }
    }

    /**
     * Try to start Treebolic activity from zip file
     *
     * @param archiveUri archive file uri
     * @param zipEntry   archive entry
     */
    private fun tryStartTreebolicBundle(archiveUri: Uri, zipEntry: String) {
        Log.d(TAG, "Start treebolic from bundle uri $archiveUri and zip entry $zipEntry")
        val source = zipEntry // alternatively: "jar:" + fileUri.toString() + "!/" + zipEntry
        val provider1 = provider!![Providers.PROVIDER].toString()
        if (provider1.isEmpty()) {
            Toast.makeText(this, R.string.error_null_provider, Toast.LENGTH_SHORT).show()
            return
        }

        val base = "jar:$archiveUri!/"
        val settings = Settings.getStringPref(this, TreebolicIface.PREF_SETTINGS)
        val style = provider!![Providers.STYLE].toString()

        val intent = TreebolicActivity.makeTreebolicIntent(this, provider1, source, base, base, settings, style)
        Log.d(TAG, "Start treebolic from bundle uri $archiveUri")
        startActivity(intent)
    }

    /**
     * Try to start Treebolic activity from zipped serialized model file
     *
     * @param archiveUri zipped serialized model file
     */
    private fun tryStartTreebolicSerialized(archiveUri: Uri?) {
        if (archiveUri == null) {
            Toast.makeText(this, R.string.error_null_source, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = TreebolicModelActivity.makeTreebolicSerializedIntent(this, archiveUri)
        Log.d(TAG, "Start treebolic from serialized uri $archiveUri")
        startActivity(intent)
    }

    /**
     * Try to start Treebolic default client activity
     */
    private fun tryStartTreebolicDefaultClient() {
        val intent = Intent()
        intent.setClass(this, TreebolicClientActivity::class.java)
        Log.d(TAG, "Start  treebolic default client")
        startActivity(intent)
    }

    /**
     * Try to start Treebolic client activity
     */
    private fun tryStartTreebolicClient(service: Service) {
        val argService = service[ServiceKeys.PACKAGE] as String? + '/' + service[ServiceKeys.NAME]

        val intent = Intent()
        intent.setClass(this, TreebolicClientActivity::class.java)
        intent.putExtra(TreebolicIface.ARG_SERVICE, argService)
        Log.d(TAG, "Start treebolic client for $argService")
        startActivity(intent)
    }

    /**
     * Try to start Treebolic settings activity
     */
    private fun tryStartTreebolicSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        if (provider != null) {
            intent.putExtra(SettingsActivity.ARG_PROVIDER_SELECTED, provider!![Providers.PROVIDER].toString())
        }
        startActivity(intent)
    }

    companion object {

        private const val TAG = "MainA"

        /**
         * State
         */
        private const val STATE_SELECTED_PROVIDER_ITEM = "org.treebolic.provider.selected"

        /**
         * Adapter - Key
         */
        private val from = arrayOf(Providers.ICON, Providers.NAME)

        /**
         * Adapter - ... mapped to res id
         */
        private val to = intArrayOf(R.id.icon, R.id.provider)

        /** Folder preference key*/
        private const val PREF_CURRENTFOLDER = "org.treebolic.folder"
    }
}
