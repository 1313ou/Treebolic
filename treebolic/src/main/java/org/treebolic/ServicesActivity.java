package org.treebolic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;

/**
 * List of Services
 *
 * @author Bernard Bou
 */
public class ServicesActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_services);

		// create the key-id mapping
		final String[] from = new String[]{Services.DRAWABLE, Services.NAME, Services.PACKAGE, Services.PROCESS, Services.ENABLED, Services.EXPORTED, Services.PERMISSION, Services.FLAGS, Services.LABEL, Services.DESCRIPTION};
		final int[] to = new int[]{R.id.icon, R.id.service, R.id.pkg, R.id.process, R.id.enabled, R.id.exported, R.id.permission, R.id.flags, R.id.label, R.id.description};

		// adapter
		final SimpleAdapter adapter = Services.makeAdapter(this, R.layout.item_services, from, to, true);
		final ListView listView = (ListView) findViewById(R.id.services);
		listView.setAdapter(adapter);
	}
}
