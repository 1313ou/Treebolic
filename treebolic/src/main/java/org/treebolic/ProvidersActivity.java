/*
 * Copyright (c) 2023. Bernard Bou
 */

package org.treebolic;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

/**
 * Activity listing providers
 *
 * @author Bernard Bou
 */
public class ProvidersActivity extends AppCompatCommonActivity
{
	// Adapter Key - Res id mapping
	private static final String[] from = new String[]{Providers.ICON, Providers.NAME, Providers.PROVIDER, Providers.PACKAGE, Providers.PROCESS, Providers.MIMETYPE, Providers.EXTENSIONS, Providers.URLSCHEME, Providers.ISPLUGIN, Providers.SOURCE, Providers.BASE, Providers.IMAGEBASE, Providers.SETTINGS};
	private static final int[] to = new int[]{R.id.icon, R.id.name, R.id.provider, R.id.pkg, R.id.process, R.id.mimetype, R.id.extension, R.id.urlScheme, R.id.plugin, R.id.source, R.id.base, R.id.imagebase, R.id.settings};

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// layout
		setContentView(R.layout.activity_providers);

		// toolbar
		final Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// set up the action bar
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayOptions(ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
		}

		// adapter
		final BaseAdapter adapter = Providers.makeAdapter(this, R.layout.item_providers, from, to, true);
		final ListView listView = findViewById(R.id.providers);
		listView.setAdapter(adapter);
	}

	@SuppressWarnings("SameReturnValue")
	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		// inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.providers, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull final MenuItem item)
	{
		if (item.getItemId() == R.id.action_rescan)
		{
			final BaseAdapter adapter = Providers.makeAdapter(this, R.layout.item_providers, from, to, true);
			final ListView listView = findViewById(R.id.providers);
			listView.setAdapter(adapter);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
