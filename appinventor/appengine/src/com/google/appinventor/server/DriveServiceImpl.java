// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.api.services.drive.Drive;
import com.google.appinventor.server.storage.StorageIo;
import com.google.appinventor.server.storage.StorageIoInstanceHolder;
import com.google.appinventor.shared.rpc.project.UserProject;
import com.google.appinventor.shared.rpc.project.DriveService;

/**
 * Implementation of the Drive service methods in DriveService.java.
 * 
 * @author obada@mit.edu (Obada Alkhatib)
 */
public class DriveServiceImpl extends OdeRemoteServiceServlet implements DriveService {
	
	private static final StorageIo storageIo = StorageIoInstanceHolder.INSTANCE;
	
	// -TODO change return type to reflect successful uploads
	@Override
	public void afterAutoSave(Set<Long> projectIds) {
		final String userId = userInfoProvider.getUserId();
		final Drive drive;
		try {
			drive = GoogleUtils.makeDrive(userId);
		} catch (IOException ioe) {
			System.out.println("failed to make Drive object");
			ioe.printStackTrace();
			return;
		}
		
		for (long projectId : projectIds) {
			DriveUtils.updateOrUploadProjectSourceToFolder(drive, 
														   storageIo.getUserProject(userId, projectId)
														   			.getProjectName(), 
														   userId);
		}
	}

	@Override
	public Set<UserProject> importAllFromDrive() {
		final String userId = userInfoProvider.getUserId();
		final Drive drive;
		try {
			drive = GoogleUtils.makeDrive(userId);
		} catch (IOException ioe) {
			System.out.println("failed to make Drive object");
			ioe.printStackTrace();
			return new HashSet<>();
		}
		
		return DriveUtils.importAllFromGoogleDrive(drive, userId);
	}
	
	@Override
	public Map<String, String> getDriveProjectNames() {
		final String userId = userInfoProvider.getUserId();
		final Drive drive;
		try {
			drive = GoogleUtils.makeDrive(userId);
		} catch (IOException ioe) {
			System.out.println("failed to make Drive object");
			ioe.printStackTrace();
			return new HashMap<String, String>();
		}
		
		return DriveUtils.getProjectsInDriveFolder(drive, true);
	}
	
	@Override
	public UserProject importProjectFromDrive(String projectName, String projectId) {
		final String userId = userInfoProvider.getUserId();
		final Drive drive;
		try {
			drive = GoogleUtils.makeDrive(userId);
		} catch (IOException ioe) {
			System.out.println("failed to make Drive object");
			ioe.printStackTrace();
			return null; 
		}
		final String projectNameNoExtension = (projectName.endsWith(".aia")) ? 
							projectName.substring(0, projectName.length() - 4) : projectName;
		return DriveUtils.importProjectFromDrive(drive, userId, projectNameNoExtension, projectId);
	}
	
	@Override
	public String drivePermissionUrl(String lastUserLocale, String baseUrl) {
		final String userId = userInfoProvider.getUserId();
		if (GoogleUtils.hasCredential(userId)) {
			storageIo.enableUserDrive(userId, true);
			userInfoProvider.enableDrive(true);
			storageIo.enableUserDrivePermissionRequests(userId, false);
		    userInfoProvider.enableDrivePermissionRequests(false);
			return "";
		}
		// no valid credentials found, so get permission url
		String locale = (lastUserLocale == null) ? "en" : lastUserLocale;
		return GoogleUtils.getAuthorizationUrl(
				baseUrl + "/credential?uri=/?locale=" + locale, 
				userInfoProvider.getUserEmail());
	}
	
	@Override
	public void enableDrivePermissionRequests(boolean value) {
		storageIo.enableUserDrivePermissionRequests(userInfoProvider.getUserId(), value);
		userInfoProvider.enableDrivePermissionRequests(value);
	}
	
	@Override
	public void enableDrive(boolean value) {
		final String userId = userInfoProvider.getUserId();
		storageIo.enableUserDrive(userId, value);
		userInfoProvider.enableDrive(value);
		if (value) {
		  storageIo.enableUserDrivePermissionRequests(userId, false);
	      userInfoProvider.enableDrivePermissionRequests(false);
		}
	}
}
