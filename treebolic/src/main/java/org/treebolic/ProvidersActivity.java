package org.treebolic;

import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
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

		// layout
		setContentView(R.layout.activity_providers);

		// toolbar
		final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// set up the action bar
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayOptions(ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
		}

		// create the key-id mapping
		final String[] from = new String[]{Providers.ICON, Providers.NAME, Providers.PROVIDER, Providers.PACKAGE, Providers.PROCESS, Providers.MIMETYPE, Providers.EXTENSIONS, Providers.URLSCHEME, Providers.ISPLUGIN, Providers.SOURCE, Providers.BASE, Providers.IMAGEBASE, Providers.SETTINGS};
		final int[] to = new int[]{R.id.icon, R.id.name, R.id.provider, R.id.pkg, R.id.process, R.id.mimetype, R.id.extension, R.id.urlScheme, R.id.plugin, R.id.source, R.id.base, R.id.imagebase, R.id.settings};

		// adapter
		final BaseAdapter adapter = Providers.makeAdapter(this, R.layout.item_providers, from, to, true);

		// prepare the list providers
		final ListView listView = (ListView) findViewById(R.id.providers);
		listView.setAdapter(adapter);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(this);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
