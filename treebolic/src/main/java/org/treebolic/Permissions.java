package org.treebolic;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Permission helper
 *
 * @author <a href="mailto:1313ou@gmail.com">Bernard Bou</a>
 */

class Permissions
{
	static private final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1313;

	@SuppressWarnings("UnusedReturnValue")
	static public boolean check(final Activity activity)
	{
		if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{
			/*
			// Should we show an explanation?
			//noinspection StatementWithEmptyBody
			if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE))
			{

				// Show an explanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.

			}
			else
			*/
			{
				// No explanation needed, we can request the permission.
				// PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an app-defined int constant. The callback method gets the result of the request.
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
				{
					ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
				}
			}
			return false;
		}
		return true;
	}

	/*
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{
		switch (requestCode)
		{
			case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
			{
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
				{
					// Permission was granted. Do the task you need to do.
				}
				else
				{
					// Permission denied. Disable the functionality that depends on this permission.
				}
				break;
			}

			// other 'case' lines to check for other permissions this app might request
		}
	}
	*/
}
