package org.treebolic;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.NoSuchElementException;

import treebolic.model.Model;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

public class Models
{
	/**
	 * Map of model references
	 */
	static private HashMap<Long, WeakReference<Model>> references = new HashMap<Long, WeakReference<Model>>();

	// T O / F R O M I N T E N T

	static public void set(final Model model, final Intent intent)
	{
		final Long key = Models.set(model);
		intent.putExtra(TreebolicIface.ARG_MODEL_REFERENCE, key);
	}

	@SuppressWarnings("boxing")
	static public Model get(final Intent intent)
	{
		final Long key = intent.getLongExtra(TreebolicIface.ARG_MODEL_REFERENCE, -1);
		return Models.get(key);
	}

	// T O / F R O M B U N D L E

	@SuppressWarnings("boxing")
	static public void set(final Model model, final Bundle bundle)
	{
		final Long key = Models.set(model);
		bundle.putLong(TreebolicIface.ARG_MODEL_REFERENCE, key);
	}

	@SuppressWarnings("boxing")
	static public Model get(final Bundle bundle)
	{
		final Long key = bundle.getLong(TreebolicIface.ARG_MODEL_REFERENCE, -1);
		return Models.get(key);
	}

	// T I M E S T A M P K E Y

	@SuppressWarnings("boxing")
	static public Long set(final Model model)
	{
		final long key = SystemClock.elapsedRealtime();
		Models.set(key, model);
		return key;
	}

	// F R O M K E Y

	static public void set(final Long key, final Model model)
	{
		final WeakReference<Model> reference = new WeakReference<Model>(model);
		Models.references.put(key, reference);
	}

	static public Model getUnguarded(final Long key)
	{
		final WeakReference<Model> reference = Models.references.get(key);
		final Model model = reference.get();
		return model;
	}

	static public Model get(final Long key) throws NoSuchElementException
	{
		final Model model = Models.getUnguarded(key);
		if (model == null)
			throw new NoSuchElementException(key.toString());
		return model;
	}
}
