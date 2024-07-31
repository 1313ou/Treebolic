/*
 * Copyright (c) 2023. Bernard Bou
 */
package org.treebolic

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import org.treebolic.search.SearchSettings
import treebolic.IWidget

/**
 * Constructor
 *
 * @param context context
 * @param widget  widget
 */
class Searcher(
    private val context: Context,
    private val widget: IWidget
) {

    /**
     * Search pending flag
     */
    private var searchPending = false

    /**
     * Search
     */
    private fun search(target: String): Boolean {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.context)

        val scope = sharedPrefs.getString(SearchSettings.PREF_SEARCH_SCOPE, SearchSettings.SCOPE_LABEL) // label, content, link, id
        val mode = sharedPrefs.getString(SearchSettings.PREF_SEARCH_MODE, SearchSettings.MODE_STARTSWITH) // equals, startswith, includes

        Log.d(TAG, "Search for $scope $mode \"$target\"")
        if ("source" == scope) {
            return false
        }

        if (!this.searchPending) {
            runSearch(scope, mode, target)
        } else {
            continueSearch()
        }
        return true
    }

    private fun runSearch(scope: String?, mode: String?, target: String?) {
        if (target.isNullOrEmpty()) {
            return
        }

        Log.d(TAG, "Search run$scope $mode $target")
        this.searchPending = true
        widget.search(CMD_SEARCH, scope, mode, target)
    }

    private fun continueSearch() {
        Log.d(TAG, "Search continue")
        widget.search(CMD_CONTINUE)
    }

    private fun resetSearch() {
        Log.d(TAG, "Search reset")
        this.searchPending = false
        widget.search(CMD_RESET)
    }

    companion object {

        private const val TAG = "Searcher"

        private const val CMD_SEARCH = "SEARCH"

        private const val CMD_RESET = "RESET"

        private const val CMD_CONTINUE = "CONTINUE"
    }
}
