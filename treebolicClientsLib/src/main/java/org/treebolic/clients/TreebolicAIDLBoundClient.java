package org.treebolic.clients;

import org.treebolic.ParcelableModel;
import org.treebolic.clients.iface.IConnectionListener;
import org.treebolic.clients.iface.IModelListener;
import org.treebolic.clients.iface.ITreebolicClient;
import org.treebolic.services.iface.ITreebolicAIDLService;
import org.treebolic.services.iface.ITreebolicService;

import treebolic.model.Model;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

/**
 * Treebolic bound client
 *
 * @author Bernard Bou
 */
public class TreebolicAIDLBoundClient implements ITreebolicClient
{
	/**
	 * Log tag
	 */
	static private final String TAG = "Treebolic AIDL Bound Client"; //$NON-NLS-1$

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
	private ITreebolicAIDLService binder;

	/**
	 * Result receiver
	 */
	private final ResultReceiver receiver;

	/**
	 * Constructor
	 *
	 * @param context0
	 *            context
	 * @param service0
	 *            service full name (pkg/class)
	 * @param connectionListener0
	 *            connectionListener
	 * @param modelListener0
	 *            modelListener
	 */
	public TreebolicAIDLBoundClient(final Context context0, final String service0, final IConnectionListener connectionListener0,
			final IModelListener modelListener0)
	{
		this.context = context0;
		this.modelListener = modelListener0;
		this.connectionListener = connectionListener0;
		final String[] serviceNameComponents = service0.split("/"); //$NON-NLS-1$
		this.servicePackage = serviceNameComponents[0];
		this.serviceName = serviceNameComponents[1];
		this.receiver = new ResultReceiver(new Handler())
		{
			@SuppressWarnings("synthetic-access")
			@Override
			protected void onReceiveResult(final int resultCode, final Bundle resultData)
			{
				resultData.setClassLoader(ParcelableModel.class.getClassLoader());

				// scheme
				final String urlScheme = resultData.getString(ITreebolicService.RESULT_URLSCHEME);

				// model
				final boolean isSerialized = resultData.getBoolean(ITreebolicService.RESULT_SERIALIZED);
				Model model;
				if (isSerialized)
				{
					model = (Model) resultData.getSerializable(ITreebolicService.RESULT_MODEL);
				}
				else
				{
					Parcelable parcelable = resultData.getParcelable(ITreebolicService.RESULT_MODEL);
					if (!ParcelableModel.class.equals(parcelable.getClass()))
					{
						Log.d(TreebolicAIDLBoundClient.TAG, "Parcel/Unparcel from source classloader " + parcelable.getClass().getClassLoader() //$NON-NLS-1$
								+ " to target classloader " + ParcelableModel.class.getClassLoader()); //$NON-NLS-1$

						// obtain parcel
						final Parcel parcel = Parcel.obtain();

						// write parcel
						parcel.setDataPosition(0);
						parcelable.writeToParcel(parcel, 0);

						// read parcel
						parcel.setDataPosition(0);
						parcelable = new ParcelableModel(parcel);

						// recycle
						parcel.recycle();
					}
					final ParcelableModel parcelModel = (ParcelableModel) parcelable;
					model = parcelModel.getModel();
				}
				TreebolicAIDLBoundClient.this.modelListener.onModel(resultCode == 0 ? model : null, urlScheme);
			}
		};
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
			Log.d(TreebolicAIDLBoundClient.TAG, "Service disconnected"); //$NON-NLS-1$
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
				Log.d(TreebolicAIDLBoundClient.TAG, "Service connected"); //$NON-NLS-1$
				TreebolicAIDLBoundClient.this.isBound = true;
				TreebolicAIDLBoundClient.this.binder = ITreebolicAIDLService.Stub.asInterface(binder0);

				// signal connected
				TreebolicAIDLBoundClient.this.connectionListener.onConnected(true);
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
				TreebolicAIDLBoundClient.this.binder = null;
			}
		};

		final Intent intent = new Intent();
		intent.setComponent(new ComponentName(this.servicePackage, this.serviceName));
		if (!this.context.bindService(intent, this.connection, Context.BIND_AUTO_CREATE))
		{
			Log.e(TreebolicAIDLBoundClient.TAG, "Service failed to bind " + this.servicePackage + '/' + this.serviceName); //$NON-NLS-1$
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
			try
			{
				this.binder.makeModel(source, base, imageBase, settings, this.receiver);
			}
			catch (final RemoteException e)
			{
				Log.e(TreebolicAIDLBoundClient.TAG, "Service request failed", e); //$NON-NLS-1$
			}
		}
		else
		{
			try
			{
				this.binder.makeAndForwardModel(source, base, imageBase, settings, forward);
			}
			catch (final RemoteException e)
			{
				Log.e(TreebolicAIDLBoundClient.TAG, "Service request failed", e); //$NON-NLS-1$
			}
		}
	}
}
