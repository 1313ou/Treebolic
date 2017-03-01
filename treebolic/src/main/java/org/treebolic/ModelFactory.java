package org.treebolic;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import treebolic.IContext;
import treebolic.model.Model;
import treebolic.provider.IProvider;
import treebolic.provider.IProviderContext;

/**
 * Model factory
 *
 * @author Bernard Bou
 */
public class ModelFactory
{
	/**
	 * Log tag
	 */
	static private final String TAG = "Model factory";

	/**
	 * Provider
	 */
	final IProvider provider;

	/**
	 * Provider context
	 */
	final IProviderContext providerContext;

	/**
	 * Context
	 */
	final IContext context;

	/**
	 * Constructor
	 *
	 * @param provider0
	 *            provider
	 * @param providerContext0
	 *            provider context
	 * @param context0
	 *            context to get provider data
	 */
	public ModelFactory(final IProvider provider0, final IProviderContext providerContext0, final IContext context0)
	{
		this.provider = provider0;
		this.providerContext = providerContext0;
		this.context = context0;
	}

	/**
	 * Make model
	 *
	 * @param source
	 *            source
	 * @param base
	 *            base
	 * @param imageBase
	 *            image base
	 * @param settings
	 *            settings
	 * @return model
	 */
	public Model make(final String source, final String base, final String imageBase, final String settings)
	{
		// provider
		this.provider.setup(this.providerContext);
		this.provider.setup(this.context);

		// model
		final Model model = this.provider.makeModel(source, ModelFactory.makeBaseURL(base), ModelFactory.makeParameters(source, base, imageBase, settings));
		Log.d(ModelFactory.TAG, "model=" + model);
		return model;
	}

	/**
	 * Make base URL
	 *
	 * @param base
	 *            base
	 * @return base URL
	 */
	private static URL makeBaseURL(final String base)
	{
		try
		{
			return new URL(base != null && !base.endsWith("/") ? base + "/" : base);
		}
		catch (final MalformedURLException e)
		{
			//
		}
		return null;
	}

	/**
	 * Make parameters
	 *
	 * @param source
	 *            source
	 * @param base
	 *            base
	 * @param imageBase
	 *            image base
	 * @param settings
	 *            settings
	 * @return parameters
	 */
	private static Properties makeParameters(final String source, final String base, final String imageBase, final String settings)
	{
		final Properties theseParameters = new Properties();
		if (source != null)
		{
			theseParameters.setProperty("source", source);
		}
		if (base != null)
		{
			theseParameters.setProperty("base", base);
		}
		if (imageBase != null)
		{
			theseParameters.setProperty("imagebase", imageBase);
		}
		if (settings != null)
		{
			theseParameters.setProperty("settings", settings);
		}

		return theseParameters;
	}
}
