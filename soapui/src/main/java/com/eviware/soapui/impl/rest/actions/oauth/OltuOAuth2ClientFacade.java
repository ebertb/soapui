/*
 * SoapUI, copyright (C) 2004-2013 smartbear.com
 *
 * SoapUI is free software; you can redistribute it and/or modify it under the
 * terms of version 2.1 of the GNU Lesser General Public License as published by
 * the Free Software Foundation.
 *
 * SoapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.impl.rest.actions.oauth;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.OAuth2Profile;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.rest.support.RestUtils;
import com.eviware.soapui.impl.support.http.HttpRequestInterface;
import com.eviware.soapui.impl.wsdl.support.http.HttpClientSupport;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.types.StringToStringsMap;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.token.OAuthToken;
import org.apache.oltu.oauth2.httpclient4.HttpClient4;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

/**
 * This class implements an OAuth2 three-legged authorization using the third party library Oltu.
 */
public class OltuOAuth2ClientFacade implements OAuth2ClientFacade
{
	public static final String CODE = "code";
	public static final String TITLE = "<TITLE>";
	public static final String OAUTH_2_OOB_URN = "urn:ietf:wg:oauth:2.0:oob";

	UserBrowserFacade browserFacade = new WebViewUserBrowserFacade();

	protected OAuthClient getOAuthClient()
	{
		return new OAuthClient( new HttpClient4( HttpClientSupport.getHttpClient() ) );
	}

	@Override
	public void requestAccessToken( OAuth2Profile profile ) throws OAuth2Exception
	{
		try
		{
			OAuth2Parameters parameters = buildParametersFrom( profile );
			validateProfileContents( parameters );
			String authorizationURL = createAuthorizationURL( parameters );
			launchConsentScreenAndGetAuthorizationCode( authorizationURL, parameters );
		}
		catch( OAuthSystemException e )
		{
			logAndThrowOAuth2Exception( e );
		}
		catch( MalformedURLException e )
		{
			logAndThrowOAuth2Exception( e );
		}
		catch( URISyntaxException e )
		{
			logAndThrowOAuth2Exception( e );
		}

	}

	private OAuth2Parameters buildParametersFrom( OAuth2Profile profile )
	{
		String authorizationUri = expandProperty( profile, profile.getAuthorizationURI() );
		String redirectUri = expandProperty( profile, profile.getRedirectURI() );
		String accessTokenUri = expandProperty( profile, profile.getAccessTokenURI() );
		String clientId = expandProperty( profile, profile.getClientID() );
		String clientSecret = expandProperty( profile, profile.getClientSecret() );
		String scope = expandProperty( profile, profile.getScope() );
		return new OAuth2Parameters( profile, authorizationUri, redirectUri, accessTokenUri, clientId, clientSecret, scope );
	}

	private void validateProfileContents( OAuth2Parameters parameters )
	{

		validateHttpUrl( parameters.authorizationUri, "Authorization URI " );
		validateHttpUrl( parameters.accessTokenUri, "Access token URI" );
		validateRedirectUri( parameters.redirectUri );
		validateRequiredStringValue( parameters.clientId, "Client ID" );
		validateRequiredStringValue( parameters.clientSecret, "Client secret" );
	}

	private void validateRedirectUri( String redirectUri )
	{
		validateRequiredStringValue( redirectUri, "Redirect URI" );
		if( !( isValidUrn( redirectUri ) || isValidHttpUrl( redirectUri ) ) )
		{
			throw new InvalidOAuth2ParametersException( "Redirect URI " + redirectUri + " is not a valid HTTP URL or URN" );
		}
	}

	private boolean isValidUrn( String uriString )
	{
		if( !uriString.startsWith( "urn:" ) )
		{
			return false;
		}
		try
		{
			new URI( uriString );
			return true;
		}
		catch( URISyntaxException e )
		{
			return false;
		}
	}

