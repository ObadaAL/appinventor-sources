// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.appinventor.server.storage.StorageIo;
import com.google.appinventor.server.storage.StorageIoInstanceHolder;
import com.google.appinventor.shared.rpc.project.ProjectSourceZip;
import com.google.appinventor.shared.rpc.project.RawFile;
import com.google.appinventor.shared.rpc.project.UserProject;

/**
 * Provides methods to handle Google Drive backup operations. 
 * 
 * @author obada@mit.edu (Obada Alkhatib)
 *
 */
public class DriveUtils {
	// Drive backup destination for App Inventor projects
	private static final String DRIVE_AI_FOLDER = "appinventor-backup";
	private static final String MIME_TYPE = "application/zip";
	
	/**
	 * Searches for the App Inventor backup folder in drive and returns its ID.
	 * 
	 * @param drive Drive object associated with some user
	 * @return App Inventor backup folder ID
	 */
	public static String getDriveFolderId(Drive drive) {
		try {
			// Get a list of all non-trashed folders in the user's Drive
			Drive.Files.List request = drive.files().list().setQ(
	                "mimeType='application/vnd.google-apps.folder' and trashed=false");
	        FileList folders = request.execute();
	        
	        for (File folder : folders.getFiles()) {
	        	if (folder.getName().equals(DRIVE_AI_FOLDER)) {
	        		return folder.getId();
	        	}
	        }
            
		} catch (Exception e) {
			System.out.println("failed to find folder");
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Creates a backup Drive folder for App Inventor project sources and returns its 
	 * ID. If backup folder already exists, it just returns its ID. Returns null if 
	 * operation fails.
	 * 
	 * @param drive Drive object
	 * @return backup folder ID iff operation succeeds, null otherwise
	 */
	public static String makeDriveBackupFolder(Drive drive) {
		// checks if folder already exists
		String folderId = getDriveFolderId(drive);
		if (folderId != null) {
			return folderId;
		}
		// create new folder and return its ID
		File fileMetadata = new File();
        fileMetadata.setName(DRIVE_AI_FOLDER);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        
        try {
        	return drive.files().create(fileMetadata).setFields("id").execute().getId();
        } catch (IOException ioe) {
        	System.out.println("failed to create folder in Drive");
        	ioe.printStackTrace();
        	return null;
        }
	}
	
	/**
	 * Returns a map of all project source file names in Google Drive backup folder 
	 * to their IDs. Creates a drive backup folder if it doesn't already exist. Returns 
	 * null if IOException is thrown.
	 * 
	 * @param drive Drive object
	 * @param withExtension whether to show the file name's extension or not
	 * @return map of project source file names to their IDs in Google Drive backup 
	 * 		   folder, null if exception thrown
	 */
	public static Map<String, String> getProjectsInDriveFolder(Drive drive, boolean withExtension) {
		Map<String, String> projectIdsToNames = new HashMap<>();
		String folderId = getDriveFolderId(drive);
		// if folder not found create one and return null
		if (folderId == null) {
			makeDriveBackupFolder(drive);
			return projectIdsToNames;
		} else {
			try {
				Drive.Files.List request = drive.files().list().setQ(
		                "trashed=false").setFields("files(id, name, parents)");
		        FileList files = request.execute();
		        
		        // Loop over all files in Drive and pick files only with the 
		        // correct parent folder
		        for (File file : files.getFiles()) {
		            List<String> parents = file.getParents();
		            if (parents != null && parents.size() == 1 && parents.get(0).equals(folderId)
		            	&& file.getName().endsWith(".aia")) {
		            	final String fileName = file.getName();
		            	// get the name without the ".aia" extension
		            	if (withExtension) {
		            		projectIdsToNames.put(fileName, file.getId());
		            	} else {
		            		projectIdsToNames.put(fileName.substring(0, fileName.length() - 4), file.getId());
		            	}
		            }
		        }
			} catch (Exception e) {
				System.out.println("could not find project source file IDs in Google Drive");
				e.printStackTrace();
			}
			// for debugging
			for (String name : projectIdsToNames.keySet()) {
				System.out.println(name + " : " + projectIdsToNames.get(name));
			}
			return projectIdsToNames;
		}
	}
	
	/**
	 * -TODO some repetition in this method and the method above
	 * 
	 * Searches project source file with name sourceName in App Inventor Drive backup 
	 * folder and returns its ID. Creates backup folder if it doesn't exist and returns null. 
	 * Otherwise, returns null.
	 * 
	 * @param drive Drive object
	 * @param sourceName project source file ID
	 * @return project source file ID iff file found, null otherwise
	 */
	public static String searchProjectSourceInDriveFolder(Drive drive, String sourceName) {
		String folderId = getDriveFolderId(drive);
		// if folder not found create one and return null.
		if (folderId == null) {
			makeDriveBackupFolder(drive);
		} else {
			try {
				Drive.Files.List request = drive.files().list().setQ(
		                "trashed=false").setFields("files(id, name, parents)");
		        FileList files = request.execute();

		        for (File file : files.getFiles()) {
		            List<String> parents = file.getParents();
		            if (file.getName().equals(sourceName + ".aia") && parents != null 
		                && parents.size() == 1 && parents.get(0).equals(folderId)) {
		                return file.getId();
		            }
		        }
			} catch (IOException ioe) {
				System.out.println("could not find project source file ID in Google Drive");
				ioe.printStackTrace();
			}
		}
		return null;
		
	}
	
	/**
	 * Returns byte array contents of a project with name project for userId. Returns null 
	 * if the project does not exist or could not be loaded. Most of the code is adapted 
	 * from the code in DownloadServlet.java.
	 * 
	 * @param project project name
	 * @param userId user ID
	 * @return byte array content of project iff project could be successfully loaded, null 
	 * 		   otherwise
	 */
	private static byte[] getProjectContents(String project, String userId) {
		FileExporter fileExporter = new FileExporterImpl();
		StorageIo storageIo = StorageIoInstanceHolder.INSTANCE;
		String zipName = project + (project.endsWith(".aia") ? "" : ".aia");
		String projectName = project + (project.endsWith(".aia") ? project.substring(0, project.length() - 4) : "");
		long projectId = 0;
		
		for (Long pid: storageIo.getProjects(userId)) {
            if (storageIo.getProjectName(userId, pid).equals(projectName)) {
            	projectId = pid;
            }
		}
		
		if (projectId == 0) {
            // didn't find project by name
            throw new IllegalArgumentException("Can't find a project named " 
                + projectName + " for user id " + userId);
		}
		
		try {
			ProjectSourceZip zipFile = fileExporter.exportProjectSourceZip(userId,
			          projectId, true, true, zipName, true, true, false, false);
			RawFile downloadableFile = zipFile.getRawFile();
			return downloadableFile.getContent();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Either uploads user's project with projectName and contents fileContents to Drive, 
	 * or it updates if there already exists a project with name projectName inside the 
	 * Drive backup folder.
	 * 
	 * @param drive Drive object
	 * @param projectName project name
	 * @param userId user ID
	 * @param fileContents contents of the project to be uploaded or updated
	 */
	public static void updateOrUploadProjectSourceToFolder(Drive drive, 
														   String projectName, 
														   String userId) {
		// find the backup folder or make it
		String folderId = makeDriveBackupFolder(drive);
		// try to find a file with projectName inside the backup folder
		String fileId = searchProjectSourceInDriveFolder(drive, projectName);
		byte[] fileContents = getProjectContents(projectName, userId);

        try {
            File fileMetadata = new File();
            fileMetadata.setName(projectName + (projectName.endsWith(".aia") ? "" : ".aia"));
            ByteArrayContent mediaContent = new ByteArrayContent(MIME_TYPE, fileContents);
            // either create a new project
            if (fileId != null) {
                drive.files().update(fileId, fileMetadata, mediaContent).execute();
                System.out.println("UPDATED!");
            // or update the already existing one
            } else {
                fileMetadata.setParents(Collections.singletonList(folderId));
                File file = drive.files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
                fileId = file.getId();
            }
            System.out.println(fileId);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	/**
	 * Returns a list of the names of the user's project in the database.
	 * 
	 * @param userId user ID
	 * @return list of user projects in DB
	 */
	private static Set<String> userProjectsInDB(String userId) {
		final StorageIo storageIo = StorageIoInstanceHolder.INSTANCE;
		final Set<String> userProjectNames = new HashSet<>();
		final List<Long> allUserProjects = storageIo.getProjects(userId);
		for (long projectId : allUserProjects) {
			userProjectNames.add(storageIo.getProjectName(userId, projectId));
		}
		return userProjectNames;
	}
	
	/**
	 * Import project with projectName and projectId from the user's Drive. 
	 * 
	 * @param drive Drive object
	 * @param userId user ID
	 * @param projectName project name
	 * @param projectId project ID in Drive
	 * @return information object for the imported project iff no exception 
	 * 		   is thrown in the process
	 */
	public static UserProject importProjectFromDrive(Drive drive, 
													 String userId, 
													 String projectName, 
													 String projectId) {
		FileImporter importer = new FileImporterImpl(); 
		// get all local projects so that we do no import a duplicate
		Set<String> userProjectNames = userProjectsInDB(userId);
		if (userProjectNames.contains(projectName)) {
			return null;
		}
		
		final Drive.Files files = drive.files();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			files.get(projectId).executeMediaAndDownloadTo(os);
			InputStream is = new ByteArrayInputStream(os.toByteArray());
			return importer.importProject(userId, projectName, is);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Imports all backed-up projects in google drive to App Inventor.
	 * 
	 * @param drive drive object
	 * @return set of user project objects containing information of the projects 
	 * 		   that were successfully uploaded to Drive
	 */
	public static Set<UserProject> importAllFromGoogleDrive(Drive drive, String userId) {
		Set<UserProject> userProjects = new HashSet<>();
		FileImporter importer = new FileImporterImpl();
		// get all projects in the backup drive folder
		Map<String, String> projectNamesToIds = getProjectsInDriveFolder(drive, false);
		// get all local projects so that we do not import any duplicates
		Set<String> userProjectsNames = userProjectsInDB(userId);
		
		final Drive.Files files = drive.files();
		for (String projectName : projectNamesToIds.keySet()) {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			final String fileId = projectNamesToIds.get(projectName);
			try {
				// Only download and store in the database if there is no project with the 
				// same name
				if (! userProjectsNames.contains(projectName)) {
					files.get(fileId).executeMediaAndDownloadTo(os);
					InputStream is = new ByteArrayInputStream(os.toByteArray());
					userProjects.add(importer.importProject(userId, projectName, is));
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("could not import file with id: " + fileId);
			}
		}
		return userProjects;
	}
	
	/**
	 * Private constructor for non-instantiability. 
	 */
	private DriveUtils() {
	}
}