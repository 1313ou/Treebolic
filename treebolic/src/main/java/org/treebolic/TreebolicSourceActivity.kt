/*
 * Copyright (c) 2023. Bernard Bou
 */
package org.treebolic

import android.content.Intent
import android.os.Bundle
import androidx.annotation.MenuRes
import java.util.Properties

abstract class TreebolicSourceActivity(@MenuRes menuRes: Int) : TreebolicBasicActivity(menuRes) {

    /**
     * Parameter : source (interpreted by provider)
     */
    @JvmField
    protected var source: String? = null

    /**
     * Parameter : data provider
     */
    @JvmField
    protected var providerName: String? = null

    /**
     * Restoring
     */
    private var restoring = false

    // L I F E C Y C L E

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // restoring status
        restoring = savedInstanceState != null
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState)

        // restore
        source = savedInstanceState.getString(TreebolicIface.ARG_SOURCE)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        // save
        outState.putString(TreebolicIface.ARG_SOURCE, source)

        // always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState)
    }

    // T R E E B O L I C   C O N T E X T

    override fun makeParameters(): Properties? {
        val parameters = super.makeParameters()

        if (source != null) {
            parameters!!.setProperty("source", source)
            parameters.setProperty("doc", source)
        }
        if (providerName != null) {
            parameters!!.setProperty("provider", providerName)
        }
        return parameters
    }

    // U N M A R S H A L

    /**
     * Unmarshal parameters from intent
     *
     * @param intent intent
     */
    override fun unmarshalArgs(intent: Intent) {
        val args = intent.extras!!
        providerName = args.getString(TreebolicIface.ARG_PROVIDER)
        if (!restoring) {
            source = args.getString(TreebolicIface.ARG_SOURCE)
        }

        // super
        super.unmarshalArgs(intent)
    }
}
