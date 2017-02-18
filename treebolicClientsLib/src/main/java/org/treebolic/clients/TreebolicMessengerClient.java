package org.treebolic.clients;

import org.treebolic.ParcelableModel;
import org.treebolic.clients.iface.IConnectionListener;
import org.treebolic.clients.iface.IModelListener;
import org.treebolic.clients.iface.ITreebolicClient;
import org.treebolic.services.iface.ITreebolicService;

import treebolic.model.Model;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

/**
 * Treebolic messenger bound client
 *
 * @author Bernard Bou
 */
public class TreebolicMessengerClient implements ITreebolicClient
{
	/**
	 * Log tag
	 */
	static private final String TAG = "Treebolic Messenger Bound Client"; //$NON-NLS-1$

	/**
	 * Handler of incoming messages (results) from service
	 */
	static class IncomingHandler extends Handler
	{
		/**
		 * Client
		 */
		private final TreebolicMessengerClient client;

		/**
		 * Constructor
		 *
		 * @param client0
		 *            client
		 */
		public IncomingHandler(final TreebolicMessengerClient client0)
		{
			super();
			this.client = client0;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@SuppressWarnings("synthetic-access")
		@Override
		public void handleMessage(final Message msg)
		{
			switch (msg.what)
			{
			case ITreebolicService.MSG_REQUEST_MODEL:
				final Bundle resultData = msg.getData();
				resultData.setClassLoader(ParcelableModel.class.getClassLoader());
				final String urlScheme = resultData.getString(ITreebolicService.RESULT_URLSCHEME);
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
						Log.d(TreebolicMessengerClient.TAG, "Parcel/Unparcel from source classloader " + parcelable.getClass().getClassLoader() //$NON-NLS-1$
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
				this.client.modelListener.onModel(model, urlScheme);
				break;

			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Context
	 */
	private final Context context;

	/**
	 * Service package
	 */
	protected final String servicePackage;

	/**
	 * Service name
	 */
	protected final String serviceName;

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
	 * Bind status
	 */
	private boolean isBound = false;

	/**
	 * Messenger returned by service when binding
	 */
	private Messenger service;

	/**
	 * Messenger used to receive data from service
	 */
	private Messenger inMessenger;

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
	public TreebolicMessengerClient(final Context context0, final String service0, final IConnectionListener connectionListener0,
			final IModelListener modelListener0)
	{
		this.context = context0;
		this.connectionListener = connectionListener0;
		this.modelListener = modelListener0;
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
			Log.d(TreebolicMessengerClient.TAG, "Service disconnected"); //$NON-NLS-1$
			// Toast.makeText(this.context, R.string.disconnected, Toast.LENGTH_SHORT).show();

			// if we have received the service, and hence registered with it
			if (this.service != null)
			{
				try
				{
					final Message msg = Message.obtain(null, ITreebolicService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = this.inMessenger;
					this.service.send(msg);
				}
				catch (final RemoteException e)
				{
					// there is nothing special we need to do if the service has crashed.
				}
			}

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
		// prepare connection
		this.inMessenger = new Messenger(new IncomingHandler(this));
		this.connection = new ServiceConnection()
		{
			@SuppressWarnings("synthetic-access")
			@Override
			public void onServiceConnected(final ComponentName name, final IBinder binder0)
			{
				Log.d(TreebolicMessengerClient.TAG, "Service bound"); //$NON-NLS-1$
				TreebolicMessengerClient.this.isBound = true;

				// pass service in-messenger to post results to
				TreebolicMessengerClient.this.service = new Messenger(binder0);
				final Message msg = Message.obtain(null, ITreebolicService.MSG_REGISTER_CLIENT);
				msg.replyTo = TreebolicMessengerClient.this.inMessenger;
				try
				{
					TreebolicMessengerClient.this.service.send(msg);
				}
				catch (final RemoteException e)
				{
					Log.e(TreebolicMessengerClient.TAG, "Send error", e); //$NON-NLS-1$
				}

				// signal connected
				TreebolicMessengerClient.this.connectionListener.onConnected(true);
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
				TreebolicMessengerClient.this.service = null;
			}
		};

		// bind
		final Intent intent = new Intent();
		intent.setComponent(new ComponentName(this.servicePackage, this.serviceName));
		if (!this.context.bindService(intent, this.connection, Context.BIND_AUTO_CREATE))
		{
			Log.e(TreebolicMessengerClient.TAG, "Service failed to bind"); //$NON-NLS-1$
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
		// bundle
		final Bundle bundle = new Bundle();
		bundle.putString(ITreebolicService.EXTRA_SOURCE, source);
		bundle.putString(ITreebolicService.EXTRA_BASE, base);
		bundle.putString(ITreebolicService.EXTRA_IMAGEBASE, imageBase);
		bundle.putString(ITreebolicService.EXTRA_SETTINGS, settings);
		bundle.putParcelable(ITreebolicService.EXTRA_FORWARD_RESULT_TO, forward);

		// request message
		final Message msg = Message.obtain(null, ITreebolicService.MSG_REQUEST_MODEL, 0, 0);

		// attach bundle
		msg.setData(bundle);

		// send message
		try
		{
			TreebolicMessengerClient.this.service.send(msg);
		}
		catch (final RemoteException e)
		{
			Log.e(TreebolicMessengerClient.TAG, "Send error", e); //$NON-NLS-1$
		}
	}
}
