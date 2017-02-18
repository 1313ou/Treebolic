package org.treebolic.clients;

import org.treebolic.ParcelableModel;
import org.treebolic.clients.iface.IConnectionListener;
import org.treebolic.clients.iface.IModelListener;
import org.treebolic.services.iface.ITreebolicService;

import treebolic.model.Model;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

/**
 * Treebolic intent service client
 *
 * @author Bernard Bou
 */
public class TreebolicIntentClient implements org.treebolic.clients.iface.ITreebolicClient
{
	/**
	 * Log tag
	 */
	static private final String TAG = "Treebolic Intent Client"; //$NON-NLS-1$

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
	 *            connection listener
	 * @param modelListener0
	 *            model listener
	 */
	public TreebolicIntentClient(final Context context0, final String service0, final IConnectionListener connectionListener0,
			final IModelListener modelListener0)
	{
		this.context = context0;
		this.connectionListener = connectionListener0;
		this.modelListener = modelListener0;
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
						Log.d(TreebolicIntentClient.TAG, "Parcel/Unparcel from source classloader " + parcelable.getClass().getClassLoader() //$NON-NLS-1$
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
				TreebolicIntentClient.this.modelListener.onModel(resultCode == 0 ? model : null, urlScheme);
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
		Log.d(TreebolicIntentClient.TAG, "Service connected"); //$NON-NLS-1$

		// signal connected immediately
		this.connectionListener.onConnected(true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.treebolic.clients.iface.ITreebolicClient#disconnect()
	 */
	@Override
	public void disconnect()
	{
		Log.d(TreebolicIntentClient.TAG, "Service disconnected"); //$NON-NLS-1$
		// Toast.makeText(this.context, R.string.disconnected, Toast.LENGTH_SHORT).show();
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
		final Intent intent = new Intent();
		intent.setComponent(new ComponentName(this.servicePackage, this.serviceName));
		intent.setAction(ITreebolicService.ACTION_MAKEMODEL);
		intent.putExtra(ITreebolicService.EXTRA_SOURCE, source);
		intent.putExtra(ITreebolicService.EXTRA_BASE, base);
		intent.putExtra(ITreebolicService.EXTRA_IMAGEBASE, imageBase);
		intent.putExtra(ITreebolicService.EXTRA_SETTINGS, settings);
		intent.putExtra(ITreebolicService.EXTRA_RECEIVER, this.receiver);
		intent.putExtra(ITreebolicService.EXTRA_FORWARD_RESULT_TO, forward);
		if (this.context.startService(intent) == null)
		{
			Log.e(TreebolicIntentClient.TAG, "Intent service failed to start " + this.servicePackage + '/' + this.serviceName); //$NON-NLS-1$
			Toast.makeText(this.context, R.string.fail_start, Toast.LENGTH_LONG).show();
		}
	}
}
