// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.shared.rpc.project;

import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface DriveServiceAsync {
	
	/**
	 * @see DriveService#afterAutoSave(Set)
	 */
	public void afterAutoSave(Set<Long> projectIds, AsyncCallback<Void> callback);
	
	/**
	 * @see DriveService#importAllFromDrive()
	 */
	public void importAllFromDrive(AsyncCallback<Set<UserProject>> callback);
	
	/**
	 * @see DriveService#getDriveProjectNames()
	 */
	public void getDriveProjectNames(AsyncCallback<Map<String, String>> callback);
	
	/**
	 * @see DriveService#importProjectFromDrive(String)
	 */
	public void importProjectFromDrive(String projectName, 
									   String projectId, 
									   AsyncCallback<UserProject> callback);
	
	/**
	 * @see DriveService#drivePermissionUrl(String)
	 */
	public void drivePermissionUrl(String lastUserLocale, 
								   String baseUrl, 
								   AsyncCallback<String> callback);
	
	/**
	 * @see DriveService#enableDrivePermissionRequests(boolean)
	 */
	public void enableDrivePermissionRequests(boolean value, AsyncCallback<Void> callback);
	
	/**
	 * @see DriveService#enableDrive(boolean)
	 */
	public void enableDrive(boolean value, AsyncCallback<Void> callback);
}
