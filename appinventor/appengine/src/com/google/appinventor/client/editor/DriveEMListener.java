// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.gwt.user.client.Timer;

/**
 * Listener class that makes frequent Drive backup calls upon request from the 
 * Editor Manager object.
 * 
 * @author obada@mit.edu (Obada Alkhatib)
 *
 */
public class DriveEMListener implements EditorManagerListener {
	
	// time to wait until the last changes to a specific project to make 
	// a new drive backup call to save the most recent changes
	private static final int AUTO_SAVE_TIMEOUT = 100000;
	
	// time to wait after user goes idle for a specific project to force 
	// save the most recent changes to the project
	private static final int FORCED_AUTO_SAVE_TIMEOUT = 150000;
	
	// set of projects with recent enough changes to be scheduled for forced 
	// saving unless they get saved to Drive in handleEMListener
	private final Set<Long> dirtyProjectsForcedSave;
	
	// maps each project to the last time in millis at which it was saved to 
	// Drive
	private final Map<Long, Long> lastBackup;
	
	// timer for forced auto-saves
	private final Timer autoSaveTimer;
	
	public DriveEMListener() {
		
		lastBackup = new HashMap<>();
		dirtyProjectsForcedSave = new HashSet<>();
		
		autoSaveTimer = new Timer() {
			@Override
			public void run() {
				if (dirtyProjectsForcedSave.size() == 0) {
					return;
				}
				// when the timer goes off, send a Drive backup request through 
				// DriveService to save all the dirty projects
				final Ode ode = Ode.getInstance();
				ode.lockScreens(true);
				ode.getDriveService().afterAutoSave(dirtyProjectsForcedSave, 
						new OdeAsyncCallback<Void>() {
							@Override
							public void onSuccess(Void nothing) {
								for (long projectId : dirtyProjectsForcedSave) {
									lastBackup.put(projectId, System.currentTimeMillis());
								}
								dirtyProjectsForcedSave.clear();
							}
				});
				// unlock screens either way (success or failure)
				ode.lockScreens(false);
			}
		};
	}
	
	@Override
	public void handleEMSaveEvent(Set<Long> dirtyProjectIds) {
		// filter projects with last changes made more than AUTO_SAVE_TIMEOUT 
		// milliseconds in the past
		final Set<Long> toBeSaved = new HashSet<>();
		
		final Ode ode = Ode.getInstance();
		
		for (long projectId : dirtyProjectIds) {
			if (lastBackup.containsKey(projectId)) {
				if (System.currentTimeMillis() - lastBackup.get(projectId) <= AUTO_SAVE_TIMEOUT) {
					if (dirtyProjectsForcedSave.size() == 0) {
						autoSaveTimer.schedule(FORCED_AUTO_SAVE_TIMEOUT);
					}
					dirtyProjectsForcedSave.add(projectId);
					continue;
				}
			}
			toBeSaved.add(projectId);
			dirtyProjectsForcedSave.remove(projectId);
		}
		
		ode.lockScreens(true);
		// save the projects with old enough save times
		ode.getDriveService().afterAutoSave(toBeSaved, 
				new OdeAsyncCallback<Void>() {
					@Override
					public void onSuccess(Void nothing) {
						for (long projectId : toBeSaved) {
							lastBackup.put(projectId, System.currentTimeMillis());
						}
					}
		});
		// unlock screens either way (success or failure)
		ode.lockScreens(false);
	}

}
