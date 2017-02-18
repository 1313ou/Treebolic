package org.treebolic.clients.iface;

/**
 * Model consumer interface
 *
 * @author Bernard Bou
 */
public interface IConnectionListener
{
	/**
	 * Connected callback
	 *
	 * @param success
	 *            whether connection is successful
	 */
	void onConnected(boolean success);
}
