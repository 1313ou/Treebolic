package org.treebolic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.BaseAdapter;
import android.widget.ListView;

/**
 * Activity listing providers
 *
 * @author Bernard Bou
 */
public class ProvidersActivity extends AppCompatActivity
{

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_providers);

		// create the key-id mapping
		final String[] from = new String[]{Providers.ICON, Providers.NAME, Providers.PROVIDER, Providers.PACKAGE, Providers.PROCESS, Providers.MIMETYPE, Providers.EXTENSIONS, Providers.URLSCHEME, Providers.ISPLUGIN, Providers.SOURCE, Providers.BASE, Providers.IMAGEBASE, Providers.SETTINGS};
		final int[] to = new int[]{R.id.icon, R.id.name, R.id.provider, R.id.pkg, R.id.process, R.id.mimetype, R.id.extension, R.id.urlScheme, R.id.plugin, R.id.source, R.id.base, R.id.imagebase, R.id.settings};

		// adapter
		final BaseAdapter adapter = Providers.makeAdapter(this, R.layout.item_providers, from, to, true);

		// prepare the list providers
		final ListView listView = (ListView) findViewById(R.id.providers);
		listView.setAdapter(adapter);
	}
}
