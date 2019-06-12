/*
 * Copyright (c) Treebolic 2019. Bernard Bou <1313ou@gmail.com>
 */

package org.treebolic;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.treebolic.download.Deploy;
import org.treebolic.storage.Storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;

/**
 * Download activity
 *
 * @author Bernard Bou
 */
public class DownloadActivity extends org.treebolic.download.DownloadActivity
{

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		this.expandArchiveCheckbox.setVisibility(View.VISIBLE);

		final String base = Settings.getStringPref(this, Settings.PREF_DOWNLOAD_BASE);
		final String file = Settings.getStringPref(this, Settings.PREF_DOWNLOAD_FILE);
		if (base != null && !base.isEmpty() && file != null && !file.isEmpty())
		{
			this.downloadUrl = base + '/' + file;
		}
		if (this.downloadUrl == null || this.downloadUrl.isEmpty())
		{
			Toast.makeText(this, R.string.error_null_download_url, Toast.LENGTH_SHORT).show();
			finish();
		}
	}


	@Override
	public void start()
	{
		start(R.string.treebolic);
	}

	// P O S T P R O C E S S I N G

	@SuppressWarnings("SameReturnValue")
	@Override
	protected boolean doProcessing()
	{
		return true;
	}

	@SuppressWarnings("SameReturnValue")
	@Override
	protected boolean process(@NonNull final InputStream inputStream) throws IOException
	{
		final File storage = Storage.getTreebolicStorage(this);

		if (this.expandArchive)
		{
			Deploy.expand(inputStream, storage, false);
			return true;
		}

		final File destFile = new File(storage, this.downloadUri.getLastPathSegment());
		Deploy.copy(inputStream, destFile);
		return true;
	}
}
