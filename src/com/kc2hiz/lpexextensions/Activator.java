package com.kc2hiz.lpexextensions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin 
	implements com.ibm.lpex.alef.LpexPreload {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.kc2hiz.lpexextensions"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.lpex.alef.LpexPreload#preload()
	 * This is to hook into Lpex
	 */
	
	/**
	 * @since 1.1
	 */
	public void preload() {
		return;
	}

	// part of 'property'
	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @since 1.1
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
}
