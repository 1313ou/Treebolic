/*
 * Copyright (c) 2023. Bernard Bou
 */
package org.treebolic

import android.os.Bundle
import android.widget.BaseAdapter
import android.widget.ListView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import org.treebolic.Providers.makeAdapter

/**
 * Activity listing providers
 *
 * @author Bernard Bou
 */
class ProvidersActivity : AppCompatCommonActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // layout
        setContentView(R.layout.activity_providers)

        // toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // set up the action bar
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.displayOptions = ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP
        }

        // adapter
        val adapter: BaseAdapter? = makeAdapter(this, R.layout.item_providers, from, to)
        val listView = findViewById<ListView>(R.id.providers)
        listView.adapter = adapter
        //listView.setDivider(AppCompatResources.getDrawable(this, R.drawable.divider))
    }

    companion object {

        // Adapter
        // key-resid mapping
        private val from = arrayOf(
            Providers.ICON,
            Providers.NAME,
            Providers.PROVIDER,
            Providers.DESCRIPTION,
            Providers.PACKAGE,
            Providers.PROCESS,
            Providers.MIMETYPE,
            Providers.EXTENSIONS,
            Providers.URLSCHEME,
            Providers.SOURCE,
            Providers.BASE,
            Providers.IMAGEBASE,
            Providers.SETTINGS
        )
        private val to = intArrayOf(R.id.icon, R.id.name, R.id.provider, R.id.description, R.id.pkg, R.id.process, R.id.mimetype, R.id.extension, R.id.urlScheme, R.id.source, R.id.base, R.id.imagebase, R.id.settings)
    }
}
