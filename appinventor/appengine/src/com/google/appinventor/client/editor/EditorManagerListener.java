// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor;

import java.util.Set;

/**
 * Interface for EditorManager listeners that do things upon dirty project 
 * saves.
 * 
 * @author obada@mit.edu (Obada Alkhatib)
 *
 */
public interface EditorManagerListener {
	
	/**
	 * Handles save events by the Editor Manager. 
	 * 
	 * @param dirtyProjectIds IDs of dirty projects
	 */
	void handleEMSaveEvent(Set<Long> dirtyProjectIds);
}
