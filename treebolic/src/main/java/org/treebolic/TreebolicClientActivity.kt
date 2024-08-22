/*
 * Copyright (c) 2023. Bernard Bou
 */
package org.treebolic

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import org.treebolic.Settings.getStringPref
import org.treebolic.Settings.getURLPref
import org.treebolic.clients.TreebolicAIDLBoundClient
import org.treebolic.clients.TreebolicBoundClient
import org.treebolic.clients.TreebolicBroadcastClient
import org.treebolic.clients.TreebolicClientActivityStub
import org.treebolic.clients.TreebolicMessengerClient
import org.treebolic.clients.iface.ITreebolicClient
import org.treebolic.guide.AboutActivity
import org.treebolic.guide.HelpActivity
import org.treebolic.guide.Tip
import org.treebolic.search.SearchSettings
import org.treebolic.services.iface.ITreebolicService
import treebolic.IContext
import treebolic.Widget
import treebolic.glue.component.Surface
import treebolic.glue.component.Utils
import treebolic.model.Model
import treebolic.model.ModelDump
import java.net.URL
import java.util.Properties

/**
 * Treebolic client activity (requests model from server) and dispatches returned model to display.
 * May instruct server to forward model directly to rendering activity.
 *
 * @author Bernard Bou
 */
class TreebolicClientActivity : TreebolicClientActivityStub(), IContext {

    /**
     * Client
     */
    private var argService: String? = null

    /**
     * Parameters
     */
    private var parameters: Properties? = null

    /**
     * Treebolic widget
     */
    private var widget: Widget? = null

    /**
     * Search view on action bar
     */
    private var searchView: SearchView? = null

    /**
     * Client status indicator
     */
    private var clientStatusMenuItem: MenuItem? = null

    // L I F E C Y C L E

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // widget
        widget = Widget(this, this)

        // content view
        setContentView(R.layout.activity_treebolic_client)
        val container = findViewById<ViewGroup>(R.id.container)
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        val view: View = widget as View
        container.addView(view, params)

        // toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        // action bar
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.elevation = 0f
            actionBar.displayOptions = ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP
        }

        // floating action button
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.visibility = View.GONE
        fab.setOnClickListener {
            fab.visibility = View.GONE
            handleQuery()
        }

        // init widget with model is asynchronous
    }

    override fun onPause() {
        Log.d(TAG, "Activity paused, terminating surface drawing thread")

        // terminate thread
        val surface: Surface? = widget?.view
        surface?.thread?.terminate()

        // super
        super.onPause()
    }

    // M E N U

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // menu
        menuInflater.inflate(R.menu.treebolic_client, menu)

        // client status
        clientStatusMenuItem = menu.findItem(R.id.action_client_status)
        clientStatusMenuItem!!.setOnMenuItemClickListener {
            Toast.makeText(this@TreebolicClientActivity, if (clientStatus) R.string.client_up else R.string.client_down, Toast.LENGTH_SHORT).show()
            true
        }
        updateClientStatus(clientStatus)

        // search view
        val searchMenuItem = menu.findItem(R.id.action_search)
        searchMenuItem.expandActionView()
        searchView = searchMenuItem.actionView as SearchView?

        // search view width
        val screenWidth = Utils.screenWidth(this)
        searchView!!.maxWidth = screenWidth / 2

        // search view listener
        searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchView!!.clearFocus()
                searchView!!.setQuery("", false)
                handleQueryChanged(query, true)
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                handleQueryChanged(query, false)
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (R.id.action_treebolic_client_toggle == id) {
            if (clientStatus) {
                stop()
            } else {
                start()
            }
            return true
        } else if (R.id.action_settings == id) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            return true
        } else if (R.id.action_settings_service == id) {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra(AppCompatCommonPreferenceActivity.INITIAL_ARG, SettingsActivity.ServicePreferenceFragment::class.java.name)
            startActivity(intent)
            return true
        } else if (R.id.action_finish == id) {
            finish()
            return true
        } else if (R.id.action_kill == id) {
            Process.killProcess(Process.myPid())
            return true
        } else if (R.id.action_tips == id) {
            Tip.show(supportFragmentManager)
            return true
        } else if (R.id.action_help == id) {
            startActivity(Intent(this, HelpActivity::class.java))
            return true
        } else if (R.id.action_about == id) {
            startActivity(Intent(this, AboutActivity::class.java))
            return true
        } else if (R.id.action_search_run == id) {
            handleSearchRun()
            return true
        } else if (R.id.action_search_reset == id) {
            handleSearchReset()
            return true
        } else if (R.id.action_search_settings == id) {
            SearchSettings.show(supportFragmentManager)
            return true
        } else {
            return false
        }
    }

    // T R E E B O L I C   C O N T E X T

    override fun getBase(): URL? {
        return getURLPref(this, TreebolicIface.PREF_BASE)
    }

    override fun getImagesBase(): URL? {
        return getURLPref(this, TreebolicIface.PREF_IMAGEBASE)
    }

    override fun getParameters(): Properties {
        if (parameters == null) {
            parameters = makeParameters()
        }
        return parameters!!
    }

    override fun getStyle(): String {
        return Settings.STYLE_DEFAULT
    }

    override fun linkTo(url: String, target: String?): Boolean {
        // if we handle url, initiate another query/response cycle
        if (urlScheme != null && url.startsWith(urlScheme!!)) {
            val source2 = url.substring(urlScheme!!.length)
            query(source2)
            return true
        }

        // standard handling
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.parse(url)
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            val mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            intent.setDataAndType(uri, mimetype)
            startActivity(intent)
            return true
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_link, Toast.LENGTH_LONG).show()
            Log.w(TAG, "Error following link '" + url + "' " + e.message)
        }
        return false
    }

    override fun getInput(): String {
        return searchView!!.query.toString()
    }

    override fun warn(message: String) {
        // toast(message, Toast.LENGTH_LONG)
        snackbar(message, Snackbar.LENGTH_LONG)
    }

    override fun status(message: String) {
        // toast(message, Toast.LENGTH_SHORT)
        snackbar(message, Snackbar.LENGTH_SHORT)
    }

    /**
     * Make parameters from bundle
     *
     * @return properties
     */
    private fun makeParameters(): Properties {
        val parameters = Properties()
        parameters.setProperty("base", getStringPref(this, TreebolicIface.PREF_BASE))
        parameters.setProperty("imagebase", getStringPref(this, TreebolicIface.PREF_IMAGEBASE))
        parameters.setProperty("settings", getStringPref(this, TreebolicIface.PREF_SETTINGS))
        return parameters
    }

    // U N M A R S H A L

    private fun unmarshalArgs(intent: Intent) {
        argService = intent.getStringExtra(TreebolicIface.ARG_SERVICE)
    }

    // C L I E N T

    override fun makeClient(): ITreebolicClient? {
        if (argService == null || argService!!.isEmpty()) {
            // default
            argService = getStringPref(this, Settings.PREF_SERVICE)
        }

        return service2Client(argService)
    }

    /**
     * Make client from service name
     *
     * @param service name
     * @return client to service
     */
    private fun service2Client(service: String?): ITreebolicClient? {
        if (!service.isNullOrEmpty()) {
            if (service.contains(ITreebolicService.TYPE_BROADCAST)) {
                Log.d(TAG, "Making treebolic client to broadcast service$service")
                return TreebolicBroadcastClient(this, service, this, this)
            } else if (service.contains(ITreebolicService.TYPE_AIDL_BOUND)) {
                Log.d(TAG, "Making treebolic client to AIDL bound service $service")
                return TreebolicAIDLBoundClient(this, service, this, this)
            } else if (service.contains(ITreebolicService.TYPE_BOUND)) {
                Log.d(TAG, "Making treebolic client to bound service $service")
                return TreebolicBoundClient(this, service, this, this)
            } else if (service.contains(ITreebolicService.TYPE_MESSENGER)) {
                Log.d(TAG, "Making treebolic client to messenger service $service")
                return TreebolicMessengerClient(this, service, this, this)
            }
        }
        Log.d(TAG, "Null service")
        return null
    }

    // M O D E L

    /**
     * Query model from source
     *
     * @param source source
     */
    private fun query(source: String?) {
        Log.d(TAG, "Query $source")
        if (client == null) {
            Log.d(TAG, "Null client")
            return
        }
        Log.d(TAG, "Requesting model for source $source")
        client!!.requestModel(source!!, null, null, null, null)
    }

    override fun onModel(model: Model?, urlScheme: String?) {
        Log.d(TAG, "Receiving model" + (if (BuildConfig.DEBUG) "\n${ModelDump.toString(model)}\n" else ' '.toString() + (model?.toString() ?: "null")))

        // abort
        if (model == null) {
            Toast.makeText(this, R.string.error_null_model, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // init widget with model
        this.urlScheme = urlScheme
        widget!!.init(model)
    }

    /**
     * Search pending flag
     */
    private var searchPending = false

    /**
     * SearchView query change listener
     *
     * @param query  new query
     * @param submit whether submit was changed
     */
    private fun handleQueryChanged(query: String, submit: Boolean) {
        // clear keyboard out of the way
        if (submit) {
            closeKeyboard()
        }

        // reset current search if any
        widget!!.search(CMD_RESET)

        if (submit /*|| query.length() > SEARCH_TRIGGER_LEVEL*/) {
            // query applies to source: search is a requery
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
            val scope = sharedPrefs.getString(SearchSettings.PREF_SEARCH_SCOPE, SearchSettings.SCOPE_LABEL) // label, content, link, id
            if (SearchSettings.SCOPE_SOURCE == scope) {
                Log.d(TAG, "Source \"$query\"")
                query(query)
                return
            }

            // query applies to non-source scope (label, content, ..): tree search
            val mode = sharedPrefs.getString(SearchSettings.PREF_SEARCH_MODE, SearchSettings.MODE_STARTSWITH) // equals, startswith, includes
            runSearch(scope, mode, query)
        }
    }

    /**
     * Tree search handler
     */
    private fun handleSearchRun() {
        // clear keyboard out of the way
        closeKeyboard()

        // new or continued search
        if (!searchPending) {
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
            val query = searchView!!.query.toString()
            val scope = sharedPrefs.getString(SearchSettings.PREF_SEARCH_SCOPE, SearchSettings.SCOPE_LABEL) // label, content, link, id
            if (SearchSettings.SCOPE_SOURCE == scope) {
                Log.d(TAG, "Source \"$query\"")
                query(query)
                return
            }

            val mode = sharedPrefs.getString(SearchSettings.PREF_SEARCH_MODE, SearchSettings.MODE_STARTSWITH) // equals, startswith, includes
            runSearch(scope, mode, query)
        } else {
            continueSearch()
        }
    }

    /**
     * Tree query handler
     */
    private fun handleQuery() {
        // clear keyboard out of the way
        closeKeyboard()

        // new or continued search
        if (!searchPending) {
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
            val query = sharedPrefs.getString(Settings.PREF_SERVICE_SOURCE, "")
            Log.d(TAG, "Source \"$query\"")
            query(query)
        }
    }

    /**
     * Tree search reset handler
     */
    private fun handleSearchReset() {
        // clear keyboard out of the way
        closeKeyboard()

        // clear current query
        searchView!!.setQuery("", false)

        resetSearch()
    }

    private fun closeKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = checkNotNull(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // S E A R C H   I N T E R F A C E

    private fun runSearch(scope: String?, mode: String?, target: String) {
        Log.d(TAG, "Search run$scope $mode $target")
        searchPending = true
        widget!!.search(CMD_SEARCH, scope, mode, target)
    }

    private fun continueSearch() {
        Log.d(TAG, "Search continue")
        widget!!.search(CMD_CONTINUE)
    }

    private fun resetSearch() {
        Log.d(TAG, "Search reset")
        searchPending = false
        widget!!.search(CMD_RESET)
    }

    // C O N N E C T I O N L I S T E N E R

    override fun onConnected(flag: Boolean) {
        updateClientStatus(flag)
        super.onConnected(flag)
    }

    // S T A T U S

    private fun updateClientStatus(flag: Boolean) {
        // snackbar
        val fields = if (argService == null) null else argService!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val message = getString(if (flag) R.string.status_client_connected else R.string.error_client_not_connected) + ' ' + (fields?.get(1) ?: "")
        if (flag) {
            snackbar(message, Snackbar.LENGTH_LONG)
        } else {
            stickySnackbar(message)
        }

        // status icon
        clientStatusMenuItem?.setIcon(if (flag) R.drawable.ic_status_up else R.drawable.ic_status_down)

        // fab
        if (flag) {
            val fab = findViewById<FloatingActionButton>(R.id.fab)
            fab.visibility = View.VISIBLE
        }
    }

    /**
     * Put toast on UI thread
     *
     * @param message  message
     * @param duration duration
     */
    private fun toast(message: String, duration: Int) {
        runOnUiThread { Toast.makeText(this@TreebolicClientActivity, message, duration).show() }
    }

    /**
     * Put snackbar on UI thread
     *
     * @param message  message
     * @param duration duration
     */
    private fun snackbar(message: String, duration: Int) {
        runOnUiThread {
            val snack: Snackbar = Snackbar.make(widget as View, message, duration)
            val view = snack.view
            view.setBackgroundColor(ContextCompat.getColor(this@TreebolicClientActivity, R.color.snackbar_color))
            snack.show()
        }
    }

    /**
     * Put sticky snackbar on UI thread
     *
     * @param message message
     */
    private fun stickySnackbar(message: String) {
        runOnUiThread {
            val snack: Snackbar = Snackbar.make(widget as View, message, Snackbar.LENGTH_INDEFINITE)
            snack.setAction(android.R.string.ok) { snack.dismiss() }
            snack.show()
        }
    }

    companion object {

        private const val TAG = "TreebolicClientA"

        // S E A R C H

        private const val CMD_SEARCH = "SEARCH"

        private const val CMD_RESET = "RESET"

        private const val CMD_CONTINUE = "CONTINUE"

        // H E L P E R S

        /**
         * Initialize search preferences
         *
         * @param context context
         */
        fun initializeSearchPrefs(context: Context) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sharedPref.edit()
            editor.putString(SearchSettings.PREF_SEARCH_SCOPE, SearchSettings.SCOPE_SOURCE)
            editor.putString(SearchSettings.PREF_SEARCH_MODE, SearchSettings.MODE_IS)
            editor.apply()
        }
    }
}