	private String expandProperty( OAuth2Profile profile, String value )
	{
		return PropertyExpander.expandProperties( profile.getContainer().getProject(), value );
	}

	private void validateRequiredStringValue( String value, String propertyName )
	{
		if( !StringUtils.hasContent( value ) )
		{
			throw new InvalidOAuth2ParametersException( propertyName + " is empty" );
		}
	}

	private void validateHttpUrl( String authorizationUri, String uriName )
	{
		if( !isValidHttpUrl( authorizationUri ) )
		{
			throw new InvalidOAuth2ParametersException( uriName + " " + authorizationUri + " is not a valid HTTP URL" );
		}
	}

	private boolean isValidHttpUrl( String authorizationUri )
	{
		if( !StringUtils.hasContent( authorizationUri ) )
		{
			return false;
		}
		try
		{
			URL url = new URL( authorizationUri );
			return url.getProtocol().startsWith( "http" );
		}
		catch( MalformedURLException e )
		{
			return false;
		}
	}

	private void logAndThrowOAuth2Exception( Exception e ) throws OAuth2Exception
	{
		SoapUI.logError( e, "Failed to create the authorization URL" );
		throw new OAuth2Exception( e );
	}

	private String createAuthorizationURL( OAuth2Parameters profile ) throws OAuthSystemException
	{
		return OAuthClientRequest
				.authorizationLocation( profile.authorizationUri )
				.setClientId( profile.clientId )
				.setResponseType( CODE )
				.setScope( profile.scope )
				.setRedirectURI( profile.redirectUri )
				.buildQueryMessage().getLocationUri();

	}

	private void launchConsentScreenAndGetAuthorizationCode( String authorizationURL, final OAuth2Parameters parameters )
			throws URISyntaxException, MalformedURLException
	{
		browserFacade.addBrowserStateListener( new BrowserStateChangeListener()
		{
			@Override
			public void locationChanged( String newLocation )
			{
				if( isValidHttpUrl( parameters.redirectUri ) )
				{
					getAccessTokenAndSaveToProfile( parameters, extractAuthorizationCode( newLocation ) );
				}
			}

			@Override
			public void contentChanged( String newContent )
			{
				if( !isValidHttpUrl( parameters.redirectUri ) )
				{
					int titlePosition = newContent.indexOf( TITLE );
					if( titlePosition != -1 )
					{
						String title = newContent.substring( titlePosition + TITLE.length(), newContent.indexOf( "</TITLE>" ) );
						getAccessTokenAndSaveToProfile( parameters, extractAuthorizationCode( title ) );
					}
				}
			}

		} );
		parameters.startAccessTokenFlow();
		browserFacade.open( new URI( authorizationURL ).toURL() );
	}

	private String extractAuthorizationCode( String title )
	{
		if( title.contains( "code=" ) )
		{
			return title.substring( title.indexOf( "code=" ) + 5 );
		}
		return null;
	}

	private void getAccessTokenAndSaveToProfile( OAuth2Parameters parameters, String authorizationCode )
	{
		if( authorizationCode != null )
		{
			try
			{
				OAuthClientRequest accessTokenRequest = OAuthClientRequest
						.tokenLocation( parameters.accessTokenUri )
						.setGrantType( GrantType.AUTHORIZATION_CODE )
						.setClientId( parameters.clientId )
						.setClientSecret( parameters.clientSecret )
						.setRedirectURI( parameters.redirectUri )
						.setCode( authorizationCode )
						.buildBodyMessage();
				OAuthToken token = getOAuthClient().accessToken( accessTokenRequest, OAuthJSONAccessTokenResponse.class ).getOAuthToken();
				if( token != null && token.getAccessToken() != null )
				{
					parameters.setAccessTokenInProfile( token.getAccessToken() );
					parameters.setRefreshTokenInProfile( token.getRefreshToken() );
					browserFacade.close();
				}
			}
			catch( OAuthSystemException e )
			{
				SoapUI.logError( e );
			}
			catch( OAuthProblemException e )
			{
				SoapUI.logError( e );
			}
		}
	}

