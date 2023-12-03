/*
 * Copyright (c) 2023. Bernard Bou
 */

package org.treebolic;

import android.os.Bundle;
import android.widget.BaseAdapter;
import android.widget.ListView;

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
	private final String[] from = new String[]{Service.DRAWABLE, Service.NAME, Service.PACKAGE, Service.PROCESS, Service.ENABLED, Service.EXPORTED, Service.PERMISSION, Service.FLAGS, Service.LABEL, Service.DESCRIPTION};
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
		final BaseAdapter adapter = Services.makeAdapter(this, R.layout.item_services, from, to);
		final ListView listView = findViewById(R.id.services);
		listView.setAdapter(adapter);

		// view
		//listView.setDivider(AppCompatResources.getDrawable(this, R.drawable.divider));
	}
}
