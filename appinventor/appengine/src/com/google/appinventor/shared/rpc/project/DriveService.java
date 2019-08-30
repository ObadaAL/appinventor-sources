// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.shared.rpc.project;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.Map;
import java.util.Set;

import com.google.appinventor.shared.rpc.ServerLayout;

/**
 * Methods for syncing and importing projects from Google Drive, as well as managing 
 * permissions and Drive access.
 * 
 * @author obada@mit.edu (Obada Alkhatib)
 */
@RemoteServiceRelativePath(ServerLayout.DRIVE_SERVICE)
public interface DriveService extends RemoteService {
	
	/**
	 * Method to be called after project changes have been successfully auto-saved 
	 * to the database.
	 * 
	 * @param projectIds set of project IDs for projects to be saved to Drive
	 */
	public void afterAutoSave(Set<Long> projectIds);
	
	/**
	 * Method to be called to import all .aia source files from the user's Drive to 
	 * their App Inventor list of projects.
	 * 
	 * @return set of user project objects containing information about the projects 
	 * 		   that were successfully imported from Drive
	 */
	public Set<UserProject> importAllFromDrive();
	
	/**
	 * Method for returning a map of the names of the projects in the user's Drive backup 
	 * folder to their IDs with .aia extensions. 
	 * 
	 * @return map of project names in Drive to their IDs
	 */
	public Map<String, String> getDriveProjectNames();
	
	/**
	 * Method to be called to import a single .aia project source file from user's 
	 * Drive to their App Inventor list of projects. 
	 * 
	 * @param projectName project name
	 * @param projectId project ID in Drive
	 * @return information about that project that was successfully imported, or null 
	 * 		   if project could not be imported
	 */
	public UserProject importProjectFromDrive(String projectName, String projectId);
	
	/**
	 * Returns a URL for granting permission to access Google Drive if the user does not 
	 * have valid tokens. Otherwise, returns an empty string "".
	 * 
	 * @param lastUserLocale locale before redirection
	 * @param baseUrl base URL
	 * @return Drive access permission URL
	 */
	public String drivePermissionUrl(String lastUserLocale, String baseUrl);
	
	/**
	 * Sets user's drivePermissionRequests parameter to true or false in the database and 
	 * local user object.
	 */
	public void enableDrivePermissionRequests(boolean value);
	
	/**
	 * Enables or disables Google Drive backup.
	 */
	public void enableDrive(boolean value);
}
