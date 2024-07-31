/*
 * Copyright (c) 2023. Bernard Bou
 */
package org.treebolic

import android.os.Bundle
import android.widget.BaseAdapter
import android.widget.ListView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import org.treebolic.Services.makeAdapter

/**
 * Activity listing services
 *
 * @author Bernard Bou
 */
class ServicesActivity : AppCompatCommonActivity() {

    // adapter
    // key-resid mapping
    private val from = arrayOf(ServiceKeys.DRAWABLE, ServiceKeys.NAME, ServiceKeys.PACKAGE, ServiceKeys.PROCESS, ServiceKeys.ENABLED, ServiceKeys.EXPORTED, ServiceKeys.PERMISSION, ServiceKeys.FLAGS, ServiceKeys.LABEL, ServiceKeys.DESCRIPTION)
    private val to = intArrayOf(R.id.icon, R.id.service, R.id.pkg, R.id.process, R.id.enabled, R.id.exported, R.id.permission, R.id.flags, R.id.label, R.id.description)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // layout
        setContentView(R.layout.activity_services)

        // toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // set up the action bar
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.displayOptions = ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP
        }

        // adapter
        val adapter: BaseAdapter? = makeAdapter(this, R.layout.item_services, from, to)
        val listView = findViewById<ListView>(R.id.services)
        listView.adapter = adapter

        // view
        //listView.setDivider(AppCompatResources.getDrawable(this, R.drawable.divider))
    }
}
