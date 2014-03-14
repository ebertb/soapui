package com.eviware.soapui.support.components;

import com.eviware.soapui.impl.rest.actions.oauth.BrowserListener;

import java.awt.*;

/**
 * @author joel.jonsson
 */
public interface WebViewBasedBrowserComponent
{
	Component getComponent();

	void navigate( String url );

	void setContent( String contentAsString );

	void setContent( String contentAsString, String contentType );

	void close( boolean cascade );

	void addBrowserStateListener( BrowserListener listener );

	void removeBrowserStateListener( BrowserListener listener );

	void executeJavaScript( String script );
}
