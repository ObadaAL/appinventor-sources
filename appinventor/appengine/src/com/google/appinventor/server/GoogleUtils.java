// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.server;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import com.google.api.client.extensions.appengine.datastore.AppEngineDataStoreFactory;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.appinventor.server.flags.Flag;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.json.JsonFactory;

/**
 * Methods to handle Google Drive API access permissions and tokens, as well as 
 * making Drive objects to handle Google Drive backup operations.
 * 
 * @author obada@mit.edu (Obada Alkhatib)
 *
 */
public class GoogleUtils {
	
	private static final String  CLIENT_ID = Flag.createFlag("GOOGLE_CLIENT_ID", "").get();
	private static final String CLIENT_SECRET = Flag.createFlag("GOOGLE_CLIENT_SECRET", "").get();
	private static final String APPLICATION_NAME = Flag.createFlag("GOOGLE_APPLICATION_NAME", "").get();
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final AppEngineDataStoreFactory TOKENS_STORE = AppEngineDataStoreFactory.getDefaultInstance();
	private static final Set<String> SCOPES = Collections.singleton("https://www.googleapis.com/auth/drive");
	private static final HttpTransport HTTP_TRANSPORT = new UrlFetchTransport();
	
	/**
	 * Initializes a new GoogleAuthorizationFlowObject using the shared tokens store and json factory.
	 * 
	 * @return new flow object
	 */
	public static GoogleAuthorizationCodeFlow initializeFlow() {
		GoogleAuthorizationCodeFlow flow;
		try {
			flow = new GoogleAuthorizationCodeFlow.Builder(
	                HTTP_TRANSPORT, JSON_FACTORY, CLIENT_ID, CLIENT_SECRET, SCOPES)
	                .setDataStoreFactory(TOKENS_STORE)
	                .setAccessType("offline")
	                .build();
			return flow;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Checks whether user with userId has a valid access token or not.
	 * 
	 * @param userId user Id
	 * @return true iff user has a valid access token
	 */
	public static boolean hasCredential(String userId) {
		try {
			final Credential credential = initializeFlow().loadCredential(userId);
			if (credential != null && 
				credential.getRefreshToken() != null && 
				! credential.getRefreshToken().equals("") && 
				credential.refreshToken()) {
				// I think this hacky way around checking the validity of the user's credentials 
				// can be re-written, but Google's documentation for the methods did not help
				return true;
			}
			return false;
		} catch (IOException ioe) {
			System.out.println("User possibly revoked access permission to Google Drive");
			ioe.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Builds an Google authorization url to get an authorization code from a user.
	 * 
	 * @param redirectUri 
	 * @param email Google user email
	 * @return authorization url
	 */
	public static String getAuthorizationUrl(String redirectUrl, String email) {
		GoogleAuthorizationCodeFlow flow = initializeFlow();
		return flow.newAuthorizationUrl().setRedirectUri(redirectUrl).build() + 
				"&login_hint=" + email;
	}
	
	/**
	 * Creates a credential for userId using an authorization code.
	 * 
	 * @param userId user Id
	 * @param code authorization code
	 * @param redirectUri redirect URI used for the Google authorization request URI
	 */
	public static void createAndSaveCredential(String userId, String code, String redirectUri) {
		GoogleAuthorizationCodeFlow flow = initializeFlow();
		try {
			TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
			flow.createAndStoreCredential(response, userId);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	/**
	 * Returns stored credential associated with userId. Requires userId has a stored credential.
	 * 
	 * @param userId user Id
	 * @return user credential object
	 * @throws IOException
	 */
	private static Credential getCredential(String userId) throws IOException {
		return initializeFlow().loadCredential(userId);
	}
	
	/**
	 * Makes a Drive object to handle Google Drive backup operations.
	 * 
	 * @param userId user Id
	 * @return Drive object
	 * @throws IOException
	 */
	public static Drive makeDrive(String userId) throws IOException {
		return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredential(userId))
				.setApplicationName(APPLICATION_NAME)
				.build();
	}
	
	/**
	 * private constructor for non-instantiability
	 */
	private GoogleUtils() {
	}
}