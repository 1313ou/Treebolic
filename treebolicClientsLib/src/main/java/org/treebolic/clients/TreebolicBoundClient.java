package org.treebolic.clients;

import org.treebolic.clients.iface.IConnectionListener;
import org.treebolic.clients.iface.IModelListener;
import org.treebolic.clients.iface.ITreebolicClient;
import org.treebolic.services.iface.ITreebolicServiceBinder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * Treebolic bound client
 *
 * @author Bernard Bou
 */
public class TreebolicBoundClient implements ITreebolicClient
{
	/**
	 * Log tag
	 */
	static private final String TAG = "Treebolic Bound Client"; //$NON-NLS-1$

	/**
	 * Abstract: Service package
	 */
	protected final String servicePackage;

	/**
	 * Abstract: Service name
	 */
	protected final String serviceName;

	/**
	 * Context
	 */
	private final Context context;

	/**
	 * Connection listener
	 */
	private final IConnectionListener connectionListener;

	/**
	 * Model listener
	 */
	private final IModelListener modelListener;

	/**
	 * Connection
	 */
	private ServiceConnection connection;

	/**
	 * Bind state
	 */
	private boolean isBound = false;

	/**
	 * Binder
	 */
	private ITreebolicServiceBinder binder;

	/**
	 * Constructor
	 *
	 * @param context0
	 *            context
	 * @param service0
	 *            service full name (pkg/class)
	 * @param connectionListener0
	 *            connection listener
	 * @param modelListener0
	 *            model listener
	 */
	public TreebolicBoundClient(final Context context0, final String service0, final IConnectionListener connectionListener0,
			final IModelListener modelListener0)
	{
		this.context = context0;
		this.modelListener = modelListener0;
		this.connectionListener = connectionListener0;
		final String[] serviceNameComponents = service0.split("/"); //$NON-NLS-1$
		this.servicePackage = serviceNameComponents[0];
		this.serviceName = serviceNameComponents[1];
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.treebolic.clients.iface.ITreebolicClient#connect()
	 */
	@Override
	public void connect()
	{
		bind();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.treebolic.clients.iface.ITreebolicClient#disconnect()
	 */
	@Override
	public void disconnect()
	{
		if (this.isBound)
		{
			Log.d(TreebolicBoundClient.TAG, "Service disconnected"); //$NON-NLS-1$
			// Toast.makeText(this.context, R.string.disconnected, Toast.LENGTH_SHORT).show();

			// detach our existing connection.
			this.context.unbindService(this.connection);
			this.isBound = false;
		}
	}

	/**
	 * Bind client to service
	 */
	private void bind()
	{
		this.connection = new ServiceConnection()
		{
			@SuppressWarnings("synthetic-access")
			@Override
			public void onServiceConnected(final ComponentName name, final IBinder binder0)
			{
				Log.d(TreebolicBoundClient.TAG, "Service connected"); //$NON-NLS-1$
				TreebolicBoundClient.this.isBound = true;
				TreebolicBoundClient.this.binder = (ITreebolicServiceBinder) binder0;

				// signal connected
				TreebolicBoundClient.this.connectionListener.onConnected(true);
			}

			/*
			 * (non-Javadoc)
			 *
			 * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
			 */
			@SuppressWarnings("synthetic-access")
			@Override
			public void onServiceDisconnected(final ComponentName name)
			{
				TreebolicBoundClient.this.binder = null;
			}
		};

		final Intent intent = new Intent();
		intent.setComponent(new ComponentName(this.servicePackage, this.serviceName));
		if (!this.context.bindService(intent, this.connection, Context.BIND_AUTO_CREATE))
		{
			Log.e(TreebolicBoundClient.TAG, "Service failed to bind"); //$NON-NLS-1$
			Toast.makeText(this.context, R.string.fail_bind, Toast.LENGTH_LONG).show();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.treebolic.clients.iface.ITreebolicClient#requestModel(java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 * android.content.Intent)
	 */
	@Override
	public void requestModel(final String source, final String base, final String imageBase, final String settings, final Intent forward)
	{
		if (forward == null)
		{
			this.binder.makeModel(source, base, imageBase, settings, this.modelListener);
		}
		else
		{
			this.binder.makeModel(source, base, imageBase, settings, forward);
		}
	}
}
