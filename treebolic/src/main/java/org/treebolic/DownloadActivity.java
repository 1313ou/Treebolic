package org.treebolic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.treebolic.download.Deploy;
import org.treebolic.storage.Storage;

import android.os.Bundle;
import android.widget.Toast;

/**
 * Dot download activity
 *
 * @author Bernard Bou
 */
public class DownloadActivity extends org.treebolic.download.DownloadActivity
{
	/*
	 * (non-Javadoc)
	 *
	 * @see org.treebolic.download.DownloadActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.destDir = Storage.getTreebolicStorage(this);
		this.downloadUrl = Settings.getStringPref(this, Settings.PREF_DOWNLOAD);
		if (this.downloadUrl == null || this.downloadUrl.isEmpty())
		{
			Toast.makeText(this, R.string.error_null_download_url, Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.treebolic.download.DownloadActivity#start()
	 */
	@Override
	public void start()
	{
		start(R.string.treebolic);
	}

	// P O S T P R O C E S S I N G

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.treebolic.download.DownloadActivity#doProcessing()
	 */
	@Override
	protected boolean doProcessing()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.treebolic.download.DownloadActivity#process(java.io.InputStream)
	 */
	@Override
	protected boolean process(final InputStream inputStream) throws IOException
	{
		if(this.expandArchive)
		{
			Deploy.expand(inputStream, Storage.getTreebolicStorage(this), false);
			return true;
		}
		Deploy.copy(inputStream, new File(Storage.getTreebolicStorage(this), this.destUri.getLastPathSegment()));
		return true;
	}
}
