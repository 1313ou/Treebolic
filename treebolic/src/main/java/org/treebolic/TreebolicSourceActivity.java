package org.treebolic;

import android.content.Intent;
import android.os.Bundle;

import java.util.Properties;

public abstract class TreebolicSourceActivity extends TreebolicBasicActivity
{
	/**
	 * Parameter : source (interpreted by provider)
	 */
	@SuppressWarnings("WeakerAccess")
	protected String source;

	/**
	 * Parameter : data provider
	 */
	@SuppressWarnings("WeakerAccess")
	protected String providerName;

	/**
	 * Restoring
	 */
	private boolean restoring;

	// C O N S T R U C T O R

	public TreebolicSourceActivity(int menuId0)
	{
		super(menuId0);
	}

	// L I F E C Y C L E

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// restoring status
		this.restoring = savedInstanceState != null;
	}

	@Override
	public void onRestoreInstanceState(final Bundle savedInstanceState)
	{
		// always call the superclass so it can restore the view hierarchy
		super.onRestoreInstanceState(savedInstanceState);

		// restore
		this.source = savedInstanceState.getString(TreebolicIface.ARG_SOURCE);
	}

	@Override
	public void onSaveInstanceState(final Bundle savedInstanceState)
	{
		// save
		savedInstanceState.putString(TreebolicIface.ARG_SOURCE, this.source);

		// always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	// T R E E B O L I C C O N T E X T

	@Override
	protected Properties makeParameters()
	{
		final Properties theseParameters = super.makeParameters();

		if (this.source != null)
		{
			theseParameters.setProperty("source", this.source);
			theseParameters.setProperty("doc", this.source);
		}
		if (this.providerName != null)
		{
			theseParameters.setProperty("provider", this.providerName);
		}
		return theseParameters;
	}

	// U N M A R S H A L

	/**
	 * Unmarshal parameters from intent
	 *
	 * @param intent intent
	 */
	@Override
	protected void unmarshalArgs(final Intent intent)
	{
		final Bundle params = intent.getExtras();
		this.providerName = params.getString(TreebolicIface.ARG_PROVIDER);
		if (!this.restoring)
		{
			this.source = params.getString(TreebolicIface.ARG_SOURCE);
		}

		// super
		super.unmarshalArgs(intent);
	}
}
