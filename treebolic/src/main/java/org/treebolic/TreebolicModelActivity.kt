/*
 * Copyright (c) 2023. Bernard Bou
 */
package org.treebolic

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import org.treebolic.Models.get
import treebolic.model.Model
import treebolic.model.ModelDump
import treebolic.model.ModelReader
import java.io.IOException

/**
 * Treebolic model-rendering activity (display server).
 * This activity receives a model in its intent and visualizes it.
 *
 * @author Bernard Bou
 */
class TreebolicModelActivity : TreebolicBasicActivity(R.menu.treebolic) {

    /**
     * Parameter : Model
     */
    private var model: Model? = null

    /**
     * Parameter : serialized model uri
     */
    private var serializedModel: Uri? = null

    // U N M A R S H A L

    /**
     * Unmarshal model and parameters from intent
     *
     * @param intent intent
     */
    override fun unmarshalArgs(intent: Intent) {
        // retrieve arguments
        val params = checkNotNull(intent.extras)
        params.classLoader = classLoader

        // retrieve model
        val key = params.getLong(TreebolicIface.ARG_MODEL_REFERENCE, -1L)
        if (key != -1L) {
            try {
                this.model = get(key)
            } catch (ignored: NoSuchElementException) {
                this.model = null
            }
        } else {
            val isSerialized = params.getBoolean(TreebolicIface.ARG_SERIALIZED)
            if (isSerialized) {
                @Suppress("DEPRECATION")
                this.model = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    params.getSerializable(TreebolicIface.ARG_MODEL, Model::class.java) else
                    params.getSerializable(TreebolicIface.ARG_MODEL) as Model?
            } else {
                @Suppress("DEPRECATION")
                val parcelModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    params.getParcelable(TreebolicIface.ARG_MODEL, ParcelableModel::class.java) else
                    params.getParcelable(TreebolicIface.ARG_MODEL)
                if (parcelModel != null) {
                    this.model = parcelModel.model
                }
            }
        }
        Log.d(
            TAG, "Unmarshalled Model" + (if (BuildConfig.DEBUG) "\n${ModelDump.toString(model)}\n".trimIndent() else ' '.toString() + (if (model == null) "null" else model.toString()))
        )

        // retrieve other parameters
        @Suppress("DEPRECATION")
        this.serializedModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) //
            params.getParcelable(TreebolicIface.ARG_SERIALIZED_MODEL_URI, Uri::class.java) else  //
            params.getParcelable(TreebolicIface.ARG_SERIALIZED_MODEL_URI)

        // super
        super.unmarshalArgs(intent)
    }

    // Q U E R Y

    override fun query() {
        // sanity check
        if (this.model == null && this.serializedModel == null) {
            Toast.makeText(this, R.string.error_null_model, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // query
        // init widget with model
        if (this.serializedModel != null) {
            Log.d(TAG, "Using serialized model")
            val model = deserializeGuarded(ModelReader(serializedModel!!.path))
            widget!!.init(model)
        } else {
            widget!!.init(this.model)
        }
    }

    override fun requery(source: String?) {
        if (this.parentActivityIntentArg != null) {
            Log.d(TAG, "Requesting model from $source")
            try {
                parentActivityIntentArg!!.putExtra(TreebolicIface.ARG_SOURCE, source)
                startActivity(this.parentActivityIntentArg)
            } catch (ignored: Exception) {
                Toast.makeText(this, R.string.error_query, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Deserialize model
     *
     * @param reader model reader
     * @return model
     */
    private fun deserializeGuarded(reader: ModelReader): Model? {
        try {
            return reader.deserialize()
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "Class not found while deserializing", e)
            Toast.makeText(this@TreebolicModelActivity, R.string.error_deserialize, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.d(TAG, "IOException while deserializing", e)
            Toast.makeText(this@TreebolicModelActivity, R.string.error_deserialize, Toast.LENGTH_SHORT).show()
        }
        return null
    }

    companion object {

        private const val TAG = "TreebolicModelA"

        // I N T E N T

        /**
         * Make Treebolic serialized model activity intent
         *
         * @param context    context
         * @param serialized serialized model uti
         * @return intent
         */
        fun makeTreebolicSerializedIntent(context: Context?, serialized: Uri?): Intent {
            val intent = Intent(context, TreebolicModelActivity::class.java)
            intent.putExtra(TreebolicIface.ARG_SERIALIZED_MODEL_URI, serialized)
            return intent
        }
    }
}
