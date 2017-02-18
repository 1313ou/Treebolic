package org.treebolic.clients.iface;

import treebolic.model.Model;

/**
 * Model consumer interface
 *
 * @author Bernard Bou
 */
public interface IModelListener
{
	/**
	 * Model available callback
	 *
	 * @param model
	 *            model
	 * @param urlScheme
	 *            url scheme
	 */
	void onModel(Model model, String urlScheme);
}
