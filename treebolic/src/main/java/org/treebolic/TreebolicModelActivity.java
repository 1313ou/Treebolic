/*
 * Copyright (c) 2023. Bernard Bou
 */

package org.treebolic;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.NoSuchElementException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import treebolic.model.Model;
import treebolic.model.ModelDump;
import treebolic.model.ModelReader;

/**
 * Treebolic model-rendering activity (display server).
 * This activity receives a model in its intent and visualizes it.
 *
 * @author Bernard Bou
 */
public class TreebolicModelActivity extends TreebolicBasicActivity
{
	/**
	 * Log tag
	 */
	private static final String TAG = "TreebolicModelA";

	/**
	 * Parameter : Model
	 */
	@Nullable
	private Model model;

	/**
	 * Parameter : serialized model uri
	 */
	@Nullable
	private Uri serializedModel;

	// C O N S T R U C T O R

	public TreebolicModelActivity()
	{
		super(R.menu.treebolic);
	}

	// U N M A R S H A L

	/**
	 * Unmarshal model and parameters from intent
	 *
	 * @param intent intent
	 */
	@Override
	@SuppressWarnings("boxing")
	protected void unmarshalArgs(@NonNull final Intent intent)
	{
		// retrieve arguments
		final Bundle params = intent.getExtras();
		assert params != null;
		params.setClassLoader(getClassLoader());

		// retrieve model
		final long key = params.getLong(TreebolicIface.ARG_MODEL_REFERENCE, -1L);
		if (key != -1L)
		{
			try
			{
				this.model = Models.get(key);
			}
			catch (@NonNull final NoSuchElementException ignored)
			{
				this.model = null;
			}
		}
		else
		{
			final boolean isSerialized = params.getBoolean(TreebolicIface.ARG_SERIALIZED);
			if (isSerialized)
			{
				this.model = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? //
						params.getSerializable(TreebolicIface.ARG_MODEL, Model.class) : //
						(Model) params.getSerializable(TreebolicIface.ARG_MODEL);
			}
			else
			{
				final ParcelableModel parcelModel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? //
						params.getParcelable(TreebolicIface.ARG_MODEL, ParcelableModel.class) : //
						params.getParcelable(TreebolicIface.ARG_MODEL);
				if (parcelModel != null)
				{
					this.model = parcelModel.getModel();
				}
			}
		}
		Log.d(TAG, "Unmarshalled Model" + (BuildConfig.DEBUG ? '\n' + ModelDump.toString(model) : ' ' + (model == null ? "null" : model.toString())));

		// retrieve other parameters
		this.serializedModel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? //
				params.getParcelable(TreebolicIface.ARG_SERIALIZED_MODEL_URI, Uri.class) : //
				params.getParcelable(TreebolicIface.ARG_SERIALIZED_MODEL_URI);

		// super
		super.unmarshalArgs(intent);
	}

	// Q U E R Y

	@Override
	protected void query()
	{
		// sanity check
		if (this.model == null && this.serializedModel == null)
		{
			Toast.makeText(this, R.string.error_null_model, Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		// query
		// init widget with model
		if (this.serializedModel != null)
		{
			Log.d(TAG, "Using serialized model");
			final Model model = deserializeGuarded(new ModelReader(this.serializedModel.getPath()));
			this.widget.init(model);
		}
		else
		{
			this.widget.init(this.model);
		}
	}

	@Override
	protected void requery(final String source)
	{
		if (this.parentActivity != null)
		{
			Log.d(TAG, "Requesting model from " + source);
			try
			{
				this.parentActivity.putExtra(TreebolicIface.ARG_SOURCE, source);
				startActivity(this.parentActivity);
			}
			catch (@NonNull final Exception ignored)
			{
				Toast.makeText(this, R.string.error_query, Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * Deserialize model
	 *
	 * @param reader model reader
	 * @return model
	 */
	@Nullable
	private Model deserializeGuarded(@NonNull final ModelReader reader)
	{
		try
		{
			return reader.deserialize();
		}
		catch (@NonNull final ClassNotFoundException e)
		{
			Log.d(TAG, "Class not found while deserializing", e);
			Toast.makeText(TreebolicModelActivity.this, R.string.error_deserialize, Toast.LENGTH_SHORT).show();
		}
		catch (@NonNull final IOException e)
		{
			Log.d(TAG, "IOException while deserializing", e);
			Toast.makeText(TreebolicModelActivity.this, R.string.error_deserialize, Toast.LENGTH_SHORT).show();
		}
		return null;
	}

	// I N T E N T

	/**
	 * Make Treebolic serialized model activity intent
	 *
	 * @param context    context
	 * @param serialized serialized model uti
	 * @return intent
	 */
	@NonNull
	static public Intent makeTreebolicSerializedIntent(final Context context, final Uri serialized)
	{
		final Intent intent = new Intent(context, TreebolicModelActivity.class);
		intent.putExtra(TreebolicIface.ARG_SERIALIZED_MODEL_URI, serialized);
		return intent;
	}
}
