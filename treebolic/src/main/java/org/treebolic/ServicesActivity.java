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
 * Activity listing services
 *
 * @author Bernard Bou
 */
public class ServicesActivity extends AppCompatCommonActivity
{
	// Adapter Key - Res id mapping
	private final String[] from = new String[]{Services.DRAWABLE, Services.NAME, Services.PACKAGE, Services.PROCESS, Services.ENABLED, Services.EXPORTED, Services.PERMISSION, Services.FLAGS, Services.LABEL, Services.DESCRIPTION};
	private final int[] to = new int[]{R.id.icon, R.id.service, R.id.pkg, R.id.process, R.id.enabled, R.id.exported, R.id.permission, R.id.flags, R.id.label, R.id.description};

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// layout
		setContentView(R.layout.activity_services);

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
		final BaseAdapter adapter = Services.makeAdapter(this, R.layout.item_services, from, to, true);
		final ListView listView = findViewById(R.id.services);
		listView.setAdapter(adapter);
		//listView.setDivider(AppCompatResources.getDrawable(this, R.drawable.divider));
	}

	@SuppressWarnings("SameReturnValue")
	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		// inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.services, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull final MenuItem item)
	{
		if (item.getItemId() == R.id.action_rescan)
		{
			final BaseAdapter adapter = Services.makeAdapter(this, R.layout.item_services, from, to, true);
			final ListView listView = findViewById(R.id.services);
			listView.setAdapter(adapter);
			//listView.setDivider(AppCompatResources.getDrawable(this, R.drawable.divider));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