	@Override
	public void applyAccessToken( OAuth2Profile profile, RestRequestInterface request )
	{

		if( StringUtils.isNullOrEmpty( profile.getAccessToken() ) )
		{
			// Since access token is null/empty we don't append it
			StringToStringsMap requestHeaders = request.getRequestHeaders();
			requestHeaders.remove( OAuth.HeaderType.AUTHORIZATION );
			request.setRequestHeaders( requestHeaders );
			return;
		}


		String uri = request.getPath();
		OAuthBearerClientRequest oAuthBearerClientRequest = new OAuthBearerClientRequest( uri ).setAccessToken( profile.getAccessToken() );

		try
		{
			switch( profile.getAccessTokenPosition() )
			{
				case QUERY:
					appendAccessTokenToQuery( request, oAuthBearerClientRequest );
					break;
				case BODY:
					appendAccessTokenToBody( request, oAuthBearerClientRequest );
					break;
				case HEADER:
				default:
					appendAccessTokenToHeader( request, oAuthBearerClientRequest );
					break;
			}
		}
		catch( OAuthSystemException e )
		{
			SoapUI.logError( e );
		}
	}

	private void appendAccessTokenToBody( RestRequestInterface request, OAuthBearerClientRequest oAuthBearerClientRequest ) throws OAuthSystemException
	{
		String bodyWithAccessToken = oAuthBearerClientRequest.buildBodyMessage().getBody();
		request.setRequestContent( request.getRequestContent() + bodyWithAccessToken );
	}

	private void appendAccessTokenToQuery( RestRequestInterface request, OAuthBearerClientRequest oAuthBearerClientRequest ) throws OAuthSystemException
	{
		String uriWithAccessToken = oAuthBearerClientRequest.buildQueryMessage().getLocationUri();
		String queryString = uriWithAccessToken.split( "\\?" )[1];
		RestUtils.extractParamsFromQueryString( request.getParams(), queryString );
	}

	private void appendAccessTokenToHeader( HttpRequestInterface request, OAuthBearerClientRequest oAuthBearerClientRequest ) throws OAuthSystemException
	{
		OAuthClientRequest oAuthClientRequest = oAuthBearerClientRequest.buildHeaderMessage();

		Map<String, String> oAuthHeaders = oAuthClientRequest.getHeaders();
		StringToStringsMap requestHeaders = request.getRequestHeaders();
		// replacing the old value since we no more care about it
		requestHeaders.remove( OAuth.HeaderType.AUTHORIZATION );
		requestHeaders.add( OAuth.HeaderType.AUTHORIZATION, oAuthHeaders.get( OAuth.HeaderType.AUTHORIZATION ) );
		request.setRequestHeaders( requestHeaders );
	}

	@Override
	public void refreshAccessToken( OAuth2Profile profile ) throws Exception
	{
		String refreshToken = profile.getRefreshToken();
		validateRequiredStringValue( refreshToken, "refresh token" );
		OAuth2Parameters parameters = buildParametersFrom( profile );
		validateRequiredStringValue( parameters.clientId, "client ID" );
		validateRequiredStringValue( parameters.clientSecret, "client secret" );
		OAuthClientRequest accessTokenRequest = OAuthClientRequest
				.tokenLocation( parameters.accessTokenUri )
				.setGrantType( GrantType.REFRESH_TOKEN )
				.setClientId( parameters.clientId )
				.setClientSecret( parameters.clientSecret )
				.setRefreshToken( refreshToken )
				.buildBodyMessage();

		OAuthClient oAuthClient = getOAuthClient();

		OAuthToken oAuthToken = oAuthClient.accessToken( accessTokenRequest, OAuthJSONAccessTokenResponse.class ).getOAuthToken();
		profile.applyRetrievedAccessToken( oAuthToken.getAccessToken() );
	}
}
